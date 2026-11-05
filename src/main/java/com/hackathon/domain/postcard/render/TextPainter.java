package com.hackathon.domain.postcard.render;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;

/**
 * 칸 안에 글을 그린다 (기본 가운데 정렬, {@code align: "left"}면 왼쪽. {@code baselines}가 있으면 밑줄 위에 한 줄씩).
 * - 7개 언어(한·영·베·중·일·몽골 키릴·우즈벡)가 들어올 수 있다. 문장마다 템플릿 글꼴 후보를 앞에서부터 보고 모든 글자가 있는
 *   첫 글꼴로 쓴다. 없으면 문장 통째로 SansSerif(도커에서는 Noto Sans CJK). 그래도 없는 글자(이모지 등)는 글자 단위로 바꾼다
 * - 칸 너비를 넘으면 줄을 바꾸고, 줄이 칸 높이를 넘으면 글자 크기를 조금씩 줄인다 (최소 60%)
 */
final class TextPainter {

    private static final ScaledFont FALLBACK = new ScaledFont(new Font(Font.SANS_SERIF, Font.PLAIN, 1), 1f);
    private static final float MIN_SCALE = 0.6f;
    private static final float STEP = 0.05f;
    private static final float LINE_SPACING = 1.2f;

    private TextPainter() {
    }

    static void draw(Graphics2D g, String text, PostcardLayout.TextBox box, List<ScaledFont> fonts) {
        if (text == null || text.isBlank()) return;
        g.setColor(Color.decode(box.color()));
        FontRenderContext context = g.getFontRenderContext();

        String content = normalize(box.ruled() ? text.strip().replace('\n', ' ') : text.strip());
        // 문장의 모든 글자가 있는 첫 글꼴로 쓴다 (한국어는 한글 손글씨체, 베트남어는 성조 글자가 있는 글꼴 …).
        // 글자마다 바꾸면 한 단어 안에서 글꼴이 섞인다
        ScaledFont chosen = choose(content, fonts);
        List<TextLayout> lines = List.of();
        Font font = chosen.font();
        for (float scale = 1f; scale >= MIN_SCALE - 0.001f; scale -= STEP) {
            font = chosen.font().deriveFont(box.fontSize() * chosen.scale() * scale);
            lines = layout(content, font, box.width(), context);
            if (box.ruled() ? lines.size() <= box.baselines().size() : height(lines) <= box.height()) break;
        }

        if (box.ruled()) {
            // 밑줄 위에 한 줄씩. 가장 작은 글자로도 넘치면 마지막 줄 끝을 "…"로 줄인다
            int max = box.baselines().size();
            if (lines.size() > max) lines = withEllipsis(content, lines, max, font, box.width(), context);
            for (int i = 0; i < lines.size(); i++) {
                lines.get(i).draw(g, x(box, lines.get(i)), box.baselines().get(i));
            }
            return;
        }

        float y = box.y() + Math.max(0, (box.height() - height(lines)) / 2f);
        for (TextLayout line : lines) {
            float lineHeight = (line.getAscent() + line.getDescent()) * LINE_SPACING;
            if (y + lineHeight > box.y() + box.height() + 1) break;   // 가장 작은 글자로도 넘치면 남은 줄은 버린다
            line.draw(g, x(box, line), y + (lineHeight - line.getAscent() - line.getDescent()) / 2f + line.getAscent());
            y += lineHeight;
        }
    }

    private static ScaledFont choose(String content, List<ScaledFont> fonts) {
        for (ScaledFont candidate : fonts) {
            if (candidate.font().canDisplayUpTo(content) == -1) return candidate;
        }
        return FALLBACK;
    }

    /** 우즈베크어 ʻ(U+02BB)는 대부분의 글꼴에 없어 모양이 같은 ‘(U+2018)로 바꾼다 */
    private static String normalize(String text) {
        return text.replace('\u02BB', '\u2018');
    }

    /** 앞 max-1줄은 그대로 두고, 남은 글을 마지막 줄 하나에 "…"까지 들어가게 자른다 */
    private static List<TextLayout> withEllipsis(String text, List<TextLayout> lines, int max, Font font, int width,
                                                 FontRenderContext context) {
        List<TextLayout> kept = new ArrayList<>(lines.subList(0, max - 1));
        int start = 0;
        for (TextLayout line : kept) start += line.getCharacterCount();
        String rest = text.substring(start).strip();
        for (int end = rest.length(); end > 0; end--) {
            if (Character.isLowSurrogate(rest.charAt(end - 1))) continue;
            String candidate = rest.substring(0, end).stripTrailing() + "…";
            TextLayout last = new TextLayout(withFallback(candidate, font).getIterator(), context);
            if (last.getAdvance() <= width) {
                kept.add(last);
                return kept;
            }
        }
        return kept;
    }

    private static float x(PostcardLayout.TextBox box, TextLayout line) {
        return box.leftAligned() ? box.x() : box.x() + (box.width() - line.getAdvance()) / 2f;
    }

    private static List<TextLayout> layout(String text, Font font, int width, FontRenderContext context) {
        List<TextLayout> lines = new ArrayList<>();
        for (String paragraph : text.split("\n")) {
            if (paragraph.isBlank()) continue;
            AttributedCharacterIterator chars = withFallback(paragraph, font).getIterator();
            LineBreakMeasurer measurer = new LineBreakMeasurer(chars, context);
            while (measurer.getPosition() < chars.getEndIndex()) {
                lines.add(measurer.nextLayout(width));
            }
        }
        return lines;
    }

    /** 글꼴에 없는 글자(예: 한글 글꼴의 몽골어 키릴 문자)만 SansSerif로 바꾼다 */
    private static AttributedString withFallback(String text, Font font) {
        AttributedString attributed = new AttributedString(text);
        Font fallback = new Font(Font.SANS_SERIF, font.getStyle(), font.getSize()).deriveFont(font.getSize2D());
        int start = 0;
        while (start < text.length()) {
            boolean primary = font.canDisplay(text.codePointAt(start));
            int end = start;
            while (end < text.length() && font.canDisplay(text.codePointAt(end)) == primary) {
                end += Character.charCount(text.codePointAt(end));
            }
            attributed.addAttribute(TextAttribute.FONT, primary ? font : fallback, start, end);
            start = end;
        }
        return attributed;
    }

    private static float height(List<TextLayout> lines) {
        float total = 0;
        for (TextLayout line : lines) total += (line.getAscent() + line.getDescent()) * LINE_SPACING;
        return total;
    }
}
