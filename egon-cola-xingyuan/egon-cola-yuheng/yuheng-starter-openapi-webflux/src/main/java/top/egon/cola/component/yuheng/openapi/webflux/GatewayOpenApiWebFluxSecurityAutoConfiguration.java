package top.egon.cola.component.yuheng.openapi.webflux;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.webflux.api.MultipleOpenApiWebFluxResource;
import org.springdoc.webflux.api.OpenApiResource;
import top.egon.cola.component.yuheng.openapi.config.GatewayOpenApiAutoConfiguration;
import top.egon.cola.component.yuheng.openapi.config.GatewayOpenApiProperties;
import top.egon.cola.component.yuheng.openapi.customizer.EgonOpenApiCustomizer;
import top.egon.cola.component.yuheng.openapi.customizer.EgonOperationCustomizer;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpAuthenticationToken;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpJwtVerifier;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.OAuthRevocationReader;
import top.egon.cola.platform.tianquan.shoubing.starter.security.ServiceAccessTokenVerifier;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TenantIssuerAuthenticationManagerComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.VerifiedPrincipalConverter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.IdpAuthorizationObservationContextBO;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditProofBO;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.VerifiedJwtBO;
import top.egon.cola.platform.tianquan.shoubing.starter.security.reactive.ReactiveOAuthRevocationReader;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityOAuthClientStateReader;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerStateReader;

import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Isolates the secured Spring WebFlux Springdoc resources from application routes.
 */
@Slf4j
@AutoConfiguration(after = GatewayOpenApiAutoConfiguration.class)
@ConditionalOnClass({
        ServerHttpSecurity.class,
        SecurityWebFilterChain.class,
        OpenApiResource.class
})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnProperty(
        prefix = "egon.cola.component.yuheng.openapi",
        name = "enabled",
        havingValue = "true"
)
public class GatewayOpenApiWebFluxSecurityAutoConfiguration {
    private static final String OBSERVATION = GatewayOpenApiWebFluxSecurityAutoConfiguration.class.getName() + ".observation";
    private static final String PROOF = GatewayOpenApiWebFluxSecurityAutoConfiguration.class.getName() + ".proof";
    private static final String TRACE = GatewayOpenApiWebFluxSecurityAutoConfiguration.class.getName() + ".trace";

