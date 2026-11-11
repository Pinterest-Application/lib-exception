# Lib-Exception

`lib-exception` is a Spring Boot auto-configuration library that standardizes error handling across all your microservices. It ensures that every service returns the exact same JSON error shape, providing a consistent API for your front-end, mobile clients, or other microservices.

## Features

- **Standardized Error Responses**: A shared `ErrorResponse` model so clients only need to write one error parser.
- **Auto-Configured GlobalExceptionHandler**: Seamlessly catches standard Spring exceptions (like validation, bad request) and translates them into the standard model. No manual `@ComponentScan` or `@Import` is required.
- **Pre-built HTTP Exceptions**: Extendable `BaseException` and ready-to-use HTTP exceptions (`NotFoundException`, `BadRequestException`, etc.).
- **Built-in Error Reporting**: Out-of-the-box integration with Sentry-protocol bug trackers (Sentry, Bugsink, GlitchTip) to automatically report unexpected errors.
- **Feign Error Propagation**: A deliberate downstream error (`404 MEDIA_NOT_FOUND`) reaches your caller as that same error instead of being flattened into a `500`.

## Getting Started

1. Add the dependency to your microservice's `build.gradle`:

```groovy
dependencies {
    implementation 'com.example:lib-exception:1.0.0-SNAPSHOT'
}
```

2. That's it! The library uses Spring Boot's Auto-Configuration (`AutoConfiguration.imports`). 
When a service throws an unhandled exception or a `BaseException`, the `GlobalExceptionHandler` will automatically intercept it and return a standardized JSON response:

```json
{
  "timestamp": "2023-10-27T10:00:00Z",
  "status": 404,
  "error": "Not Found",
  "code": "USER_NOT_FOUND",
  "message": "User with ID 123 was not found.",
  "path": "/api/users/123",
  "traceId": "trace-12345"
}
```

## Documentation

- [Configuration](./configuration.md)
- [Exceptions](./exceptions.md)
- [Error Reporting](./error-reporting.md)
- [Feign Error Propagation](./feign.md)
- [Trace ID Setup](./trace-id.md)
