package top.egon.cola.platform.idp.admin.support.security;

import org.springframework.beans.factory.ObjectProvider;
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
import top.egon.cola.platform.idp.starter.security.IdpBearerAuthenticationFilter;
import top.egon.cola.platform.idp.starter.security.IdpEndpointAuthenticationPolicy;
import top.egon.cola.platform.rbac3.contract.authorization.Decision;
import top.egon.cola.platform.rbac3.contract.authorization.PermissionRequest;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationService;
import top.egon.cola.platform.rbac3.starter.security.Rbac3BearerAuthenticationFilter;

import java.util.List;

/**
 * IdP 管理 API、OAuth 协议端点与身份过滤器链的安全装配。
 *
 * <p>Security wiring for IdP administration APIs, OAuth protocol endpoints, and the identity
 * filter chain.</p>
 */
@Configuration(proxyBeanMethods = false)
public class IdpSecurityConfig {

    /**
     * 将 IdP 开放协议面精确标记为 PUBLIC；其余 IdP Admin 路径默认使用 USER。
     *
     * <p>Marks only the IdP public protocol surface as PUBLIC; all other IdP Admin paths use the
     * starter's USER default.</p>
     */
    @Bean
    @ConditionalOnMissingBean(IdpEndpointAuthenticationPolicy.class)
    IdpEndpointAuthenticationPolicy idpEndpointAuthenticationPolicy() {
        return new IdpEndpointAuthenticationPolicy(
                List.of(
                        "/.well-known/oauth-authorization-server",
                        "/oauth2/jwks",
                        "/oauth2/login/csrf",
                        "/oauth2/login",
                        "/oauth2/token",
                        "/oauth2/revoke",
                        "/oauth2/logout",
                        "/actuator/health/liveness",
                        "/actuator/health/readiness"),
                List.of());
    }

    /**
     * 创建 IdP Security 配置实例。
     *
     * <p>Creates the IdP Security configuration instance.</p>
     */
    public IdpSecurityConfig() {
    }

    /**
     * 配置无状态身份链，并只公开必须匿名访问的 OAuth 与健康检查端点。
     *
     * <p>Configures the stateless identity chain and exposes only OAuth and health endpoints that
     * require anonymous access.</p>
     *
     * @param http Spring Security HTTP 配置；Spring Security HTTP configuration
     * @param idpFilters IdP Bearer 过滤器候选；IdP Bearer filter candidate
     * @param rbac3Filters RBAC3 Bearer 过滤器候选；RBAC3 Bearer filter candidate
     * @return IdP Security Filter Chain；IdP Security Filter Chain
     * @throws Exception Spring Security 装配失败时抛出；when Spring Security wiring fails
     */
    @Bean
    SecurityFilterChain idpAdminSecurityFilterChain(
            HttpSecurity http,
            ObjectProvider<IdpBearerAuthenticationFilter> idpFilters,
            ObjectProvider<Rbac3BearerAuthenticationFilter> rbac3Filters)
            throws Exception {
        IdpBearerAuthenticationFilter idpFilter = idpFilters.getIfAvailable();
        Rbac3BearerAuthenticationFilter rbac3Filter = rbac3Filters.getIfAvailable();
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        "/api/**",
                        "/oauth2/login",
                        "/oauth2/token",
                        "/oauth2/revoke",
                        "/oauth2/logout"
                ))
                .cors(cors -> { })
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS
                ))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/.well-known/oauth-authorization-server",
                                "/oauth2/jwks",
                                "/oauth2/login/**",
                                "/oauth2/token",
                                "/oauth2/revoke",
                                "/oauth2/logout",
                                "/actuator/health/liveness",
                                "/actuator/health/readiness"
                        ).permitAll()
                        .anyRequest().authenticated())
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
                    "RBAC3 authentication filter requires the IdP bearer filter");
        }
        return http.build();
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
                    "${egon.idp.oauth.allowed-origins:}")
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
                "X-IDP-CSRF"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/oauth2/**", configuration);
        return source;
    }

    /**
     * 创建 IdP 管理权限闸门；存在 RBAC3 Authorization Service 时委托其做 USER 权限决策。
     *
     * <p>Creates the IdP administration permission gate, delegating USER permission decisions to
     * RBAC3 when its Authorization Service is available.</p>
     *
     * @param authorizationServices RBAC3 Authorization Service 候选；RBAC3 Authorization Service
     * candidate
     * @return IdP 管理权限端口；IdP administration authorization port
     */
    @Bean
    @ConditionalOnMissingBean(IdpAdminAuthorizationPort.class)
    IdpAdminAuthorizationPort idpAdminAuthorizationPort(
            ObjectProvider<AuthorizationService> authorizationServices) {
        return (principal, permission) -> {
            AuthorizationService authorization = authorizationServices.getIfAvailable();
            if (authorization == null) {
                throw new AccessDeniedException(
                        "RBAC3 authorization adapter is not configured");
            }
            var decision = authorization.requirePermission(
                    PermissionRequest.of(permission));
            if (decision.decision() != Decision.ALLOW) {
                throw new AccessDeniedException(decision.reasonCode());
            }
        };
    }
}
