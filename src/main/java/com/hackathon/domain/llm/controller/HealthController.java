package com.hackathon.domain.llm.controller;

import com.hackathon.domain.llm.dto.response.HealthResponseDto;
import com.hackathon.domain.llm.service.LlmClient;
import com.hackathon.global.response.ApiResponse;
import com.hackathon.global.response.ApiResult;
import com.hackathon.global.response.type.SuccessType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health", description = "서버 상태 확인")
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final LlmClient llmClient;

    @Operation(summary = "헬스 체크", description = "서버가 떠 있는지, 목 모드인지 확인한다.")
    @GetMapping("/api/health")
    public ResponseEntity<ApiResult<HealthResponseDto>> health() {
        return ApiResponse.of(SuccessType.SUCCESS, new HealthResponseDto("ok", llmClient.isMock()));
    }
}
