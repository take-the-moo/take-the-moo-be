package com.hackathon.domain.journey.controller;

import com.hackathon.domain.journey.dto.response.JourneyResponseDto;
import com.hackathon.domain.journey.dto.response.JourneyStoryResponseDto;
import com.hackathon.domain.journey.service.JourneyService;
import com.hackathon.domain.journey.service.JourneyStoryService;
import com.hackathon.global.response.ApiResponse;
import com.hackathon.global.response.ApiResult;
import com.hackathon.global.response.type.SuccessType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Journey", description = "F8 나의 발자취 — 유학을 돌아보는 기간, 숫자, 순간 카드, AI 이야기")
@RestController
@RequestMapping("/api/users/{userId}/journey")
@RequiredArgsConstructor
public class JourneyController {

    private final JourneyService journeyService;
    private final JourneyStoryService journeyStoryService;

    @Operation(summary = "나의 발자취", description = """
            기간, 숫자, 순간 카드를 돌려준다. AI를 부르지 않아 바로 응답한다. 카드는 개수 제한 없이 전부 준다.
            방문 시각과 위치는 내보내지 않는다.
            """)
    @GetMapping
    public ResponseEntity<ApiResult<JourneyResponseDto>> journey(
            @Parameter(description = "로그인한 사용자의 userId (토큰의 사용자와 같아야 한다)") @PathVariable String userId
    ) {
        return ApiResponse.of(SuccessType.SUCCESS, journeyService.getJourney(userId));
    }

    @Operation(summary = "나의 발자취 이야기", description = """
            사용자의 기록으로 쓴 회고 글(모국어 + 쉬운 한국어)을 돌려준다. 저장된 글이 있고 기록이 그대로면 바로 응답하고,
            아니면 새로 만드느라 10초 안팎 걸린다. 기록이 없거나, 저장된 글이 없는데 AI 호출이 실패하면 story는 null이다.
            """)
    @GetMapping("/story")
    public ResponseEntity<ApiResult<JourneyStoryResponseDto>> story(
            @Parameter(description = "로그인한 사용자의 userId (토큰의 사용자와 같아야 한다)") @PathVariable String userId
    ) {
        return ApiResponse.of(SuccessType.SUCCESS, journeyStoryService.getStory(userId));
    }
}
