package com.hackathon.domain.visit.controller;

import com.hackathon.domain.visit.dto.response.VisitHistoryResponseDto;
import com.hackathon.domain.visit.service.VisitHistoryService;
import com.hackathon.global.response.ApiResponse;
import com.hackathon.global.response.ApiResult;
import com.hackathon.global.response.type.SuccessType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Visit", description = "F5 방문 도장 — 장소 근처에서 찍는 도장과 코스 진행도")
@RestController
@RequiredArgsConstructor
public class VisitHistoryController {

    private final VisitHistoryService visitHistoryService;

    @Operation(summary = "나의 방문 기록", description = """
            나의 기록 화면에 필요한 값을 한 번에 돌려준다: 도장, 안 간 코스 장소, 코스 진행도, 날짜별 기록.
            코스에는 시작 전 코스도 포함되므로 완주 표시는 completed로 거른다. 방문 시각과 위치는 내보내지 않는다.
            """)
    @GetMapping("/api/users/{userId}/visits")
    public ResponseEntity<ApiResult<VisitHistoryResponseDto>> history(
            @Parameter(description = "POST /api/users가 돌려준 userId") @PathVariable String userId
    ) {
        return ApiResponse.of(SuccessType.SUCCESS, visitHistoryService.getHistory(userId));
    }
}
