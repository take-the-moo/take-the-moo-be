package com.hackathon.domain.review.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathon.domain.photo.service.PhotoService;
import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.repository.PlaceRepository;
import com.hackathon.support.TestUsers;
import com.hackathon.domain.user.service.UserService;
import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** 한걸음 사진 첨부: 내 사진만 붙고, 작성 응답·목록에 photoUrl로 나온다 */
@SpringBootTest(properties = {"llm.mock=true", "app.storage.dir=build/test-uploads"})
@AutoConfigureMockMvc
class ReviewPhotoTest {

    private static final AtomicInteger CONTENT_ID = new AtomicInteger(850_000);

    @Autowired
    MockMvc mvc;

    @Autowired
    PlaceRepository placeRepository;

    @Autowired
    UserService userService;

    @Autowired
    PhotoService photoService;

    Long placeId;
    String userId;

    @BeforeEach
    void setUp() {
        Place place = Place.fromTour(String.valueOf(CONTENT_ID.incrementAndGet()));
        place.updateTourInfo("12", "독립문", "서울 서대문구", 37.57, 126.96, null, "20261101000000");
        place.applyRules(null, true, false);
        placeId = placeRepository.save(place).getId();
        userId = createUser();
    }

    private String createUser() {
        return TestUsers.create(userService, Nationality.VN, Language.VI);
    }

    private String uploadPhoto(String owner) {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});
        return photoService.upload(owner, placeId, file).photoId();
    }

    private ResultActions write(String photoId) throws Exception {
        return mvc.perform(post("/api/places/{placeId}/reviews", placeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"userId": "%s", "content": "문 앞에서 찍었어요", "language": "KO", "photoId": "%s"}
                        """.formatted(userId, photoId)));
    }

    @Test
    void 내_사진을_붙이면_작성_응답과_목록에_사진_주소가_나온다() throws Exception {
        String photoId = uploadPhoto(userId);

        write(photoId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.photoUrl").value("/api/photos/" + photoId));

        mvc.perform(get("/api/places/{placeId}/reviews", placeId).param("language", "KO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviews[0].photoUrl").value("/api/photos/" + photoId));
    }

    @Test
    void 사진_없이_쓰면_photoUrl은_null() throws Exception {
        mvc.perform(post("/api/places/{placeId}/reviews", placeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": "%s", "content": "사진 없이 남겨요", "language": "KO"}
                                """.formatted(userId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.photoUrl").doesNotExist());
    }

    @Test
    void 남의_사진이면_403이고_저장하지_않는다() throws Exception {
        String othersPhoto = uploadPhoto(createUser());

        write(othersPhoto)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PHOTO_FORBIDDEN"));

        mvc.perform(get("/api/places/{placeId}/reviews", placeId).param("language", "KO"))
                .andExpect(jsonPath("$.data.reviews").isEmpty());
    }

    @Test
    void 없는_사진이면_404() throws Exception {
        write("no-such-photo")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PHOTO_NOT_FOUND"));
    }
}
