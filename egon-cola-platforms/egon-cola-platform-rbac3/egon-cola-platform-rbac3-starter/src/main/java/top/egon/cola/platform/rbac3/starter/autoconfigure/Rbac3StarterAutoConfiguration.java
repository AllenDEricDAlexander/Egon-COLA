package top.egon.cola.platform.rbac3.starter.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.platform.idp.starter.autoconfigure.IdpStarterAutoConfiguration;
import top.egon.cola.platform.idp.starter.security.IdpJwtVerifier;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationService;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationBootstrapService;
import top.egon.cola.platform.rbac3.starter.authorization.DefaultAuthorizationService;
import top.egon.cola.platform.rbac3.starter.cache.AuthorizationSnapshotCache;
import top.egon.cola.platform.rbac3.starter.cache.RedisAuthorizationSnapshotCache;
import top.egon.cola.platform.rbac3.starter.cache.SingleFlightSnapshotLoader;
import top.egon.cola.platform.rbac3.starter.client.HttpRbac3AuthorizationClient;
import top.egon.cola.platform.rbac3.starter.client.Rbac3AuthorizationClient;
import top.egon.cola.platform.rbac3.starter.event.Rbac3AuthorizationInvalidationConsumer;
import top.egon.cola.platform.rbac3.starter.runtime.Rbac3RuntimeRedissonConfiguration;
import top.egon.cola.platform.rbac3.starter.security.Rbac3BearerAuthenticationFilter;
import top.egon.cola.platform.rbac3.starter.security.Rbac3ContextAuthentication;
import top.egon.cola.platform.rbac3.starter.security.Rbac3MethodAuthorizationAspect;
import top.egon.cola.platform.rbac3.starter.web.Rbac3AuthorizationExceptionHandler;

import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;

@AutoConfiguration
@AutoConfigureAfter(IdpStarterAutoConfiguration.class)
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
    @ConditionalOnBean(name = "rbac3RuntimeRedissonClient")
    @ConditionalOnMissingBean
    public RedisAuthorizationSnapshotCache redisAuthorizationSnapshotCache(
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            ObjectMapper objectMapper,
            Rbac3StarterProperties properties) {
        return new RedisAuthorizationSnapshotCache(
                redisson, objectMapper,
                properties.getAuthorization().getMaximumJitter());
    }

    @Bean
    @ConditionalOnBean(RedisAuthorizationSnapshotCache.class)
    @ConditionalOnMissingBean
    public AuthorizationSnapshotCache authorizationSnapshotCache(
            RedisAuthorizationSnapshotCache store,
            @Qualifier("rbac3Clock") Clock clock,
            Rbac3StarterProperties properties) {
        return new AuthorizationSnapshotCache(
                store, clock,
                properties.getAuthorization().getNearCacheTtl());
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "egon.cola.platform.rbac3.authorization",
            name = {"endpoint", "service-credential-file"})
    @ConditionalOnMissingBean
    public Rbac3AuthorizationClient rbac3AuthorizationClient(
            ObjectMapper objectMapper,
            Rbac3StarterProperties properties) {
        var authorization = properties.getAuthorization();
        return new HttpRbac3AuthorizationClient(
                URI.create(required(authorization.getEndpoint(), "authorization.endpoint")),
                Path.of(required(authorization.getServiceCredentialFile(),
                        "authorization.serviceCredentialFile")),
                authorization.getFetchTimeout(), objectMapper);
    }

    @Bean
    @ConditionalOnBean({AuthorizationSnapshotCache.class,
            Rbac3AuthorizationClient.class})
    @ConditionalOnMissingBean
    public SingleFlightSnapshotLoader singleFlightSnapshotLoader(
            AuthorizationSnapshotCache cache,
            Rbac3AuthorizationClient client,
            Rbac3StarterProperties properties,
            @Qualifier("rbac3Clock") Clock clock) {
        return new SingleFlightSnapshotLoader(
                cache, client, required(properties.getSystemCode(), "systemCode"),
                properties.getAuthorization().getCacheTtl(), clock);
    }

    @Bean
    @ConditionalOnBean(AuthorizationSnapshotCache.class)
    @ConditionalOnMissingBean
    public Rbac3AuthorizationInvalidationConsumer
            rbac3AuthorizationInvalidationConsumer(
                    AuthorizationSnapshotCache cache,
                    Rbac3StarterProperties properties) {
        return new Rbac3AuthorizationInvalidationConsumer(
                required(properties.getSystemCode(), "systemCode"), cache);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthorizationService.RuntimeContextSource rbac3RuntimeContextSource() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext()
                    .getAuthentication();
            if (authentication instanceof Rbac3ContextAuthentication token) {
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
            @Qualifier("rbac3Clock") Clock clock) {
        return request -> new AuthorizationService.FenceResult(
                false, "FENCE_VERIFIER_UNAVAILABLE", clock.instant(), List.of());
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthorizationService authorizationService(
            AuthorizationService.RuntimeContextSource contextSource,
            AuthorizationService.OperationSodEvaluator operationSodEvaluator,
            AuthorizationService.FenceVerifier fenceVerifier,
            @Qualifier("rbac3Clock") Clock clock) {
        return new DefaultAuthorizationService(
                contextSource, operationSodEvaluator, fenceVerifier, clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthorizationBootstrapService authorizationBootstrapService(
            AuthorizationService.RuntimeContextSource contextSource) {
        return new AuthorizationBootstrapService(contextSource);
    }

    @Bean
    @ConditionalOnMissingBean
    public Rbac3MethodAuthorizationAspect rbac3MethodAuthorizationAspect(
            AuthorizationService authorizationService) {
        return new Rbac3MethodAuthorizationAspect(authorizationService);
    }

    @Bean
    @ConditionalOnMissingBean
    public Rbac3AuthorizationExceptionHandler rbac3AuthorizationExceptionHandler() {
        return new Rbac3AuthorizationExceptionHandler();
    }

    @Bean
    @ConditionalOnBean({IdpJwtVerifier.class, SingleFlightSnapshotLoader.class})
    @ConditionalOnMissingBean
    public Rbac3BearerAuthenticationFilter rbac3BearerAuthenticationFilter(
            SingleFlightSnapshotLoader snapshotLoader,
            ObjectMapper objectMapper) {
        return new Rbac3BearerAuthenticationFilter(snapshotLoader, objectMapper);
    }

    @Bean
    @ConditionalOnBean(Rbac3BearerAuthenticationFilter.class)
    @ConditionalOnMissingBean(name = "rbac3BearerFilterRegistration")
    public FilterRegistrationBean<Rbac3BearerAuthenticationFilter>
            rbac3BearerFilterRegistration(
                    Rbac3BearerAuthenticationFilter filter,
                    Rbac3StarterProperties properties) {
        FilterRegistrationBean<Rbac3BearerAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setName("rbac3BearerAuthenticationFilter");
        registration.setOrder(-101);
        registration.setEnabled(properties.isRegisterFilter());
        return registration;
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }
}
