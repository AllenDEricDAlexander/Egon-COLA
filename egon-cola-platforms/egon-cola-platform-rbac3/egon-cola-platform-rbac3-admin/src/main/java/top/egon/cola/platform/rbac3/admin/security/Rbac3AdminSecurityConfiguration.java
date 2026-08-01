package top.egon.cola.platform.rbac3.admin.security;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.annotation.AnnotationTemplateExpressionDefaults;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import top.egon.cola.platform.rbac3.admin.tenant.TenantContextFilter;
import top.egon.cola.platform.rbac3.admin.tenant.TenantContextResolver;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class Rbac3AdminSecurityConfiguration {

    @Bean
    static AnnotationTemplateExpressionDefaults annotationTemplateExpressionDefaults() {
        return new AnnotationTemplateExpressionDefaults();
    }

    @Bean
    TenantContextResolver tenantContextResolver() {
        return new TenantContextResolver();
    }

    @Bean
    TenantContextFilter tenantContextFilter(TenantContextResolver resolver) {
        return new TenantContextFilter(resolver);
    }

    @Bean
    SecurityFilterChain rbac3SecurityFilterChain(
            HttpSecurity http,
            TenantContextFilter tenantFilter,
            Rbac3JwtAuthenticationConverter authenticationConverter
    ) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health/liveness",
                                "/actuator/health/readiness",
                                "/api/rbac3/v1/auth/login",
                                "/api/rbac3/v1/auth/refresh",
                                "/api/rbac3/v1/auth/jwks")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(
                        jwt -> jwt.jwtAuthenticationConverter(authenticationConverter)))
                .addFilterAfter(tenantFilter, AnonymousAuthenticationFilter.class)
                .build();
    }

    @Bean
    Jackson2ObjectMapperBuilderCustomizer failOnUnknownJsonFields() {
        return builder -> builder.featuresToEnable(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
