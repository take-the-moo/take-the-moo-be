package com.hackathon.domain.llm.service;

import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.models.messages.OutputConfig;
import com.hackathon.domain.llm.dto.response.ChatResponseDto;
import com.hackathon.domain.llm.dto.response.StreamEvent;
import com.hackathon.domain.llm.dto.response.UsageDto;
import com.hackathon.domain.llm.exception.LlmErrorType;
import com.hackathon.domain.llm.exception.LlmException;
import com.hackathon.domain.llm.prompt.LlmPrompt;
import com.hackathon.global.config.LlmProperties;
import java.io.InterruptedIOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.PromptUserSpec;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;

/**
 * ChatClient 호출은 모두 여기를 거친다.
 * - 목 모드(llm.mock=true)면 API를 부르지 않고 고정 응답을 돌려준다
 * - 시간 초과·호출 실패·출력 파싱 실패를 LlmException으로 바꾼다 (원본 오류는 detail → 로그에만)
 * - promptKey·모델·지연 시간·토큰 수를 한 줄로 로깅한다 (프롬프트·입력 원문은 로깅하지 않는다)
 */
@Slf4j
@Component
public class LlmClient {

    /** 구조화 출력이 스키마에 맞지 않을 때 오류를 덧붙여 다시 요청하는 횟수 */
    private static final int STRUCTURED_OUTPUT_RETRIES = 2;
    private static final Duration MOCK_TOKEN_DELAY = Duration.ofMillis(40);
    private static final int MAX_DETAIL_LENGTH = 300;

    private final ChatClient chatClient;
    private final boolean mock;

    public LlmClient(ChatClient.Builder chatClientBuilder, LlmProperties properties) {
        this.chatClient = chatClientBuilder.build();
        this.mock = properties.mock();
    }

    public boolean isMock() {
        return mock;
    }

    /** 텍스트 → 텍스트 */
    public ChatResponseDto call(LlmPrompt prompt, String userText) {
        return call(prompt, user -> user.text(userText), () -> mockAnswer(prompt, userText));
    }

    /** 텍스트 + 이미지 → 텍스트 */
    public ChatResponseDto call(LlmPrompt prompt, String userText, MimeType mimeType, Resource media) {
        return call(prompt, user -> user.text(userText).media(mimeType, media),
                () -> mockAnswer(prompt, userText) + " (이미지 " + mimeType + ")");
    }

    /**
     * 텍스트 → 구조화 출력(type). StructuredOutputValidationAdvisor가 JSON 스키마 검증에 실패하면
     * 오류를 덧붙여 다시 요청하고, 최종적으로도 파싱하지 못하면 LLM_INVALID_OUTPUT을 던진다.
     */
    public <T> T entity(LlmPrompt prompt, String userText, Class<T> type, Supplier<T> mockResult) {
        long start = System.nanoTime();
        if (mock) {
            T result = mockResult.get();
            logDone(prompt, prompt.model(), UsageDto.EMPTY, elapsedMs(start));
            return result;
        }

        StructuredOutputValidationAdvisor validation = StructuredOutputValidationAdvisor.builder()
                .outputType(type)
                .maxRepeatAttempts(STRUCTURED_OUTPUT_RETRIES)
                .build();

        ResponseEntity<ChatResponse, T> response;
        try {
            response = request(prompt, user -> user.text(userText))
                    .advisors(validation)
                    .call()
                    .responseEntity(type);
        } catch (JacksonException e) {
            throw new LlmException(LlmErrorType.INVALID_OUTPUT, detail(prompt, e), e);
        } catch (RuntimeException e) {
            throw translate(prompt, e);
        }

        long latencyMs = elapsedMs(start);
        logDone(prompt, modelOf(response.response(), prompt), usageOf(response.response()), latencyMs);
        if (response.entity() == null) {
            throw new LlmException(LlmErrorType.INVALID_OUTPUT, "promptKey=" + prompt.key() + ", 빈 응답");
        }
        return response.entity();
    }

    /**
     * 스트리밍. token 이벤트를 흘려보내고 마지막에 done 이벤트 하나를 보낸다.
     * 스트림 중 오류는 LlmException으로 바꿔 onError로 내보낸다 (컨트롤러가 error 이벤트로 바꾼다).
     */
    public Flux<StreamEvent> stream(LlmPrompt prompt, String userText) {
        return Flux.defer(() -> {
            long start = System.nanoTime();
            if (mock) return mockStream(prompt, userText, start);

            AtomicReference<ChatResponse> last = new AtomicReference<>();
            AtomicReference<UsageDto> usage = new AtomicReference<>(UsageDto.EMPTY);

            Flux<StreamEvent> tokens = request(prompt, user -> user.text(userText))
                    .stream()
                    .chatResponse()
                    .doOnNext(chunk -> {
                        last.set(chunk);
                        UsageDto chunkUsage = usageOf(chunk);
                        if (chunkUsage.inputTokens() + chunkUsage.outputTokens() > 0) usage.set(chunkUsage);
                    })
                    .mapNotNull(LlmClient::textOf)
                    .filter(text -> !text.isEmpty())
                    .map(StreamEvent.Token::new);

            Mono<StreamEvent> done = Mono.fromSupplier(() -> {
                long latencyMs = elapsedMs(start);
                String model = modelOf(last.get(), prompt);
                logDone(prompt, model, usage.get(), latencyMs);
                return new StreamEvent.Done(model, usage.get(), latencyMs);
            });

            return tokens.concatWith(done);
        }).onErrorMap(e -> !(e instanceof LlmException), e -> translate(prompt, e));
    }

