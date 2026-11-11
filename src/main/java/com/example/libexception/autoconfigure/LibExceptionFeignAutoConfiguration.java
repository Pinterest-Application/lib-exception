package com.example.libexception.autoconfigure;

import com.example.libexception.feign.LibExceptionErrorDecoder;
import com.example.libexception.handler.GlobalExceptionHandler;
import feign.codec.ErrorDecoder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

/**
 * Registers a {@link LibExceptionErrorDecoder} in services that use OpenFeign, so a
 * deliberate downstream error (e.g. {@code ms-media} answering
 * {@code 404 MEDIA_NOT_FOUND}) reaches our caller as that same error instead of being
 * flattened into a {@code 500} by {@link GlobalExceptionHandler}'s catch-all.
 * <p>
 * Backs off entirely when Feign is absent, when the service declares its own
 * {@link ErrorDecoder}, or when
 * {@code lib-exception.feign.translate-client-errors=false}.
 */
@AutoConfiguration
@ConditionalOnClass(ErrorDecoder.class)
@ConditionalOnProperty(prefix = "lib-exception.feign", name = "translate-client-errors",
        matchIfMissing = true)
@EnableConfigurationProperties(LibExceptionProperties.class)
public class LibExceptionFeignAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ErrorDecoder.class)
    public ErrorDecoder libExceptionErrorDecoder(ObjectMapper objectMapper, LibExceptionProperties properties) {
        return new LibExceptionErrorDecoder(objectMapper, properties.getFeign().getPropagatedStatuses());
    }
}
