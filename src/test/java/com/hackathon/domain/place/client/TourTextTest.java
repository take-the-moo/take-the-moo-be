package com.hackathon.domain.place.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TourTextTest {

    @Test
    void 개요의_br_태그는_줄바꿈으로_바꾸고_나머지_태그는_지운다() {
        String raw = "대현문화공원은 신촌에 있다.<br />산책하기 좋다.<BR>  <b>무료</b>";

        assertThat(TourText.cleanOverview(raw)).isEqualTo("대현문화공원은 신촌에 있다.\n산책하기 좋다.\n무료");
    }

    @Test
    void 개요의_HTML_엔티티를_글자로_바꾼다() {
        String raw = "독립문&nbsp;&middot;&nbsp;독립관 &amp; 공원 &lt;사적&gt; &#39;1897&#39; &#x27;A&#x27; &amp;lt;";

        assertThat(TourText.cleanOverview(raw)).isEqualTo("독립문 · 독립관 & 공원 <사적> '1897' 'A' &lt;");
    }

    @Test
    void 연속된_빈_줄과_공백을_줄이고_비면_null이다() {
        assertThat(TourText.cleanOverview("가<br><br><br><br>나   다  ")).isEqualTo("가\n\n나 다");
        assertThat(TourText.cleanOverview("<br />  ")).isNull();
        assertThat(TourText.cleanOverview(null)).isNull();
    }

    @Test
    void 깨끗한_개요는_그대로다() {
        String clean = "서대문형무소역사관은 1908년에 세워졌다.\n지금은 역사관이다.";

        assertThat(TourText.cleanOverview(clean)).isEqualTo(clean);
    }

    @Test
    void http_이미지_주소를_https로_바꾼다() {
        assertThat(TourText.toHttps("http://tong.visitkorea.or.kr/cms/resource/45/a.jpg"))
                .isEqualTo("https://tong.visitkorea.or.kr/cms/resource/45/a.jpg");
        assertThat(TourText.toHttps("https://tong.visitkorea.or.kr/a.jpg")).isEqualTo("https://tong.visitkorea.or.kr/a.jpg");
        assertThat(TourText.toHttps("  ")).isNull();
        assertThat(TourText.toHttps(null)).isNull();
    }
}
