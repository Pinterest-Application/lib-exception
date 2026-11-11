package com.example.libexception.exception;

import com.example.libexception.error.ErrorCode;
import com.example.libexception.error.RemoteErrorCode;

/**
 * Carries a downstream HTTP status that none of the other exceptions in this library
 * model - e.g. a service that adds {@code 410} to
 * {@code lib-exception.feign.propagated-statuses}.
 * <p>
 * The status comes entirely from the attached {@link ErrorCode} (normally a
 * {@link RemoteErrorCode}), so there is no fixed status for this type.
 */
public class RemoteStatusException extends BaseException {

    public RemoteStatusException(ErrorCode errorCode) {
        super(errorCode);
    }

    public RemoteStatusException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
