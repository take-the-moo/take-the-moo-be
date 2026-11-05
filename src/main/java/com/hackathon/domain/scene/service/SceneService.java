package com.hackathon.domain.scene.service;

import com.hackathon.domain.llm.exception.LlmErrorType;
import com.hackathon.domain.llm.exception.LlmException;
import com.hackathon.domain.llm.prompt.LlmPrompt;
import com.hackathon.domain.llm.prompt.PromptRegistry;
import com.hackathon.domain.llm.service.LlmClient;
import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.service.PlaceService;
import com.hackathon.domain.scene.dto.request.SceneRequestDto;
import com.hackathon.domain.scene.dto.response.SceneResponseDto;
import com.hackathon.domain.scene.dto.response.SceneStory;
import com.hackathon.domain.scene.entity.Scene;
import com.hackathon.domain.scene.exception.SceneErrorType;
import com.hackathon.domain.scene.exception.SceneException;
import com.hackathon.domain.scene.repository.SceneRepository;
import com.hackathon.global.common.Nationality;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/**
 * F3 맞춤 장면. 장소 × 관점 국적 × 언어별로 이야기를 만들고 DB에 캐싱한다.
 * - 캐시 키는 (placeId, nationality, language). 저장된 sourceHash가 지금 입력의 해시와 같으면 AI를 부르지 않는다
 * - sourceHash = 모델 + 완성된 system 프롬프트 + 장소 입력(내 나라 연결 포함) (+ 목 모드 표시). 장소 설명·연결·프롬프트가 바뀌면 다시 만든다
 * - AI 호출은 트랜잭션 밖에서 한다 (DB 커넥션을 수 초간 잡지 않는다)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SceneService {

    private static final String PROMPT_KEY = "scene";
    private static final int CONNECTION_LINE_MAX = 300;

    private final PlaceService placeService;
    private final SceneRepository sceneRepository;
    private final PromptRegistry promptRegistry;
    private final LlmClient llmClient;
    private final JsonMapper jsonMapper;

    public SceneResponseDto getScene(SceneRequestDto request) {
        Place place = placeService.getActivePlace(request.placeId());
        String source = place.fieldExplanation();
        if (source == null) {
            throw new SceneException(SceneErrorType.SOURCE_MISSING, "placeId=" + place.getId());
        }

        LlmPrompt prompt = promptRegistry.resolve(PROMPT_KEY, Map.of(
                "nationality", request.nationality().getKoreanName(),
                "language", request.language().getKoreanName()));
        String input = placeInput(place, source, request.nationality());
        String sourceHash = sha256(prompt.model() + "\n" + prompt.system() + "\n" + input
                + (llmClient.isMock() ? "\nmock" : ""));

        Optional<Scene> saved = sceneRepository.findByPlaceIdAndNationalityAndLanguage(
                place.getId(), request.nationality(), request.language());
        if (saved.isPresent() && sourceHash.equals(saved.get().getSourceHash())) {
            return SceneResponseDto.of(saved.get(), place.getName(), true);
        }

        SceneStory story = llmClient.entity(prompt, input, SceneStory.class,
                () -> SceneStory.mock(place.getName(), request.nationality(), request.language()));
        validate(story);

        Scene scene = saved.orElseGet(() -> Scene.create(place.getId(), request.nationality(), request.language()));
        scene.update(story.storyNative().strip(), story.storyEasyKorean().strip(),
                truncate(story.connectionLine().strip(), CONNECTION_LINE_MAX), sourceHash);
        try {
            scene = sceneRepository.save(scene);
        } catch (DataIntegrityViolationException e) {
            // 보통은 같은 조합을 동시에 요청해 다른 요청이 먼저 저장한 경우다. 만든 이야기는 그대로 돌려준다
            // 그 밖의 원인(길이 초과 등)이면 캐시가 계속 안 되므로 WARN으로 원인을 남긴다
            log.warn("장면 캐시 저장 실패, 저장 없이 응답: placeId={}, nationality={}, language={}, cause={}",
                    place.getId(), request.nationality(), request.language(),
                    e.getMostSpecificCause().getMessage());
        }
        return SceneResponseDto.of(scene, place.getName(), false);
    }

    /**
     * user 메시지로 넘기는 장소 정보. 필드 의미는 prompts.yml scene.system에 적혀 있다.
     * connection(사람이 검토한 내 나라 연결)도 해시에 들어가므로 연결 데이터를 고치면 그 조합의 장면을 다시 만든다
     */
    private String placeInput(Place place, String source, Nationality nationality) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("name", place.getName());
        input.put("address", place.getAddress());
        input.put("sensitiveHistory", place.isSensitiveHistory());
        input.put("source", source);
        input.put("connection", placeService.findConnection(place, nationality).orElse(null));
        return jsonMapper.writeValueAsString(input);
    }

    private static void validate(SceneStory story) {
        if (story == null || isBlank(story.storyNative()) || isBlank(story.storyEasyKorean())
                || isBlank(story.connectionLine())) {
            throw new LlmException(LlmErrorType.INVALID_OUTPUT, "promptKey=" + PROMPT_KEY + ", 빈 필드");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 쓸 수 없습니다", e);
        }
    }
}
