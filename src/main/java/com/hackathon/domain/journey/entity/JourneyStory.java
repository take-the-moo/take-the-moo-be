package com.hackathon.domain.journey.entity;

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
 * 나의 발자취 이야기 캐시 (docs/db.md journey_story). 사용자 × 언어마다 한 줄.
 * sourceHash가 다르면 기록이나 프롬프트가 바뀐 것이므로 다시 생성해 덮어쓴다.
 */
@Getter
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_journey_story_user_lang", columnNames = {"userId", "language"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JourneyStory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String userId;

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

    @Column(nullable = false, length = 64)
    private String sourceHash;

    public static JourneyStory create(String userId, Language language) {
        JourneyStory story = new JourneyStory();
        story.userId = userId;
        story.language = language;
        return story;
    }

    public void update(String storyNative, String storyEasyKorean, String sourceHash) {
        this.storyNative = storyNative;
        this.storyEasyKorean = storyEasyKorean;
        this.sourceHash = sourceHash;
    }
}
