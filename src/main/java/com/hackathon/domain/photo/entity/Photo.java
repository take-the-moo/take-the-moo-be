package com.hackathon.domain.photo.entity;

import com.hackathon.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 업로드한 사진 (docs/db.md photo). 파일은 FileStorage(app.storage.dir)에 있고 여기에는 저장된 이름만 둔다.
 * id는 UUID라 주소(/api/photos/{id})를 추측할 수 없다. 다른 도메인은 photoId(String)로 참조한다.
 */
@Getter
@Entity
@Table(name = "photo", indexes = @Index(name = "idx_photo_user_created", columnList = "user_id, created_at"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Photo extends BaseTimeEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    /** 장소 없이 올릴 수 있다 */
    @Column(name = "place_id")
    private Long placeId;

    @Column(name = "stored_name", nullable = false, length = 100)
    private String storedName;

    @Column(name = "content_type", nullable = false, length = 30)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    private Photo(String userId, Long placeId, String storedName, String contentType, long sizeBytes) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.placeId = placeId;
        this.storedName = storedName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

    public static Photo create(String userId, Long placeId, String storedName, String contentType, long sizeBytes) {
        return new Photo(userId, placeId, storedName, contentType, sizeBytes);
    }

    public boolean isOwnedBy(String userId) {
        return this.userId.equals(userId);
    }
}
