# Exceptions

The library provides a standard `BaseException` and common HTTP-specific subclasses to make throwing standard errors straightforward.

## Throwing Exceptions

To trigger a standardized `ErrorResponse` in your controller or service layer, simply throw one of the provided exception classes. All of them require an `ErrorCode`, which represents a stable, machine-readable identifier for the error.

```java
import com.example.libexception.exception.NotFoundException;

public User getUser(String id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new NotFoundException(
            UserErrorCode.USER_NOT_FOUND, 
            "User with ID " + id + " does not exist."
        ));
}
```

## Available Built-in Exceptions

All of these extend `BaseException` and map to specific HTTP statuses:

- `BadRequestException` (400)
- `UnauthorizedException` (401)
- `ForbiddenException` (403)
- `NotFoundException` (404)
- `ConflictException` (409)
- `TooManyRequestsException` (429)
- `ValidationException` (400) - specifically used for field-level validation errors, carrying a list of `FieldErrorDetail`.

## Creating Custom Error Codes

Your microservices should define their own `ErrorCode` enums implementing the `ErrorCode` interface.

```java
import com.example.libexception.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "The user was not found");

    private final HttpStatus httpStatus;
    private final String code;
    private final String defaultMessage;

    // constructor, getters...
}
```

## Creating Custom Exception Classes

If you need to throw an HTTP status code that is not covered by the built-in exceptions (e.g., `422 Unprocessable Entity` or `418 I'm a teapot`), you can easily create your own exception by extending `BaseException`.

```java
import com.example.libexception.exception.BaseException;
import com.example.libexception.error.ErrorCode;

public class UnprocessableEntityException extends BaseException {

    public UnprocessableEntityException(ErrorCode errorCode) {
        super(errorCode);
    }

    public UnprocessableEntityException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
```

As long as the `ErrorCode` you pass in returns the correct `HttpStatus`, the `GlobalExceptionHandler` will automatically translate it correctly.

## Standard Spring Exception Handling

In addition to the explicit exceptions above, the `GlobalExceptionHandler` automatically intercepts common Spring MVC exceptions and formats them as a standard `ErrorResponse`. This includes:

- `MethodArgumentNotValidException` (mapped to 400 with `FieldErrorDetail`)
- `ConstraintViolationException` (mapped to 400 with `FieldErrorDetail`)
- `HandlerMethodValidationException` (mapped to 400 with `FieldErrorDetail`) - constraints on controller method parameters such as `@RequestParam @Min(1) int size`; no `@Validated` needed on the controller
- `HttpMessageNotReadableException` (mapped to 400)
- `MissingServletRequestParameterException` (mapped to 400)
- `MethodArgumentTypeMismatchException` (mapped to 400)
- `HttpRequestMethodNotSupportedException` (mapped to 405)
- General `Exception` (mapped to 500, logged, and reported via the configured error tracker)
