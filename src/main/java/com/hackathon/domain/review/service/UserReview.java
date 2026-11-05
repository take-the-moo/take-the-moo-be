package com.hackathon.domain.review.service;

import com.hackathon.domain.review.entity.Review;
import java.time.LocalDate;

/** 다른 도메인(F8)에 넘기는 사용자의 한걸음. 시각 대신 날짜만 준다 */
public record UserReview(Long placeId, String content, String photoId, LocalDate date) {

    static UserReview from(Review review) {
        return new UserReview(review.getPlaceId(), review.getContent(), review.getPhotoId(),
                review.getCreatedAt().toLocalDate());
    }
}
