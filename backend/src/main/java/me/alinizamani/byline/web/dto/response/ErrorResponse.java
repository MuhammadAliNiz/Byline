package me.alinizamani.byline.web.dto.response;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        boolean          success,
        String           code,
        String           message,
        List<FieldError> details,
        Instant          timestamp
) {
    public record ErrorDetail(
            String           code,
            String           message,
            List<FieldError> details
    ) {}

    public record FieldError(String field, String message) {}

    public static ErrorResponse of(String message) {
        return new ErrorResponse(false, null, message, List.of(), Instant.now());
    }

    public static ErrorResponse ofWithCode(String code, String message) {
        return new ErrorResponse(false, code, message, List.of(), Instant.now());
    }

    public static ErrorResponse ofValidation(List<FieldError> details) {
        return new ErrorResponse(false, null,
                "Request validation failed", details, Instant.now());
    }
}