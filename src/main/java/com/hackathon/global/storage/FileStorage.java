package com.hackathon.global.storage;

import org.springframework.core.io.Resource;

/**
 * 사진·엽서 같은 파일 저장소. 파일은 여기에, DB에는 save()가 돌려준 이름만 둔다.
 * 지금은 로컬 디스크(LocalFileStorage, app.storage.dir)만 있다. 배포처가 바뀌어 S3 같은 외부 저장소가 필요하면
 * 구현을 하나 더 만들면 되고, 이 인터페이스를 쓰는 서비스 코드는 바뀌지 않는다.
 */
public interface FileStorage {

    /**
     * 파일을 저장하고 저장된 이름(UUID + 확장자, 예: "3f2a…c1e.jpg")을 돌려준다. 원래 파일 이름은 쓰지 않는다.
     *
     * @param extension 점 없는 확장자 (jpg, png, webp). contentType에서 고르려면 {@link #extensionOf(String)}
     */
    String save(byte[] content, String extension);

    /** 저장된 파일을 읽는다. 없으면 404 STORAGE_NOT_FOUND */
    Resource load(String storedName);

    /** 저장된 파일을 지운다. 없으면 아무것도 하지 않는다 */
    void delete(String storedName);

    /** 업로드 허용 형식(jpeg·png·webp)의 확장자. 그 밖이면 null */
    static String extensionOf(String contentType) {
        if (contentType == null) return null;
        return switch (contentType.toLowerCase()) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> null;
        };
    }
}
