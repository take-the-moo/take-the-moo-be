package com.hackathon.domain.visit.controller;

import com.hackathon.domain.visit.dto.request.VisitRequestDto;
import com.hackathon.domain.visit.dto.response.VisitResponseDto;
import com.hackathon.domain.visit.service.VisitService;
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

@Tag(name = "Visit", description = "F5 방문 도장 — 장소 근처에서 찍는 도장과 코스 진행도")
@RestController
@RequestMapping("/api/visits")
@RequiredArgsConstructor
public class VisitController {

    private final VisitService visitService;

    @Operation(summary = "방문 도장", description = """
            장소 근처(기본 200m)에 있으면 도장을 찍고, 이어 가기 좋은 코스와 다음 장소를 함께 돌려준다.
            새로 찍으면 201, 오늘 이미 찍었으면 200과 기존 도장(거리는 보지 않는다), 멀면 422 VISIT_TOO_FAR다.
            좌표는 거리 계산에만 쓰고 저장하지 않는다. 시연에서는 장소 좌표를 그대로 보내고 simulatedWeather·simulatedTime을 쓴다.
            """)
    @PostMapping
    public ResponseEntity<ApiResult<VisitResponseDto>> stamp(@Valid @RequestBody VisitRequestDto request) {
        VisitResponseDto visit = visitService.stamp(request);
        return ApiResponse.of(visit.alreadyStamped() ? SuccessType.SUCCESS : SuccessType.CREATED, visit);
    }
}
