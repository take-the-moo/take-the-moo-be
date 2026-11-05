package com.hackathon.domain.llm.service;

import com.hackathon.domain.llm.prompt.LlmPrompt;
import com.hackathon.global.config.LlmProperties;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

/**
 * OpenAI 이미지 API 호출은 모두 여기를 거친다 (F7 엽서). LlmClient(Claude)와 같은 규칙을 따른다.
 * - 사진을 입력으로 넣어 다시 그리는 편집 API(/v1/images/edits)를 직접 부른다.
 *   Spring AI OpenAI 스타터는 쓰지 않는다 — 넣으면 ChatModel이 둘이 되어 Claude 자동 설정이 흔들린다
 * - 프롬프트는 prompts.yml(model, system = 이미지 지시문)에 둔다
 * - 목 모드·키 없음·호출 실패면 Optional.empty()를 돌려준다. 호출하는 쪽은 원본 사진으로 대체한다 (시연이 끊기지 않게)
 * - 키·응답 원문은 로그에 남기지 않는다 (OpenAI 오류 메시지에 키 일부가 들어갈 수 있다). 상태 코드만 남긴다
 */
@Slf4j
@Component
public class ImageClient {

    private static final String EDITS_URL = "https://api.openai.com/v1/images/edits";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    // 이미지 생성은 수십 초~1분 넘게 걸린다. 엽서는 비동기라 길게 둔다
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(180);
    // 비용·속도 절충. 결과가 아쉬우면 high로 올린다
    private static final String QUALITY = "medium";

    private final RestClient restClient;
    private final String apiKey;
    private final boolean mock;

    public ImageClient(LlmProperties properties,
                       @Value("${spring.ai.openai.api-key:${OPENAI_API_KEY:}}") String apiKey) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
        this.apiKey = apiKey;
        this.mock = properties.mock();
        if (!mock && !StringUtils.hasText(apiKey)) {
            log.warn("OPENAI_API_KEY가 설정되지 않았습니다. 엽서는 원본 사진으로 대체됩니다.");
        }
    }

    /** 실제 이미지 생성을 할 수 있는지 (목 모드가 아니고 키가 있음) */
    public boolean isAvailable() {
        return !mock && StringUtils.hasText(apiKey);
    }

    /**
     * 사진을 prompt.system() 지시에 따라 다시 그린다.
     *
     * @param image       원본 사진 (jpeg·png·webp)
     * @param contentType 원본 사진 형식 (예: image/jpeg)
     * @param size        결과 크기: "1024x1024", "1024x1536"(세로), "1536x1024"(가로), "auto"
     * @return PNG 바이트. 목 모드·키 없음·실패면 empty
     */
    public Optional<byte[]> redraw(LlmPrompt prompt, byte[] image, String contentType, String size) {
        long start = System.nanoTime();
        if (!isAvailable()) {
            log.info("이미지 생성 건너뜀(대체 이미지 사용): promptKey={}, mock={}", prompt.key(), mock);
            return Optional.empty();
        }

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("model", prompt.model());
        form.add("prompt", prompt.system());
        form.add("size", size);
        form.add("quality", QUALITY);
        form.add("image", imagePart(image, contentType));

        try {
            JsonNode body = restClient.post()
                    .uri(EDITS_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);
            String base64 = body == null ? null : body.path("data").path(0).path("b64_json").asString(null);
            if (!StringUtils.hasText(base64)) {
                log.warn("이미지 생성 응답이 비었습니다: promptKey={}, latencyMs={}", prompt.key(), elapsedMs(start));
                return Optional.empty();
            }
            byte[] result = Base64.getDecoder().decode(base64);
            log.info("이미지 생성 완료: promptKey={}, model={}, latencyMs={}, bytes={}",
                    prompt.key(), prompt.model(), elapsedMs(start), result.length);
            return Optional.of(result);
        } catch (RestClientResponseException e) {
            log.warn("이미지 생성 실패: promptKey={}, model={}, status={}, latencyMs={}",
                    prompt.key(), prompt.model(), e.getStatusCode().value(), elapsedMs(start));
        } catch (RuntimeException e) {
            log.warn("이미지 생성 실패: promptKey={}, model={}, error={}, latencyMs={}",
                    prompt.key(), prompt.model(), e.getClass().getSimpleName(), elapsedMs(start));
        }
        return Optional.empty();
    }

    private static Object imagePart(byte[] image, String contentType) {
        String extension = switch (contentType == null ? "" : contentType.toLowerCase()) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType == null ? "image/jpeg" : contentType));
        ByteArrayResource resource = new ByteArrayResource(image) {
            @Override
            public String getFilename() {
                // 파일 이름이 없으면 multipart 파일 파트로 보내지지 않는다
                return "photo." + extension;
            }
        };
        return new HttpEntity<>(resource, headers);
    }

    private static long elapsedMs(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }
}
