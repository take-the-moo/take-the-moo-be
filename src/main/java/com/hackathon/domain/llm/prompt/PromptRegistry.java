package com.hackathon.domain.llm.prompt;

import com.hackathon.domain.llm.exception.LlmErrorType;
import com.hackathon.domain.llm.exception.LlmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * promptKey로 prompts.yml 항목을 찾고 system의 {변수}를 params(없으면 defaults)로 치환한다.
 * ChatClient의 템플릿 렌더러를 쓰지 않으므로 사용자 입력에 중괄호가 있어도 깨지지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromptRegistry {

    private static final Pattern VARIABLE = Pattern.compile("\\{([A-Za-z0-9_]+)}");

    private final PromptProperties properties;

    public LlmPrompt resolve(String promptKey, Map<String, String> params) {
        PromptProperties.Spec spec = properties.prompts().get(promptKey);
        if (spec == null) {
            throw new LlmException(LlmErrorType.PROMPT_NOT_FOUND, "promptKey=" + promptKey);
        }

        Map<String, String> variables = new HashMap<>(spec.defaults());
        if (params != null) variables.putAll(params);

        return new LlmPrompt(promptKey, spec.model(), spec.temperature(), spec.maxTokens(),
                render(promptKey, spec.system(), variables));
    }

    public Set<String> keys() {
        return properties.prompts().keySet();
    }

    private String render(String promptKey, String template, Map<String, String> variables) {
        Matcher matcher = VARIABLE.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            String value = variables.get(name);
            if (value == null) {
                log.warn("프롬프트 변수 값이 없어 그대로 둡니다: promptKey={}, variable={}", promptKey, name);
                value = matcher.group();
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString().strip();
    }
}
