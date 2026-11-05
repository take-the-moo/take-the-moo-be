package com.hackathon.domain.visit.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.hackathon.domain.place.curation.PlaceCuration;
import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.weather.dto.WeatherCondition;
import com.hackathon.global.common.TimeSlot;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 독립문에서 남은 곳이 형무소역사관(실내·낮, 409m)과 영천시장(실외·오후~저녁, 309m)일 때 */
class CourseRecommenderTest {

    private final Place gate = place("127425", 37.5724, 126.9595, false,
            List.of(TimeSlot.MORNING, TimeSlot.AFTERNOON, TimeSlot.EVENING));
    private final Place prison = place("130152", 37.574303895, 126.9555345762, true,
            List.of(TimeSlot.MORNING, TimeSlot.AFTERNOON));
    private final Place market = place("2789102", 37.5703342733, 126.9618475045, false,
            List.of(TimeSlot.AFTERNOON, TimeSlot.EVENING));
    private final List<Place> remaining = List.of(prison, market);

    private Place next(String time, WeatherCondition weather) {
        return CourseRecommender.selectNext(gate, remaining, LocalTime.parse(time), weather);
    }

    @Test
    void 맑은_오후는_둘_다_통과해_가까운_곳() {
        assertThat(next("15:00", WeatherCondition.CLEAR)).isSameAs(market);
    }

    @Test
    void 맑은_오전은_문_여는_곳() {
        assertThat(next("09:00", WeatherCondition.CLEAR)).isSameAs(prison);
    }

    @Test
    void 저녁은_저녁에_좋은_곳() {
        assertThat(next("18:00", WeatherCondition.CLEAR)).isSameAs(market);
    }

    @Test
    void 비_오는_오후는_더_멀어도_실내() {
        assertThat(next("15:00", WeatherCondition.RAINY)).isSameAs(prison);
    }

    @Test
    void 눈이_와도_실내() {
        assertThat(next("15:00", WeatherCondition.SNOWY)).isSameAs(prison);
    }

    @Test
    void 밤에는_시간대_조건을_빼고_가까운_곳() {
        assertThat(next("23:00", WeatherCondition.CLEAR)).isSameAs(market);
    }

    @Test
    void 밤에_비가_오면_실내() {
        assertThat(next("23:00", WeatherCondition.RAINY)).isSameAs(prison);
    }

    @Test
    void 규칙을_통과한_곳이_없으면_전체에서_가까운_곳() {
        // 영천시장은 오전에 안 맞고 실외. 남은 곳이 영천시장뿐이어도 안내한다
        assertThat(CourseRecommender.selectNext(gate, List.of(market), LocalTime.parse("09:00"), WeatherCondition.RAINY))
                .isSameAs(market);
    }

    @Test
    void 남은_거리는_가장_짧은_순서로_계산한다() {
        assertThat(CourseRecommender.shortestRemainingMeters(gate, remaining)).isBetween(1015, 1025);
        assertThat(CourseRecommender.shortestRemainingMeters(gate, List.of())).isZero();
    }

    private static Place place(String contentId, double lat, double lng, boolean indoor, List<TimeSlot> slots) {
        Place place = Place.fromTour(contentId);
        place.updateTourInfo("12", contentId, "서울", lat, lng, null, "20261101000000");
        place.applyRules(new PlaceCuration(contentId, contentId, slots, indoor, null, null), true, false);
        return place;
    }
}
