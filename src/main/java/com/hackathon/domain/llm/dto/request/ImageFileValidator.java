package com.hackathon.domain.llm.dto.request;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;
import org.springframework.web.multipart.MultipartFile;

public class ImageFileValidator implements ConstraintValidator<ImageFile, MultipartFile> {

    public static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private long maxBytes;

    @Override
    public void initialize(ImageFile annotation) {
        this.maxBytes = annotation.maxBytes();
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) return false;
        if (file.getSize() > maxBytes) return false;
        return file.getContentType() != null && ALLOWED_TYPES.contains(file.getContentType().toLowerCase());
    }
}