    private ChatResponseDto call(LlmPrompt prompt, Consumer<PromptUserSpec> user, Supplier<String> mockAnswer) {
        long start = System.nanoTime();
        if (mock) {
            ChatResponseDto result = new ChatResponseDto(mockAnswer.get(), prompt.model(), UsageDto.EMPTY,
                    elapsedMs(start));
            logDone(prompt, prompt.model(), UsageDto.EMPTY, result.latencyMs());
            return result;
        }

        ChatResponse response;
        try {
            response = request(prompt, user).call().chatResponse();
        } catch (RuntimeException e) {
            throw translate(prompt, e);
        }

        long latencyMs = elapsedMs(start);
        String model = modelOf(response, prompt);
        UsageDto usage = usageOf(response);
        logDone(prompt, model, usage, latencyMs);

        String answer = textOf(response);
        if (answer == null || answer.isBlank()) {
            throw new LlmException(LlmErrorType.INVALID_OUTPUT, "promptKey=" + prompt.key() + ", 빈 응답");
        }
        return new ChatResponseDto(answer, model, usage, latencyMs);
    }

    private ChatClient.ChatClientRequestSpec request(LlmPrompt prompt, Consumer<PromptUserSpec> user) {
        // 2.0의 옵션은 불변이다. 호출마다 builder로 넘기면 모델 기본 옵션 위에 합쳐진다
        AnthropicChatOptions.Builder options = AnthropicChatOptions.builder()
                .model(prompt.model())
                .temperature(prompt.temperature())
                .maxTokens(prompt.maxTokens());
        // prompts.yml에 적은 것만 보낸다 (없으면 모델 기본값: Claude 5는 adaptive thinking, effort medium)
        if (prompt.effort() != null) options.effort(OutputConfig.Effort.of(prompt.effort()));
        if ("disabled".equals(prompt.thinking())) options.thinkingDisabled();
        if ("adaptive".equals(prompt.thinking())) options.thinkingAdaptive();

        return chatClient.prompt()
                .options(options)
                .system(prompt.system())
                .user(user);
    }

    private Flux<StreamEvent> mockStream(LlmPrompt prompt, String userText, long start) {
        List<StreamEvent> tokens = Arrays.stream(mockAnswer(prompt, userText).split("(?<= )"))
                .<StreamEvent>map(StreamEvent.Token::new)
                .toList();
        return Flux.fromIterable(tokens)
                .delayElements(MOCK_TOKEN_DELAY)
                .concatWith(Mono.fromSupplier(() -> {
                    long latencyMs = elapsedMs(start);
                    logDone(prompt, prompt.model(), UsageDto.EMPTY, latencyMs);
                    return new StreamEvent.Done(prompt.model(), UsageDto.EMPTY, latencyMs);
                }));
    }

    private String mockAnswer(LlmPrompt prompt, String userText) {
        String preview = userText.length() > 30 ? userText.substring(0, 30) + "…" : userText;
        return "[MOCK] '" + preview + "'에 대한 " + prompt.key() + " 고정 응답입니다. 실제 호출은 llm.mock=false로 켜세요.";
    }

    private LlmException translate(LlmPrompt prompt, Throwable e) {
        if (e instanceof LlmException llmException) return llmException;
        LlmErrorType type = isTimeout(e) ? LlmErrorType.TIMEOUT : LlmErrorType.CALL_FAILED;
        return new LlmException(type, detail(prompt, e), e);
    }

    private static boolean isTimeout(Throwable e) {
        for (Throwable current = e; current != null; current = current.getCause()) {
            // OkHttp의 connect/read 초과는 SocketTimeoutException, 전체 요청 초과는 InterruptedIOException("timeout")
            if (current instanceof InterruptedIOException || current instanceof TimeoutException) return true;
            if (current.getCause() == current) break;
        }
        return false;
    }

    private static String detail(LlmPrompt prompt, Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();

        StringBuilder detail = new StringBuilder("promptKey=").append(prompt.key())
                .append(", model=").append(prompt.model())
                .append(", error=").append(e.getClass().getSimpleName());
        AnthropicServiceException service = findCause(e, AnthropicServiceException.class);
        if (service != null) detail.append(", status=").append(service.statusCode());
        if (root != e) detail.append(", root=").append(root.getClass().getSimpleName());
        String message = root.getMessage();
        if (message != null) {
            detail.append(", message=").append(message.length() > MAX_DETAIL_LENGTH
                    ? message.substring(0, MAX_DETAIL_LENGTH) + "…" : message);
        }
        return detail.toString();
    }

    private static <T extends Throwable> T findCause(Throwable e, Class<T> type) {
        for (Throwable current = e; current != null; current = current.getCause()) {
            if (type.isInstance(current)) return type.cast(current);
            if (current.getCause() == current) break;
        }
        return null;
    }

    private static String textOf(ChatResponse response) {
        if (response == null || response.getResult() == null) return null;
        return response.getResult().getOutput().getText();
    }

    private static String modelOf(ChatResponse response, LlmPrompt prompt) {
        if (response == null || response.getMetadata() == null) return prompt.model();
        String model = response.getMetadata().getModel();
        return model == null || model.isBlank() ? prompt.model() : model;
    }

    private static UsageDto usageOf(ChatResponse response) {
        if (response == null || response.getMetadata() == null) return UsageDto.EMPTY;
        Usage usage = response.getMetadata().getUsage();
        if (usage == null) return UsageDto.EMPTY;
        return new UsageDto(nullToZero(usage.getPromptTokens()), nullToZero(usage.getCompletionTokens()));
    }

    private static int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private static long elapsedMs(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }

    private void logDone(LlmPrompt prompt, String model, UsageDto usage, long latencyMs) {
        log.info("LLM 호출 완료: promptKey={}, model={}, latencyMs={}, inputTokens={}, outputTokens={}, mock={}",
                prompt.key(), model, latencyMs, usage.inputTokens(), usage.outputTokens(), mock);
    }
}
