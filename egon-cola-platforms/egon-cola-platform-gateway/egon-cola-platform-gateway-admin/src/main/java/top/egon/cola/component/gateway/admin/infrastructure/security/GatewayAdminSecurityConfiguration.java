package top.egon.cola.component.gateway.admin.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import top.egon.cola.platform.idp.starter.security.IdpBearerAuthenticationFilter;
import top.egon.cola.platform.rbac3.starter.security.Rbac3BearerAuthenticationFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class GatewayAdminSecurityConfiguration {

    @Bean
    public SecurityFilterChain gatewayAdminSecurityFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper,
            ObjectProvider<IdpBearerAuthenticationFilter> idpFilters,
            ObjectProvider<Rbac3BearerAuthenticationFilter> rbac3Filters)
            throws Exception {
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
                                "/api/v1/gateway/openapi/interface-definitions/**",
                                "/actuator/health/**",
                                "/actuator/info"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, error) ->
                                writeSecurityError(response, objectMapper,
                                        HttpServletResponse.SC_UNAUTHORIZED,
                                        "GATEWAY_ADMIN_AUTHENTICATION_REQUIRED"))
                        .accessDeniedHandler((request, response, error) ->
                                writeSecurityError(response, objectMapper,
                                        HttpServletResponse.SC_FORBIDDEN,
                                        "GATEWAY_ADMIN_CAPABILITY_REQUIRED")));
        if (idpFilter != null && rbac3Filter != null) {
            http.addFilterBefore(idpFilter, AnonymousAuthenticationFilter.class);
            http.addFilterAfter(rbac3Filter, IdpBearerAuthenticationFilter.class);
        } else if (idpFilter == null && rbac3Filter == null) {
            http.oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(
                            new GatewayAdminJwtAuthenticationConverter()))
                    .authenticationEntryPoint((request, response, error) ->
                            writeSecurityError(response, objectMapper,
                                    HttpServletResponse.SC_UNAUTHORIZED,
                                    "GATEWAY_ADMIN_AUTHENTICATION_REQUIRED"))
                    .accessDeniedHandler((request, response, error) ->
                            writeSecurityError(response, objectMapper,
                                    HttpServletResponse.SC_FORBIDDEN,
                                    "GATEWAY_ADMIN_CAPABILITY_REQUIRED")));
        } else {
            throw new IllegalStateException(
                    "IdP and RBAC3 authentication filters must be configured together");
        }
        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    public JwtDecoder gatewayAdminJwtDecoder(
            @Value("${gateway.admin.security.jwk-set-uri:}")
            String jwkSetUri,
            @Value("${gateway.admin.security.issuer:}")
            String issuer,
            @Value("${gateway.admin.security.hmac-secret-base64:}")
            String hmacSecretBase64) {
        if (jwkSetUri == null || jwkSetUri.isBlank()) {
            return hmacDecoder(hmacSecretBase64, issuer);
        }
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(jwkSetUri.trim())
                .build();
        if (issuer != null && !issuer.isBlank()) {
            decoder.setJwtValidator(
                    JwtValidators.createDefaultWithIssuer(issuer.trim())
            );
        }
        return decoder;
    }

    private JwtDecoder hmacDecoder(
            String hmacSecretBase64,
            String issuer) {
        if (hmacSecretBase64 == null || hmacSecretBase64.isBlank()) {
            return token -> {
                throw new JwtException(
                        "gateway admin JWT decoder is not configured"
                );
            };
        }
        byte[] secret;
        try {
            secret = Base64.getDecoder().decode(hmacSecretBase64.trim());
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException(
                    "gateway admin HMAC secret is not valid Base64",
                    failure
            );
        }
        if (secret.length < 32) {
            throw new IllegalArgumentException(
                    "gateway admin HMAC secret must contain at least 32 bytes"
            );
        }
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(secret, "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        if (issuer != null && !issuer.isBlank()) {
            decoder.setJwtValidator(
                    JwtValidators.createDefaultWithIssuer(issuer.trim())
            );
        }
        return decoder;
    }

    private void writeSecurityError(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            int status,
            String code) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "code", code,
                "message", code,
                "timestamp", Instant.now().toString()
        ));
    }
}
