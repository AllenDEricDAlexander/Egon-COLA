package top.egon.cola.component.yuheng.engine.bootstrap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Keeps the engine's Spring management server stateless and separate from
 * data-plane authorization, which is enforced by the Gateway processors.
 */
@Configuration(proxyBeanMethods = false)
public class GatewayEngineManagementSecurityConfiguration {

    /** Exposes health probes while denying unrelated management-server requests. */
    @Bean
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
}
