package com.example.libexception.dto;

import com.example.libexception.error.ErrorCode;
import com.example.libexception.handler.GlobalExceptionHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Standard error body returned by every microservice that installs
 * {@link GlobalExceptionHandler}. Keeping this
 * shape identical across services is the whole point of this library: any
 * client (web, mobile, or another microservice) can parse errors with one model.
 */
@Getter
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private final Instant timestamp = Instant.now();

    /** HTTP status code, e.g. 404. */
    private final int status;

    /** HTTP reason phrase, e.g. "Not Found". */
    private final String error;

    /** Stable machine-readable code from an {@link ErrorCode}, e.g. "PIN_NOT_FOUND". */
    private final String code;

    /** Human-readable message, safe to show to a developer or end user. */
    private final String message;

    /** Request path that produced the error. */
    private final String path;

    /** Correlation/trace id for cross-service log lookup, if one was present (e.g. via MDC). */
    private final String traceId;

    /** Field-level validation failures, only present for validation errors. */
    private final List<FieldErrorDetail> errors;
}
