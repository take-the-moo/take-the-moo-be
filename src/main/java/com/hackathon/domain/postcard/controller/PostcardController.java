package com.hackathon.domain.postcard.controller;

import com.hackathon.domain.postcard.dto.request.PostcardCreateRequestDto;
import com.hackathon.domain.postcard.dto.response.PostcardResponseDto;
import com.hackathon.domain.postcard.service.PostcardService;
import com.hackathon.global.response.ApiResponse;
import com.hackathon.global.response.ApiResult;
import com.hackathon.global.response.type.SuccessType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Postcard", description = "F7 회고 엽서 — 사진 1장 + 한 문장 → 템플릿 엽서 이미지 (비동기)")
@RestController
@RequestMapping("/api/postcards")
@RequiredArgsConstructor
public class PostcardController {

    private static final String DOWNLOAD_NAME = "seodaemun-postcard.jpg";

    private final PostcardService postcardService;

    @Operation(summary = "엽서 생성 시작",
            description = "바로 PENDING 상태로 돌려준다. GET /api/postcards/{postcardId}로 2초쯤마다 상태를 확인한다.")
    @PostMapping
    public ResponseEntity<ApiResult<PostcardResponseDto>> create(@Valid @RequestBody PostcardCreateRequestDto request) {
        return ApiResponse.of(SuccessType.CREATED, postcardService.create(request));
    }

    @Operation(summary = "엽서 상태", description = "COMPLETED면 imageUrl·downloadUrl, FAILED면 errorCode가 온다.")
    @GetMapping("/{postcardId}")
    public ResponseEntity<ApiResult<PostcardResponseDto>> get(
            @Parameter(description = "엽서 ID", example = "c7d8e9f0-1a2b-3c4d-5e6f-7a8b9c0d1e2f")
            @PathVariable String postcardId
    ) {
        return ApiResponse.of(SuccessType.SUCCESS, postcardService.get(postcardId));
    }

    @Operation(summary = "엽서 이미지",
            description = "JPEG 바이트를 그대로 준다 (JSON 아님). download=true면 파일로 내려받는다. 완성 전이면 409 POSTCARD_NOT_READY.")
    @GetMapping("/{postcardId}/image")
    public ResponseEntity<Resource> image(
            @Parameter(description = "엽서 ID", example = "c7d8e9f0-1a2b-3c4d-5e6f-7a8b9c0d1e2f")
            @PathVariable String postcardId,
            @Parameter(description = "true면 Content-Disposition: attachment (내려받기)")
            @RequestParam(defaultValue = "false") boolean download
    ) {
        Resource image = postcardService.getImage(postcardId);
        ContentDisposition disposition = (download ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(DOWNLOAD_NAME)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                // 완성된 엽서는 바뀌지 않는다
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic().immutable())
                .body(image);
    }
}
