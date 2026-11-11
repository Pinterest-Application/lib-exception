package com.example.libexception.feign;

import com.example.libexception.dto.ErrorResponse;
import com.example.libexception.error.CommonErrorCode;
import com.example.libexception.error.ErrorCode;
import com.example.libexception.error.RemoteErrorCode;
import com.example.libexception.exception.ConflictException;
import com.example.libexception.exception.NotFoundException;
import com.example.libexception.exception.RemoteStatusException;
import com.example.libexception.exception.TooManyRequestsException;
import com.example.libexception.handler.GlobalExceptionHandler;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;

import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Translates a failed Feign call into one of this library's exceptions, so an error a
 * downstream service reported deliberately stays that error instead of becoming a
 * {@code FeignException} that {@link GlobalExceptionHandler} can only report as
 * {@code 500 INTERNAL_SERVER_ERROR}.
 * <p>
 * Only statuses that describe <em>our caller's</em> request are propagated (by default
 * 404, 409 and 429). Everything else is left to {@link ErrorDecoder.Default}, on purpose:
 * <ul>
 *     <li>{@code 400} means <em>we</em> built a bad request - our bug, not the caller's,
 *         so it must surface as a 500 and reach the error tracker;</li>
 *     <li>{@code 401}/{@code 403} are about <em>our</em> service-to-service credentials;
 *         forwarding them would bounce an innocent caller to a login screen;</li>
 *     <li>{@code 5xx} means the downstream broke, which is an incident worth reporting.</li>
 * </ul>
 * The response body is parsed as the shared {@link ErrorResponse} to keep the downstream
 * {@code code} and {@code message}; a body that is empty, truncated or not JSON at all
 * never fails the translation, it only falls back to a {@link CommonErrorCode}.
 */
public class LibExceptionErrorDecoder implements ErrorDecoder {

    private static final Logger log = LoggerFactory.getLogger(LibExceptionErrorDecoder.class);

    private final ObjectReader errorReader;
    private final Set<Integer> propagatedStatuses;
    private final ErrorDecoder delegate;

    public LibExceptionErrorDecoder(ObjectMapper objectMapper, Collection<Integer> propagatedStatuses) {
        this(objectMapper, propagatedStatuses, new ErrorDecoder.Default());
    }

    /**
     * @param delegate decoder used for every status that is not propagated; normally
     *                 {@link ErrorDecoder.Default}, which produces the usual
     *                 {@code FeignException}.
     */
    public LibExceptionErrorDecoder(ObjectMapper objectMapper, Collection<Integer> propagatedStatuses,
                                    ErrorDecoder delegate) {
        // Reuse the service's own mapper (modules, naming strategy), but never let an extra
        // field a downstream service added cost us its error code.
        this.errorReader = Objects.requireNonNull(objectMapper, "objectMapper must not be null")
                .readerFor(ErrorResponse.class)
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.propagatedStatuses = Set.copyOf(new LinkedHashSet<>(
                Objects.requireNonNull(propagatedStatuses, "propagatedStatuses must not be null")));
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        HttpStatus status = HttpStatus.resolve(response.status());
        if (status == null || !propagatedStatuses.contains(response.status())) {
            return delegate.decode(methodKey, response);
        }

        ErrorResponse downstream = readBody(response);
        ErrorCode errorCode = new RemoteErrorCode(resolveCode(downstream, status), status);
        String message = resolveMessage(downstream, status);

        log.warn("Feign call {} failed with {} {}; propagating downstream error as-is",
                methodKey, status.value(), errorCode.getCode());

        return toException(status, errorCode, message);
    }

    /** Never throws: an unreadable body simply means we fall back to a generic code and message. */
    private ErrorResponse readBody(Response response) {
        Response.Body body = response.body();
        if (body == null) {
            return null;
        }
        try (InputStream in = body.asInputStream()) {
            byte[] raw = in.readAllBytes();
            if (raw.length == 0) {
                return null;
            }
            return errorReader.readValue(raw);
        } catch (Exception e) {
            log.debug("Could not read downstream error body as ErrorResponse: {}", e.getMessage());
            return null;
        }
    }

    private String resolveCode(ErrorResponse downstream, HttpStatus status) {
        if (downstream != null && StringUtils.hasText(downstream.getCode())) {
            return downstream.getCode();
        }
        return fallbackCode(status).getCode();
    }

    private String resolveMessage(ErrorResponse downstream, HttpStatus status) {
        if (downstream != null && StringUtils.hasText(downstream.getMessage())) {
            return downstream.getMessage();
        }
        return fallbackCode(status).getDefaultMessage();
    }

    private static CommonErrorCode fallbackCode(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> CommonErrorCode.BAD_REQUEST;
            case UNAUTHORIZED -> CommonErrorCode.UNAUTHORIZED;
            case FORBIDDEN -> CommonErrorCode.FORBIDDEN;
            case NOT_FOUND -> CommonErrorCode.RESOURCE_NOT_FOUND;
            case CONFLICT -> CommonErrorCode.CONFLICT;
            case TOO_MANY_REQUESTS -> CommonErrorCode.TOO_MANY_REQUESTS;
            default -> CommonErrorCode.INTERNAL_SERVER_ERROR;
        };
    }

    private static Exception toException(HttpStatus status, ErrorCode errorCode, String message) {
        return switch (status) {
            case NOT_FOUND -> new NotFoundException(errorCode, message);
            case CONFLICT -> new ConflictException(errorCode, message);
            case TOO_MANY_REQUESTS -> new TooManyRequestsException(errorCode, message);
            default -> new RemoteStatusException(errorCode, message);
        };
    }
}
