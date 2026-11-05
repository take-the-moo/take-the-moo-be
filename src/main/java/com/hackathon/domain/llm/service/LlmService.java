package com.hackathon.domain.llm.service;

import com.hackathon.domain.llm.dto.request.AnalyzeRequestDto;
import com.hackathon.domain.llm.dto.request.ChatRequestDto;
import com.hackathon.domain.llm.dto.response.AnalysisResult;
import com.hackathon.domain.llm.dto.response.ChatResponseDto;
import com.hackathon.domain.llm.dto.response.StreamEvent;
import com.hackathon.domain.llm.prompt.LlmPrompt;
import com.hackathon.domain.llm.prompt.PromptRegistry;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

/**
 * 기능별 진입점. 새 AI 기능은 여기에 메서드 하나를 추가한다:
 * promptKey로 프롬프트를 찾고(PromptRegistry) → LlmClient의 call/entity/stream 중 하나를 부른다.
 */
@Service
@RequiredArgsConstructor
public class LlmService {

    private final PromptRegistry promptRegistry;
    private final LlmClient llmClient;

    public ChatResponseDto chat(ChatRequestDto request) {
        LlmPrompt prompt = promptRegistry.resolve(request.promptKeyOrDefault(), request.params());
        return llmClient.call(prompt, request.message());
    }

    /** promptKey가 없으면 여기서 바로 예외가 난다 — 스트림 시작 전이라 ApiResult로 응답된다 */
    public Flux<StreamEvent> stream(String promptKey, String message) {
        LlmPrompt prompt = promptRegistry.resolve(promptKey, Map.of());
        return llmClient.stream(prompt, message);
    }

    public AnalysisResult analyze(AnalyzeRequestDto request) {
        LlmPrompt prompt = promptRegistry.resolve(request.promptKeyOrDefault(), request.params());
        return llmClient.entity(prompt, request.input(), AnalysisResult.class, AnalysisResult::mock);
    }

    public ChatResponseDto image(MultipartFile file, String question, String promptKey) throws IOException {
        LlmPrompt prompt = promptRegistry.resolve(promptKey, Map.of());
        MimeType mimeType = MimeType.valueOf(file.getContentType());
        return llmClient.call(prompt, question, mimeType, new ByteArrayResource(file.getBytes()));
    }
}
