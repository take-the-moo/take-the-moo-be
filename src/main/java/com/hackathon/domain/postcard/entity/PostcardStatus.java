package com.hackathon.domain.postcard.entity;

/** 엽서 생성 상태. PENDING → PROCESSING → COMPLETED 또는 FAILED */
public enum PostcardStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}
