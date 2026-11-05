package com.hackathon.domain.user.controller;

import com.hackathon.domain.user.dto.request.UserCreateRequestDto;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "F1 처음 설정 — 로그인 없는 익명 사용자")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "익명 사용자 생성", description = "국적·언어로 사용자를 만들고 userId를 돌려준다. 프론트는 userId를 기기에 저장한다.")
    @PostMapping
    public ResponseEntity<ApiResult<UserResponseDto>> create(@Valid @RequestBody UserCreateRequestDto request) {
        return ApiResponse.of(SuccessType.CREATED, userService.create(request));
    }

    @Operation(summary = "사용자 조회", description = "저장해 둔 userId로 국적·언어를 확인한다. 없으면 404 USER_NOT_FOUND.")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResult<UserResponseDto>> get(
            @Parameter(description = "POST /api/users가 돌려준 userId", example = "3f2b8c1e-7a4d-4e9b-9c2a-1d5e6f7a8b9c")
            @PathVariable String userId
    ) {
        return ApiResponse.of(SuccessType.SUCCESS, userService.getUser(userId));
    }
}
