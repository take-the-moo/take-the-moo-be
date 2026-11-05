package com.hackathon.domain.recommend.controller;

import com.hackathon.domain.recommend.dto.response.StationResponseDto;
import com.hackathon.domain.recommend.service.RecommendService;
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

@Tag(name = "Recommend", description = "F2 지금 추천 — 시간·날씨·위치에 맞는 장소")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    @Operation(summary = "역 선택 목록",
            description = "위치 권한을 거부했거나 데모에서 역을 고를 때 쓴다. 서대문 생활권 역을 정해진 순서로 주고, "
                    + "같은 이름의 역은 호선마다 따로 준다. 거리로 거르지 않는다.")
    @GetMapping("/stations")
    public ResponseEntity<ApiResult<List<StationResponseDto>>> stations() {
        return ApiResponse.of(SuccessType.SUCCESS, recommendService.getStations());
    }
}
