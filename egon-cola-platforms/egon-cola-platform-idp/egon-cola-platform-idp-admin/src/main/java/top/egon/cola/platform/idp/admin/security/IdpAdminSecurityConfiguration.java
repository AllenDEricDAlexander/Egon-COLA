package top.egon.cola.platform.idp.admin.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class IdpAdminSecurityConfiguration {

    @Bean
    SecurityFilterChain idpAdminSecurityFilterChain(HttpSecurity http)
            throws Exception {
        IdpJwtAuthenticationConverter converter =
                new IdpJwtAuthenticationConverter();
        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        "/api/**",
                        "/oauth2/token",
                        "/oauth2/revoke"
                ))
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS
                ))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/.well-known/oauth-authorization-server",
                                "/oauth2/jwks",
                                "/oauth2/login",
                                "/oauth2/token",
                                "/oauth2/revoke",
                                "/actuator/health/liveness",
                                "/actuator/health/readiness"
                        ).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(
                        jwt -> jwt.jwtAuthenticationConverter(converter)
                ))
                .headers(headers -> headers
                        .contentTypeOptions(contentType -> { })
                        .referrerPolicy(referrer -> referrer.policy(
                                org.springframework.security.web.header.writers
                                        .ReferrerPolicyHeaderWriter.ReferrerPolicy
                                        .NO_REFERRER
                        )))
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(IdpAdminAuthorizationPort.class)
    IdpAdminAuthorizationPort failClosedIdpAdminAuthorizationPort() {
        return (principal, permission) -> {
            throw new AccessDeniedException(
                    "RBAC3 authorization adapter is not configured"
            );
        };
    }
}
