package com.hackathon.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.hackathon.support.TestUsers;
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
        String vn = TestUsers.create(userService, Nationality.VN, Language.VI);
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
        String id = TestUsers.create(userService, Nationality.CN, Language.ZH);
        assertThat(appUserRepository.findById(id)).hasValueSatisfying(user -> assertThat(user.isSample()).isFalse());
    }
    @Test
    void 국적별_샘플_사용자는_하나만_만든다() {
        String first = userService.getOrCreateSampleUser(Nationality.UZ);
        String second = userService.getOrCreateSampleUser(Nationality.UZ);

        assertThat(second).isEqualTo(first);
        assertThat(appUserRepository.findById(first)).hasValueSatisfying(user -> {
            assertThat(user.isSample()).isTrue();
            assertThat(user.getLanguage()).isEqualTo(Language.UZ);
        });
    }

    @Test
    void 국적별_샘플_사용자를_여러_명_만들고_순서는_늘_같다() {
        List<String> three = userService.getOrCreateSampleUsers(Nationality.OTHER, 3);
        List<String> two = userService.getOrCreateSampleUsers(Nationality.OTHER, 2);

        assertThat(three).hasSize(3).doesNotHaveDuplicates();
        assertThat(two).as("앞에서부터 같은 사람").containsExactlyElementsOf(three.subList(0, 2));
        assertThat(userService.getOrCreateSampleUser(Nationality.OTHER)).as("첫 번째 = 한걸음 작성자").isEqualTo(three.getFirst());
        assertThat(userService.getAllSampleUserIds()).containsAll(three);
    }

    @Test
    void 샘플_사용자만_골라낸다() {
        String sample = userService.getOrCreateSampleUser(Nationality.MN);
        String real = TestUsers.create(userService, Nationality.MN, Language.MN);

        assertThat(userService.getSampleUserIds(List.of(sample, real, "no-such-user"))).containsExactly(sample);
    }
}
