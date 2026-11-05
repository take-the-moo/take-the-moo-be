package com.hackathon.domain.visit.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hackathon.global.common.Language;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** 실제 data/courses.json이 읽히는지 (형식이 깨지면 서버가 안 뜨므로 테스트에서 먼저 잡는다) */
class CourseRegistryTest {

    @Test
    void 코스_파일을_읽는다() throws Exception {
        CourseRegistry registry = new CourseRegistry(JsonMapper.builder().build());

        assertThat(registry.findAll()).hasSize(14);
        assertThat(registry.findAll()).allSatisfy(course -> {
            assertThat(course.name()).containsKeys(Language.KO, Language.EN, Language.VI, Language.ZH, Language.JA);
            assertThat(course.contentIds()).hasSize(3).doesNotHaveDuplicates();
        });
        assertThat(registry.findByContentId("130152")).extracting(Course::courseId).containsExactly("independence");
        assertThat(registry.findByContentId("127425")).extracting(Course::courseId)
                .containsExactly("independence", "independence-park", "seo-jae-pil");
        assertThat(registry.findByContentId("없는-장소")).isEmpty();
    }

    @Test
    void 번역이_없으면_영어_그다음_한국어로_보여준다() {
        Course both = new Course("a", Map.of(Language.KO, "독립", Language.EN, "Independence"), List.of("1"));
        Course koreanOnly = new Course("b", Map.of(Language.KO, "독립"), List.of("1"));

        assertThat(both.localizedName(Language.KO)).isEqualTo("독립");
        assertThat(both.localizedName(Language.UZ)).isEqualTo("Independence");
        assertThat(koreanOnly.localizedName(Language.UZ)).isEqualTo("독립");
    }

    @Test
    void 잘못된_코스면_서버가_뜨지_않는다() {
        Map<Language, String> korean = Map.of(Language.KO, "독립");

        assertThatThrownBy(() -> new CourseRegistry(List.of(new Course("a", Map.of(Language.EN, "x"), List.of("1")))))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("한국어");
        assertThatThrownBy(() -> new CourseRegistry(List.of(new Course(" ", korean, List.of("1")))))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("courseId");
        assertThatThrownBy(() -> new CourseRegistry(List.of(new Course("a", korean, List.of()))))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("장소");
        assertThatThrownBy(() -> new CourseRegistry(List.of(
                new Course("a", korean, List.of("1")), new Course("a", korean, List.of("2")))))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("중복");
    }
}
