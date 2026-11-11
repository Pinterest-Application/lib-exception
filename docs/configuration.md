# Configuration

`lib-exception` exposes a few properties to customize its behavior, primarily related to error reporting. These can be configured in your microservice's `application.yml` or `application.properties`.

## Properties

| Property | Type | Default | Description |
|---|---|---|---|
| `lib-exception.error-reporting.enabled` | `boolean` | `true` | Master switch to turn error reporting on or off. Useful to disable reporting in local development. |
| `lib-exception.error-reporting.dsn` | `String` | *(none)* | The DSN of the server speaking the Sentry event-ingestion protocol (e.g., Sentry, Bugsink, GlitchTip). If left unset, reporting is completely disabled via a NoOp reporter. |
| `lib-exception.error-reporting.environment` | `String` | *(none)* | Reported as the "environment" tag in your bug tracker (e.g., `production`, `staging`, `dev`). |
| `lib-exception.error-reporting.minimum-status` | `int` | `500` | The minimum HTTP status code that triggers an error report. By default, only server errors (500+) are forwarded to the tracker. Client errors (4xx) are only logged. |
| `lib-exception.feign.translate-client-errors` | `boolean` | `true` | Master switch for the Feign error decoder. Set to `false` to get plain `FeignException`s back (every failed Feign call then becomes a 500). Ignored when Feign is not on the classpath. See [Feign Error Propagation](./feign.md). |
| `lib-exception.feign.propagated-statuses` | `Set<Integer>` | `404,409,429` | Downstream statuses re-emitted to your own caller unchanged. Deliberately excludes 400 (we built a malformed request) and 401/403 (our own service credentials). |

## Example `application.yml`

```yaml
lib-exception:
  error-reporting:
    enabled: true
    dsn: "http://<public-key>@your-bugsink-host:8001/1"
    environment: "production"
    minimum-status: 500
  feign:
    translate-client-errors: true
    propagated-statuses: 404,409,429
```
