# Feign Error Propagation

When a service calls another one with OpenFeign, Feign turns every non-2xx response into a
`FeignException`. `FeignException` is not a `BaseException`, so `GlobalExceptionHandler` can
only catch it with its last-resort handler:

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleUnexpected(...) {
    log.error(...);
    errorReporter.report(ex);
    return build(HttpStatus.INTERNAL_SERVER_ERROR, CommonErrorCode.INTERNAL_SERVER_ERROR...);
}
```

So when `ms-media` answers `404 MEDIA_NOT_FOUND`, `ms-account` replies `500 INTERNAL_SERVER_ERROR`
to the front end. Three things go wrong at once:

1. the status lies — the caller's mistake looks like our server crashing;
2. the downstream `code` and `message` are lost;
3. `lib-exception.error-reporting` pages someone about an "incident" that is just a missing record.

`LibExceptionErrorDecoder` fixes this for every service that has Feign on its classpath — no
per-service `ErrorDecoder` and no extra configuration required.

## How it works

The decoder is registered automatically by `LibExceptionFeignAutoConfiguration`. For a failed
call it:

1. checks the downstream status against the propagated set — anything outside it is handed to
   `ErrorDecoder.Default`, which produces the usual `FeignException`;
2. parses the response body as the shared [`ErrorResponse`](./index.md) to recover the downstream
   `code` and `message`;
3. wraps that code plus the downstream status in a `RemoteErrorCode` and throws the matching
   library exception, so `GlobalExceptionHandler` re-emits exactly that status and code;
4. logs a `WARN` line with the Feign method key, the status and the code.

Parsing never fails the call: an empty body, an HTML error page or a truncated response just
falls back to the matching `CommonErrorCode` and its default message.

## Status policy

Not every 4xx belongs to our caller — "propagate all 4xx" would be wrong.

| Downstream | What it actually means | Result |
|---|---|---|
| `404`, `409`, `429` | caused by the data our caller sent | same status re-emitted (`NotFoundException`, `ConflictException`, `TooManyRequestsException`), downstream `code` preserved |
| `400` | **we** built a malformed request — our bug, not the caller's | `500` + reported to the error tracker |
| `401`, `403` | **our** service-to-service token or permissions | `500` — never bounce an innocent caller to a login screen |
| `5xx`, unparseable | the downstream broke | `ErrorDecoder.Default` → `FeignException` → `500` + reported |

What reaches the caller for a propagated error is the ordinary `ErrorResponse`:

```json
{
  "timestamp": "2026-11-11T10:00:00Z",
  "status": 404,
  "error": "Not Found",
  "code": "MEDIA_NOT_FOUND",
  "message": "Media 42 does not exist.",
  "path": "/account/avatar",
  "traceId": "abc-123"
}
```

Note that `path` is *our* endpoint and `traceId` *our* request: only the `status`, `code` and
`message` come from the downstream service.

## Configuration

| Property | Type | Default | Description |
|---|---|---|---|
| `lib-exception.feign.translate-client-errors` | `boolean` | `true` | Master switch. `false` restores plain `FeignException`s (every failed call becomes a 500). |
| `lib-exception.feign.propagated-statuses` | `Set<Integer>` | `404,409,429` | Downstream statuses re-emitted unchanged. |

```yaml
lib-exception:
  feign:
    translate-client-errors: true
    propagated-statuses: 404,409,429
```

A status that has no dedicated exception in this library (say you add `410`) is propagated as a
`RemoteStatusException`, which carries the status on its `ErrorCode` just the same.

## Turning it off

Per service, in `application.yml`:

```yaml
lib-exception:
  feign:
    translate-client-errors: false
```

The bean is also skipped entirely when `feign-core` is not on the classpath, so a service without
Feign is unaffected.

## Replacing it with your own decoder

The auto-configuration backs off (`@ConditionalOnMissingBean(ErrorDecoder.class)`) as soon as the
service declares an `ErrorDecoder` bean:

```java
@Configuration
public class FeignConfig {

    @Bean
    public ErrorDecoder errorDecoder() {
        return new MyOwnErrorDecoder();
    }
}
```

To keep this behaviour and only change the status policy, wrap the library decoder instead of
replacing it — the second constructor takes the decoder to fall back to:

```java
@Bean
public ErrorDecoder errorDecoder(ObjectMapper objectMapper) {
    return new LibExceptionErrorDecoder(objectMapper, Set.of(404, 409, 410), new MyFallbackDecoder());
}
```

## Throwing the same errors by hand

Nothing stops a service from reporting a remote error itself — e.g. from a `RestClient` call:

```java
throw new NotFoundException(new RemoteErrorCode("MEDIA_NOT_FOUND", HttpStatus.NOT_FOUND),
        "Media " + mediaId + " does not exist.");
```

`GlobalExceptionHandler` reads the status and the code off the `ErrorCode`, so this produces
exactly the same response as the decoder does.
