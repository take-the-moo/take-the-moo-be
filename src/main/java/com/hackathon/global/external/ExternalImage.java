package com.hackathon.global.external;

/** 외부에서 받은 이미지 */
public record ExternalImage(byte[] content, String contentType) {
}
