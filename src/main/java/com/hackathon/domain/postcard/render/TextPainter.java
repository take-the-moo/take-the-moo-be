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
 * 칸 안에 글을 가운데 정렬로 그린다.
 * - 7개 언어(한·영·베·중·일·몽골 키릴·우즈벡)가 섞일 수 있어, 기본 글꼴에 없는 글자는 SansSerif로 바꿔 그린다
 * - 칸 너비를 넘으면 줄을 바꾸고, 줄이 칸 높이를 넘으면 글자 크기를 조금씩 줄인다 (최소 60%)
 */
final class TextPainter {

    private static final float MIN_SCALE = 0.6f;
    private static final float STEP = 0.05f;
    private static final float LINE_SPACING = 1.2f;

    private TextPainter() {
    }

    static void draw(Graphics2D g, String text, PostcardLayout.TextBox box, Font baseFont) {
        if (text == null || text.isBlank()) return;
        g.setColor(Color.decode(box.color()));
        FontRenderContext context = g.getFontRenderContext();

        List<TextLayout> lines = List.of();
        for (float scale = 1f; scale >= MIN_SCALE - 0.001f; scale -= STEP) {
            lines = layout(text.strip(), baseFont.deriveFont(box.fontSize() * scale), box.width(), context);
            if (height(lines) <= box.height()) break;
        }

        float y = box.y() + Math.max(0, (box.height() - height(lines)) / 2f);
        for (TextLayout line : lines) {
            float lineHeight = (line.getAscent() + line.getDescent()) * LINE_SPACING;
            if (y + lineHeight > box.y() + box.height() + 1) break;   // 가장 작은 글자로도 넘치면 남은 줄은 버린다
            float x = box.x() + (box.width() - line.getAdvance()) / 2f;
            line.draw(g, x, y + (lineHeight - line.getAscent() - line.getDescent()) / 2f + line.getAscent());
            y += lineHeight;
        }
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
