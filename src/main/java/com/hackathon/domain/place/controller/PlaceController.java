package com.hackathon.domain.place.controller;

import com.hackathon.domain.place.dto.response.NearbyPlaceResponseDto;
import com.hackathon.domain.place.service.BaseLocation;
import com.hackathon.domain.place.service.PlaceImageService;
import com.hackathon.domain.place.service.PlaceService;
import com.hackathon.domain.place.service.StationService;
import com.hackathon.global.external.ExternalImage;
import com.hackathon.global.response.ApiResponse;
import com.hackathon.global.response.ApiResult;
import com.hackathon.global.response.type.SuccessType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Place", description = "F4 현장 해설 — 주변 장소와 해설 번역")
@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

    static final int DEFAULT_LIMIT = 5;
    static final int MAX_LIMIT = 20;

    private final PlaceService placeService;
    private final StationService stationService;
    private final PlaceImageService placeImageService;

    @Operation(summary = "주변 장소", description = """
            현장 해설을 볼 수 있는 장소를 가까운 순으로 준다. 위치 = lat·lng → 없으면 stationId → 없으면 기본 역(독립문).
            위치는 계산에만 쓰고 저장하지 않는다. 거리 제한은 없다.
            """)
    @GetMapping("/nearby")
    public ResponseEntity<ApiResult<List<NearbyPlaceResponseDto>>> nearby(
            @Parameter(description = "위도 (lng와 함께)", example = "37.5745") @RequestParam(required = false) Double lat,
            @Parameter(description = "경도 (lat과 함께)", example = "126.9577") @RequestParam(required = false) Double lng,
            @Parameter(description = "GET /api/stations의 역 ID. lat·lng가 있으면 무시한다", example = "12")
            @RequestParam(required = false) Long stationId,
            @Parameter(description = "개수 (기본 5, 최대 20)", example = "5") @RequestParam(required = false) Integer limit
    ) {
        BaseLocation location = stationService.resolve(lat, lng, stationId);
        int size = limit == null ? DEFAULT_LIMIT : Math.clamp(limit, 1, MAX_LIMIT);
        List<NearbyPlaceResponseDto> places = placeService.getNearbyWithExplanation(location, size).stream()
                .map(NearbyPlaceResponseDto::from)
                .toList();
        return ApiResponse.of(SuccessType.SUCCESS, places);
    }

    @Operation(summary = "장소 사진 파일", description = """
            이미지 바이트를 그대로 돌려준다 (JSON 아님). <img src>에 쓴다. 토큰 없이 부른다.
            관광공사 사진은 CORS 헤더가 없어 웹에서 화면을 캡처하면 저장이 실패할 수 있으므로 서버를 거쳐 준다.
            """)
    @GetMapping("/{placeId}/image")
    public ResponseEntity<byte[]> image(
            @Parameter(description = "장소 ID", example = "34") @PathVariable Long placeId
    ) {
        ExternalImage image = placeImageService.getImage(placeId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .body(image.content());
    }
}
