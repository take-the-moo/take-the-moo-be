package com.hackathon.domain.user.service;

import com.hackathon.domain.user.dto.request.LoginRequestDto;
import com.hackathon.domain.user.dto.request.SignUpRequestDto;
import com.hackathon.domain.user.dto.response.AuthResponseDto;
import com.hackathon.domain.user.dto.response.LoginIdCheckResponseDto;
import com.hackathon.domain.user.dto.response.UserResponseDto;
import com.hackathon.domain.user.exception.UserErrorType;
import com.hackathon.domain.user.exception.UserException;
import com.hackathon.global.auth.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * 앱 내 로그인: 아이디 중복 확인·회원가입·로그인. 가입·로그인이 성공하면 JWT를 발급한다.
 * 사용자 데이터는 UserService가 다룬다 (트랜잭션은 거기). 비밀번호·토큰은 로그에 남기지 않는다.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtProvider jwtProvider;

    public LoginIdCheckResponseDto checkLoginId(String loginId) {
        return new LoginIdCheckResponseDto(loginId, userService.isLoginIdAvailable(loginId));
    }

    public AuthResponseDto signUp(SignUpRequestDto request) {
        UserResponseDto user;
        try {
            user = userService.signUp(request.loginId(), request.password(), request.name().strip(),
                    request.nationality(), request.language());
        } catch (DataIntegrityViolationException e) {
            // 같은 아이디로 동시에 가입해 다른 요청이 먼저 저장했다
            throw new UserException(UserErrorType.LOGIN_ID_TAKEN);
        }
        return issue(user);
    }

    public AuthResponseDto login(LoginRequestDto request) {
        return issue(userService.checkCredentials(request.loginId(), request.password()));
    }

    private AuthResponseDto issue(UserResponseDto user) {
        JwtProvider.IssuedToken token = jwtProvider.issue(user.userId());
        return new AuthResponseDto(token.token(), token.expiresAt(), user);
    }
}