    /**
     * Protects only grouped Springdoc documents; application security chains
     * remain responsible for every other request.
     */
    @Bean(name = "gatewayOpenApiWebFluxSecurityWebFilterChain")
    @Order(Ordered.LOWEST_PRECEDENCE - 100)
    @ConditionalOnMissingBean(name = "gatewayOpenApiWebFluxSecurityWebFilterChain")
    SecurityWebFilterChain gatewayOpenApiWebFluxSecurityWebFilterChain(
            ServerHttpSecurity http,
            @Qualifier("idpStarterProperties") IdpStarterProperties properties,
            @Qualifier("tenantIssuerAuthenticationManagerComponent") TenantIssuerAuthenticationManagerComponent issuers,
            @Qualifier("identityResourceServerStateReader") IdentityResourceServerStateReader resources,
            @Qualifier("identityOAuthClientStateReader") IdentityOAuthClientStateReader clients,
            @Qualifier("verifiedPrincipalConverter") VerifiedPrincipalConverter converter,
            @Qualifier("reactiveOAuthRevocationReader") ReactiveOAuthRevocationReader revocations,
            @Qualifier("idpObservedVerificationComponent") IdpObservedVerificationComponent observations,
            @Qualifier("idpClock") Clock clock) {
        properties.validate();
        if (!issuers.isConfigured() || !observations.enabled()) {
            throw new IllegalStateException("OPENAPI_SECURITY_OBSERVATION_REQUIRED");
        }
        var verifier = IdpJwtVerifier.createConfigured(null, resources, clients, properties.getResourceServerId(),
                properties.getResourceUri(), properties.getPlatformAudience(), clock, issuers,
                new WorkerRevocationReader(revocations, properties.getRevocation().getTimeout()), converter, null, observations);
        var bearer = new DocumentServiceWebFilter(new ServiceAccessTokenVerifier(verifier), observations);
        return http
                .securityMatcher(ServerWebExchangeMatchers.pathMatchers(
                        "/v3/api-docs/**"))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .requestCache(ServerHttpSecurity.RequestCacheSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .addFilterAt(bearer, SecurityWebFiltersOrder.AUTHENTICATION)
                .authorizeExchange(authorize -> authorize
                        .anyExchange().hasAuthority("SCOPE_yuheng.openapi.read"))
                .exceptionHandling(errors -> errors.authenticationEntryPoint((exchange, failure) ->
                        Mono.fromCallable(observations::authenticationFailure).subscribeOn(Schedulers.boundedElastic())
                                .flatMap(delivered -> securityStatus(exchange, delivered ? 401 : 503)))
                        .accessDeniedHandler((exchange, failure) -> {
                            var marker = exchange.getAttribute(OBSERVATION);
                            var proof = exchange.getAttribute(PROOF);
                            String trace = exchange.getAttribute(TRACE);
                            return Mono.fromCallable(() -> marker instanceof IdpAuthorizationObservationContextBO state
                                    && proof instanceof TokenAuditProofBO verified
                                    && state.denyOnce(() -> observations.authorizationDenied(verified, trace)))
                                    .subscribeOn(Schedulers.boundedElastic())
                                    .flatMap(delivered -> securityStatus(exchange, delivered ? 403 : 503));
                        }))
                .build();
    }

    /** Adapts the existing full verifier to its reactive Redis port only on a bounded worker. */
    @Slf4j
    @RequiredArgsConstructor
    private static class WorkerRevocationReader implements OAuthRevocationReader {
        @Qualifier("reactiveOAuthRevocationReader") private final ReactiveOAuthRevocationReader delegate;
        private final Duration timeout;

        @Override
        public Result check(@jakarta.validation.Valid VerifiedJwtBO verified) {
            if (Schedulers.isInNonBlockingThread()) {
                throw new IllegalStateException("OPENAPI_VERIFICATION_WORKER_REQUIRED");
            }
            // The SDK's synchronous contract owns one full verification; never repeat its decode.
            return delegate.check(verified).block(timeout.plusMillis(100));
        }
    }

    /** Keeps credentials local and binds the immutable verified identity only for this subscription. */
    @Slf4j
    @RequiredArgsConstructor
    private static class DocumentServiceWebFilter implements WebFilter {
        @Qualifier("serviceAccessTokenVerifier") private final ServiceAccessTokenVerifier verifier;
        @Qualifier("idpObservedVerificationComponent") private final IdpObservedVerificationComponent observations;

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
            return Mono.defer(() -> {
                var marker = new IdpAuthorizationObservationContextBO();
                exchange.getAttributes().put(OBSERVATION, marker);
                exchange.getAttributes().put(TRACE, UUID.randomUUID().toString().replace("-", ""));
                exchange.getResponse().getHeaders().setCacheControl("no-store");
                List<String> headers = exchange.getRequest().getHeaders().getOrEmpty(HttpHeaders.AUTHORIZATION);
                Mono<Void> request;
                if (headers.size() != 1 || headers.getFirst().length() > 8199
                        || !headers.getFirst().regionMatches(true, 0, "Bearer ", 0, 7)
                        || headers.getFirst().substring(7).trim().isEmpty() || headers.getFirst().contains(",")) {
                    request = Mono.fromCallable(observations::authenticationFailure).subscribeOn(Schedulers.boundedElastic())
                            .flatMap(delivered -> securityStatus(exchange, delivered ? 401 : 503));
                } else {
                    String token = headers.getFirst().substring(7).trim();
                    request = Mono.fromCallable(() -> verifier.verify(token)).subscribeOn(Schedulers.boundedElastic())
                            .flatMap(result -> {
                                if (result instanceof AccessTokenVerification.Valid<?> valid && valid.proof() != null) {
                                    var authentication = new IdpAuthenticationToken(valid.principal(), valid.scopes().stream()
                                            .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope)).toList());
                                    authentication.setDetails(valid.proof());
                                    exchange.getAttributes().put(PROOF, valid.proof());
                                    return chain.filter(exchange).contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
                                }
                                return securityStatus(exchange, result instanceof AccessTokenVerification.Unavailable<?> ? 503 : 401);
                            });
                }
                return request.doFinally(signal -> {
                    if (exchange.getAttribute(OBSERVATION) == marker) {
                        exchange.getAttributes().remove(OBSERVATION);
                        exchange.getAttributes().remove(PROOF);
                        exchange.getAttributes().remove(TRACE);
                    }
                    // Closing shares the strict delivery lock; cancellation may arrive on an event thread.
                    if (Schedulers.isInNonBlockingThread()) { Schedulers.boundedElastic().schedule(marker::close); }
                    else { marker.close(); }
                });
            });
        }
    }

    private static Mono<Void> securityStatus(ServerWebExchange exchange, int status) {
        exchange.getResponse().setStatusCode(HttpStatus.valueOf(status));
        exchange.getResponse().getHeaders().setCacheControl("no-store");
        if (status == 401) { exchange.getResponse().getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer"); }
        return exchange.getResponse().setComplete();
    }

    /**
     * Adds the common governance customizers to the provider-owned groups
     * after Springdoc has materialized both bean and property groups.
     */
    @Bean(name = "gatewayOpenApiWebFluxGroupedOpenApiCustomizer")
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(name = "gatewayOpenApiWebFluxGroupedOpenApiCustomizer")
    static BeanPostProcessor gatewayOpenApiWebFluxGroupedOpenApiCustomizer(
            ConfigurableListableBeanFactory beanFactory) {
        return new GroupedOpenApiGovernancePostProcessor(beanFactory);
    }

    private static final class GroupedOpenApiGovernancePostProcessor
            implements BeanPostProcessor {

        private final ConfigurableListableBeanFactory beanFactory;
        private GatewayOpenApiProperties properties;
        private EgonOperationCustomizer operationCustomizer;
        private EgonOpenApiCustomizer openApiCustomizer;
        private final Set<GroupedOpenApi> customized = Collections.newSetFromMap(
                new IdentityHashMap<>());

        private GroupedOpenApiGovernancePostProcessor(
                ConfigurableListableBeanFactory beanFactory) {
            this.beanFactory = beanFactory;
        }

        @Override
        public Object postProcessBeforeInitialization(
                Object bean,
                String beanName) {
            if (bean instanceof MultipleOpenApiWebFluxResource) {
                ensureDelegates();
                beanFactory.getBeansOfType(GroupedOpenApi.class).values()
                        .forEach(this::customize);
            } else if (bean instanceof GroupedOpenApi groupedOpenApi) {
                ensureDelegates();
                customize(groupedOpenApi);
            }
            return bean;
        }

        private void ensureDelegates() {
            if (properties != null) {
                return;
            }
            properties = beanFactory.getBean(
                    "gatewayOpenApiProperties", GatewayOpenApiProperties.class);
            operationCustomizer = beanFactory.getBean(
                    "egonOperationCustomizer", EgonOperationCustomizer.class);
            openApiCustomizer = beanFactory.getBean(
                    "egonOpenApiCustomizer", EgonOpenApiCustomizer.class);
            properties.validate();
        }

        private void customize(GroupedOpenApi groupedOpenApi) {
            if (!properties.getPublishedGroups().contains(
                    groupedOpenApi.getGroup()) || !customized.add(
                    groupedOpenApi)) {
                return;
            }
            if (!groupedOpenApi.getOperationCustomizers().contains(
                    operationCustomizer)) {
                groupedOpenApi.addAllOperationCustomizer(
                        List.of(operationCustomizer));
            }
            groupedOpenApi.addAllOpenApiCustomizer(List.of(
                    new GroupRootOpenApiCustomizer(
                            openApiCustomizer,
                            groupedOpenApi.getGroup())));
        }
    }

    private record GroupRootOpenApiCustomizer(
            EgonOpenApiCustomizer delegate,
            String group) implements org.springdoc.core.customizers.OpenApiCustomizer {

        @Override
        public void customise(io.swagger.v3.oas.models.OpenAPI openAPI) {
            delegate.customise(openAPI, group);
        }
    }
}
