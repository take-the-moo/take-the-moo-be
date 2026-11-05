package com.hackathon.domain.review.service;

import com.hackathon.domain.llm.exception.LlmErrorType;
import com.hackathon.domain.llm.exception.LlmException;
import com.hackathon.domain.llm.prompt.LlmPrompt;
import com.hackathon.domain.llm.prompt.PromptRegistry;
import com.hackathon.domain.llm.service.LlmClient;
import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.service.PlaceService;
import com.hackathon.domain.review.dto.request.ReviewCreateRequestDto;
import com.hackathon.domain.review.dto.response.ModerationResult;
import com.hackathon.domain.review.dto.response.ReviewResponseDto;
import com.hackathon.domain.review.entity.Review;
import com.hackathon.domain.review.exception.ReviewErrorType;
import com.hackathon.domain.review.exception.ReviewException;
import com.hackathon.domain.review.repository.ReviewRepository;
import com.hackathon.domain.user.dto.response.UserResponseDto;
import com.hackathon.domain.user.service.UserService;
import com.hackathon.global.common.Language;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/**
 * F6 한걸음. 작성 전에 두 번 거른다.
 * 1. ReviewContentRules: 링크·이메일·전화번호 (AI 없이, 목 모드에서도 동작)
 * 2. moderate 프롬프트: 욕설·혐오·광고·스팸 + 실제 작성 언어 감지
 * 걸리면 422 REVIEW_INAPPROPRIATE이고 저장하지 않는다. 후기 원문은 로그·예외 detail에 남기지 않는다.
 * AI 호출은 트랜잭션 밖에서 한다.
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final String MODERATE_PROMPT_KEY = "moderate";

    private final ReviewRepository reviewRepository;
    private final UserService userService;
    private final PlaceService placeService;
    private final PromptRegistry promptRegistry;
    private final LlmClient llmClient;
    private final JsonMapper jsonMapper;

    public ReviewResponseDto create(Long placeId, ReviewCreateRequestDto request) {
        UserResponseDto user = userService.getUser(request.userId());
        Place place = placeService.getActivePlace(placeId);
        String content = request.content().strip();

        if (ReviewContentRules.isBlocked(content)) {
            throw new ReviewException(ReviewErrorType.INAPPROPRIATE, "placeId=" + place.getId() + ", rule=link·email·phone");
        }

        ModerationResult moderation = moderate(content, request.language());
        if (!moderation.allowed()) {
            throw new ReviewException(ReviewErrorType.INAPPROPRIATE,
                    "placeId=" + place.getId() + ", reason=" + moderation.reason());
        }

        Language language = request.language() != null ? request.language() : moderation.language();
        Review review = reviewRepository.save(Review.create(place.getId(), user.userId(), content, language));
        return ReviewResponseDto.of(review, user.nationality());
    }

    private ModerationResult moderate(String content, Language hint) {
        LlmPrompt prompt = promptRegistry.resolve(MODERATE_PROMPT_KEY, Map.of());
        String input = jsonMapper.writeValueAsString(Map.of("content", content));
        ModerationResult result = llmClient.entity(prompt, input, ModerationResult.class,
                () -> ModerationResult.mock(hint));
        if (result == null || (result.allowed() && result.language() == null && hint == null)) {
            throw new LlmException(LlmErrorType.INVALID_OUTPUT, "promptKey=" + MODERATE_PROMPT_KEY + ", 언어 없음");
        }
        return result;
    }
}
