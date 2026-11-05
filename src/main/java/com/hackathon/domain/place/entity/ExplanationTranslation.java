package com.hackathon.domain.place.entity;

import com.hackathon.global.common.Language;
import com.hackathon.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.Length;

/**
 * 현장 해설 번역 캐시 (F4). 장소 × 언어마다 한 줄.
 * sourceHash(모델 + 프롬프트 + 원문의 해시)가 지금과 다르면 원문이나 프롬프트가 바뀐 것이므로 다시 번역해 덮어쓴다.
 * 다른 도메인 규칙대로 Place와 연관관계를 맺지 않고 placeId로 참조한다.
 */
@Getter
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_explanation_translation_place_lang", columnNames = {"placeId", "language"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExplanationTranslation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long placeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Language language;

    // length를 안 주면 MySQL tinytext가 되어 해설이 잘린다. LONG32면 longtext (docs/db.md의 LONGTEXT)
    @Lob
    @Column(nullable = false, length = Length.LONG32)
    private String translatedText;

    @Column(nullable = false, length = 64)
    private String sourceHash;

    public static ExplanationTranslation create(Long placeId, Language language) {
        ExplanationTranslation translation = new ExplanationTranslation();
        translation.placeId = placeId;
        translation.language = language;
        return translation;
    }

    public void update(String translatedText, String sourceHash) {
        this.translatedText = translatedText;
        this.sourceHash = sourceHash;
    }
}
