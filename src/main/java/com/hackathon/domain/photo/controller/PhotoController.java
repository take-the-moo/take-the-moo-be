package com.hackathon.domain.photo.controller;

import com.hackathon.domain.llm.dto.request.ImageFile;
import com.hackathon.domain.photo.dto.response.PhotoResponseDto;
import com.hackathon.domain.photo.service.PhotoFile;
import com.hackathon.domain.photo.service.PhotoService;
import com.hackathon.global.response.ApiResponse;
import com.hackathon.global.response.ApiResult;
import com.hackathon.global.response.type.SuccessType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Photo", description = "사진 업로드·조회 — 한걸음(F6)·엽서(F7)에 photoId로 붙인다")
@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
public class PhotoController {

    // spring.servlet.multipart.max-file-size(10MB)와 맞춘다
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final PhotoService photoService;

    @Operation(summary = "사진 업로드",
            description = "multipart로 사진을 올리고 photoId와 파일 주소를 받는다. 10MB 이하 jpeg·png·webp만 (HEIC 불가).")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResult<PhotoResponseDto>> upload(
            @Parameter(description = "사진 파일 (10MB 이하, jpeg/png/webp)")
            @RequestPart("file")
            @ImageFile(maxBytes = MAX_BYTES, message = "10MB 이하의 jpeg, png, webp 이미지만 업로드할 수 있습니다.")
            MultipartFile file,
            @Parameter(description = "POST /api/users가 돌려준 userId", example = "3f2b8c1e-7a4d-4e9b-9c2a-1d5e6f7a8b9c")
            @RequestParam @NotBlank(message = "userId를 입력해 주세요.") @Size(max = 36, message = "36자 이하로 입력해 주세요.")
            String userId,
            @Parameter(description = "찍은 장소 ID (선택)", example = "7")
            @RequestParam(required = false) Long placeId
    ) {
        return ApiResponse.of(SuccessType.CREATED, photoService.upload(userId, placeId, file));
    }

    @Operation(summary = "사진 파일", description = "이미지 바이트를 그대로 돌려준다 (JSON 아님). <img src>에 쓴다. 없으면 404 PHOTO_NOT_FOUND.")
    @GetMapping("/{photoId}")
    public ResponseEntity<Resource> file(
            @Parameter(description = "사진 ID", example = "9b1c2d3e-4f5a-6b7c-8d9e-0f1a2b3c4d5e")
            @PathVariable String photoId
    ) {
        PhotoFile photo = photoService.getFile(photoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.contentType()))
                .contentLength(photo.sizeBytes())
                // 한 번 올린 사진은 바뀌지 않는다
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic().immutable())
                .body(photo.resource());
    }
}
