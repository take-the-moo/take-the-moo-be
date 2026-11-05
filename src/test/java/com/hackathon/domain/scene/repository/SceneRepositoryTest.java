package com.hackathon.domain.scene.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.hackathon.domain.scene.entity.Scene;
import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 실제 AI 이야기는 목 응답보다 훨씬 길다 (한국어·베트남어 4문장 ≈ 1KB 이상). 컬럼이 짧게 만들어지면 저장이 실패해 캐시가 안 된다.
 */
@SpringBootTest(properties = "llm.mock=true")
class SceneRepositoryTest {

    @Autowired
    SceneRepository sceneRepository;

    @Test
    void 긴_이야기도_저장된다() {
        String longStory = "이곳은 기억과 평화의 공간이에요. ".repeat(150);
        Scene scene = Scene.create(777_001L, Nationality.VN, Language.VI);
        scene.update(longStory, longStory, "한 줄", "a".repeat(64));

        Scene saved = sceneRepository.saveAndFlush(scene);

        assertThat(sceneRepository.findById(saved.getId()))
                .hasValueSatisfying(found -> assertThat(found.getStoryNative()).isEqualTo(longStory));
    }
}
