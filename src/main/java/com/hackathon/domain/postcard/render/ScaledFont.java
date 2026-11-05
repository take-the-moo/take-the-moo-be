package com.hackathon.domain.postcard.render;

import java.awt.Font;

/** 템플릿 글꼴 하나와 글자 크기 배율 ({@link PostcardLayout.FontRef}를 읽은 것) */
public record ScaledFont(Font font, float scale) {
}
