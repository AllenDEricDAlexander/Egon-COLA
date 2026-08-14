package top.egon.cola.component.gateway.admin.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import top.egon.cola.platform.idp.starter.security.IdpBearerAuthenticationFilter;
import top.egon.cola.platform.rbac3.starter.security.Rbac3BearerAuthenticationFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

/**
 * 中文说明：{@code GatewayAdminSecurityConfiguration} 是配置类，位于当前 Gateway 模块的相关包中，负责网关管理端安全配置相关的职责与边界。
 * English summary: {@code GatewayAdminSecurityConfiguration} is a gateway admin security configuration configuration in the current Gateway module; it owns the gateway admin security configuration-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class GatewayAdminSecurityConfiguration {

    /**
     * Creates the narrow SERVICE-scope adapter used by Gateway Admin control-plane calls.
     *
     * @return service-scope authority adapter
     */
    @Bean
    public GatewayAdminServiceAuthorityFilter gatewayAdminServiceAuthorityFilter() {
        return new GatewayAdminServiceAuthorityFilter();
    }

    /**
     * 中文说明：执行 网关管理端安全过滤器Chain 操作；该方法是 {@code GatewayAdminSecurityConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway admin security filter chain operation; this method is the invocation entry point on {@code GatewayAdminSecurityConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminSecurityConfiguration.gatewayAdminSecurityFilterChain(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param http 参数 http；parameter http。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param idpFilters 参数 idpFilters；parameter idp filters。
     * @param rbac3Filters 参数 rbac3Filters；parameter rbac3 filters。
     * @return 返回 网关管理端安全过滤器Chain 的处理结果；returns the result of the operation.
     */
    @Bean
    public SecurityFilterChain gatewayAdminSecurityFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper,
            ObjectProvider<IdpBearerAuthenticationFilter> idpFilters,
            ObjectProvider<Rbac3BearerAuthenticationFilter> rbac3Filters,
            ObjectProvider<GatewayAdminServiceAuthorityFilter> serviceAuthorityFilters)
            throws Exception {
        IdpBearerAuthenticationFilter idpFilter = idpFilters.getIfAvailable();
        Rbac3BearerAuthenticationFilter rbac3Filter = rbac3Filters.getIfAvailable();
        GatewayAdminServiceAuthorityFilter serviceAuthorityFilter =
                serviceAuthorityFilters.getIfAvailable();
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
        if (idpFilter != null && rbac3Filter != null
                && serviceAuthorityFilter != null) {
            http.addFilterBefore(idpFilter, AnonymousAuthenticationFilter.class);
            http.addFilterAfter(
                    serviceAuthorityFilter,
                    IdpBearerAuthenticationFilter.class);
            http.addFilterAfter(
                    rbac3Filter,
                    GatewayAdminServiceAuthorityFilter.class);
        } else {
            throw new IllegalStateException(
                    "IdP, Gateway service, and RBAC3 authentication filters must be configured together");
        }
        return http.build();
    }

    /**
     * 中文说明：执行 write安全Error 操作；该方法是 {@code GatewayAdminSecurityConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the write security error operation; this method is the invocation entry point on {@code GatewayAdminSecurityConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminSecurityConfiguration.writeSecurityError(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param response 参数 响应；parameter response。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param status 参数 status；parameter status。
     * @param code 参数 code；parameter code。
     */
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
