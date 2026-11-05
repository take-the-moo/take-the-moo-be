package com.hackathon.domain.visit.controller;

import com.hackathon.domain.visit.dto.response.MapPinResponseDto;
import com.hackathon.domain.visit.service.MapPinService;
import com.hackathon.global.response.ApiResponse;
import com.hackathon.global.response.ApiResult;
import com.hackathon.global.response.type.SuccessType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Map", description = "F6 흔적 지도 — 먼저 온 유학생의 발자취")
@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapController {

    private final MapPinService mapPinService;

    @Operation(summary = "흔적 지도 핀", description = """
            노출 중인 장소마다 다녀간 사람 수, 국적 분포, 한걸음 수를 준다. 방문자가 많은 순이다.
            장소 단위 집계만 담고, 개인의 방문 날짜·시각과 사용자 ID는 내보내지 않는다.
            """)
    @GetMapping("/pins")
    public ResponseEntity<ApiResult<List<MapPinResponseDto>>> pins() {
        return ApiResponse.of(SuccessType.SUCCESS, mapPinService.getPins());
    }
}
