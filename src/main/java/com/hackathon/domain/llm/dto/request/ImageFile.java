package com.hackathon.domain.llm.dto.request;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 업로드 이미지 검증: 비어 있지 않고, maxBytes 이하, jpeg/png/webp만. 위반 시 422 COMMON_VALIDATION_FAILED.
 */
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ImageFileValidator.class)
public @interface ImageFile {

    String message() default "5MB 이하의 jpeg, png, webp 이미지만 업로드할 수 있습니다.";

    long maxBytes() default 5 * 1024 * 1024;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
