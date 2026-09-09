package top.egon.cola.component.tianshu.admin.security.management;

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
import top.egon.cola.component.tianshu.admin.config.DdcAdminProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpBearerAuthenticationFilter;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3BearerAuthenticationFilter;

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
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/auth/bootstrap",
                                "/api/v1/tianshu/auth/bootstrap"
                        )
                        .authenticated()
                        .requestMatchers("/api/v1/tianshu/cache/**")
                        .hasAnyAuthority(
                                DdcAdminCapability.CACHE.authority(),
                                DdcAdminCapability.ALL.authority()
                        )
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/tianshu/configs/*/publish",
                                "/api/v1/tianshu/publish-tasks/*/retry"
                        ).hasAnyAuthority(
                                DdcAdminCapability.PUBLISH.authority(),
                                DdcAdminCapability.ALL.authority()
                        )
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/tianshu/configs",
                                "/api/v1/tianshu/configs/*/versions",
                                "/api/v1/tianshu/apps",
                                "/api/v1/tianshu/apps/*",
                                "/api/v1/tianshu/bizs",
                                "/api/v1/tianshu/bizs/*",
                                "/api/v1/tianshu/envs",
                                "/api/v1/tianshu/envs/*",
                                "/api/v1/tianshu/namespaces",
                                "/api/v1/tianshu/namespaces/*",
                                "/api/v1/tianshu/namespace-env-app-bindings",
                                "/api/v1/tianshu/instances",
                                "/api/v1/tianshu/registry/**",
                                "/api/v1/tianshu/publish-tasks",
                                "/api/v1/tianshu/publish-tasks/*"
                        ).hasAnyAuthority(
                                DdcAdminCapability.READ.authority(),
                                DdcAdminCapability.ALL.authority()
                        )
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/tianshu/configs/page",
                                "/api/v1/tianshu/configs/*/versions/page",
                                "/api/v1/tianshu/apps/page",
                                "/api/v1/tianshu/bizs/page",
                                "/api/v1/tianshu/envs/page",
                                "/api/v1/tianshu/namespaces/page",
                                "/api/v1/tianshu/namespace-env-app-bindings/page",
                                "/api/v1/tianshu/instances/page",
                                "/api/v1/tianshu/publish-tasks/page"
                        ).hasAnyAuthority(
                                DdcAdminCapability.READ.authority(),
                                DdcAdminCapability.ALL.authority()
                        )
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/tianshu/configs",
                                "/api/v1/tianshu/configs/*/rollback",
                                "/api/v1/tianshu/apps",
                                "/api/v1/tianshu/bizs",
                                "/api/v1/tianshu/envs",
                                "/api/v1/tianshu/namespaces",
                                "/api/v1/tianshu/namespace-env-app-bindings"
                        ).hasAnyAuthority(
                                DdcAdminCapability.WRITE.authority(),
                                DdcAdminCapability.ALL.authority()
                        )
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/v1/tianshu/configs/*",
                                "/api/v1/tianshu/apps/*",
                                "/api/v1/tianshu/apps/*/enabled",
                                "/api/v1/tianshu/bizs/*",
                                "/api/v1/tianshu/bizs/*/enabled",
                                "/api/v1/tianshu/envs/*",
                                "/api/v1/tianshu/envs/*/enabled",
                                "/api/v1/tianshu/namespaces/*",
                                "/api/v1/tianshu/namespaces/*/enabled",
                                "/api/v1/tianshu/namespace-env-app-bindings/*"
                        ).hasAnyAuthority(
                                DdcAdminCapability.WRITE.authority(),
                                DdcAdminCapability.ALL.authority()
                        )
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/tianshu/configs/*",
                                "/api/v1/tianshu/apps/*",
                                "/api/v1/tianshu/bizs/*",
                                "/api/v1/tianshu/envs/*",
                                "/api/v1/tianshu/namespaces/*",
                                "/api/v1/tianshu/namespace-env-app-bindings/*"
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
                    "Tianquan-Shoubing and Tianquan-Jianshen authentication filters must be configured together");
        }
        return http.build();
    }

}
