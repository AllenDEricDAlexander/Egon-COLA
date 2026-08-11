package top.egon.cola.component.gateway.test.mcp.provider;

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
import top.egon.cola.platform.idp.starter.security.IdpBearerAuthenticationFilter;
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.rbac3.starter.security.Rbac3BearerAuthenticationFilter;

@Configuration(proxyBeanMethods = false)
public class McpTestProviderSecurityConfiguration {

    /** Provider Operation 的 SERVICE Scope；SERVICE scope required by provider operations. */
    private static final String SERVICE_SCOPE = "mcp:operation:invoke";

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
                    "IdP and RBAC3 authentication filters are required"
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
     * USER 身份沿用 Gateway/RBAC3 前置决策，SERVICE 身份只读取 IdP 签名 Scope。
     * Keeps the upstream Gateway/RBAC3 decision for USER identities and evaluates only the
     * IdP-signed scope for SERVICE identities.
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
