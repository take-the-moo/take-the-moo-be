package com.hackathon.domain.postcard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathon.domain.photo.service.PhotoService;
import com.hackathon.domain.postcard.entity.Postcard;
import com.hackathon.domain.postcard.entity.PostcardStatus;
import com.hackathon.domain.postcard.entity.PostcardTemplate;
import com.hackathon.domain.postcard.repository.PostcardRepository;
import com.hackathon.domain.postcard.service.PostcardService;
import com.hackathon.domain.user.dto.request.UserCreateRequestDto;
import com.hackathon.domain.user.service.UserService;
import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import com.jayway.jsonpath.JsonPath;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** 목 모드: AI 그림·제목 없이 원본 사진 + 기본 제목으로 엽서가 끝까지 만들어진다 */
@SpringBootTest(properties = {"llm.mock=true", "app.storage.dir=build/test-uploads"})
@AutoConfigureMockMvc
class PostcardControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserService userService;

    @Autowired
    PhotoService photoService;

    @Autowired
    PostcardRepository postcardRepository;

    @Autowired
    PostcardService postcardService;

    String userId;

    @BeforeEach
    void setUp() {
        userId = createUser();
    }

    private String createUser() {
        return userService.create(new UserCreateRequestDto(Nationality.VN, Language.VI)).userId();
    }

    private String uploadPhoto(String owner, String contentType, byte[] bytes) {
        return photoService.upload(owner, null, new MockMultipartFile("file", "photo", contentType, bytes)).photoId();
    }

    private static byte[] png(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(90, 140, 200));
        g.fillRect(0, 0, width, height);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private ResultActions create(String photoId, String comment) throws Exception {
        return mvc.perform(post("/api/postcards").contentType(MediaType.APPLICATION_JSON).content("""
                {"userId": "%s", "photoId": "%s", "comment": "%s"}
                """.formatted(userId, photoId, comment)));
    }

    /** 백그라운드 생성이 끝날 때까지 상태를 확인한다 */
    private String waitFor(String postcardId, String expected) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
        while (true) {
            String body = mvc.perform(get("/api/postcards/{id}", postcardId))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            String status = JsonPath.read(body, "$.data.status");
            if (status.equals(expected) || System.nanoTime() > deadline) {
                assertThat(status).isEqualTo(expected);
                return body;
            }
            Thread.sleep(100);
        }
    }

    @Nested
    class 성공 {

        @Test
        void 사진과_한_문장으로_엽서를_만들고_내려받는다() throws Exception {
            String photoId = uploadPhoto(userId, "image/png", png(800, 600));

            String body = create(photoId, "Lần đầu tiên tôi cảm nhận mùa thu Hàn Quốc trước Độc Lập Môn")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.data.template").value("BASIC"))
                    .andExpect(jsonPath("$.data.imageUrl").doesNotExist())
                    .andReturn().getResponse().getContentAsString();
            String postcardId = JsonPath.read(body, "$.data.postcardId");

            String done = waitFor(postcardId, "COMPLETED");
            assertThat((String) JsonPath.read(done, "$.data.imageUrl")).isEqualTo("/api/postcards/" + postcardId + "/image");
            assertThat((String) JsonPath.read(done, "$.data.downloadUrl")).endsWith("/image?download=true");

            byte[] jpeg = mvc.perform(get("/api/postcards/{id}/image", postcardId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                    .andExpect(header().string("Content-Disposition", startsWith("inline")))
                    .andReturn().getResponse().getContentAsByteArray();
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(jpeg));
            assertThat(image.getWidth()).isEqualTo(1200);
            assertThat(image.getHeight()).isEqualTo(1800);

            mvc.perform(get("/api/postcards/{id}/image", postcardId).param("download", "true"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", startsWith("attachment")));
        }
    }

    @Nested
    class 실패 {

        @Test
        void 남의_사진이면_403() throws Exception {
            String othersPhoto = uploadPhoto(createUser(), "image/png", png(10, 10));

            create(othersPhoto, "안녕")
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("PHOTO_FORBIDDEN"));
        }

        @Test
        void 한_문장이_없으면_422() throws Exception {
            create(uploadPhoto(userId, "image/png", png(10, 10)), " ")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
        }

        @Test
        void 읽을_수_없는_사진이면_FAILED와_사유() throws Exception {
            String broken = uploadPhoto(userId, "image/jpeg", new byte[]{1, 2, 3});

            String body = create(broken, "안녕").andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            String done = waitFor(JsonPath.read(body, "$.data.postcardId"), "FAILED");

            assertThat((String) JsonPath.read(done, "$.data.errorCode")).isEqualTo("RENDER_FAILED");
            assertThat((Object) JsonPath.read(done, "$.data.imageUrl")).isNull();
        }

        @Test
        void 완성_전_이미지는_409() throws Exception {
            String photoId = uploadPhoto(userId, "image/png", png(10, 10));
            Postcard pending = postcardRepository.save(Postcard.create(userId, photoId, "안녕", PostcardTemplate.BASIC));

            mvc.perform(get("/api/postcards/{id}/image", pending.getId()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("POSTCARD_NOT_READY"));
        }

        @Test
        void 없는_엽서는_404() throws Exception {
            mvc.perform(get("/api/postcards/{id}", "no-such-postcard"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("POSTCARD_NOT_FOUND"));
        }
    }

    @Test
    void 재시작하면_만들던_엽서는_INTERRUPTED로_실패한다() {
        String photoId = uploadPhoto(userId, "image/png", new byte[]{1});
        Postcard processing = Postcard.create(userId, photoId, "안녕", PostcardTemplate.BASIC);
        processing.startProcessing();
        postcardRepository.save(processing);

        postcardService.failInterrupted();

        Postcard after = postcardRepository.findById(processing.getId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(PostcardStatus.FAILED);
        assertThat(after.getErrorCode()).isEqualTo("INTERRUPTED");
    }
}
