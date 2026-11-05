package com.hackathon.domain.postcard.entity;

import com.hackathon.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회고 엽서 (docs/db.md postcard). 사진 1장 + 한 문장을 템플릿에 넣은 JPEG 한 장.
 * 생성은 비동기다: 요청 때 PENDING으로 저장하고 PostcardGenerator가 백그라운드에서 만든다.
 * 완성 파일은 FileStorage에 있고 여기에는 이름(imageName)만 둔다.
 */
@Getter
@Entity
@Table(name = "postcard", indexes = @Index(name = "idx_postcard_user_status", columnList = "user_id, status"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Postcard extends BaseTimeEntity {

    public static final int COMMENT_MAX = 100;

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "photo_id", nullable = false, length = 36)
    private String photoId;

    @Column(nullable = false, length = COMMENT_MAX)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostcardTemplate template;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostcardStatus status;

    @Column(name = "image_name", length = 100)
    private String imageName;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    public static Postcard create(String userId, String photoId, String comment, PostcardTemplate template) {
        Postcard postcard = new Postcard();
        postcard.id = UUID.randomUUID().toString();
        postcard.userId = userId;
        postcard.photoId = photoId;
        postcard.comment = comment;
        postcard.template = template;
        postcard.status = PostcardStatus.PENDING;
        return postcard;
    }

    public void startProcessing() {
        this.status = PostcardStatus.PROCESSING;
    }

    public void complete(String imageName) {
        this.status = PostcardStatus.COMPLETED;
        this.imageName = imageName;
        this.errorCode = null;
    }

    public void fail(String errorCode) {
        this.status = PostcardStatus.FAILED;
        this.errorCode = errorCode;
    }
}
