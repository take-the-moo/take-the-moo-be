package com.hackathon.domain.postcard.render;

import java.util.List;

/**
 * 템플릿 하나의 배치 (resources/postcard/templates.json). 좌표는 px, 왼쪽 위가 (0, 0).
 *
 * @param backgroundImage 배경 PNG의 classpath 경로. null이면 backgroundColor만 칠한다. 캔버스 크기로 늘려 사진 <b>위에</b> 덮어
 *                        그리므로 사진 칸은 투명하게 뚫어 둔다 (사진 칸 테두리·둥근 모서리가 사진 위에 그대로 남는다).
 *                        사진 칸 가운데가 투명하지 않으면 서버가 시작되지 않는다
 * @param fonts           글꼴 후보 (ttf·otf). 문장마다 앞에서부터 보고, 문장의 모든 글자가 있는 첫 글꼴로 쓴다.
 *                        모두 안 되면 시스템 SansSerif(도커에서는 Noto Sans CJK). null·빈 목록이면 SansSerif만 쓴다
 * @param art             원본 사진이 들어갈 칸. 비율이 다르면 가운데를 잘라 채운다. 배경의 투명한 칸보다 조금 크게 잡아 테두리 밑까지 채운다
 * @param title           제목(사용자 언어로 날짜·장소) 칸
 * @param comment         사용자가 쓴 한 문장 칸. 길면 줄을 바꾸고, 그래도 넘치면 글자를 줄인다
 */
public record PostcardLayout(
        int width,
        int height,
        String backgroundColor,
        String backgroundImage,
        List<FontRef> fonts,
        Box art,
        TextBox title,
        TextBox comment
) {

    public record Box(int x, int y, int width, int height) {
    }

    /**
     * @param file  글꼴 파일의 classpath 경로 (예: postcard/fonts/NanumPenScript-Regular.ttf)
     * @param scale 글자 크기 배율 (null이면 1). 손글씨체는 같은 크기에서도 작게 그려지는 것이 많아 보통 글꼴과 눈으로 맞춘다
     */
    public record FontRef(String file, Float scale) {
    }

    /**
     * @param align     {@code "left"} 또는 {@code "center"} (null이면 center)
     * @param baselines 밑줄 위에 쓰듯 줄마다 글자 바닥(baseline) y를 정한다. 있으면 이 개수까지만 줄을 쓰고, 넘치면 글자를 줄인다.
     *                  y·height는 무시한다. null이면 칸 안 세로 가운데에 쓴다
     */
    public record TextBox(int x, int y, int width, int height, float fontSize, String color,
                          String align, List<Integer> baselines) {

        boolean leftAligned() {
            return "left".equalsIgnoreCase(align);
        }

        boolean ruled() {
            return baselines != null && !baselines.isEmpty();
        }
    }
}
