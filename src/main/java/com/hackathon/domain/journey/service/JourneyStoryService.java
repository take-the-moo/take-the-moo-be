package com.hackathon.domain.journey.service;

import com.hackathon.domain.journey.dto.response.JourneyStoryResponseDto;
import com.hackathon.domain.journey.dto.response.JourneyStoryText;
import com.hackathon.domain.journey.entity.JourneyStory;
import com.hackathon.domain.journey.repository.JourneyStoryRepository;
import com.hackathon.domain.llm.exception.LlmErrorType;
import com.hackathon.domain.llm.exception.LlmException;
import com.hackathon.domain.llm.prompt.LlmPrompt;
import com.hackathon.domain.llm.prompt.PromptRegistry;
import com.hackathon.domain.llm.service.LlmClient;
import com.hackathon.domain.review.service.UserReview;
import com.hackathon.domain.user.dto.response.UserResponseDto;
import com.hackathon.domain.user.service.UserService;
import com.hackathon.domain.visit.dto.response.VisitHistoryResponseDto.Stamp;
import com.hackathon.domain.visit.service.JourneyVisits;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/**
 * F8 나의 발자취 이야기. 사용자 × 언어별로 글을 만들어 DB에 캐싱한다.
 * - sourceHash = 모델 + 완성된 system 프롬프트 + 기록 (+ 목 모드 표시). 같으면 AI를 부르지 않는다. 국적은 프롬프트에 들어 해시에 반영된다
 * - 기록이 없으면 AI를 부르지 않는다. AI가 실패하면 이전 글을, 없으면 null을 준다 (화면을 막지 않는다)
 * - AI 호출은 트랜잭션 밖에서 한다. 이름과 아이디는 AI에 보내지 않는다
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JourneyStoryService {

    private static final String PROMPT_KEY = "journey";
    /** 카드는 제한 없이 주지만 AI 입력은 비용 때문에 최근 기록만 넣는다 */
    static final int MAX_INPUT_RECORDS = 30;

    private final JourneyService journeyService;
    private final UserService userService;
    private final JourneyStoryRepository journeyStoryRepository;
    private final PromptRegistry promptRegistry;
    private final LlmClient llmClient;
    private final JsonMapper jsonMapper;

    public JourneyStoryResponseDto getStory(String userId) {
        JourneyRecords records = journeyService.loadRecords(userId);
        if (records.isEmpty()) return new JourneyStoryResponseDto(null);

        UserResponseDto user = userService.getUser(userId);
        LlmPrompt prompt = promptRegistry.resolve(PROMPT_KEY, Map.of(
                "nationality", user.nationality().getKoreanName(),
                "language", user.language().getKoreanName()));
        String input = input(records);
        String sourceHash = sha256(prompt.model() + "\n" + prompt.system() + "\n" + input
                + (llmClient.isMock() ? "\nmock" : ""));

        var saved = journeyStoryRepository.findByUserIdAndLanguage(user.userId(), user.language());
        if (saved.isPresent() && sourceHash.equals(saved.get().getSourceHash())) {
            return new JourneyStoryResponseDto(toText(saved.get()));
        }

        JourneyStoryText text;
        try {
            text = generate(prompt, input, records, user);
        } catch (LlmException e) {
            log.warn("발자취 이야기 생성 실패: language={}, previous={}, code={}",
                    user.language(), saved.isPresent(), e.getErrorCode().getCode());
            return new JourneyStoryResponseDto(saved.map(JourneyStoryService::toText).orElse(null));
        }

        JourneyStory story = saved.orElseGet(() -> JourneyStory.create(user.userId(), user.language()));
        story.update(text.storyNative(), text.storyEasyKorean(), sourceHash);
        try {
            journeyStoryRepository.save(story);
        } catch (DataIntegrityViolationException e) {
            // 보통은 같은 요청이 동시에 와서 다른 요청이 먼저 저장한 경우다. 만든 글은 그대로 돌려준다
            log.warn("발자취 이야기 저장 실패, 저장 없이 응답: language={}, cause={}",
                    user.language(), e.getMostSpecificCause().getMessage());
        }
        return new JourneyStoryResponseDto(text);
    }

    private JourneyStoryText generate(LlmPrompt prompt, String input, JourneyRecords records, UserResponseDto user) {
        JourneyStoryText text = llmClient.entity(prompt, input, JourneyStoryText.class,
                () -> JourneyStoryText.mock(records.visits().stamps().size(), records.reviews().size(), user.language()));
        if (text == null || isBlank(text.storyNative()) || isBlank(text.storyEasyKorean())) {
            throw new LlmException(LlmErrorType.INVALID_OUTPUT, "promptKey=" + PROMPT_KEY + ", 빈 필드");
        }
        return new JourneyStoryText(text.storyNative().strip(), text.storyEasyKorean().strip());
    }

    /** user 메시지로 넘기는 기록. 필드 의미는 prompts.yml journey.system에 적혀 있다. 순서가 늘 같아야 해시가 같다 */
    private String input(JourneyRecords records) {
        JourneyVisits visits = records.visits();
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("startDate", String.valueOf(records.startDate()));
        input.put("endDate", String.valueOf(records.endDate()));
        input.put("stampCount", visits.stamps().size());
        input.put("completedCourseCount", visits.completed().size());
        input.put("reviewCount", records.reviews().size());
        input.put("completedCourses", visits.completed().stream()
                .sorted(Comparator.comparing(JourneyVisits.CompletedCourse::date)
                        .thenComparing(JourneyVisits.CompletedCourse::name))
                .map(course -> Map.of("name", course.name(), "date", course.date().toString()))
                .toList());
        // 도장은 최근 순이므로 앞에서 자른 뒤 오래된 순으로 뒤집는다
        List<Stamp> recentStamps = visits.stamps().stream().limit(MAX_INPUT_RECORDS).toList().reversed();
        input.put("places", recentStamps.stream()
                .map(stamp -> Map.of("name", stamp.name(), "date", stamp.firstVisitedDate().toString()))
                .toList());
        List<UserReview> reviews = records.reviews();
        input.put("reviews", reviews.subList(Math.max(0, reviews.size() - MAX_INPUT_RECORDS), reviews.size()).stream()
                .map(review -> Map.of(
                        "place", records.places().get(review.placeId()).getName(),
                        "date", review.date().toString(),
                        "content", review.content()))
                .toList());
        return jsonMapper.writeValueAsString(input);
    }

    private static JourneyStoryText toText(JourneyStory story) {
        return new JourneyStoryText(story.getStoryNative(), story.getStoryEasyKorean());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
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
