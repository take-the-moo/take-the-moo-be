package com.hackathon.domain.place.client;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TourAPI 값 정제. 동기화 때 DB에 넣기 전에 쓴다.
 * - 개요: HTML 태그·엔티티가 섞여 온다 (예: "...공원이다.<br />") → 줄바꿈과 일반 글자로 바꾼다
 * - 이미지: 일부가 http:// 로 온다. https 페이지에서 막히므로 https로 바꾼다 (tong.visitkorea.or.kr은 https로도 응답한다)
 */
public final class TourText {

    private static final Pattern BR = Pattern.compile("(?i)<br\\s*/?>|</p>");
    private static final Pattern TAG = Pattern.compile("<[^>]*>");
    private static final Pattern NUMERIC_ENTITY = Pattern.compile("&#(x?)([0-9a-fA-F]+);");
    private static final Map<String, String> NAMED_ENTITIES = Map.of(
            "&nbsp;", " ", "&lt;", "<", "&gt;", ">", "&quot;", "\"", "&apos;", "'", "&middot;", "·");

    private TourText() {
    }

    /** HTML 태그를 지우고 엔티티를 글자로 바꾼다. 비면 null */
    public static String cleanOverview(String raw) {
        if (raw == null) return null;
        String text = BR.matcher(raw).replaceAll("\n");
        text = TAG.matcher(text).replaceAll("");
        text = decodeEntities(text);
        text = text.replaceAll("[ \\t\\x0B\\f\\r]*\\n[ \\t\\x0B\\f\\r]*", "\n")   // 줄 앞뒤 공백
                .replaceAll("[ \\t\\x0B\\f\\r]{2,}", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
        return text.isEmpty() ? null : text;
    }

    /** http:// 이미지 주소를 https://로 바꾼다. 비면 null */
    public static String toHttps(String url) {
        if (url == null || url.isBlank()) return null;
        String trimmed = url.strip();
        return trimmed.regionMatches(true, 0, "http://", 0, 7) ? "https://" + trimmed.substring(7) : trimmed;
    }

    private static String decodeEntities(String text) {
        for (Map.Entry<String, String> entity : NAMED_ENTITIES.entrySet()) {
            text = text.replace(entity.getKey(), entity.getValue());
        }
        Matcher matcher = NUMERIC_ENTITY.matcher(text);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String replacement = matcher.group();
            try {
                int codePoint = Integer.parseInt(matcher.group(2), matcher.group(1).isEmpty() ? 10 : 16);
                if (Character.isValidCodePoint(codePoint)) replacement = Character.toString(codePoint);
            } catch (NumberFormatException ignored) {
                // "&#12a;"처럼 잘못된 엔티티는 그대로 둔다
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        // &amp;는 마지막에 바꾼다 ("&amp;lt;"가 "<"로 두 번 풀리지 않게)
        return out.toString().replace("&amp;", "&");
    }
}
