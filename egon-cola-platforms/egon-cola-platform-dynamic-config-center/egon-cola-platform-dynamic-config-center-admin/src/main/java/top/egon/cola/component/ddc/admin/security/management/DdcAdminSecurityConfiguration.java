package top.egon.cola.component.ddc.admin.security.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import top.egon.cola.component.ddc.admin.config.DdcAdminProperties;
import top.egon.cola.platform.idp.starter.security.IdpBearerAuthenticationFilter;
import top.egon.cola.platform.rbac3.starter.security.Rbac3BearerAuthenticationFilter;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
@EnableConfigurationProperties(DdcAdminProperties.class)
public class DdcAdminSecurityConfiguration {

    @Bean
    public SecurityFilterChain ddcAdminSecurityFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper,
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
                );
        if (idpFilter != null && rbac3Filter != null) {
            http.addFilterBefore(idpFilter, AnonymousAuthenticationFilter.class);
            http.addFilterAfter(rbac3Filter, IdpBearerAuthenticationFilter.class);
        } else {
            throw new IllegalStateException(
                    "IdP and RBAC3 authentication filters must be configured together");
        }
        return http.build();
    }

}
