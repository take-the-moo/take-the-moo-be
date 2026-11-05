package com.hackathon.domain.user.controller;

import com.hackathon.domain.user.dto.request.LoginIdRule;
import com.hackathon.domain.user.dto.request.LoginRequestDto;
import com.hackathon.domain.user.dto.request.SignUpRequestDto;
import com.hackathon.domain.user.dto.response.AuthResponseDto;
import com.hackathon.domain.user.dto.response.LoginIdCheckResponseDto;
import com.hackathon.domain.user.service.AuthService;
import com.hackathon.global.response.ApiResponse;
import com.hackathon.global.response.ApiResult;
import com.hackathon.global.response.type.SuccessType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "F1 앱 내 로그인 — 아이디·비밀번호 가입과 로그인 (JWT)")
@Validated
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "아이디 중복 확인", description = "가입 화면에서 아이디를 입력할 때 부른다. 형식이 틀리면 422.")
    @GetMapping("/check-id")
    public ResponseEntity<ApiResult<LoginIdCheckResponseDto>> checkId(
            @Parameter(description = "확인할 아이디 (영문 소문자·숫자·밑줄 4~20자)", example = "linh_2026")
            @RequestParam @NotBlank(message = "아이디를 입력해 주세요.")
            @Pattern(regexp = LoginIdRule.REGEX, message = LoginIdRule.MESSAGE) String loginId
    ) {
        return ApiResponse.of(SuccessType.SUCCESS, authService.checkLoginId(loginId));
    }

    @Operation(summary = "회원가입", description = "가입하면 바로 로그인되어 토큰을 준다. 아이디가 이미 있으면 409.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResult<AuthResponseDto>> signUp(@Valid @RequestBody SignUpRequestDto request) {
        return ApiResponse.of(SuccessType.CREATED, authService.signUp(request));
    }

    @Operation(summary = "로그인", description = "토큰을 준다. 아이디가 없거나 비밀번호가 틀리면 같은 401.")
    @PostMapping("/login")
    public ResponseEntity<ApiResult<AuthResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        return ApiResponse.of(SuccessType.SUCCESS, authService.login(request));
    }
}
