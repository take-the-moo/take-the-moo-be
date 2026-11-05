package com.hackathon.domain.review.controller;

import com.hackathon.domain.review.dto.request.ReviewCreateRequestDto;
import com.hackathon.domain.review.dto.response.ReviewListResponseDto;
import com.hackathon.domain.review.dto.response.ReviewResponseDto;
import com.hackathon.domain.review.service.ReviewService;
import com.hackathon.global.common.Language;
import com.hackathon.global.response.ApiResponse;
import com.hackathon.global.response.ApiResult;
import com.hackathon.global.response.type.SuccessType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Review", description = "F6 한걸음 — 장소에 남기는 한 줄 후기")
@RestController
@RequestMapping("/api/places/{placeId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "한걸음 목록", description = """
            장소의 한걸음을 최신순으로 돌려준다. 원문 언어가 보는 사람 언어와 다르면 translatedContent에 번역을 함께 준다.
            보는 사람 언어 = language, 없으면 userId의 언어, 둘 다 없으면 한국어. 번역은 저장해 두고 다시 쓴다.
            번역이 실패해도 목록은 원문으로 온다(translatedContent가 null).
            """)
    @GetMapping
    public ResponseEntity<ApiResult<ReviewListResponseDto>> list(
            @Parameter(description = "장소 ID", example = "7") @PathVariable Long placeId,
            @Parameter(description = "보는 사람 userId (언어를 정하는 데 쓴다)") @RequestParam(required = false) String userId,
            @Parameter(description = "보는 사람 언어. userId보다 우선한다", example = "VI") @RequestParam(required = false) Language language,
            @Parameter(description = "개수 (기본 20, 최대 50)", example = "20") @RequestParam(required = false) Integer limit
    ) {
        return ApiResponse.of(SuccessType.SUCCESS, reviewService.list(placeId, userId, language, limit));
    }

    @Operation(summary = "한걸음 작성", description = """
            장소에 한 줄 후기를 남긴다. 링크·이메일·전화번호, 욕설·혐오·광고가 있으면 422 REVIEW_INAPPROPRIATE이고 저장하지 않는다.
            language를 비우면 서버가 글을 보고 작성 언어를 정한다.
            """)
    @PostMapping
    public ResponseEntity<ApiResult<ReviewResponseDto>> create(
            @Parameter(description = "장소 ID", example = "7") @PathVariable Long placeId,
            @Valid @RequestBody ReviewCreateRequestDto request
    ) {
        return ApiResponse.of(SuccessType.CREATED, reviewService.create(placeId, request));
    }
}
