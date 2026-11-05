package com.hackathon.domain.visit.course;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hackathon.global.common.Language;
import java.util.List;
import java.util.Map;

/**
 * 코스 한 건 (data/courses.json). contentIds는 추천 순서이고, 방문 순서를 강제하지 않는다.
 * placeNames는 사람이 알아보기 위한 참고용이라 읽지 않는다. 규칙은 docs/db.md 참고.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Course(
        String courseId,
        Map<Language, String> name,
        List<String> contentIds
) {

    /** 사용자 언어 → 영어 → 한국어 순서로 고른다 */
    public String localizedName(Language language) {
        String localized = name.get(language);
        if (localized != null) return localized;
        return name.getOrDefault(Language.EN, name.get(Language.KO));
    }
}
