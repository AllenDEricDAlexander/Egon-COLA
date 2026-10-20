package top.egon.cola.platform.tianquan.shoubing.admin.support.security;

import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3UserDetails;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditProofBO;
import top.egon.cola.platform.tianquan.shoubing.starter.security.rpc.IdpRpcSecurityContext;
import java.util.Arrays;
import org.springframework.core.env.Environment;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.cors.CorsProcessor;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.security.web.firewall.ObservationMarkingRequestRejectedHandler;
import org.springframework.security.web.firewall.HttpStatusRequestRejectedHandler;
import io.micrometer.observation.ObservationRegistry;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.config.OAuthCorsProperties;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.config.OAuthNativeCorsProcessor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller.filter.LegacyOAuthRetirementFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpBearerAuthenticationFilter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpSecurityObservationFilter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpEndpointAuthenticationPolicy;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.Decision;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.PermissionRequest;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3BearerAuthenticationFilter;

import java.util.List;

/**
 * Tianquan-Shoubing 管理 API、OAuth 协议端点与身份过滤器链的安全装配。
 *
 * <p>Security wiring for Tianquan-Shoubing administration APIs, OAuth protocol endpoints, and the identity
 * filter chain.</p>
 */
@EnableConfigurationProperties
@Slf4j
@Configuration(value = "idpSecurityConfig", proxyBeanMethods = false)
public class IdpSecurityConfig {

    private final java.util.concurrent.atomic.AtomicBoolean scopeMetricFailureLogged = new java.util.concurrent.atomic.AtomicBoolean();

    /**
     * 仅将健康检查标记为 PUBLIC；管理 API 默认使用 USER，新协议与旧协议退役分别由有序安全链处理。
     *
     * <p>Marks only health checks as PUBLIC; administration APIs use USER and protocol chains own their endpoints.</p>
     */
    @Bean
    @ConditionalOnMissingBean(IdpEndpointAuthenticationPolicy.class)
    IdpEndpointAuthenticationPolicy idpEndpointAuthenticationPolicy() {
        return new IdpEndpointAuthenticationPolicy(
                List.of("/actuator/health/liveness", "/actuator/health/readiness"),
                List.of());
    }

    /**
     * 创建 Tianquan-Shoubing Security 配置实例。
     *
     * <p>Creates the Tianquan-Shoubing Security configuration instance.</p>
     */
    public IdpSecurityConfig() {
    }

