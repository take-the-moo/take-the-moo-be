package com.hackathon.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReviewContentRulesTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "자세한 건 https://bit.ly/abc",
            "www.myshop.kr 에서 할인",
            "instagram.com/myshop 팔로우",
            "문의는 hello@mail.com",
            "010-1234-5678로 연락",
            "+84 912 345 678 nhắn tin",
            "01012345678"
    })
    void 링크_이메일_전화번호는_막는다(String content) {
        assertThat(ReviewContentRules.isBlocked(content)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1908-1987년까지 감옥이었다",
            "입장료 3,000원, 9시 30분에 열어요",
            "3·1운동 기념탑 앞에서",
            "떡볶이 5000원 맛있어요!",
            "Đẹp lắm, nên đến vào buổi sáng"
    })
    void 연도_가격_평범한_글은_통과한다(String content) {
        assertThat(ReviewContentRules.isBlocked(content)).isFalse();
    }
}
