package top.egon.cola.component.gateway.mcp.engine.bootstrap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 中文说明：MCP 管理端仅开放健康探针，数据面的身份与权限仍由 MCP 处理器执行。
 * English summary: Keeps the MCP management server stateless and separate from data-plane authorization.
 */
@Configuration(proxyBeanMethods = false)
public class McpGatewayManagementSecurityConfiguration {

    /** Exposes health probes while denying unrelated management-server requests. */
    @Bean
    SecurityFilterChain mcpGatewayManagementSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .requestCache(cache -> cache.disable())
                .securityContext(context -> context.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .anyRequest().denyAll())
                .build();
    }
}
