package com.example.libexception.dto;

import com.example.libexception.exception.ValidationException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/** One field-level validation failure, as reported by Bean Validation or a {@link ValidationException}. */
@Getter
@Builder
@Jacksonized
@AllArgsConstructor
public class FieldErrorDetail {
    private final String field;
    private final String message;
    private final Object rejectedValue;
}
