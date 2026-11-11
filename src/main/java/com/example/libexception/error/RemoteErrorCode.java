package com.example.libexception.error;

import com.example.libexception.handler.GlobalExceptionHandler;
import org.springframework.http.HttpStatus;

import java.util.Objects;

/**
 * An {@link ErrorCode} that was not declared locally but received from another
 * microservice, so a downstream code such as {@code MEDIA_NOT_FOUND} survives the
 * hop instead of collapsing into a generic one.
 * <p>
 * Used by the Feign error decoder: {@link GlobalExceptionHandler} reads both the HTTP
 * status and the {@code code} of a response straight off the thrown exception's
 * {@code ErrorCode}, so re-emitting a downstream error only requires wrapping its
 * code and status in one of these.
 *
 * @param code       machine-readable code as reported by the downstream service
 * @param httpStatus status to re-emit to our own caller
 */
public record RemoteErrorCode(String code, HttpStatus httpStatus) implements ErrorCode {

    public RemoteErrorCode {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(httpStatus, "httpStatus must not be null");
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    /**
     * The downstream message is always passed explicitly to the exception constructor,
     * so this is only a last-resort label.
     */
    @Override
    public String getDefaultMessage() {
        return httpStatus.getReasonPhrase();
    }
}
