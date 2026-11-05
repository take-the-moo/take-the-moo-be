package com.hackathon.domain.llm.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.hackathon.domain.llm.prompt.LlmPrompt;
import com.hackathon.global.config.LlmProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ImageClientTest {

    private static final LlmPrompt PROMPT = new LlmPrompt("postcard-image", "gpt-image-1", null, null, "redraw");

    @Test
    void 목_모드면_호출하지_않고_비어_있다() {
        ImageClient client = new ImageClient(properties(true), "test-key");

        assertThat(client.isAvailable()).isFalse();
        assertThat(client.redraw(PROMPT, new byte[]{1}, "image/jpeg", "1024x1024")).isEmpty();
    }

    @Test
    void 키가_없으면_호출하지_않고_비어_있다() {
        ImageClient client = new ImageClient(properties(false), "");

        assertThat(client.isAvailable()).isFalse();
        assertThat(client.redraw(PROMPT, new byte[]{1}, "image/jpeg", "1024x1024")).isEmpty();
    }

    private static LlmProperties properties(boolean mock) {
        return new LlmProperties(mock,
                new LlmProperties.Timeout(Duration.ofSeconds(5), Duration.ofSeconds(60), Duration.ofSeconds(120)), 1);
    }
}
