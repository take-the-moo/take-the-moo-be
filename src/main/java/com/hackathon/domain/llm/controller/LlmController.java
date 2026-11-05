package com.hackathon.domain.llm.controller;

import com.hackathon.domain.llm.dto.request.AnalyzeRequestDto;
import com.hackathon.domain.llm.dto.request.ChatRequestDto;
import com.hackathon.domain.llm.dto.request.ImageFile;
import com.hackathon.domain.llm.dto.response.AnalysisResult;
import com.hackathon.domain.llm.dto.response.ChatResponseDto;
import com.hackathon.domain.llm.dto.response.StreamEvent;
import com.hackathon.domain.llm.exception.LlmErrorType;
import com.hackathon.domain.llm.exception.LlmException;
import com.hackathon.domain.llm.service.LlmService;
import com.hackathon.global.response.ApiResponse;
import com.hackathon.global.response.ApiResult;
import com.hackathon.global.response.type.SuccessType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

@Slf4j
@Tag(name = "LLM", description = "prompts.yml의 promptKey로 동작하는 범용 AI API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LlmController {

    private final LlmService llmService;

    @Operation(summary = "텍스트 채팅", description = "promptKey의 시스템 프롬프트로 한 번 묻고 답을 받는다.")
    @PostMapping("/chat")
    public ResponseEntity<ApiResult<ChatResponseDto>> chat(@Valid @RequestBody ChatRequestDto request) {
        return ApiResponse.of(SuccessType.SUCCESS, llmService.chat(request));
    }

    @Operation(
            summary = "텍스트 채팅 (SSE 스트리밍)",
            description = """
                    text/event-stream으로 토큰을 흘려보낸다. 이벤트 종류:
                    - `token`: data `{"text": "..."}` — 받은 순서대로 이어 붙인다
                    - `done`: data `{"model", "usage": {"inputTokens", "outputTokens"}, "latencyMs"}` — 마지막 이벤트
                    - `error`: data `{"code", "message"}` — 스트림 중 오류. 이 이벤트 뒤 스트림이 닫힌다

                    스트림 시작 전 오류(검증 실패, 없는 promptKey)는 일반 API처럼 ApiResult JSON으로 응답한다.
                    """)
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "SSE 스트림",
            content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE, examples = @ExampleObject(value = """
                    event:token
                    data:{"text":"안녕"}

                    event:token
                    data:{"text":"하세요"}

                    event:done
                    data:{"model":"claude-haiku-5-5","usage":{"inputTokens":21,"outputTokens":5},"latencyMs":812}
                    """)))
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<StreamEvent>> stream(
            @Parameter(description = "prompts.yml의 키", example = "chat")
            @RequestParam(defaultValue = "chat") @Size(max = 50, message = "50자 이하로 입력해 주세요.") String promptKey,
            @Parameter(description = "사용자 메시지", example = "자기소개 해줘")
            @RequestParam @NotBlank(message = "메시지를 입력해 주세요.")
            @Size(max = 10_000, message = "10000자 이하로 입력해 주세요.") String message
    ) {
        return llmService.stream(promptKey, message)
                .onErrorResume(e -> Flux.just(errorEvent(e)))
                .map(event -> ServerSentEvent.<StreamEvent>builder().event(event.name()).data(event).build());
    }

    @Operation(summary = "구조화 분석", description = "입력을 분석해 정해진 JSON 구조(AnalysisResult)로 돌려준다.")
    @PostMapping("/analyze")
    public ResponseEntity<ApiResult<AnalysisResult>> analyze(@Valid @RequestBody AnalyzeRequestDto request) {
        return ApiResponse.of(SuccessType.SUCCESS, llmService.analyze(request));
    }

    @Operation(summary = "이미지 질문", description = "이미지(5MB 이하 jpeg/png/webp)와 질문을 보내면 텍스트로 답한다. 응답 형식은 /api/chat과 같다.")
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResult<ChatResponseDto>> image(
            @Parameter(description = "이미지 파일 (5MB 이하, jpeg/png/webp)")
            @RequestPart("file") @ImageFile MultipartFile file,
            @Parameter(description = "이미지에 대한 질문", example = "이 사진에 뭐가 보여?")
            @RequestParam @NotBlank(message = "질문을 입력해 주세요.")
            @Size(max = 2_000, message = "2000자 이하로 입력해 주세요.") String question,
            @Parameter(description = "prompts.yml의 키", example = "image")
            @RequestParam(defaultValue = "image") @Size(max = 50, message = "50자 이하로 입력해 주세요.") String promptKey
    ) throws IOException {
        return ApiResponse.of(SuccessType.SUCCESS, llmService.image(file, question, promptKey));
    }

    private StreamEvent errorEvent(Throwable e) {
        LlmException exception = e instanceof LlmException llm
                ? llm
                : new LlmException(LlmErrorType.CALL_FAILED, e.getClass().getSimpleName(), e);
        log.warn("스트림 중 오류: [{}] {}", exception.getErrorCode().getCode(), exception.getDetail());
        return new StreamEvent.Error(exception.getErrorCode().getCode(), exception.getMessage());
    }
}
