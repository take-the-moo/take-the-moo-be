package com.hackathon.domain.postcard.entity;

/**
 * 엽서 템플릿. 배치(그림·제목·한 문장 칸)·배경·글꼴은 resources/postcard/templates.json에 이름별로 둔다.
 * 템플릿을 늘리면 상수와 templates.json 항목을 같이 추가한다 (빠지면 서버가 시작되지 않는다).
 */
public enum PostcardTemplate {
    /** 임시 기본 템플릿. 팀 템플릿이 나오면 templates.json의 배경·좌표·글꼴만 바꾼다 */
    BASIC
}