    /**
     * 配置无状态管理 API 身份链；协议与健康检查由其他有序链处理。
     *
     * <p>Configures the stateless administration identity chain; ordered protocol/fallback chains own other endpoints.</p>
     *
     * @param http Spring Security HTTP 配置；Spring Security HTTP configuration
     * @param idpFilters Tianquan-Shoubing Bearer 过滤器候选；Tianquan-Shoubing Bearer filter candidate
     * @param rbac3Filters Tianquan-Jianshen Bearer 过滤器候选；Tianquan-Jianshen Bearer filter candidate
     * @return Tianquan-Shoubing Security Filter Chain；Tianquan-Shoubing Security Filter Chain
     * @throws Exception Spring Security 装配失败时抛出；when Spring Security wiring fails
     */
    @Bean("idpAdminSecurityFilterChain")
    @Order(2)
    SecurityFilterChain idpAdminSecurityFilterChain(
            HttpSecurity http,
            ObjectProvider<IdpBearerAuthenticationFilter> idpFilters,
            ObjectProvider<Rbac3BearerAuthenticationFilter> rbac3Filters,
            @Qualifier("jacksonObjectMapper") com.fasterxml.jackson.databind.ObjectMapper objectMapper,
            @Qualifier("idpSecurityObservationFilter") ObjectProvider<IdpSecurityObservationFilter> observations)
            throws Exception {
        IdpBearerAuthenticationFilter idpFilter = idpFilters.getIfAvailable();
        Rbac3BearerAuthenticationFilter rbac3Filter = rbac3Filters.getIfAvailable();
        http.securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .cors(cors -> { })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .exceptionHandling(exception -> exception.authenticationEntryPoint((request, response, failure) -> {
                    boolean delivered = IdpSecurityObservationFilter.beforeAuthenticationFailure(request);
                    response.setStatus(delivered ? 401 : 503);
                    if (delivered) { response.setHeader("WWW-Authenticate", "Bearer"); }
                    response.setContentType("application/json");
                    response.setHeader("Cache-Control", "no-store");
                    objectMapper.writeValue(response.getWriter(), java.util.Map.of(
                            "error", delivered ? "invalid_token" : "temporarily_unavailable", "error_description", "Request rejected"));
                }).accessDeniedHandler((request, response, failure) -> {
                    boolean delivered = IdpSecurityObservationFilter.beforeAuthorizationDenied(request);
                    response.setStatus(delivered ? 403 : 503); response.setHeader("Cache-Control", "no-store"); response.setContentType("application/json");
                    objectMapper.writeValue(response.getWriter(), java.util.Map.of("error", delivered ? "access_denied" : "temporarily_unavailable",
                            "error_description", "Request rejected"));
                }))
                .headers(headers -> headers
                        .contentTypeOptions(contentType -> { })
                        .referrerPolicy(referrer -> referrer.policy(
                                org.springframework.security.web.header.writers
                                        .ReferrerPolicyHeaderWriter.ReferrerPolicy
                                        .NO_REFERRER
                        )));
        IdpSecurityObservationFilter observation = observations.getIfAvailable();
        if (idpFilter == null && observation != null) { http.addFilterBefore(observation, AnonymousAuthenticationFilter.class); }
        if (idpFilter != null) {
            http.addFilterBefore(idpFilter, AnonymousAuthenticationFilter.class);
            if (observation != null) { http.addFilterBefore(observation, IdpBearerAuthenticationFilter.class); }
            if (rbac3Filter != null) {
                http.addFilterAfter(rbac3Filter, IdpBearerAuthenticationFilter.class);
            }
        } else if (rbac3Filter != null) {
            throw new IllegalStateException(
                    "Tianquan-Jianshen authentication filter requires the Tianquan-Shoubing bearer filter");
        }
        return http.build();
    }

