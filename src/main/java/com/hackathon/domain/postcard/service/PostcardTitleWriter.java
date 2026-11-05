package com.hackathon.domain.postcard.service;

import com.hackathon.domain.llm.exception.LlmException;
import com.hackathon.domain.llm.prompt.LlmPrompt;
import com.hackathon.domain.llm.prompt.PromptRegistry;
import com.hackathon.domain.llm.service.LlmClient;
import com.hackathon.domain.postcard.dto.response.PostcardTitle;
import com.hackathon.global.common.Language;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * 엽서 제목 한 줄: 사용자 언어로 "날짜, 어디서" (예: 2026년 11월 7일, 독립문에서).
 * 장소 이름을 사용자 언어로 옮겨야 해서 Claude(postcard-title)를 한 번 부른다. 엽서 생성이 백그라운드라 시간 부담이 없다.
 * 목 모드·호출 실패면 날짜만 언어별 형식으로 쓰고 장소 이름은 한국어 그대로 쓴다 (엽서는 멈추지 않는다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostcardTitleWriter {

    static final String PROMPT_KEY = "postcard-title";
    /** 사진에 장소가 없을 때 */
    static final String DEFAULT_PLACE = "서대문";
    private static final int MAX_LENGTH = 60;

    private final PromptRegistry promptRegistry;
    private final LlmClient llmClient;
    private final JsonMapper jsonMapper;

    public String write(LocalDate date, String placeName, Language language) {
        String place = placeName == null || placeName.isBlank() ? DEFAULT_PLACE : placeName;
        String fallback = fallback(date, place, language);

        LlmPrompt prompt = promptRegistry.resolve(PROMPT_KEY, Map.of("language", language.getKoreanName()));
        Map<String, String> input = new LinkedHashMap<>();
        input.put("date", date.toString());
        input.put("place", place);
        try {
            PostcardTitle result = llmClient.entity(prompt, jsonMapper.writeValueAsString(input), PostcardTitle.class,
                    () -> new PostcardTitle(fallback));
            String title = result == null || result.title() == null ? "" : result.title().strip();
            if (title.isEmpty() || title.length() > MAX_LENGTH || title.contains("\n")) {
                log.warn("엽서 제목이 형식에 맞지 않아 기본 제목을 씁니다: language={}, length={}", language, title.length());
                return fallback;
            }
            return title;
        } catch (LlmException e) {
            log.warn("엽서 제목 생성 실패, 기본 제목을 씁니다: language={}, code={}", language, e.getErrorCode().getCode());
            return fallback;
        }
    }

    /** 날짜는 언어별 긴 형식, 장소는 한국어 그대로 */
    static String fallback(LocalDate date, String place, Language language) {
        String formatted = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale(language)));
        return formatted + " · " + place;
    }

    private static Locale locale(Language language) {
        return Locale.forLanguageTag(language.name().toLowerCase());
    }
}
