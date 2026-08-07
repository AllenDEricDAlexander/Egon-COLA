package top.egon.cola.component.ddc.admin.security.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import top.egon.cola.component.ddc.admin.config.DdcAdminProperties;
import top.egon.cola.component.ddc.admin.security.openapi.DdcOpenApiHmacFilter;
import top.egon.cola.platform.idp.starter.security.IdpBearerAuthenticationFilter;
import top.egon.cola.platform.rbac3.starter.security.Rbac3BearerAuthenticationFilter;

import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
@EnableConfigurationProperties(DdcAdminProperties.class)
public class DdcAdminSecurityConfiguration {

    @Bean
    public SecurityFilterChain ddcAdminSecurityFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper,
            DdcOpenApiHmacFilter hmacFilter,
            ObjectProvider<IdpBearerAuthenticationFilter> idpFilters,
            ObjectProvider<Rbac3BearerAuthenticationFilter> rbac3Filters)
            throws Exception {
        DdcAdminAuthenticationEntryPoint securityHandler =
                new DdcAdminAuthenticationEntryPoint(objectMapper);
        IdpBearerAuthenticationFilter idpFilter = idpFilters.getIfAvailable();
        Rbac3BearerAuthenticationFilter rbac3Filter = rbac3Filters.getIfAvailable();
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS
                ))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/actuator/health/**",
                                "/actuator/info"
                        ).permitAll()
                        .requestMatchers("/api/v1/ddc/openapi/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/bootstrap")
                        .authenticated()
                        .requestMatchers("/api/v1/ddc/cache/**")
                        .hasAnyAuthority(
                                DdcAdminCapability.CACHE.authority(),
                                DdcAdminCapability.ALL.authority()
                        )
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/ddc/configs/*/publish",
                                "/api/v1/ddc/publish-tasks/*/retry"
                        ).hasAnyAuthority(
                                DdcAdminCapability.PUBLISH.authority(),
                                DdcAdminCapability.ALL.authority()
                        )
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/ddc/configs",
                                "/api/v1/ddc/configs/*/versions",
                                "/api/v1/ddc/apps",
                                "/api/v1/ddc/apps/*",
                                "/api/v1/ddc/bizs",
                                "/api/v1/ddc/bizs/*",
                                "/api/v1/ddc/envs",
                                "/api/v1/ddc/envs/*",
                                "/api/v1/ddc/namespaces",
                                "/api/v1/ddc/namespaces/*",
                                "/api/v1/ddc/namespace-env-app-bindings",
                                "/api/v1/ddc/instances",
                                "/api/v1/ddc/registry/**",
                                "/api/v1/ddc/publish-tasks",
                                "/api/v1/ddc/publish-tasks/*"
                        ).hasAnyAuthority(
                                DdcAdminCapability.READ.authority(),
                                DdcAdminCapability.ALL.authority()
                        )
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/ddc/configs",
                                "/api/v1/ddc/configs/*/rollback",
                                "/api/v1/ddc/apps",
                                "/api/v1/ddc/bizs",
                                "/api/v1/ddc/envs",
                                "/api/v1/ddc/namespaces",
                                "/api/v1/ddc/namespace-env-app-bindings"
                        ).hasAnyAuthority(
                                DdcAdminCapability.WRITE.authority(),
                                DdcAdminCapability.ALL.authority()
                        )
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/v1/ddc/configs/*",
                                "/api/v1/ddc/apps/*",
                                "/api/v1/ddc/apps/*/enabled",
                                "/api/v1/ddc/bizs/*",
                                "/api/v1/ddc/bizs/*/enabled",
                                "/api/v1/ddc/envs/*",
                                "/api/v1/ddc/envs/*/enabled",
                                "/api/v1/ddc/namespaces/*",
                                "/api/v1/ddc/namespaces/*/enabled",
                                "/api/v1/ddc/namespace-env-app-bindings/*"
                        ).hasAnyAuthority(
                                DdcAdminCapability.WRITE.authority(),
                                DdcAdminCapability.ALL.authority()
                        )
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/ddc/configs/*",
                                "/api/v1/ddc/apps/*",
                                "/api/v1/ddc/bizs/*",
                                "/api/v1/ddc/envs/*",
                                "/api/v1/ddc/namespaces/*",
                                "/api/v1/ddc/namespace-env-app-bindings/*"
                        ).hasAnyAuthority(
                                DdcAdminCapability.WRITE.authority(),
                                DdcAdminCapability.ALL.authority()
                        )
                        .anyRequest().denyAll()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(securityHandler)
                        .accessDeniedHandler(securityHandler)
                )
                .addFilterBefore(
                        hmacFilter,
                        BearerTokenAuthenticationFilter.class
                );
        if (idpFilter != null && rbac3Filter != null) {
            http.addFilterBefore(idpFilter, AnonymousAuthenticationFilter.class);
            http.addFilterAfter(rbac3Filter, IdpBearerAuthenticationFilter.class);
        } else if (idpFilter == null && rbac3Filter == null) {
            http.oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(
                            new DdcAdminJwtAuthenticationConverter()))
                    .authenticationEntryPoint(securityHandler)
                    .accessDeniedHandler(securityHandler));
        } else {
            throw new IllegalStateException(
                    "IdP and RBAC3 authentication filters must be configured together");
        }
        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    public JwtDecoder ddcAdminJwtDecoder(DdcAdminProperties properties) {
        DdcAdminProperties.Jwt jwt = properties.getSecurity().getJwt();
        NimbusJwtDecoder decoder;
        if (hasText(jwt.getJwkSetUri())) {
            decoder = NimbusJwtDecoder
                    .withJwkSetUri(jwt.getJwkSetUri().trim())
                    .build();
        } else if (hasText(jwt.getHmacSecretBase64())) {
            decoder = hmacDecoder(jwt.getHmacSecretBase64());
        } else {
            return token -> {
                throw new JwtException(
                        "DDC Admin JWT decoder is not configured"
                );
            };
        }
        decoder.setJwtValidator(jwtValidator(
                jwt.getIssuer(),
                jwt.getAudience()
        ));
        return decoder;
    }

    private NimbusJwtDecoder hmacDecoder(String hmacSecretBase64) {
        byte[] secret;
        try {
            secret = Base64.getDecoder().decode(
                    hmacSecretBase64.trim()
            );
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException(
                    "DDC Admin HMAC secret is not valid Base64",
                    failure
            );
        }
        if (secret.length < 32) {
            throw new IllegalArgumentException(
                    "DDC Admin HMAC secret must contain at least 32 bytes"
            );
        }
        return NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(secret, "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    private OAuth2TokenValidator<Jwt> jwtValidator(
            String issuer,
            String audience) {
        OAuth2TokenValidator<Jwt> standard = hasText(issuer)
                ? JwtValidators.createDefaultWithIssuer(issuer.trim())
                : JwtValidators.createDefault();
        if (!hasText(audience)) {
            return standard;
        }
        String expectedAudience = audience.trim();
        OAuth2TokenValidator<Jwt> audienceValidator = jwt ->
                jwt.getAudience().contains(expectedAudience)
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(
                                new OAuth2Error(
                                        "invalid_token",
                                        "DDC Admin JWT audience is invalid",
                                        null
                                )
                        );
        return new DelegatingOAuth2TokenValidator<>(
                standard,
                audienceValidator
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
