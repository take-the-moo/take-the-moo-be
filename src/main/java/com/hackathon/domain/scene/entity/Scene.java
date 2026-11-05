package com.hackathon.domain.scene.entity;

import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
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
 * 맞춤 장면 캐시. 장소 × 관점 국적 × 언어마다 한 줄.
 * sourceHash(프롬프트 + 장소 입력의 해시)가 다르면 장소 설명이나 프롬프트가 바뀐 것이므로 다시 생성해 덮어쓴다.
 */
@Getter
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_scene_place_nat_lang", columnNames = {"placeId", "nationality", "language"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Scene extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long placeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Nationality nationality;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Language language;

    // @Column에 length를 안 주면 기본 255라 MySQL tinytext가 되어 이야기가 잘린다. LONG32면 longtext (docs/db.md)
    @Lob
    @Column(nullable = false, length = Length.LONG32)
    private String storyNative;

    @Lob
    @Column(nullable = false, length = Length.LONG32)
    private String storyEasyKorean;

    @Column(nullable = false, length = 300)
    private String connectionLine;

    @Column(nullable = false, length = 64)
    private String sourceHash;

    public static Scene create(Long placeId, Nationality nationality, Language language) {
        Scene scene = new Scene();
        scene.placeId = placeId;
        scene.nationality = nationality;
        scene.language = language;
        return scene;
    }

    public void update(String storyNative, String storyEasyKorean, String connectionLine, String sourceHash) {
        this.storyNative = storyNative;
        this.storyEasyKorean = storyEasyKorean;
        this.connectionLine = connectionLine;
        this.sourceHash = sourceHash;
    }
}
