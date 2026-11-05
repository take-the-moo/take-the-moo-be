package com.hackathon.domain.photo.service;

import com.hackathon.domain.photo.dto.response.PhotoResponseDto;
import com.hackathon.domain.photo.entity.Photo;

/**
 * 다른 도메인(F4·F6·F7)에 넘기는 사진 정보. 엔티티 대신 이것을 쓴다.
 *
 * @param url 사진 파일 주소 (/api/photos/{photoId})
 */
public record PhotoInfo(String photoId, String userId, Long placeId, String contentType, String url) {

    static PhotoInfo from(Photo photo) {
        return new PhotoInfo(photo.getId(), photo.getUserId(), photo.getPlaceId(), photo.getContentType(),
                PhotoResponseDto.urlOf(photo.getId()));
    }
}
