package com.hackathon.domain.place.controller;

import com.hackathon.domain.place.dto.request.ExplanationRequestDto;
import com.hackathon.domain.place.dto.response.ExplanationResponseDto;
import com.hackathon.domain.place.service.ExplanationService;
import com.hackathon.global.response.ApiResponse;
import com.hackathon.global.response.ApiResult;
import com.hackathon.global.response.type.SuccessType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Place", description = "F4 현장 해설 — 주변 장소와 해설 번역")
@RestController
@RequestMapping("/api/explanations")
@RequiredArgsConstructor
public class ExplanationController {

    private final ExplanationService explanationService;

    @Operation(summary = "현장 해설", description = """
            장소에 저장된 한국어 해설을 요청 언어로 번역해 준다. AI는 번역만 하고 내용을 더하거나 빼지 않는다.
            KO면 원문을 그대로 준다. 번역은 장소 × 언어별로 저장해 두고, 두 번째부터는 바로 온다(cached=true).
            """)
    @PostMapping
    public ResponseEntity<ApiResult<ExplanationResponseDto>> explain(@Valid @RequestBody ExplanationRequestDto request) {
        return ApiResponse.of(SuccessType.SUCCESS, explanationService.explain(request));
    }
}
