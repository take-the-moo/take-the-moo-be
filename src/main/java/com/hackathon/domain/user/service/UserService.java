package com.hackathon.domain.user.service;

import com.hackathon.domain.user.dto.request.UserCreateRequestDto;
import com.hackathon.domain.user.dto.response.UserResponseDto;
import com.hackathon.domain.user.entity.AppUser;
import com.hackathon.domain.user.exception.UserErrorType;
import com.hackathon.domain.user.exception.UserException;
import com.hackathon.domain.user.repository.AppUserRepository;
import com.hackathon.global.common.Nationality;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    /** 모든 샘플 사용자 id. 샘플 방문을 지우고 다시 넣을 때 쓴다 */
    public Set<String> getAllSampleUserIds() {
        return appUserRepository.findAllBySampleTrue().stream()
                .map(AppUser::getId)
                .collect(Collectors.toSet());
    }

    /** 주어진 사용자 중 샘플 사용자 id (화면에서 샘플 표시용). 없는 userId는 빠진다 */
    public Set<String> getSampleUserIds(Collection<String> userIds) {
        if (userIds.isEmpty()) return Set.of();
        return appUserRepository.findAllByIdInAndSampleTrue(userIds).stream()
                .map(AppUser::getId)
                .collect(Collectors.toSet());
    }

    /**
     * 국적별 첫 번째 샘플 사용자 id. 없으면 만든다 (언어는 그 국적의 기본 언어).
     * 샘플 한걸음(F6)을 쓴 사람이다. 샘플 방문(F5·F6)의 첫 번째 방문자와 같은 사람이라 한걸음 작성자도 지도에 방문자로 잡힌다.
     */
    @Transactional
    public String getOrCreateSampleUser(Nationality nationality) {
        return getOrCreateSampleUsers(nationality, 1).getFirst();
    }

    /**
     * 국적별 샘플 사용자 count명의 id. 모자라면 만든다. 순서는 늘 같다 (만든 순 → id 순)
     * 지도에서 한 국적이 여러 명으로 보이도록 샘플 방문이 쓴다.
     */
    @Transactional
    public List<String> getOrCreateSampleUsers(Nationality nationality, int count) {
        List<String> ids = sampleUserIds(nationality);
        if (ids.size() < count) {
            for (int i = ids.size(); i < count; i++) {
                appUserRepository.save(AppUser.sample(nationality, nationality.getDefaultLanguage()));
            }
            // 한 번에 만든 사용자는 만든 시각이 같을 수 있어 id 순으로 정해진다. 다시 읽어 다음 호출과 같은 순서로 돌려준다
            appUserRepository.flush();
            ids = sampleUserIds(nationality);
        }
        return List.copyOf(ids.subList(0, count));
    }

    private List<String> sampleUserIds(Nationality nationality) {
        return appUserRepository.findAllBySampleTrueAndNationalityOrderByCreatedAtAscIdAsc(nationality).stream()
                .map(AppUser::getId)
                .toList();
    }
}
