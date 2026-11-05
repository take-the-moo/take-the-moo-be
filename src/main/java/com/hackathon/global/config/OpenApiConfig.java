package com.hackathon.global.config;

import com.hackathon.domain.llm.exception.LlmErrorType;
import com.hackathon.domain.place.exception.PlaceErrorType;
import com.hackathon.domain.review.exception.ReviewErrorType;
import com.hackathon.domain.scene.exception.SceneErrorType;
import com.hackathon.domain.user.exception.UserErrorType;
import com.hackathon.global.external.ExternalApiErrorType;
import com.hackathon.global.response.ApiResult;
import com.hackathon.global.response.type.CommonErrorType;
import com.hackathon.global.response.type.ErrorCode;
import com.hackathon.global.response.type.ResultType;
import com.hackathon.global.storage.StorageErrorType;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI: /swagger-ui.html
 * 성공 응답은 ResponseEntity<ApiResult<T>> 반환 타입에서 자동으로 문서화된다.
 * 실패 응답은 ErrorCode enum을 HTTP 상태별로 묶어 모든 /api 엔드포인트에 ApiResult 예시로 붙인다.
 * 새 ErrorType enum을 만들면 ERROR_TYPES에 추가한다.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    private static final List<ErrorCode> ERROR_TYPES = Stream.<ErrorCode[]>of(
                    CommonErrorType.values(),
                    LlmErrorType.values(),
                    ExternalApiErrorType.values(),
                    PlaceErrorType.values(),
                    SceneErrorType.values(),
                    UserErrorType.values(),
                    ReviewErrorType.values(),
                    StorageErrorType.values())
            .flatMap(Stream::of)
            // 404·405·415는 라우팅 단계 오류라 엔드포인트마다 붙이지 않는다
            .filter(code -> !List.of(404, 405, 415).contains(code.getHttpStatusCode()))
            .toList();

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("서대문 발자취 API")
                .version("v1")
                .description("모든 JSON 응답은 ApiResult { resultType, httpStatusCode, code(실패 시), message, data }로 감싼다."));
    }

    @Bean
    public OpenApiCustomizer errorResponseCustomizer() {
        Map<Integer, List<ErrorCode>> byStatus = new TreeMap<>();
        ERROR_TYPES.forEach(code -> byStatus.computeIfAbsent(code.getHttpStatusCode(), k -> new java.util.ArrayList<>()).add(code));

        return openApi -> openApi.getPaths().forEach((path, item) -> {
            if (!path.startsWith("/api/") || path.equals("/api/health")) return;
            item.readOperations().forEach(operation -> byStatus.forEach((status, codes) -> {
                MediaType mediaType = new MediaType();
                codes.forEach(code -> mediaType.addExamples(code.getCode(), new Example()
                        .summary(code.getMessage())
                        .value(errorExample(code))));
                operation.getResponses().addApiResponse(String.valueOf(status), new ApiResponse()
                        .description(String.join(", ", codes.stream().map(ErrorCode::getCode).toList()))
                        .content(new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE, mediaType)));
            }));
        });
    }

    // ApiResult.error()와 같은 모양. swagger 직렬화기가 레코드를 다르게 다룰 수 있어 Map으로 만든다
    private static Map<String, Object> errorExample(ErrorCode code) {
        ApiResult<?> result = ApiResult.error(code, code.getMessage());
        Map<String, Object> example = new LinkedHashMap<>();
        example.put("resultType", ResultType.FAIL.name());
        example.put("httpStatusCode", result.httpStatusCode());
        example.put("code", result.code());
        example.put("message", result.message());
        return example;
    }
}
