package top.egon.cola.component.yuheng.test.mcp.provider;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpBearerAuthenticationFilter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpEndpointAuthenticationPolicy;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3BearerAuthenticationFilter;

import java.util.List;

@Configuration(proxyBeanMethods = false)
public class McpTestProviderSecurityConfiguration {

    /** Provider Operation 的 SERVICE Scope；SERVICE scope required by provider operations. */
    private static final String SERVICE_SCOPE = "mcp:operation:invoke";

    @Bean
    IdpEndpointAuthenticationPolicy mcpTestProviderEndpointPolicy() {
        return new IdpEndpointAuthenticationPolicy(
                List.of("/actuator/health/**", "/actuator/info"),
                List.of(),
                List.of("/api/mcp-fixtures/**"),
                false
        );
    }

    @Bean
    SecurityFilterChain mcpTestProviderSecurityFilterChain(
            HttpSecurity http,
            ObjectProvider<IdpBearerAuthenticationFilter> idpFilters,
            ObjectProvider<Rbac3BearerAuthenticationFilter> rbac3Filters)
            throws Exception {
        IdpBearerAuthenticationFilter idpFilter = idpFilters.getIfAvailable();
        Rbac3BearerAuthenticationFilter rbac3Filter =
                rbac3Filters.getIfAvailable();
        if (idpFilter == null || rbac3Filter == null) {
            throw new IllegalStateException(
                    "Tianquan-Shoubing and Tianquan-Jianshen authentication filters are required"
            );
        }
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health/**", "/actuator/info")
                        .permitAll()
                        .requestMatchers("/api/mcp-fixtures/**")
                        .access((authentication, context) ->
                                providerAccess(authentication.get()))
                        .anyRequest().denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(
                                HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(
                        idpFilter,
                        AnonymousAuthenticationFilter.class
                )
                .addFilterAfter(
                        rbac3Filter,
                        IdpBearerAuthenticationFilter.class
                );
        return http.build();
    }

    /**
     * USER 身份沿用 Gateway/Tianquan-Jianshen 前置决策，SERVICE 身份只读取 Tianquan-Shoubing 签名 Scope。
     * Keeps the upstream Gateway/Tianquan-Jianshen decision for USER identities and evaluates only the
     * Tianquan-Shoubing-signed scope for SERVICE identities.
     */
    private AuthorizationDecision providerAccess(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return new AuthorizationDecision(false);
        }
        if (authentication.getPrincipal()
                instanceof ServiceIdentityPrincipal service) {
            return new AuthorizationDecision(
                    service.scopes().contains(SERVICE_SCOPE)
            );
        }
        return new AuthorizationDecision(true);
    }
}
