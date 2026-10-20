package top.egon.cola.component.yuheng.engine.bootstrap.config;

import org.springframework.context.annotation.Bean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Keeps the engine's Spring management server stateless and separate from
 * data-plane authorization, which is enforced by the Gateway processors.
 */
@Slf4j
@Configuration(value = "gatewayEngineManagementSecurityConfiguration", proxyBeanMethods = false)
public class GatewayEngineManagementSecurityConfiguration {

    /** Exposes health probes while denying unrelated management-server requests. */
    @Bean(name = "gatewayEngineManagementSecurityFilterChain")
    SecurityFilterChain gatewayEngineManagementSecurityFilterChain(
            HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .requestCache(cache -> cache.disable())
                .securityContext(context -> context.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS
                ))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/actuator/health/**",
                                "/actuator/info"
                        ).permitAll()
                        .anyRequest().denyAll()
                )
                .build();
    }

    /** Composes the shared audit readiness with this existing health-only management surface. */
    @Bean(name = "gatewayEngineAuditReadiness")
    org.springframework.boot.actuate.health.HealthIndicator auditReadiness(
            @Qualifier("idpSecurityAuditReady") java.util.function.BooleanSupplier ready) {
        return () -> ready.getAsBoolean() ? org.springframework.boot.actuate.health.Health.up().build()
                : org.springframework.boot.actuate.health.Health.down().withDetail("failedCheck", "oauthAudit").build();
    }

    @Bean(name = "gatewayEngineSecurityObservationStartupCheck")
    Object securityObservationStartupCheck(
            @Qualifier("oauthUsageObservationProperties") top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties usage,
            @Qualifier("idpSecurityAuditReady") java.util.function.BooleanSupplier ready,
            @Qualifier("idpStarterProperties") top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties identity,
            @Qualifier("defaultValidator") jakarta.validation.Validator validator,
            @Qualifier("environment") org.springframework.core.env.Environment environment) {
        new top.egon.cola.component.common.core.validation.ValidationUtils(validator).validate(usage);
        identity.validate();
        boolean localRedis = usage.isAllowLocalPlaintext()
                && java.util.Arrays.asList(environment.getActiveProfiles()).contains("local")
                && "local".equals(environment.getProperty("DEPLOYMENT_ENV"));
        if (!usage.isEnabled() || !ready.getAsBoolean() || identity.getTrustedIssuers().isEmpty()
                || identity.getResourceUri() == null || !"https".equals(identity.getResourceUri().getScheme())
                || (!identity.getRevocation().getPrimaryEndpoint().startsWith("rediss://") && !localRedis)
                || !environment.getProperty("egon.cola.component.yuheng.engine.http.public-tls.enabled", Boolean.class, false)
                || !environment.getProperty("egon.cola.component.yuheng.engine.http.internal-tls.enabled", Boolean.class, false)) {
            throw new IllegalStateException("YUHENG_SECURITY_OBSERVATION_NOT_READY");
        }
        return new Object();
    }
}
