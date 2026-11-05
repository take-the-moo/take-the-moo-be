package com.hackathon.domain.photo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathon.domain.photo.exception.PhotoErrorType;
import com.hackathon.domain.photo.exception.PhotoException;
import com.hackathon.domain.photo.service.PhotoInfo;
import com.hackathon.domain.photo.service.PhotoService;
import com.hackathon.domain.user.service.UserService;
import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import com.hackathon.support.TestUsers;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = {"llm.mock=true", "app.storage.dir=build/test-uploads"})
@AutoConfigureMockMvc
class PhotoControllerTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01, 0x02};

    @Autowired
    MockMvc mvc;

    @Autowired
    PhotoService photoService;

    @Autowired
    UserService userService;

    private String createUser() {
        return TestUsers.create(userService, Nationality.VN, Language.VI);
    }

    private ResultActions upload(MockMultipartFile file, String userId) throws Exception {
        return mvc.perform(multipart("/api/photos").file(file).param("userId", userId));
    }

    private static MockMultipartFile jpeg() {
        return new MockMultipartFile("file", "photo.jpg", "image/jpeg", JPEG);
    }

    @Nested
    class 성공 {

        @Test
        void 올린_사진을_주소로_다시_받는다() throws Exception {
            String userId = createUser();

            String body = upload(jpeg(), userId)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.resultType").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.photoId").isString())
                    .andExpect(jsonPath("$.data.placeId").doesNotExist())
                    .andReturn().getResponse().getContentAsString();
            String photoId = JsonPath.read(body, "$.data.photoId");
            String url = JsonPath.read(body, "$.data.url");
            assertThat(url).isEqualTo("/api/photos/" + photoId);

            mvc.perform(get(url))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                    .andExpect(content().bytes(JPEG))
                    .andExpect(header().string("Cache-Control", containsString("max-age")));
        }

        @Test
        void 내_사진이면_정보를_주고_남의_사진이면_403() throws Exception {
            String owner = createUser();
            String other = createUser();
            String body = upload(jpeg(), owner).andReturn().getResponse().getContentAsString();
            String photoId = JsonPath.read(body, "$.data.photoId");

            PhotoInfo info = photoService.getOwnedPhoto(owner, photoId);
            assertThat(info.userId()).isEqualTo(owner);
            assertThat(info.contentType()).isEqualTo("image/jpeg");

            assertThatThrownBy(() -> photoService.getOwnedPhoto(other, photoId))
                    .isInstanceOf(PhotoException.class)
                    .extracting(e -> ((PhotoException) e).getErrorCode()).isEqualTo(PhotoErrorType.FORBIDDEN);
        }
    }

    @Nested
    class 실패 {

        @Test
        void 허용하지_않는_형식은_422() throws Exception {
            MockMultipartFile heic = new MockMultipartFile("file", "photo.heic", "image/heic", JPEG);

            upload(heic, createUser())
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
        }

        @Test
        void 없는_사용자는_404() throws Exception {
            upload(jpeg(), "no-such-user")
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
        }

        @Test
        void 없는_장소는_404() throws Exception {
            mvc.perform(multipart("/api/photos").file(jpeg()).param("userId", createUser()).param("placeId", "999999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"));
        }

        @Test
        void 없는_사진은_404() throws Exception {
            mvc.perform(get("/api/photos/{photoId}", "no-such-photo"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PHOTO_NOT_FOUND"));
        }
    }
}
