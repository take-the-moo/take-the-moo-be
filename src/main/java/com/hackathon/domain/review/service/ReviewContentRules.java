package com.hackathon.domain.review.service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * AI를 부르기 전에 규칙으로 거르는 것: 링크·이메일·전화번호 (광고·개인정보).
 * 확실한 것만 잡는다. 애매한 표현(욕설·혐오 등)은 moderate 프롬프트가 판단한다.
 */
final class ReviewContentRules {

    private static final List<Pattern> BLOCKED = List.of(
            // 링크: http(s)://, www., 흔한 도메인 끝
            Pattern.compile("(?i)https?://|www\\.|\\b[a-z0-9-]+\\.(com|net|kr|co|io|me|ly|shop|link)\\b"),
            // 이메일
            Pattern.compile("(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}"),
            // 전화번호: 숫자 9자리 이상 (하이픈·공백·점 허용) — 연도(4자리)·가격 같은 짧은 숫자는 걸리지 않는다
            Pattern.compile("(?<!\\d)(\\+?\\d[\\d\\s.-]{7,}\\d)(?!\\d)")
    );

    private ReviewContentRules() {}

    static boolean isBlocked(String content) {
        return BLOCKED.stream().anyMatch(pattern -> {
            var matcher = pattern.matcher(content);
            while (matcher.find()) {
                String found = matcher.group();
                // 전화번호 패턴은 숫자가 9개 이상일 때만 막는다 (예: "1908-1987"은 8자리)
                if (pattern == BLOCKED.get(2) && found.replaceAll("\\D", "").length() < 9) continue;
                return true;
            }
            return false;
        });
    }
}
