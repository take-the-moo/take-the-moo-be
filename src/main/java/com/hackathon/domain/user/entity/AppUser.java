package com.hackathon.domain.user.entity;

import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import com.hackathon.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

/**
 * 앱 내 로그인 사용자 (docs/db.md app_user). id는 사용자가 정한 로그인 아이디이고 다른 테이블의 user_id다.
 * 샘플 방문·한걸음을 쓴 사용자도 이 테이블에 sample=true로 넣는다 (id는 UUID, 비밀번호 없음 — 로그인할 수 없다).
 * 다른 도메인은 연관관계 대신 userId(String)로 참조한다.
 * <p>
 * id를 직접 정하므로 Persistable로 "새 행"임을 알린다. 그러지 않으면 save()가 같은 아이디의 기존 행을 덮어쓴다
 * (동시에 같은 아이디로 가입하면 남의 비밀번호를 덮어쓸 수 있다). 새 행은 persist되어 중복이면 오류가 난다.
 */
@Getter
@Entity
// user는 H2 예약어라 쓰지 않는다
@Table(name = "app_user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUser extends BaseTimeEntity implements Persistable<String> {

    @Id
    @Column(length = 36)
    private String id;

    /** BCrypt 해시. 샘플 사용자는 null */
    @Column(length = 100)
    private String passwordHash;

    /** 화면에 보이는 이름. 샘플 사용자는 null */
    @Column(length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Nationality nationality;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Language language;

    @Column(nullable = false)
    private boolean sample;

    @Transient
    @Getter(AccessLevel.NONE)
    private boolean newEntity = true;

    private AppUser(String id, String passwordHash, String name, Nationality nationality, Language language,
                    boolean sample) {
        this.id = id;
        this.passwordHash = passwordHash;
        this.name = name;
        this.nationality = nationality;
        this.language = language;
        this.sample = sample;
    }

    /** 회원가입. passwordHash는 이미 해시한 값이다 */
    public static AppUser signUp(String loginId, String passwordHash, String name, Nationality nationality,
                                 Language language) {
        return new AppUser(loginId, passwordHash, name, nationality, language, false);
    }

    /** 샘플 데이터용 사용자. 샘플 방문·한걸음은 이 사용자의 sample로 구분한다 */
    public static AppUser sample(Nationality nationality, Language language) {
        return new AppUser(UUID.randomUUID().toString(), null, null, nationality, language, true);
    }

    /** 로그인할 수 있는 사용자인지 (샘플 사용자는 아니다) */
    public boolean canLogIn() {
        return !sample && passwordHash != null;
    }

    /** 이름·국적·언어 중 null이 아닌 것만 바꾼다 */
    public void updateProfile(String name, Nationality nationality, Language language) {
        if (name != null) this.name = name;
        if (nationality != null) this.nationality = nationality;
        if (language != null) this.language = language;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.newEntity = false;
    }
}
