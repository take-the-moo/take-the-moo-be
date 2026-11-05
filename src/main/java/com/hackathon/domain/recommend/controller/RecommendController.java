package com.hackathon.domain.recommend.controller;

import com.hackathon.domain.recommend.dto.response.NowResponseDto;
import com.hackathon.domain.recommend.dto.response.StationResponseDto;
import com.hackathon.domain.recommend.service.RecommendService;
import com.hackathon.domain.weather.dto.WeatherCondition;
import com.hackathon.global.response.ApiResponse;
import com.hackathon.global.response.ApiResult;
import com.hackathon.global.response.type.SuccessType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Recommend", description = "F2 지금 추천 — 시간·날씨·위치에 맞는 장소")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    @Operation(summary = "지금 추천", description = """
            지금 시간·날씨·위치에 맞는 장소 1곳과 그 이유를 준다. 질문 없이 앱을 열자마자 부른다.
            규칙으로 거른 후보(지금 시간대, 비·눈이면 실내, 가까운 순) 중 AI가 1곳을 고르고, 이유는 사용자 나라의 비슷한 사례로 잇는다.
            모든 파라미터는 선택이다. 위치·날씨·시간은 시연용으로 지정할 수 있다. 결과와 위치는 저장하지 않는다.
            """)
    @GetMapping("/now")
    public ResponseEntity<ApiResult<NowResponseDto>> now(
            @Parameter(description = "사용자 ID. 언어·국적으로 이유를 쓴다. 없으면 국적 OTHER·언어 EN")
            @RequestParam(required = false) String userId,
            @Parameter(description = "위도 (lng와 함께)", example = "37.5745") @RequestParam(required = false) Double lat,
            @Parameter(description = "경도 (lat과 함께)", example = "126.9577") @RequestParam(required = false) Double lng,
            @Parameter(description = "GET /api/stations의 역 ID. lat·lng가 있으면 무시한다", example = "12")
            @RequestParam(required = false) Long stationId,
            @Parameter(description = "날씨 시뮬레이션 (CLEAR, RAINY, SNOWY). 없으면 기상청 실제 날씨", example = "RAINY")
            @RequestParam(required = false) WeatherCondition weather,
            @Parameter(description = "시각 시뮬레이션 (HH:mm, 서울). 없으면 지금", example = "15:00")
            @RequestParam(required = false) @DateTimeFormat(pattern = "H:mm") LocalTime time
    ) {
        return ApiResponse.of(SuccessType.SUCCESS, recommendService.now(userId, lat, lng, stationId, weather, time));
    }

    @Operation(summary = "역 선택 목록",
            description = "위치 권한을 거부했거나 데모에서 역을 고를 때 쓴다. 서대문 생활권 역을 정해진 순서로 주고, "
                    + "같은 이름의 역은 호선마다 따로 준다. 거리로 거르지 않는다.")
    @GetMapping("/stations")
    public ResponseEntity<ApiResult<List<StationResponseDto>>> stations() {
        return ApiResponse.of(SuccessType.SUCCESS, recommendService.getStations());
    }
}
