package top.egon.cola.platform.rbac3.admin.security;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.annotation.AnnotationTemplateExpressionDefaults;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import top.egon.cola.platform.idp.starter.security.IdpBearerAuthenticationFilter;
import top.egon.cola.platform.rbac3.starter.security.Rbac3BearerAuthenticationFilter;
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
    Rbac3AdminPrincipalFilter rbac3AdminPrincipalFilter() {
        return new Rbac3AdminPrincipalFilter();
    }

    @Bean
    @Order(1)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    SecurityFilterChain rbac3InternalSecurityFilterChain(
            HttpSecurity http,
            TenantContextFilter tenantFilter,
            IdpBearerAuthenticationFilter idpFilter)
            throws Exception {
        return http
                .securityMatcher("/internal/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().authenticated())
                .addFilterBefore(idpFilter, AnonymousAuthenticationFilter.class)
                .addFilterAfter(tenantFilter, IdpBearerAuthenticationFilter.class)
                .build();
    }

    @Bean
    @Order(2)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    SecurityFilterChain rbac3SecurityFilterChain(
            HttpSecurity http,
            TenantContextFilter tenantFilter,
            Rbac3JwtAuthenticationConverter authenticationConverter,
            Rbac3AdminPrincipalFilter principalFilter,
            ObjectProvider<IdpBearerAuthenticationFilter> idpFilters,
            ObjectProvider<Rbac3BearerAuthenticationFilter> rbac3Filters
    ) throws Exception {
        IdpBearerAuthenticationFilter idpFilter = idpFilters.getIfAvailable();
        Rbac3BearerAuthenticationFilter rbac3Filter = rbac3Filters.getIfAvailable();
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health/liveness",
                                "/actuator/health/readiness")
                        .permitAll()
                        .anyRequest().authenticated());
        if (idpFilter != null && rbac3Filter != null) {
            http.addFilterBefore(idpFilter, AnonymousAuthenticationFilter.class);
            http.addFilterAfter(rbac3Filter, IdpBearerAuthenticationFilter.class);
            http.addFilterAfter(principalFilter, Rbac3BearerAuthenticationFilter.class);
            http.addFilterAfter(tenantFilter, Rbac3AdminPrincipalFilter.class);
        } else if (idpFilter == null && rbac3Filter == null) {
            http.oauth2ResourceServer(resourceServer -> resourceServer.jwt(
                    jwt -> jwt.jwtAuthenticationConverter(authenticationConverter)));
            http.addFilterAfter(tenantFilter, AnonymousAuthenticationFilter.class);
        } else {
            throw new IllegalStateException(
                    "IdP and RBAC3 authentication filters must be configured together");
        }
        return http.build();
    }

    @Bean
    Jackson2ObjectMapperBuilderCustomizer failOnUnknownJsonFields() {
        return builder -> builder.featuresToEnable(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
