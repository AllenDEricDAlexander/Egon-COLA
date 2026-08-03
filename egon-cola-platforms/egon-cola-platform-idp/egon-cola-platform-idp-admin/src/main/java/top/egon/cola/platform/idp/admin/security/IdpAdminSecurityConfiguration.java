package top.egon.cola.platform.idp.admin.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import top.egon.cola.platform.idp.starter.security.IdpBearerAuthenticationFilter;
import top.egon.cola.platform.rbac3.contract.authorization.Decision;
import top.egon.cola.platform.rbac3.contract.authorization.PermissionRequest;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationService;
import top.egon.cola.platform.rbac3.starter.security.Rbac3BearerAuthenticationFilter;

import java.util.List;

@Configuration(proxyBeanMethods = false)
public class IdpAdminSecurityConfiguration {

    @Bean
    SecurityFilterChain idpAdminSecurityFilterChain(
            HttpSecurity http,
            ObjectProvider<IdpBearerAuthenticationFilter> idpFilters,
            ObjectProvider<Rbac3BearerAuthenticationFilter> rbac3Filters,
            ObjectProvider<IdpSsoAuthenticationFilter> ssoFilters,
            ObjectProvider<IdpAuthorizationAuthenticationEntryPoint>
                    authorizationEntryPoints)
            throws Exception {
        IdpBearerAuthenticationFilter idpFilter = idpFilters.getIfAvailable();
        Rbac3BearerAuthenticationFilter rbac3Filter = rbac3Filters.getIfAvailable();
        IdpSsoAuthenticationFilter ssoFilter = ssoFilters.getIfAvailable();
        IdpAuthorizationAuthenticationEntryPoint authorizationEntryPoint =
                authorizationEntryPoints.getIfAvailable();
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
        if (authorizationEntryPoint != null) {
            http.exceptionHandling(exceptions -> exceptions
                    .defaultAuthenticationEntryPointFor(
                            authorizationEntryPoint,
                            new AntPathRequestMatcher("/oauth2/authorize")
                    ));
        }
        if (ssoFilter != null) {
            http.addFilterBefore(ssoFilter, AnonymousAuthenticationFilter.class);
        }
        if (idpFilter != null && rbac3Filter != null) {
            http.addFilterBefore(idpFilter, AnonymousAuthenticationFilter.class);
            http.addFilterAfter(rbac3Filter, IdpBearerAuthenticationFilter.class);
        } else if (idpFilter == null && rbac3Filter == null) {
            http.oauth2ResourceServer(resourceServer -> resourceServer.jwt(
                    jwt -> jwt.jwtAuthenticationConverter(
                            new IdpJwtAuthenticationConverter())));
        } else {
            throw new IllegalStateException(
                    "IdP and RBAC3 authentication filters must be configured together");
        }
        return http.build();
    }

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
