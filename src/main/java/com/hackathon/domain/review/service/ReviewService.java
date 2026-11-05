package com.hackathon.domain.review.service;

import com.hackathon.domain.llm.exception.LlmErrorType;
import com.hackathon.domain.llm.exception.LlmException;
import com.hackathon.domain.llm.prompt.LlmPrompt;
import com.hackathon.domain.llm.prompt.PromptRegistry;
import com.hackathon.domain.llm.service.LlmClient;
import com.hackathon.domain.photo.service.PhotoService;
import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.service.PlaceService;
import com.hackathon.domain.review.dto.request.ReviewCreateRequestDto;
import com.hackathon.domain.review.dto.response.ModerationResult;
import com.hackathon.domain.review.dto.response.ReviewListResponseDto;
import com.hackathon.domain.review.dto.response.ReviewResponseDto;
import com.hackathon.domain.review.entity.Review;
import com.hackathon.domain.review.entity.ReviewTranslation;
import com.hackathon.domain.review.exception.ReviewErrorType;
import com.hackathon.domain.review.exception.ReviewException;
import com.hackathon.domain.review.repository.ReviewRepository;
import com.hackathon.domain.review.repository.ReviewTranslationRepository;
import com.hackathon.domain.user.dto.response.UserResponseDto;
import com.hackathon.domain.user.service.UserService;
import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/**
 * F6 한걸음. 작성 전에 두 번 거른다.
 * 1. ReviewContentRules: 링크·이메일·전화번호 (AI 없이, 목 모드에서도 동작)
 * 2. moderate 프롬프트: 욕설·혐오·광고·스팸 + 실제 작성 언어 감지
 * 걸리면 422 REVIEW_INAPPROPRIATE이고 저장하지 않는다. 후기 원문은 로그·예외 detail에 남기지 않는다.
 * 목록은 보는 사람 언어로 번역해 review_translation에 캐싱한다. 번역이 실패해도 목록은 원문으로 돌려준다.
 * AI 호출은 트랜잭션 밖에서 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final String MODERATE_PROMPT_KEY = "moderate";
    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 50;

    private final ReviewRepository reviewRepository;
    private final ReviewTranslationRepository reviewTranslationRepository;
    private final ReviewTranslator reviewTranslator;
    private final UserService userService;
    private final PlaceService placeService;
    private final PhotoService photoService;
    private final PromptRegistry promptRegistry;
    private final LlmClient llmClient;
    private final JsonMapper jsonMapper;

    public ReviewResponseDto create(Long placeId, ReviewCreateRequestDto request) {
        UserResponseDto user = userService.getUser(request.userId());
        Place place = placeService.getActivePlace(placeId);
        // AI 검사 전에 확인한다 (남의 사진이면 403 PHOTO_FORBIDDEN, 없으면 404 PHOTO_NOT_FOUND)
        String photoId = request.photoId() == null || request.photoId().isBlank() ? null
                : photoService.getOwnedPhoto(user.userId(), request.photoId().strip()).photoId();
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
        Review review = reviewRepository.save(Review.create(place.getId(), user.userId(), content, language, photoId));
        return ReviewResponseDto.of(review, user.nationality());
    }

    /**
     * 장소의 한걸음 최신순. 보는 사람 언어 = language, 없으면 userId의 언어, 둘 다 없으면 한국어.
     * 원문 언어가 다른 것만 번역하고, 저장된 번역이 없는 것만 한 번에 묶어 AI로 번역한다.
     */
    public ReviewListResponseDto list(Long placeId, String userId, Language language, Integer limit) {
        Place place = placeService.getActivePlace(placeId);
        Language viewer = language != null ? language
                : userId != null && !userId.isBlank() ? userService.getUser(userId).language()
                : Language.KO;
        int size = limit == null ? DEFAULT_LIMIT : Math.clamp(limit, 1, MAX_LIMIT);

        List<Review> reviews = reviewRepository.findByPlaceIdOrderByCreatedAtDescIdDesc(place.getId(),
                PageRequest.of(0, size));
        Map<String, Nationality> nationalities = userService.getNationalities(
                reviews.stream().map(Review::getUserId).distinct().toList());
        Map<Long, String> translations = translations(reviews, viewer);

        List<ReviewListResponseDto.Item> items = reviews.stream()
                .map(review -> ReviewListResponseDto.Item.of(review, translations.get(review.getId()),
                        nationalities.get(review.getUserId())))
                .toList();
        return new ReviewListResponseDto(place.getId(), viewer, items);
    }

    /** reviewId → 번역문. 원문 언어가 viewer와 같은 것은 들어 있지 않다 */
    private Map<Long, String> translations(List<Review> reviews, Language viewer) {
        List<Review> foreign = reviews.stream().filter(review -> review.getLanguage() != viewer).toList();
        if (foreign.isEmpty()) return Map.of();

        Map<Long, String> result = new HashMap<>();
        reviewTranslationRepository.findAllByReviewIdInAndLanguage(foreign.stream().map(Review::getId).toList(), viewer)
                .forEach(saved -> result.put(saved.getReviewId(), saved.getTranslatedContent()));

        List<Review> missing = foreign.stream().filter(review -> !result.containsKey(review.getId())).toList();
        if (missing.isEmpty()) return result;

        List<String> translated;
        try {
            translated = reviewTranslator.translate(missing.stream().map(Review::getContent).toList(), viewer);
        } catch (LlmException e) {
            // 목록은 보여 줘야 하므로 번역 없이 원문으로 돌려준다 (다음 요청 때 다시 번역한다)
            log.warn("한걸음 번역 실패, 원문으로 응답: count={}, language={}, code={}",
                    missing.size(), viewer, e.getErrorCode().getCode());
            return result;
        }
        for (int i = 0; i < missing.size(); i++) {
            ReviewTranslation translation = ReviewTranslation.create(missing.get(i).getId(), viewer, translated.get(i));
            try {
                reviewTranslationRepository.save(translation);
            } catch (DataIntegrityViolationException e) {
                // 같은 목록을 동시에 요청해 다른 요청이 먼저 저장했다. 만든 번역은 그대로 쓴다
                log.info("한걸음 번역 저장 충돌: reviewId={}, language={}", missing.get(i).getId(), viewer);
            }
            result.put(missing.get(i).getId(), translation.getTranslatedContent());
        }
        return result;
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
