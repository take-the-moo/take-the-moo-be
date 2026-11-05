package com.hackathon.domain.review.entity;

import com.hackathon.global.common.Language;
import com.hackathon.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 한걸음 (docs/db.md review). 장소에 남기는 한 줄 후기.
 * - 필터에 걸린 글은 저장하지 않으므로 상태 컬럼이 없다
 * - 작성자 국적은 복사하지 않고 표시할 때 UserService로 조회한다
 * - language는 사용자 설정이 아니라 실제 작성 언어다
 */
@Getter
@Entity
@Table(indexes = @Index(name = "idx_review_place_created", columnList = "placeId, createdAt"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseTimeEntity {

    public static final int CONTENT_MAX = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long placeId;

    @Column(nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, length = CONTENT_MAX)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Language language;

    /** 첨부 사진 (선택). 작성자 본인의 사진만 붙는다 (PhotoService.getOwnedPhoto로 확인) */
    @Column(length = 36)
    private String photoId;

    public static Review create(Long placeId, String userId, String content, Language language) {
        return create(placeId, userId, content, language, null);
    }

    public static Review create(Long placeId, String userId, String content, Language language, String photoId) {
        Review review = new Review();
        review.placeId = placeId;
        review.userId = userId;
        review.content = content;
        review.language = language;
        review.photoId = photoId;
        return review;
    }
}
