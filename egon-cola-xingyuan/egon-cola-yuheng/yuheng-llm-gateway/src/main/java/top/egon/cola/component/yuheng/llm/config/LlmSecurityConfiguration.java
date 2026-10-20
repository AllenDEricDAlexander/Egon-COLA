package top.egon.cola.component.yuheng.llm.config;

import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpBearerAuthenticationFilter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpEndpointAuthenticationPolicy;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpSecurityObservationFilter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditProofBO;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmProtocolEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.exception.LlmInvocationException;
import top.egon.cola.component.yuheng.llm.proxy.service.LlmProtocolStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties;
import top.egon.cola.platform.tianquan.jianshen.starter.runtime.Rbac3RuntimeRedissonConfiguration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import top.egon.cola.platform.tianquan.shoubing.contract.IdpPrincipal;

/** Protects native model entries with the shared verifier, scope gate and original model authorization. */
@Slf4j
@Configuration(value = "llmSecurityConfiguration", proxyBeanMethods = false)
@EnableConfigurationProperties(Rbac3StarterProperties.class)
@Import(Rbac3RuntimeRedissonConfiguration.class)
public class LlmSecurityConfiguration {

    /** Reuses the host primary connection under Common cache's explicit integration name. */
    @Bean(name = "redissonClient", destroyMethod = "")
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(name = "redissonClient")
    @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(name = "rbac3RuntimeRedissonClient")
    org.redisson.api.RedissonClient applicationCacheRedissonClient(
            @Qualifier("rbac3RuntimeRedissonClient") org.redisson.api.RedissonClient primary) {
        return primary;
    }
    private static final String BUSINESS_ENTERED = LlmSecurityConfiguration.class.getName() + ".businessEntered";
    private static final Map<String, LlmProtocolEnum> PROTOCOLS = Map.of(
            "/v1/models", LlmProtocolEnum.OPENAI_CHAT, "/v1/model-policies", LlmProtocolEnum.OPENAI_CHAT, "/v1/chat/completions", LlmProtocolEnum.OPENAI_CHAT,
            "/v1/embeddings", LlmProtocolEnum.OPENAI_EMBEDDING, "/v1/responses", LlmProtocolEnum.OPENAI_RESPONSES,
            "/v1/messages", LlmProtocolEnum.ANTHROPIC_MESSAGES);

    @Bean(name = "idpEndpointAuthenticationPolicy")
    IdpEndpointAuthenticationPolicy idpEndpointAuthenticationPolicy() {
        return new IdpEndpointAuthenticationPolicy(List.of("/actuator/health/**", "/actuator/info"), List.of(), PROTOCOLS.keySet(), false);
    }

