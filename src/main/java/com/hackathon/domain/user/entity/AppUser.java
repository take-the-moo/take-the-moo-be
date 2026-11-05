package com.hackathon.domain.user.entity;

import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import com.hackathon.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인 없는 익명 사용자 (docs/db.md app_user). id는 서버가 만든 UUID이고 프론트가 기기에 저장한다.
 * 샘플 방문·한걸음을 쓴 사용자도 이 테이블에 sample=true로 넣는다.
 * 다른 도메인은 연관관계 대신 userId(String)로 참조한다.
 */
@Getter
@Entity
// user는 H2 예약어라 쓰지 않는다
@Table(name = "app_user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUser extends BaseTimeEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Nationality nationality;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Language language;

    @Column(nullable = false)
    private boolean sample;

    private AppUser(String id, Nationality nationality, Language language, boolean sample) {
        this.id = id;
        this.nationality = nationality;
        this.language = language;
        this.sample = sample;
    }

    public static AppUser create(Nationality nationality, Language language) {
        return new AppUser(UUID.randomUUID().toString(), nationality, language, false);
    }

    /** 샘플 데이터용 사용자. 샘플 방문·한걸음은 이 사용자의 sample로 구분한다 */
    public static AppUser sample(Nationality nationality, Language language) {
        return new AppUser(UUID.randomUUID().toString(), nationality, language, true);
    }
}
