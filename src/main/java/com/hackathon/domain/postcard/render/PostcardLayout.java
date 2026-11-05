package com.hackathon.domain.postcard.render;

/**
 * 템플릿 하나의 배치 (resources/postcard/templates.json). 좌표는 px, 왼쪽 위가 (0, 0).
 *
 * @param backgroundImage 배경 PNG의 classpath 경로. null이면 backgroundColor로 칠한다. 캔버스 크기로 늘려 그린다
 * @param fontFile        글꼴 파일(ttf·otf)의 classpath 경로. null이면 시스템 SansSerif. 이 글꼴에 없는 글자는 SansSerif로 그린다
 * @param art             원본 사진이 들어갈 칸. 비율이 다르면 가운데를 잘라 채운다
 * @param title           제목(사용자 언어로 날짜·장소) 칸
 * @param comment         사용자가 쓴 한 문장 칸. 길면 줄을 바꾸고, 그래도 넘치면 글자를 줄인다
 */
public record PostcardLayout(
        int width,
        int height,
        String backgroundColor,
        String backgroundImage,
        String fontFile,
        Box art,
        TextBox title,
        TextBox comment
) {

    public record Box(int x, int y, int width, int height) {
    }

    public record TextBox(int x, int y, int width, int height, float fontSize, String color) {
    }
}
