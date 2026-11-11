package com.example.libexception.autoconfigure;

import com.example.libexception.feign.LibExceptionErrorDecoder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Configuration for exception-lib, bound from each consuming service's own
 * {@code application.yml} - e.g.:
 *
 * <pre>{@code
 * lib-exception:
 *   error-reporting:
 *     dsn: http://<key>@your-bugsink-host:8001/1
 *     environment: production
 *   feign:
 *     propagated-statuses: 404,409,429
 * }</pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "lib-exception")
public class LibExceptionProperties {

    @NestedConfigurationProperty
    private final ErrorReporting errorReporting = new ErrorReporting();

    @NestedConfigurationProperty
    private final Feign feign = new Feign();

    @Getter
    @Setter
    public static class ErrorReporting {

        /** Master switch. Even with a DSN set, reporting can be turned off (e.g. for local dev). */
        private boolean enabled = true;

        /**
         * DSN of a server speaking the Sentry event-ingestion protocol - Sentry itself,
         * self-hosted Bugsink, GlitchTip, etc. Leave unset to disable reporting entirely
         * (the default; a {@code NoopErrorReporter} is used instead).
         */
        private String dsn;

        /** Reported as the "environment" tag, e.g. "production", "staging". */
        private String environment;

        /** Only exceptions resulting in this HTTP status or higher get forwarded. Default: 500 (server errors only). */
        private int minimumStatus = 500;
    }

    /** Behaviour of the {@link LibExceptionErrorDecoder}, active only when Feign is on the classpath. */
    @Getter
    @Setter
    public static class Feign {

        /**
         * Master switch for translating downstream Feign errors into this library's
         * exceptions. Turn off to get plain {@code FeignException}s back (every failed
         * call then becomes a 500).
         */
        private boolean translateClientErrors = true;

        /**
         * Downstream statuses re-emitted to our own caller unchanged, because they are
         * caused by the data that caller sent.
         * <p>
         * Deliberately excludes 400 (we built a malformed request - our bug) and
         * 401/403 (our own service-to-service credentials); those stay 500s so they
         * reach the error tracker instead of misleading the caller.
         */
        private Set<Integer> propagatedStatuses = new LinkedHashSet<>(List.of(404, 409, 429));
    }
}
