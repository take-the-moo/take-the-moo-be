package com.hackathon.domain.recommend.service;

import com.hackathon.global.common.Language;
import java.util.Map;

/**
 * AI가 후보 밖 장소를 고르거나 이유를 비워 보냈을 때, 또는 AI 호출이 실패했을 때 쓰는 기본 문구.
 * 프롬프트가 아니라 화면에 그대로 나가는 고정 문장이다. 장소 사실을 담지 않는다.
 */
final class RecommendFallback {

    static final String REASON_KO = "지금 가까이에서 가 볼 만한 곳이에요.";

    private static final Map<Language, String> REASONS = Map.of(
            Language.KO, REASON_KO,
            Language.EN, "A good place to visit nearby right now.",
            Language.VI, "Một nơi đáng ghé thăm ở gần bạn ngay bây giờ.",
            Language.ZH, "现在就在附近，值得一去的地方。",
            Language.JA, "今、近くで立ち寄ってみたい場所です。",
            Language.MN, "Яг одоо ойрхон очиж үзэх сайхан газар.",
            Language.UZ, "Hozir yaqin atrofda borib ko'rishga arziydigan joy.");

    private RecommendFallback() {}

    static String reason(Language language) {
        return REASONS.getOrDefault(language, REASONS.get(Language.EN));
    }
}
