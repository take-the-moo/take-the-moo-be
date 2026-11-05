package com.hackathon.global.exception;

import com.hackathon.global.response.ApiResult;
import com.hackathon.global.response.type.CommonErrorType;
import com.hackathon.global.response.type.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.core.JacksonException;

/**
 * 모든 오류를 ApiResult로 감싸 응답한다.
 * 응답 message에는 입력값·API 키·프롬프트·모델 원본 오류를 넣지 않는다. 원인은 로그에만 남긴다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final int MAX_ERROR_DETAILS = 5;

    // @RequestBody 검증 실패 (MethodArgumentNotValidException은 BindException의 하위 타입)
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResult<?>> handleBindingErrors(BindException exception) {
        String detail = buildFieldErrorDetail(exception.getBindingResult().getFieldErrors());
        log.debug("유효성 검사 오류: {}", detail);
        return json(CommonErrorType.VALIDATION_FAILED, buildValidationMessage(detail));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResult<?>> handleConstraintViolation(ConstraintViolationException exception) {
        List<String> details = exception.getConstraintViolations().stream()
                .map(v -> lastPathNode(v.getPropertyPath().toString()) + ": " + v.getMessage())
                .toList();
        String detail = summarizeDetails(details);
        log.debug("제약 조건 위반: {}", detail);
        return json(CommonErrorType.VALIDATION_FAILED, buildValidationMessage(detail));
    }

    // 컨트롤러 메서드 파라미터(@RequestParam, @RequestPart 등)에 붙은 제약 조건 위반
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResult<?>> handleHandlerMethodValidation(HandlerMethodValidationException exception) {
        List<String> details = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> result.getMethodParameter().getParameterName() + ": " + error.getDefaultMessage()))
                .toList();
        String detail = summarizeDetails(details);
        log.debug("파라미터 검증 오류: {}", detail);
        return json(CommonErrorType.VALIDATION_FAILED, buildValidationMessage(detail));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResult<?>> handleMaxUploadSize(MaxUploadSizeExceededException exception) {
        log.debug("업로드 크기 초과: maxUploadSize={}", exception.getMaxUploadSize());
        return json(CommonErrorType.VALIDATION_FAILED, buildValidationMessage("file: 파일 크기가 너무 큽니다"));
    }

    // Jackson 메시지에는 입력값 일부가 들어가므로(예: Unrecognized token '...') 어느 필드인지만 남긴다
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResult<?>> handleNotReadable(HttpMessageNotReadableException exception) {
        JacksonException cause = findCause(exception, JacksonException.class);
        log.warn("요청 본문을 읽을 수 없습니다: type={}, path={}",
                cause == null ? exception.getClass().getSimpleName() : cause.getClass().getSimpleName(),
                cause == null ? null : cause.getPathReference());
        return json(CommonErrorType.INVALID_REQUEST, "요청 본문 형식이 올바르지 않습니다.");
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,
            MethodArgumentTypeMismatchException.class,
            MultipartException.class
    })
    public ResponseEntity<ApiResult<?>> handleInvalidParameter(Exception exception) {
        log.debug("요청 파라미터 오류: {}", exception.getMessage());
        return json(CommonErrorType.INVALID_REQUEST, CommonErrorType.INVALID_REQUEST.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResult<?>> handleNoResourceFound(NoResourceFoundException exception) {
        log.debug("리소스를 찾을 수 없음: {}", exception.getResourcePath());
        return json(CommonErrorType.NOT_FOUND, CommonErrorType.NOT_FOUND.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResult<?>> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        log.debug("지원하지 않는 HTTP 메서드: {}", exception.getMethod());
        return json(CommonErrorType.METHOD_NOT_ALLOWED, CommonErrorType.METHOD_NOT_ALLOWED.getMessage());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResult<?>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        log.debug("지원하지 않는 Content-Type: {}", exception.getContentType());
        return json(CommonErrorType.UNSUPPORTED_MEDIA_TYPE, CommonErrorType.UNSUPPORTED_MEDIA_TYPE.getMessage());
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResult<?>> handleDomainException(DomainException exception) {
        // detail은 내부 정보(모델 원본 오류 등)를 담을 수 있으므로 로그에만 남기고 응답에는 노출하지 않는다.
        String detail = exception.getDetail() == null ? "" : " detail=" + exception.getDetail();
        if (exception.getErrorCode().getHttpStatusCode() >= 500) {
            log.warn("도메인 예외: [{}] {}{}", exception.getErrorCode().getCode(), exception.getMessage(), detail);
        } else {
            log.info("도메인 예외: [{}] {}{}", exception.getErrorCode().getCode(), exception.getMessage(), detail);
        }
        return json(exception.getErrorCode(), exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<?>> handleException(Exception exception) {
        log.error("처리되지 않은 예외", exception);
        return json(CommonErrorType.INTERNAL_ERROR, CommonErrorType.INTERNAL_ERROR.getMessage());
    }

    // Content-Type을 미리 지정해 두면 Accept: text/event-stream 요청(SSE)에도 JSON 오류를 그대로 보낼 수 있다
    private ResponseEntity<ApiResult<?>> json(ErrorCode errorCode, String message) {
        return ResponseEntity.status(errorCode.getHttpStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResult.error(errorCode, message));
    }

    private static <T extends Throwable> T findCause(Throwable exception, Class<T> type) {
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (type.isInstance(current)) return type.cast(current);
        }
        return null;
    }

    private String buildFieldErrorDetail(List<FieldError> fieldErrors) {
        return summarizeDetails(fieldErrors.stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList());
    }

    private String summarizeDetails(List<String> details) {
        if (details.isEmpty()) return "";

        int limit = Math.min(details.size(), MAX_ERROR_DETAILS);
        String joined = String.join(", ", details.subList(0, limit));
        int remaining = details.size() - limit;

        if (remaining > 0) joined = joined + ", 외 " + remaining + "건";
        return joined;
    }

    private String buildValidationMessage(String detail) {
        if (detail == null || detail.isBlank()) return "요청 값 검증에 실패했습니다.";
        return "요청 값 검증에 실패했습니다. (" + detail + ")";
    }

    private String lastPathNode(String path) {
        int dot = path.lastIndexOf('.');
        return dot >= 0 ? path.substring(dot + 1) : path;
    }
}
