package com.hackathon.domain.place.service;

import com.hackathon.domain.llm.exception.LlmErrorType;
import com.hackathon.domain.llm.exception.LlmException;
import com.hackathon.domain.llm.prompt.LlmPrompt;
import com.hackathon.domain.llm.prompt.PromptRegistry;
import com.hackathon.domain.llm.service.LlmClient;
import com.hackathon.domain.place.dto.request.ExplanationRequestDto;
import com.hackathon.domain.place.dto.response.ExplanationResponseDto;
import com.hackathon.domain.place.dto.response.ExplanationTranslationResult;
import com.hackathon.domain.place.entity.ExplanationTranslation;
import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.exception.PlaceErrorType;
import com.hackathon.domain.place.exception.PlaceException;
import com.hackathon.domain.place.repository.ExplanationTranslationRepository;
import com.hackathon.global.common.Language;
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
 * F4 현장 해설. 저장된 해설 원문(Place.fieldExplanation)을 사용자 언어로 번역만 한다 (translate-explanation).
 * - KO면 AI 없이 원문을 그대로 준다
 * - 캐시 키는 (placeId, language). 저장된 sourceHash가 지금 입력의 해시와 같으면 AI를 부르지 않는다
 * - sourceHash = 모델 + 완성된 system 프롬프트 + 원문 입력 (+ 목 모드 표시). 원문이나 프롬프트가 바뀌면 다시 번역한다 (SceneService와 같은 방식)
 * - AI 호출은 트랜잭션 밖에서 한다 (DB 커넥션을 수 초간 잡지 않는다)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExplanationService {

    private static final String PROMPT_KEY = "translate-explanation";

    private final PlaceService placeService;
    private final ExplanationTranslationRepository translationRepository;
    private final PromptRegistry promptRegistry;
    private final LlmClient llmClient;
    private final JsonMapper jsonMapper;

    public ExplanationResponseDto explain(ExplanationRequestDto request) {
        Place place = placeService.getActivePlace(request.placeId());
        String original = place.fieldExplanation();
        if (original == null) {
            throw new PlaceException(PlaceErrorType.EXPLANATION_MISSING, "placeId=" + place.getId());
        }
        Language language = request.language();
        if (language == Language.KO) {
            return response(place, language, original, original, false);
        }

        LlmPrompt prompt = promptRegistry.resolve(PROMPT_KEY, Map.of("language", language.getKoreanName()));
        String input = input(place, original);
        String sourceHash = sha256(prompt.model() + "\n" + prompt.system() + "\n" + input
                + (llmClient.isMock() ? "\nmock" : ""));

        Optional<ExplanationTranslation> saved = translationRepository.findByPlaceIdAndLanguage(place.getId(), language);
        if (saved.isPresent() && sourceHash.equals(saved.get().getSourceHash())) {
            return response(place, language, saved.get().getTranslatedText(), original, true);
        }

        ExplanationTranslationResult result = llmClient.entity(prompt, input, ExplanationTranslationResult.class,
                () -> ExplanationTranslationResult.mock(original));
        if (result == null || result.text() == null || result.text().isBlank()) {
            throw new LlmException(LlmErrorType.INVALID_OUTPUT, "promptKey=" + PROMPT_KEY + ", 빈 번역");
        }
        String text = result.text().strip();

        ExplanationTranslation translation = saved.orElseGet(() -> ExplanationTranslation.create(place.getId(), language));
        translation.update(text, sourceHash);
        try {
            translationRepository.save(translation);
        } catch (DataIntegrityViolationException e) {
            // 보통은 같은 장소·언어를 동시에 요청해 다른 요청이 먼저 저장한 경우다. 만든 번역은 그대로 돌려준다
            log.warn("해설 번역 캐시 저장 실패, 저장 없이 응답: placeId={}, language={}, cause={}",
                    place.getId(), language, e.getMostSpecificCause().getMessage());
        }
        return response(place, language, text, original, false);
    }

    /** user 메시지로 넘기는 입력. 필드 의미는 prompts.yml translate-explanation.system에 적혀 있다 */
    private String input(Place place, String original) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("name", place.getName());
        input.put("source", original);
        return jsonMapper.writeValueAsString(input);
    }

    private static ExplanationResponseDto response(Place place, Language language, String text, String original,
                                                   boolean cached) {
        return new ExplanationResponseDto(place.getId(), place.getName(), language, text, original, cached);
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
