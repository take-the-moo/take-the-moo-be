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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 한걸음 번역 캐시 (docs/db.md review_translation). 한걸음은 고치지 않으므로 원문 해시 없이 (reviewId, language)로 찾는다.
 * 보는 사람의 언어가 원문 언어와 같으면 만들지 않는다.
 */
@Getter
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_review_translation_review_lang", columnNames = {"reviewId", "language"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewTranslation extends BaseTimeEntity {

    public static final int CONTENT_MAX = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long reviewId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Language language;

    @Column(nullable = false, length = CONTENT_MAX)
    private String translatedContent;

    public static ReviewTranslation create(Long reviewId, Language language, String translatedContent) {
        ReviewTranslation translation = new ReviewTranslation();
        translation.reviewId = reviewId;
        translation.language = language;
        translation.translatedContent = translatedContent.length() <= CONTENT_MAX
                ? translatedContent : translatedContent.substring(0, CONTENT_MAX);
        return translation;
    }
}
