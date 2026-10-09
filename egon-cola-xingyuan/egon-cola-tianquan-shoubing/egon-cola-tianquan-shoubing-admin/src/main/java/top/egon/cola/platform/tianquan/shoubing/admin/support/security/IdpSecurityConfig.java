package top.egon.cola.platform.tianquan.shoubing.admin.support.security;

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
@Configuration(proxyBeanMethods = false)
public class IdpSecurityConfig {

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
            @Qualifier("jacksonObjectMapper") com.fasterxml.jackson.databind.ObjectMapper objectMapper)
            throws Exception {
        IdpBearerAuthenticationFilter idpFilter = idpFilters.getIfAvailable();
        Rbac3BearerAuthenticationFilter rbac3Filter = rbac3Filters.getIfAvailable();
        http.securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .cors(cors -> { })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .exceptionHandling(exception -> exception.authenticationEntryPoint((request, response, failure) -> {
                    response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.setHeader("Cache-Control", "no-store");
                    objectMapper.writeValue(response.getWriter(), java.util.Map.of(
                            "error", "invalid_token", "error_description", "Request rejected"));
                }))
                .headers(headers -> headers
                        .contentTypeOptions(contentType -> { })
                        .referrerPolicy(referrer -> referrer.policy(
                                org.springframework.security.web.header.writers
                                        .ReferrerPolicyHeaderWriter.ReferrerPolicy
                                        .NO_REFERRER
                        )));
        if (idpFilter != null) {
            http.addFilterBefore(idpFilter, AnonymousAuthenticationFilter.class);
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
    @Order(3)
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

    /**
     * 创建 OAuth 浏览器端点使用的精确 CORS 策略。
     *
     * <p>Creates the exact CORS policy used by OAuth browser endpoints.</p>
     *
     * @param allowedOrigins 明确允许的浏览器 Origin；explicitly allowed browser origins
     * @return CORS 配置来源；CORS configuration source
     */
    @Bean(name = "corsConfigurationSource")
    CorsConfigurationSource idpCorsConfigurationSource(
            @org.springframework.beans.factory.annotation.Value(
                    "${egon.tianquan-shoubing.oauth.allowed-origins:}")
            List<String> allowedOrigins
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins.stream()
                .filter(origin -> origin != null && !origin.isBlank())
                .map(String::trim)
                .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-TIANQUAN-SHOUBING-CSRF"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/oauth2/**", configuration);
        return source;
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
    @Bean
    @ConditionalOnMissingBean(IdpAdminAuthorizationPort.class)
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
}
