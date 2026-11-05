package com.hackathon.domain.photo.service;

import org.springframework.core.io.Resource;

/** 사진 파일과 형식. 응답으로 내보내거나(GET /api/photos/{id}) 엽서 생성에 읽을 때 쓴다 */
public record PhotoFile(Resource resource, String contentType, long sizeBytes) {
}
