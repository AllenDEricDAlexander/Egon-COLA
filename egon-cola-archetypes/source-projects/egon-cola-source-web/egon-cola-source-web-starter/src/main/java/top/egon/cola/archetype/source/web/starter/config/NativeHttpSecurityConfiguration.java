package top.egon.cola.archetype.source.web.starter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import top.egon.cola.archetype.source.web.adapter.filter.OrganizationAuthContextFilter;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3BearerAuthenticationFilter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpBearerAuthenticationFilter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpEndpointAuthenticationPolicy;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpSecurityObservationFilter;

/** Owns the stateless USER business boundary; the existing higher-priority document chain retains its SERVICE policy. */
@Configuration(value = "nativeHttpSecurityConfiguration", proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableMethodSecurity
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class NativeHttpSecurityConfiguration {
    private static final String[] PUBLIC_PROBES = {"/actuator/health/liveness", "/actuator/health/readiness"};
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    @Bean("idpEndpointAuthenticationPolicy")
    IdpEndpointAuthenticationPolicy endpointPolicy() { return new IdpEndpointAuthenticationPolicy(List.of(PUBLIC_PROBES), List.of()); }

    @Bean("applicationSecurityFilterChain")
    @Order(Ordered.LOWEST_PRECEDENCE)
    SecurityFilterChain applicationSecurityFilterChain(HttpSecurity http,
            @Qualifier("idpBearerAuthenticationFilter") ObjectProvider<IdpBearerAuthenticationFilter> identities,
            @Qualifier("rbac3BearerAuthenticationFilter") ObjectProvider<Rbac3BearerAuthenticationFilter> authorization,
            @Qualifier("idpSecurityObservationFilter") ObjectProvider<IdpSecurityObservationFilter> observations,
            @Qualifier("organizationAuthContextFilter") OrganizationAuthContextFilter context) throws Exception {
        var identity = identities.getIfAvailable(); var rbac = authorization.getIfAvailable(); var audit = observations.getIfAvailable();
        if (identity == null || rbac == null || audit == null) { throw new IllegalStateException("NATIVE_SECURITY_NOT_READY"); }
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable()).httpBasic(basic -> basic.disable()).logout(logout -> logout.disable())
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(authorize -> authorize.requestMatchers(PUBLIC_PROBES).permitAll()
                        .requestMatchers("/api/v1/**", "/graphql").authenticated().anyRequest().denyAll())
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, failure) -> authenticationFailure(request, response))
                        .accessDeniedHandler((request, response, failure) -> authorizationFailure(request, response, failure)));
        http.addFilterBefore(identity, AnonymousAuthenticationFilter.class);
        http.addFilterAfter(rbac, IdpBearerAuthenticationFilter.class);
        http.addFilterBefore(audit, IdpBearerAuthenticationFilter.class);
        http.addFilterAfter(context, AuthorizationFilter.class);
        return http.build();
    }

    @Bean("nativeDomainContextFilterRegistration")
    FilterRegistrationBean<OrganizationAuthContextFilter> contextRegistration(@Qualifier("organizationAuthContextFilter") OrganizationAuthContextFilter filter) {
        var registration = new FilterRegistrationBean<>(filter); registration.setEnabled(false); return registration;
    }

    @Bean("idpBearerFilterRegistration")
    FilterRegistrationBean<IdpBearerAuthenticationFilter> identityRegistration(@Qualifier("idpBearerAuthenticationFilter") IdpBearerAuthenticationFilter filter) {
        var registration = new FilterRegistrationBean<>(filter); registration.setEnabled(false); return registration;
    }

    @Bean("rbac3BearerFilterRegistration")
    FilterRegistrationBean<Rbac3BearerAuthenticationFilter> authorizationRegistration(@Qualifier("rbac3BearerAuthenticationFilter") Rbac3BearerAuthenticationFilter filter) {
        var registration = new FilterRegistrationBean<>(filter); registration.setEnabled(false); return registration;
    }

    private void authenticationFailure(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!IdpSecurityObservationFilter.beforeAuthenticationFailure(request)) { auditUnavailable(response); return; }
        response.setHeader("WWW-Authenticate", "Bearer");
        writeError(response, 401, "AUTHENTICATION_REQUIRED", "Tianquan-Shoubing authentication failed");
    }

    private void authorizationFailure(HttpServletRequest request, HttpServletResponse response, AccessDeniedException failure) throws IOException {
        var marker = IdpSecurityObservationFilter.authorizationObservationContext(request);
        if (marker == null || !IdpSecurityObservationFilter.beforeAuthorizationDenied(request) || !marker.isConfirmed()) {
            auditUnavailable(response); return;
        }
        String reason = "FIELD_ACCESS_DENIED".equals(failure.getMessage()) ? "FIELD_ACCESS_DENIED" : "AUTHORIZATION_DENIED";
        writeError(response, 403, reason, "Tianquan-Jianshen authorization denied");
    }

    private void auditUnavailable(HttpServletResponse response) throws IOException {
        writeError(response, 503, "OAUTH_AUDIT_DELIVERY_UNAVAILABLE", "Tianquan-Shoubing authentication state is temporarily unavailable");
    }

    private void writeError(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status); response.setHeader("Cache-Control", "no-store"); response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of("code", code, "message", message));
    }
}