    /** Retires exact legacy operations before any credential handling and denies other unmatched requests. */
    @Bean("idpFallbackSecurityFilterChain")
    @Order(org.springframework.core.Ordered.LOWEST_PRECEDENCE)
    SecurityFilterChain idpFallbackSecurityFilterChain(HttpSecurity http,
            @Qualifier("legacyOAuthRetirementFilter") LegacyOAuthRetirementFilter retirementFilter) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health/liveness", "/actuator/health/readiness").permitAll()
                        .anyRequest().denyAll())
                .addFilterBefore(retirementFilter, SecurityContextHolderFilter.class)
                .build();
    }

    @Bean("legacyOAuthRetirementFilterRegistration")
    FilterRegistrationBean<LegacyOAuthRetirementFilter> legacyOAuthRetirementFilterRegistration(
            @Qualifier("legacyOAuthRetirementFilter") LegacyOAuthRetirementFilter retirementFilter) {
        FilterRegistrationBean<LegacyOAuthRetirementFilter> registration =
                new FilterRegistrationBean<>(retirementFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean(name = "oauthCorsProperties")
    OAuthCorsProperties oauthCorsProperties() {
        return new OAuthCorsProperties();
    }

    @Bean(name = "oauthDefaultCorsProcessor")
    CorsProcessor oauthDefaultCorsProcessor() {
        return new DefaultCorsProcessor();
    }

    @Bean(name = "oauthNativeCorsProcessor")
    OAuthNativeCorsProcessor oauthNativeCorsProcessor(
            @Qualifier("oauthDefaultCorsProcessor") CorsProcessor delegate) {
        return new OAuthNativeCorsProcessor(delegate);
    }

    /**
     * 创建 OAuth 浏览器端点使用的精确 CORS 策略。
     *
     * <p>Creates the exact CORS policy used by OAuth browser endpoints.</p>
     *
     * @param properties 校验后的精确浏览器源；validated exact browser origins
     * @param validator 既有默认 Jakarta 校验器；existing default Jakarta validator
     * @param environment 当前部署环境；current deployment environment
     * @return CORS 配置来源；CORS configuration source
     */
    @Bean(name = "corsConfigurationSource")
    CorsConfigurationSource idpCorsConfigurationSource(
            @Qualifier("oauthCorsProperties") OAuthCorsProperties properties,
            @Qualifier("defaultValidator") Validator validator,
            @Qualifier("environment") Environment environment) {
        new ValidationUtils(validator).validate(properties);
        boolean localPlaintext = Arrays.asList(environment.getActiveProfiles()).contains("local")
                && List.of("local", "dev").contains(environment.getProperty("DEPLOYMENT_ENV", ""))
                && environment.getProperty("egon.cola.platform.tianquan.shoubing.usage-observation.allow-local-plaintext", Boolean.class, false);
        if (!localPlaintext && properties.getAllowedOrigins().stream().anyMatch(origin -> origin.startsWith("http:"))) {
            throw new IllegalArgumentException("OAUTH_CORS_PLAINTEXT_ORIGIN_FORBIDDEN");
        }
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        for (String suffix : List.of("oauth2/token", "userinfo", ".well-known/openid-configuration", ".well-known/jwks.json")) {
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedOrigins(properties.getAllowedOrigins());
            configuration.setAllowedMethods(List.of(suffix.equals("oauth2/token") ? "POST" : "GET"));
            configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
            configuration.setAllowCredentials(false);
            configuration.setMaxAge(600L);
            source.registerCorsConfiguration("/tenants/*/" + suffix, configuration);
        }
        return source;
    }

    @Bean(name = "oauthRequestRejectedHandler")
    RequestRejectedHandler oauthRequestRejectedHandler(
            @Qualifier("observationRegistry") ObjectProvider<ObservationRegistry> registries) {
        var observation = new ObservationMarkingRequestRejectedHandler(
                registries.getIfAvailable(() -> ObservationRegistry.NOOP));
        var delegate = new HttpStatusRequestRejectedHandler();
        return (request, response, failure) -> {
            observation.handle(request, response, failure);
            if (OAuthNativeCorsProcessor.isNativePreflight(request)) {
                OAuthNativeCorsProcessor.reject(response);
            } else { delegate.handle(request, response, failure); }
        };
    }

    /**
     * 创建 Tianquan-Shoubing 管理权限闸门；存在 Tianquan-Jianshen Authorization Service 时委托其做 USER 权限决策。
     *
     * <p>Creates the Tianquan-Shoubing administration permission gate, delegating USER permission decisions to
     * Tianquan-Jianshen when its Authorization Service is available.</p>
     *
     * @param authorizationServices Tianquan-Jianshen Authorization Service 候选；Tianquan-Jianshen Authorization Service
     * candidate
     * @return Tianquan-Shoubing 管理权限端口；Tianquan-Shoubing administration authorization port
     */
    IdpAdminAuthorizationPort idpAdminAuthorizationPort(
            ObjectProvider<AuthorizationService> authorizationServices) {
        return (principal, permission) -> {
            AuthorizationService authorization = authorizationServices.getIfAvailable();
            if (authorization == null) {
                throw new AccessDeniedException(
                        "Tianquan-Jianshen authorization adapter is not configured");
            }
            var decision = authorization.requirePermission(
                    PermissionRequest.of(permission));
            if (decision.decision() != Decision.ALLOW) {
                throw new AccessDeniedException(decision.reasonCode());
            }
        };
    }

    /** Configured scope policy adapter; the original factory call remains available. */
    IdpAdminAuthorizationPort idpAdminAuthorizationPort(ObjectProvider<AuthorizationService> authorizationServices,
            top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties properties,
            top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent observations,
            top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder metrics, Validator validator) {
        var policy = properties.getUserScopePolicy();
        new ValidationUtils(validator).validate(policy);
        var scopeMap = policy.getPermissionScopes().entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                java.util.Map.Entry::getKey, entry -> java.util.Set.copyOf(entry.getValue())));
        boolean enabled = policy.isEnabled();
        var original = idpAdminAuthorizationPort(authorizationServices);
        return (principal, permission) -> {
            if (enabled) {
                new ValidationUtils(validator).validate(PermissionRequest.of(permission));
                var authentication = SecurityContextHolder.getContext().getAuthentication();
                IdentityPrincipal verified = authentication == null ? null : authentication.getPrincipal() instanceof IdentityPrincipal user ? user
                        : authentication.getPrincipal() instanceof Rbac3UserDetails user ? user.identity() : null;
                var required = scopeMap.get(permission);
                var scopes = authentication == null ? java.util.Set.<String>of() : authentication.getAuthorities().stream()
                        .map(org.springframework.security.core.GrantedAuthority::getAuthority).filter(java.util.Objects::nonNull)
                        .filter(value -> value.startsWith("SCOPE_")).map(value -> value.substring(6)).collect(java.util.stream.Collectors.toSet());
                if (authentication == null || !authentication.isAuthenticated() || verified == null || !verified.equals(principal)
                        || required == null || !scopes.containsAll(required)) {
                    try { metrics.record(top.egon.cola.platform.tianquan.shoubing.core.observability.SecurityMetricObservationBO.builder()
                            .signal("rbac.authorization").result("DENY").build()); }
                    catch (RuntimeException failure) {
                        if (scopeMetricFailureLogged.compareAndSet(false, true)) { log.warn("Authorization metric unavailable; failureType={}", failure.getClass().getSimpleName()); }
                    }
                    if (verified != null && observations != null && observations.enabled()) {
                        if (!(authentication.getDetails() instanceof TokenAuditProofBO proof)) {
                            throw new AuthorizationService.RuntimeUnavailableException("OAUTH_AUDIT_DELIVERY_UNAVAILABLE", verified);
                        }
                        boolean delivered;
                        var attrs = RequestContextHolder.getRequestAttributes();
                        if (attrs instanceof ServletRequestAttributes servlet) {
                            var marker = IdpSecurityObservationFilter.authorizationObservationContext(servlet.getRequest());
                            delivered = marker != null && marker.denyOnce(() -> observations.authorizationDenied(proof));
                        } else if (IdpRpcSecurityContext.currentProofOrNull() != null) {
                            var marker = IdpRpcSecurityContext.currentAuthorizationObservationContext();
                            delivered = marker != null && marker.denyOnce(() -> observations.authorizationDenied(proof));
                        } else { delivered = observations.authorizationDenied(proof); }
                        if (!delivered) { throw new AuthorizationService.RuntimeUnavailableException("OAUTH_AUDIT_DELIVERY_UNAVAILABLE", verified); }
                    }
                    throw new AccessDeniedException("OAuth scope is required");
                }
            }
            original.require(principal, permission);
        };
    }

    @Bean("idpAdminAuthorizationPort")
    @ConditionalOnMissingBean(IdpAdminAuthorizationPort.class)
    IdpAdminAuthorizationPort idpAdminAuthorizationPort(
            ObjectProvider<AuthorizationService> authorization,
            @Qualifier("egon.cola.platform.tianquan.jianshen-top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties") ObjectProvider<top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties> properties,
            @Qualifier("idpObservedVerificationComponent") ObjectProvider<top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent> observations,
            @Qualifier("idpSecurityMetricsRecorder") ObjectProvider<top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder> metrics,
            @Qualifier("defaultValidator") ObjectProvider<Validator> validators) {
        var configured = properties.getIfAvailable();
        if (configured == null || !configured.getUserScopePolicy().isEnabled()) { return idpAdminAuthorizationPort(authorization); }
        return idpAdminAuthorizationPort(authorization, configured, observations.getIfAvailable(), metrics.getIfAvailable(
                () -> top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder.NOOP), validators.getObject());
    }
}
