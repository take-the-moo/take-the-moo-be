package com.hackathon.domain.review.sample;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.repository.PlaceRepository;
import com.hackathon.domain.review.repository.ReviewRepository;
import com.hackathon.global.util.SeoulTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 실제 data/sample-reviews.json으로 확인한다. 지금 파일은 장소 5곳이고 독립문(127425)이 4개다.
 * 파일의 장소·개수를 바꾸면 이 테스트의 기대값도 고친다.
 */
@SpringBootTest(properties = "llm.mock=true")
@AutoConfigureMockMvc
class SampleReviewSeederTest {

    private static final String DONGNIMMUN = "127425";
    private static final int SAMPLE_PLACES = 5;
    private static final int DONGNIMMUN_REVIEWS = 4;

    @Autowired
    SampleReviewSeeder seeder;

    @Autowired
    PlaceRepository placeRepository;

    @Autowired
    ReviewRepository reviewRepository;

    @Autowired
    MockMvc mvc;

    @Test
    void 장소가_생기면_한_번만_넣고_며칠_전_날짜로_보인다() throws Exception {
        long before = reviewRepository.count();
        assertThat(seeder.seed()).as("장소가 아직 없다").isEqualTo(SAMPLE_PLACES);
        assertThat(reviewRepository.count()).isEqualTo(before);

        Place place = Place.fromTour(DONGNIMMUN);
        place.updateTourInfo("12", "독립문", "서울 서대문구", 37.57, 126.96, null, "20261101000000");
        place.applyRules(null, true, false);
        Long placeId = placeRepository.save(place).getId();

        assertThat(seeder.seed()).as("독립문만 넣고 나머지는 아직 없다").isEqualTo(SAMPLE_PLACES - 1);
        assertThat(reviewRepository.count()).isEqualTo(before + DONGNIMMUN_REVIEWS);
        assertThat(seeder.seed()).as("다시 불러도 두 번 넣지 않는다").isEqualTo(SAMPLE_PLACES - 1);
        assertThat(reviewRepository.count()).isEqualTo(before + DONGNIMMUN_REVIEWS);

        mvc.perform(get("/api/places/{placeId}/reviews", placeId).param("language", "KO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviews", hasSize(DONGNIMMUN_REVIEWS)))
                .andExpect(jsonPath("$.data.reviews[0].content").value("아침 일찍 오면 사람이 없어서 사진 찍기 좋아요!"))
                .andExpect(jsonPath("$.data.reviews[0].nationality").value("VN"))
                .andExpect(jsonPath("$.data.reviews[0].writtenDate").value(SeoulTime.today().minusDays(2).toString()))
                .andExpect(jsonPath("$.data.reviews[0].sample").value(true))
                .andExpect(jsonPath("$.data.reviews[1].nationality").value("JP"))
                .andExpect(jsonPath("$.data.reviews[1].language").value("JA"))
                .andExpect(jsonPath("$.data.reviews[2].writtenDate").value(SeoulTime.today().minusDays(9).toString()));
    }
}
