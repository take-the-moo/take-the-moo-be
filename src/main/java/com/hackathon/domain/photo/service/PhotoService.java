package com.hackathon.domain.photo.service;

import com.hackathon.domain.photo.dto.response.PhotoResponseDto;
import com.hackathon.domain.photo.entity.Photo;
import com.hackathon.domain.photo.exception.PhotoErrorType;
import com.hackathon.domain.photo.exception.PhotoException;
import com.hackathon.domain.photo.repository.PhotoRepository;
import com.hackathon.domain.place.service.PlaceService;
import com.hackathon.domain.user.service.UserService;
import com.hackathon.global.storage.FileStorage;
import com.hackathon.global.storage.StorageErrorType;
import com.hackathon.global.storage.StorageException;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 사진 업로드·조회 (F4·F6·F7 공통). 다른 도메인은 repository 대신 이 서비스를 쓴다.
 * - 한걸음·엽서에 photoId를 받으면 {@link #getOwnedPhoto}로 "요청한 사용자의 사진인지" 확인한다
 * - 파일 내용이 필요하면(엽서 생성) {@link #getFile}
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PhotoService {

    private final PhotoRepository photoRepository;
    private final FileStorage fileStorage;
    private final UserService userService;
    private final PlaceService placeService;

    /**
     * 형식·크기는 컨트롤러(@ImageFile)가 먼저 확인한다. 없는 사용자면 404 USER_NOT_FOUND,
     * placeId가 노출 중인 장소가 아니면 404 PLACE_NOT_FOUND.
     */
    @Transactional
    public PhotoResponseDto upload(String userId, Long placeId, MultipartFile file) {
        userService.getUser(userId);
        if (placeId != null) placeService.getActivePlace(placeId);

        String contentType = file.getContentType().toLowerCase();
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new StorageException(StorageErrorType.WRITE_FAILED, "업로드 읽기 실패: " + e.getClass().getSimpleName(), e);
        }

        String storedName = fileStorage.save(content, FileStorage.extensionOf(contentType));
        try {
            Photo photo = photoRepository.save(Photo.create(userId, placeId, storedName, contentType, content.length));
            return PhotoResponseDto.from(photo);
        } catch (RuntimeException e) {
            // DB에 못 넣었으면 파일만 남지 않게 지운다
            fileStorage.delete(storedName);
            throw e;
        }
    }

    /**
     * 요청한 사용자의 사진 (한걸음 첨부·엽서 생성 전에 확인). 없으면 404 PHOTO_NOT_FOUND,
     * 다른 사람 사진이면 403 PHOTO_FORBIDDEN.
     */
    public PhotoInfo getOwnedPhoto(String userId, String photoId) {
        Photo photo = find(photoId);
        if (!photo.isOwnedBy(userId)) {
            throw new PhotoException(PhotoErrorType.FORBIDDEN, "photoId=" + photoId + ", userId=" + userId);
        }
        return PhotoInfo.from(photo);
    }

    /** 사진 정보 (소유자 확인 없이). 없으면 404 PHOTO_NOT_FOUND */
    public PhotoInfo getPhoto(String photoId) {
        return PhotoInfo.from(find(photoId));
    }

    /** 사진 파일. 없으면 404 PHOTO_NOT_FOUND, DB에는 있는데 파일이 없으면 404 STORAGE_NOT_FOUND */
    public PhotoFile getFile(String photoId) {
        Photo photo = find(photoId);
        return new PhotoFile(fileStorage.load(photo.getStoredName()), photo.getContentType(), photo.getSizeBytes());
    }

    private Photo find(String photoId) {
        return photoRepository.findById(photoId)
                .orElseThrow(() -> new PhotoException(PhotoErrorType.NOT_FOUND, "photoId=" + photoId));
    }
}
