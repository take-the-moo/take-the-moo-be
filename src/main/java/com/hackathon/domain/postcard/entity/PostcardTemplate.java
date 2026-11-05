package com.hackathon.domain.postcard.entity;

/**
 * 엽서 템플릿. 배치(그림·제목·한 문장 칸)·배경·글꼴은 resources/postcard/templates.json에 이름별로 둔다.
 * 템플릿을 늘리면 상수와 templates.json 항목을 같이 추가한다 (빠지면 서버가 시작되지 않는다).
 */
public enum PostcardTemplate {
    /** 기본 템플릿 (팀 디자인: 사진 칸 + 발자국 도장 + 밑줄 두 줄). 배경은 postcard/basic.png */
    BASIC
}
