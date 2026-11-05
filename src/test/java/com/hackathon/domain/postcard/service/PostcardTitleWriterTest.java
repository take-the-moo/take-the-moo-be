package com.hackathon.domain.postcard.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.hackathon.global.common.Language;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PostcardTitleWriterTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 7);

    @Test
    void 기본_제목은_언어별_날짜와_한국어_장소다() {
        assertThat(PostcardTitleWriter.fallback(DATE, "독립문", Language.KO)).isEqualTo("2026년 11월 7일 · 독립문");
        assertThat(PostcardTitleWriter.fallback(DATE, "독립문", Language.EN)).isEqualTo("November 7, 2026 · 독립문");
        assertThat(PostcardTitleWriter.fallback(DATE, "독립문", Language.VI)).contains("2026").endsWith(" · 독립문");
    }
}
