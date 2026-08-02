package top.egon.cola.component.gateway.test.identity;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import top.egon.cola.platform.idp.starter.security.IdpBearerAuthenticationFilter;
import top.egon.cola.platform.rbac3.starter.security.Rbac3BearerAuthenticationFilter;

@Configuration(proxyBeanMethods = false)
public class MockBackendSecurityConfiguration {

    @Bean
    SecurityFilterChain mockBackendSecurityFilterChain(
            HttpSecurity http,
            ObjectProvider<IdpBearerAuthenticationFilter> idpFilters,
            ObjectProvider<Rbac3BearerAuthenticationFilter> rbac3Filters)
            throws Exception {
        IdpBearerAuthenticationFilter idpFilter = idpFilters.getIfAvailable();
        Rbac3BearerAuthenticationFilter rbac3Filter = rbac3Filters.getIfAvailable();
        if (idpFilter == null || rbac3Filter == null) {
            throw new IllegalStateException(
                    "IdP and RBAC3 authentication filters are required");
        }
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health/**", "/actuator/info")
                        .permitAll()
                        .requestMatchers("/api/mock/**").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(
                                HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(idpFilter, AnonymousAuthenticationFilter.class)
                .addFilterAfter(rbac3Filter, IdpBearerAuthenticationFilter.class);
        return http.build();
    }
}
