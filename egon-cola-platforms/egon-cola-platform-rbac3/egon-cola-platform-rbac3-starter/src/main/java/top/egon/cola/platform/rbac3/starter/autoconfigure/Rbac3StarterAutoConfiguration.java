package top.egon.cola.platform.rbac3.starter.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import top.egon.cola.platform.rbac3.core.runtime.Rbac3RuntimeKeyFactory;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationService;
import top.egon.cola.platform.rbac3.starter.authorization.DefaultAuthorizationService;
import top.egon.cola.platform.rbac3.starter.runtime.Rbac3RuntimeRedissonConfiguration;
import top.egon.cola.platform.rbac3.starter.runtime.Rbac3RuntimeSnapshotReader;
import top.egon.cola.platform.rbac3.starter.security.Rbac3AuthenticationToken;
import top.egon.cola.platform.rbac3.starter.security.Rbac3BearerAuthenticationFilter;
import top.egon.cola.platform.rbac3.starter.security.Rbac3JwtVerifier;
import top.egon.cola.platform.rbac3.starter.security.Rbac3MethodAuthorizationAspect;
import top.egon.cola.platform.rbac3.starter.web.Rbac3AuthorizationExceptionHandler;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(Rbac3StarterProperties.class)
@ConditionalOnProperty(
        prefix = "egon.cola.platform.rbac3",
        name = "enabled",
        havingValue = "true")
@Import(Rbac3RuntimeRedissonConfiguration.class)
public class Rbac3StarterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "rbac3Clock")
    public Clock rbac3Clock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean
    public Rbac3RuntimeKeyFactory rbac3RuntimeKeyFactory() {
        return new Rbac3RuntimeKeyFactory();
    }

    @Bean
    @ConditionalOnBean(name = "rbac3RuntimeRedissonClient")
    @ConditionalOnMissingBean
    public Rbac3RuntimeSnapshotReader rbac3RuntimeSnapshotReader(
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            ObjectMapper objectMapper,
            Rbac3RuntimeKeyFactory keyFactory,
            @Qualifier("rbac3Clock") Clock clock
    ) {
        return new Rbac3RuntimeSnapshotReader(
                redisson, objectMapper, keyFactory, clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthorizationService.RuntimeContextSource rbac3RuntimeContextSource() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext()
                    .getAuthentication();
            if (authentication instanceof Rbac3AuthenticationToken token) {
                return token.context();
            }
            throw new IllegalStateException("RBAC3 authentication is required");
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthorizationService.OperationSodEvaluator rbac3OperationSodEvaluator() {
        return request -> new AuthorizationService.OperationSodResult(
                false, "PARTICIPATION_VERIFIER_UNAVAILABLE", List.of(), List.of());
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthorizationService.FenceVerifier rbac3FenceVerifier(
            @Qualifier("rbac3Clock") Clock clock
    ) {
        return request -> new AuthorizationService.FenceResult(
                false, "FENCE_VERIFIER_UNAVAILABLE", clock.instant(), List.of());
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthorizationService authorizationService(
            AuthorizationService.RuntimeContextSource contextSource,
            AuthorizationService.OperationSodEvaluator operationSodEvaluator,
            AuthorizationService.FenceVerifier fenceVerifier,
            @Qualifier("rbac3Clock") Clock clock
    ) {
        return new DefaultAuthorizationService(
                contextSource, operationSodEvaluator, fenceVerifier, clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public Rbac3MethodAuthorizationAspect rbac3MethodAuthorizationAspect(
            AuthorizationService authorizationService
    ) {
        return new Rbac3MethodAuthorizationAspect(authorizationService);
    }

    @Bean
    @ConditionalOnMissingBean
    public Rbac3AuthorizationExceptionHandler rbac3AuthorizationExceptionHandler() {
        return new Rbac3AuthorizationExceptionHandler();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "egon.cola.platform.rbac3",
            name = "jwk-set-uri")
    @ConditionalOnMissingBean
    public JwtDecoder rbac3JwtDecoder(Rbac3StarterProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(
                properties.getJwkSetUri()).build();
        List<OAuth2TokenValidator<org.springframework.security.oauth2.jwt.Jwt>> validators =
                new ArrayList<>();
        if (hasText(properties.getIssuer())) {
            validators.add(JwtValidators.createDefaultWithIssuer(
                    properties.getIssuer().trim()));
        } else {
            validators.add(JwtValidators.createDefault());
        }
        if (hasText(properties.getAudience())) {
            String audience = properties.getAudience().trim();
            validators.add(jwt -> jwt.getAudience().contains(audience)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                            "invalid_token", "JWT audience is invalid", null)));
        }
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
        return decoder;
    }

    @Bean
    @ConditionalOnBean(JwtDecoder.class)
    @ConditionalOnMissingBean
    public Rbac3JwtVerifier rbac3JwtVerifier(JwtDecoder decoder) {
        return new Rbac3JwtVerifier(decoder);
    }

    @Bean
    @ConditionalOnBean({Rbac3JwtVerifier.class, Rbac3RuntimeSnapshotReader.class})
    @ConditionalOnMissingBean
    public Rbac3BearerAuthenticationFilter rbac3BearerAuthenticationFilter(
            Rbac3JwtVerifier verifier,
            Rbac3RuntimeSnapshotReader snapshotReader,
            ObjectMapper objectMapper
    ) {
        return new Rbac3BearerAuthenticationFilter(
                verifier, snapshotReader, objectMapper);
    }

    @Bean
    @ConditionalOnBean(Rbac3BearerAuthenticationFilter.class)
    @ConditionalOnMissingBean(name = "rbac3BearerFilterRegistration")
    public FilterRegistrationBean<Rbac3BearerAuthenticationFilter>
            rbac3BearerFilterRegistration(
                    Rbac3BearerAuthenticationFilter filter
            ) {
        FilterRegistrationBean<Rbac3BearerAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setName("rbac3BearerAuthenticationFilter");
        registration.setOrder(-101);
        return registration;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