    @Bean(name = "llmSecurityFilterChain")
    @Order(0)
    SecurityFilterChain llmSecurityFilterChain(HttpSecurity http,
            @Qualifier("idpBearerAuthenticationFilter") IdpBearerAuthenticationFilter bearer,
            @Qualifier("idpSecurityObservationFilter") IdpSecurityObservationFilter observation,
            @Qualifier("llmVerifiedPrincipalRequestFilter") OncePerRequestFilter projection,
            @Qualifier("llmProtocolSecurityResponseFilter") OncePerRequestFilter protocol) throws Exception {
        http.securityMatcher("/v1/**").csrf(csrf -> csrf.disable()).requestCache(cache -> cache.disable())
                .requiresChannel(channel -> channel.anyRequest().requiresSecure())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable()).httpBasic(basic -> basic.disable()).logout(logout -> logout.disable())
                .addFilterBefore(bearer, AnonymousAuthenticationFilter.class)
                .addFilterBefore(observation, IdpBearerAuthenticationFilter.class)
                .addFilterBefore(protocol, IdpSecurityObservationFilter.class)
                .addFilterAfter(projection, AuthorizationFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/v1/models", "/v1/model-policies").access(scope("yuheng:llm:read"))
                        .requestMatchers(HttpMethod.POST, "/v1/chat/completions", "/v1/embeddings", "/v1/responses", "/v1/messages")
                        .access(scope("yuheng:llm:invoke")).anyRequest().denyAll())
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, failure) ->
                        response.setStatus(IdpSecurityObservationFilter.authorizationObservationContext(request) != null
                                && IdpSecurityObservationFilter.beforeAuthenticationFailure(request) ? 401 : 503))
                        .accessDeniedHandler((request, response, failure) -> {
                            var marker = IdpSecurityObservationFilter.authorizationObservationContext(request);
                            response.setStatus(marker != null && IdpSecurityObservationFilter.beforeAuthorizationDenied(request) ? 403 : 503);
                        }));
        return http.build();
    }

    private AuthorizationManager<RequestAuthorizationContext> scope(String value) {
        return (authentication, context) -> {
            var current = authentication.get();
            return new AuthorizationDecision(current != null && current.isAuthenticated()
                    && current.getPrincipal() instanceof IdpPrincipal && current.getDetails() instanceof TokenAuditProofBO
                    && current.getAuthorities().stream().anyMatch(authority -> ("SCOPE_" + value).equals(authority.getAuthority())));
        };
    }

    @Bean(name = "llmProtocolSecurityResponseFilter")
    OncePerRequestFilter llmProtocolSecurityResponseFilter(@Qualifier("jacksonObjectMapper") ObjectMapper mapper,
            @Qualifier("llmSecurityProtocolStrategies") Map<LlmProtocolEnum, LlmProtocolStrategy> strategies) {
        return new ProtocolSecurityResponseFilter(mapper, strategies);
    }

    @Bean(name = "llmSecurityProtocolStrategies")
    Map<LlmProtocolEnum, LlmProtocolStrategy> llmSecurityProtocolStrategies(
            @Qualifier("llmProtocolStrategyRegistry") Map<LlmProtocolEnum, String> registry,
            Map<String, LlmProtocolStrategy> candidates) {
        var selected = new java.util.EnumMap<LlmProtocolEnum, LlmProtocolStrategy>(LlmProtocolEnum.class);
        registry.forEach((protocol, name) -> {
            var strategy = candidates.get(name);
            if (strategy == null || strategy.protocol() != protocol) { throw new IllegalStateException("LLM_SECURITY_PROTOCOL_REQUIRED"); }
            selected.put(protocol, strategy);
        });
        if (selected.size() != LlmProtocolEnum.values().length) { throw new IllegalStateException("LLM_SECURITY_PROTOCOL_REQUIRED"); }
        return Map.copyOf(selected);
    }

    @Bean(name = "llmSecurityObservationStartupCheck")
    Object llmSecurityObservationStartupCheck(
            @Qualifier("idpObservedVerificationComponent") top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent observation,
            @Qualifier("oauthUsageObservationProperties") top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties usage,
            @Qualifier("idpSecurityAuditReady") java.util.function.BooleanSupplier ready,
            @Qualifier("defaultValidator") jakarta.validation.Validator validator,
            @Qualifier("idpStarterProperties") top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties properties,
            @Qualifier("environment") org.springframework.core.env.Environment environment) {
        new top.egon.cola.component.common.core.validation.ValidationUtils(validator).validate(usage);
        if (!observation.enabled() || !ready.getAsBoolean() || !environment.getProperty("server.ssl.enabled", Boolean.class, false)) {
            throw new IllegalStateException("LLM_SECURITY_OBSERVATION_REQUIRED");
        }
        properties.validate();
        boolean localRedis = usage.isAllowLocalPlaintext()
                && java.util.Arrays.asList(environment.getActiveProfiles()).contains("local")
                && "local".equals(environment.getProperty("DEPLOYMENT_ENV"));
        if (!"https".equals(properties.getResourceUri().getScheme()) || properties.getTrustedIssuers().isEmpty()
                || (!properties.getRevocation().getPrimaryEndpoint().startsWith("rediss://") && !localRedis)) {
            throw new IllegalStateException("LLM_SECURITY_TRANSPORT_REQUIRED");
        }
        return new Object();
    }

    @Bean(name = "llmAuditReadiness")
    org.springframework.boot.actuate.health.HealthIndicator llmAuditReadiness(
            @Qualifier("idpSecurityAuditReady") java.util.function.BooleanSupplier ready) {
        return () -> ready.getAsBoolean() ? org.springframework.boot.actuate.health.Health.up().build()
                : org.springframework.boot.actuate.health.Health.down().withDetail("failedCheck", "oauthAudit").build();
    }
    /** Adapts the verified identity to the existing controller's Servlet Principal contract. */
    @Bean(name = "llmVerifiedPrincipalRequestFilter")
    OncePerRequestFilter llmVerifiedPrincipalRequestFilter() { return new VerifiedPrincipalRequestFilter(); }

    @Bean(name = "llmVerifiedPrincipalFilterRegistration")
    org.springframework.boot.web.servlet.FilterRegistrationBean<OncePerRequestFilter> llmVerifiedPrincipalFilterRegistration(
            @Qualifier("llmVerifiedPrincipalRequestFilter") OncePerRequestFilter filter) {
        var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean(name = "llmProtocolSecurityFilterRegistration")
    org.springframework.boot.web.servlet.FilterRegistrationBean<OncePerRequestFilter> llmProtocolSecurityFilterRegistration(
            @Qualifier("llmProtocolSecurityResponseFilter") OncePerRequestFilter filter) {
        var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean(name = "llmSecurityOpenApiCustomizer")
    org.springdoc.core.customizers.GlobalOpenApiCustomizer llmSecurityOpenApiCustomizer() {
        Map<String, String> scopes = Map.of("listLlmModels", "yuheng:llm:read", "describeLlmModelPolicies", "yuheng:llm:read", "createLlmChatCompletion", "yuheng:llm:invoke",
                "createLlmEmbeddings", "yuheng:llm:invoke", "createLlmResponse", "yuheng:llm:invoke", "createLlmMessage", "yuheng:llm:invoke");
        return document -> {
            if (document.getPaths() == null) { return; }
            document.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
                String scope = scopes.get(operation.getOperationId());
                if (scope == null) { return; }
                operation.addExtension("x-required-oauth-scopes", List.of(scope));
                operation.setDescription((operation.getDescription() == null ? "" : operation.getDescription())
                        + " A verified USER or SERVICE access token requires OAuth scope " + scope
                        + " and the existing model subject/capability rules. Security state or audit failure returns native 503.");
                if (operation.getResponses() != null && operation.getResponses().get("403") != null) {
                    operation.getResponses().get("403").setDescription("Required OAuth scope or model authorization is denied");
                }
            }));
        };
    }

    @Slf4j
    private static class VerifiedPrincipalRequestFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof IdpPrincipal identity) {
                String subject = identity.subject();
                if (request.getAttribute(BUSINESS_ENTERED) instanceof AtomicBoolean entered) { entered.set(true); }
                chain.doFilter(new HttpServletRequestWrapper(request) {
                    @Override public Principal getUserPrincipal() { return () -> subject; }
                    @Override public String getRemoteUser() { return subject; }
                }, response);
            } else { chain.doFilter(request, response); }
        }
    }

    /** Decorates security failures only; successful unary/SSE bytes are never buffered. */
    @Slf4j
    @RequiredArgsConstructor
    private static class ProtocolSecurityResponseFilter extends OncePerRequestFilter {
        @Qualifier("jacksonObjectMapper") private final ObjectMapper mapper;
        @Qualifier("llmSecurityProtocolStrategies") private final Map<LlmProtocolEnum, LlmProtocolStrategy> strategies;

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            LlmProtocolEnum protocol = PROTOCOLS.get(request.getRequestURI());
            if (protocol == null) { chain.doFilter(request, response); return; }
            var entered = new AtomicBoolean(); request.setAttribute(BUSINESS_ENTERED, entered);
            var guarded = new NativeSecurityResponse(request, response, entered);
            try {
                chain.doFilter(request, guarded);
                if (guarded.securityFailure() && !response.isCommitted()) {
                    int status = guarded.getStatus();
                    String code = status == 401 ? "authentication_error" : status == 403 ? "permission_denied" : "service_unavailable";
                    String message = status == 401 ? "A verified enterprise identity is required" : status == 403
                            ? "The verified identity lacks the required permission" : "Authentication or authorization is temporarily unavailable";
                    var strategy = strategies.get(protocol);
                    response.setStatus(status); response.setHeader("Cache-Control", "no-store"); response.setContentType("application/json");
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    mapper.writeValue(response.getOutputStream(), strategy.encodeError(new LlmInvocationException(status, code, null, message, status == 503)));
                }
            } finally { request.removeAttribute(BUSINESS_ENTERED); }
        }
    }

    /** Framework response adapter suppresses only a rejected security body, never the model's stream. */
    @Slf4j
    private static class NativeSecurityResponse extends HttpServletResponseWrapper {
        private final HttpServletRequest request;
        private final AtomicBoolean entered;
        private boolean forcedUnavailable;
        private boolean denialChecked;

        NativeSecurityResponse(HttpServletRequest request, HttpServletResponse response, AtomicBoolean entered) {
            super(response); this.request = request; this.entered = entered;
        }

        boolean securityFailure() { return forcedUnavailable || !entered.get() && (getStatus() == 401 || getStatus() == 403 || getStatus() == 503); }

        private void beforeWrite() {
            if (entered.get() && getStatus() == 403 && !denialChecked) {
                denialChecked = true;
                var marker = IdpSecurityObservationFilter.authorizationObservationContext(request);
                if (marker == null || !IdpSecurityObservationFilter.beforeAuthorizationDenied(request)) {
                    forcedUnavailable = true; super.setStatus(503);
                }
            }
        }

        @Override public void sendError(int status) { super.setStatus(status); }
        @Override public void sendError(int status, String message) { super.setStatus(status); }
        @Override public void flushBuffer() throws IOException { beforeWrite(); if (!securityFailure()) { super.flushBuffer(); } }
        @Override public PrintWriter getWriter() throws IOException { return new PrintWriter(new OutputStreamWriter(getOutputStream(), StandardCharsets.UTF_8)); }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            var target = super.getOutputStream();
            return new ServletOutputStream() {
                @Override public boolean isReady() { return target.isReady(); }
                @Override public void setWriteListener(WriteListener listener) { target.setWriteListener(listener); }
                @Override public void write(int value) throws IOException { beforeWrite(); if (!securityFailure()) { target.write(value); } }
                @Override public void write(byte[] bytes, int offset, int length) throws IOException { beforeWrite(); if (!securityFailure()) { target.write(bytes, offset, length); } }
                @Override public void flush() throws IOException { beforeWrite(); if (!securityFailure()) { target.flush(); } }
                @Override public void close() throws IOException { if (!securityFailure()) { target.close(); } }
            };
        }
    }
}
