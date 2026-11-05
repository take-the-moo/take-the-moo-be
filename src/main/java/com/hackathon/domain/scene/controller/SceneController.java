package com.hackathon.domain.scene.controller;

import com.hackathon.domain.scene.dto.request.SceneRequestDto;
import com.hackathon.domain.scene.dto.response.SceneResponseDto;
import com.hackathon.domain.scene.service.SceneService;
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

@Tag(name = "Scene", description = "F3 맞춤 장면 — 같은 장소, 다른 이야기")
@RestController
@RequestMapping("/api/scenes")
@RequiredArgsConstructor
public class SceneController {

    private final SceneService sceneService;

    @Operation(summary = "맞춤 장면", description = """
            장소 이야기를 요청 언어(3~4문장)와 쉬운 한국어로, 관점 국적의 역사와 잇는 한 줄과 함께 돌려준다.
            nationality만 바꿔 다시 부르면 같은 장소의 다른 이야기가 온다.
            장소 × 국적 × 언어별로 저장해 두므로 두 번째부터는 바로 응답한다(cached=true).
            """)
    @PostMapping
    public ResponseEntity<ApiResult<SceneResponseDto>> scene(@Valid @RequestBody SceneRequestDto request) {
        return ApiResponse.of(SuccessType.SUCCESS, sceneService.getScene(request));
    }
}
