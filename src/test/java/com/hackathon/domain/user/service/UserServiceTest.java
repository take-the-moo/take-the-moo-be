package com.hackathon.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.hackathon.domain.user.dto.request.UserCreateRequestDto;
import com.hackathon.domain.user.entity.AppUser;
import com.hackathon.domain.user.repository.AppUserRepository;
import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "llm.mock=true")
class UserServiceTest {

    @Autowired
    UserService userService;

    @Autowired
    AppUserRepository appUserRepository;

    @Test
    void 사용자별_국적을_한번에_조회한다() {
        String vn = userService.create(new UserCreateRequestDto(Nationality.VN, Language.VI)).userId();
        String sample = appUserRepository.save(AppUser.sample(Nationality.JP, Language.JA)).getId();

        Map<String, Nationality> nationalities = userService.getNationalities(List.of(vn, sample, "no-such-user"));

        assertThat(nationalities).containsExactlyInAnyOrderEntriesOf(Map.of(vn, Nationality.VN, sample, Nationality.JP));
    }

    @Test
    void 빈_목록이면_빈_결과() {
        assertThat(userService.getNationalities(List.of())).isEmpty();
    }

    @Test
    void 만든_사용자는_샘플이_아니다() {
        String id = userService.create(new UserCreateRequestDto(Nationality.CN, Language.ZH)).userId();
        assertThat(appUserRepository.findById(id)).hasValueSatisfying(user -> assertThat(user.isSample()).isFalse());
    }
}
