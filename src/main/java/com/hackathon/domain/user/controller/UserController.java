package com.hackathon.domain.user.controller;

import com.hackathon.domain.user.dto.request.ProfileUpdateRequestDto;
import com.hackathon.domain.user.dto.response.UserResponseDto;
import com.hackathon.domain.user.service.UserService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "F1 내 정보 — 로그인한 사용자 조회·수정 (Authorization: Bearer 토큰 필요)")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 정보", description = "토큰의 사용자. 앱을 켤 때 불러 자동 로그인에 쓴다 (401이면 로그인 화면으로).")
    @GetMapping("/me")
    public ResponseEntity<ApiResult<UserResponseDto>> me() {
        return ApiResponse.of(SuccessType.SUCCESS, userService.getMe());
    }

    @Operation(summary = "내 정보 수정", description = "이름·국적·언어 중 보낸 것만 바꾼다. 아이디는 바꿀 수 없다.")
    @PatchMapping("/me")
    public ResponseEntity<ApiResult<UserResponseDto>> updateMe(@Valid @RequestBody ProfileUpdateRequestDto request) {
        String name = request.name() == null ? null : request.name().strip();
        return ApiResponse.of(SuccessType.SUCCESS, userService.updateMe(name, request.nationality(), request.language()));
    }

    @Operation(summary = "사용자 조회", description = "본인만 조회할 수 있다 (토큰의 사용자와 같아야 한다). 보통은 GET /api/users/me를 쓴다.")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResult<UserResponseDto>> get(
            @Parameter(description = "사용자 ID (로그인 아이디)", example = "linh_2026") @PathVariable String userId
    ) {
        return ApiResponse.of(SuccessType.SUCCESS, userService.getUser(userId));
    }
}
