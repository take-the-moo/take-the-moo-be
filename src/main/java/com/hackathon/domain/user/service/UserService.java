package com.hackathon.domain.user.service;

import com.hackathon.domain.user.dto.request.UserCreateRequestDto;
import com.hackathon.domain.user.dto.response.UserResponseDto;
import com.hackathon.domain.user.entity.AppUser;
import com.hackathon.domain.user.exception.UserErrorType;
import com.hackathon.domain.user.exception.UserException;
import com.hackathon.domain.user.repository.AppUserRepository;
import com.hackathon.global.common.Nationality;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 익명 사용자 생성·조회. 다른 도메인은 이 서비스로 국적·언어를 얻는다 (repository를 직접 쓰지 않는다).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final AppUserRepository appUserRepository;

    @Transactional
    public UserResponseDto create(UserCreateRequestDto request) {
        AppUser user = appUserRepository.save(AppUser.create(request.nationality(), request.language()));
        return UserResponseDto.from(user);
    }

    /** 없는 userId면 404 USER_NOT_FOUND */
    public UserResponseDto getUser(String userId) {
        return appUserRepository.findById(userId)
                .map(UserResponseDto::from)
                .orElseThrow(() -> new UserException(UserErrorType.NOT_FOUND, "userId=" + userId));
    }

    /**
     * 사용자별 국적 (F6 지도 핀·한걸음 표시). 국적을 다른 테이블에 복사하지 않고 표시할 때 여기서 조회한다.
     * 없는 userId는 결과에서 빠진다.
     */
    public Map<String, Nationality> getNationalities(Collection<String> userIds) {
        if (userIds.isEmpty()) return Map.of();
        return appUserRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(AppUser::getId, AppUser::getNationality));
    }
}
