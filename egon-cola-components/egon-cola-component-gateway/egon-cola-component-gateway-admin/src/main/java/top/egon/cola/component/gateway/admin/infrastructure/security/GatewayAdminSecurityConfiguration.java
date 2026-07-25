package top.egon.cola.component.gateway.admin.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class GatewayAdminSecurityConfiguration {

    @Bean
    public SecurityFilterChain gatewayAdminSecurityFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper) throws Exception {
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
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                new GatewayAdminJwtAuthenticationConverter()
                        ))
                        .authenticationEntryPoint((request, response, error) ->
                                writeSecurityError(
                                        response,
                                        objectMapper,
                                        HttpServletResponse.SC_UNAUTHORIZED,
                                        "GATEWAY_ADMIN_AUTHENTICATION_REQUIRED"
                                ))
                        .accessDeniedHandler((request, response, error) ->
                                writeSecurityError(
                                        response,
                                        objectMapper,
                                        HttpServletResponse.SC_FORBIDDEN,
                                        "GATEWAY_ADMIN_CAPABILITY_REQUIRED"
                                ))
                );
        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    public JwtDecoder gatewayAdminJwtDecoder(
            @Value("${gateway.admin.security.jwk-set-uri:}")
            String jwkSetUri,
            @Value("${gateway.admin.security.issuer:}")
            String issuer) {
        if (jwkSetUri == null || jwkSetUri.isBlank()) {
            return token -> {
                throw new JwtException(
                        "gateway admin JWT decoder is not configured"
                );
            };
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
