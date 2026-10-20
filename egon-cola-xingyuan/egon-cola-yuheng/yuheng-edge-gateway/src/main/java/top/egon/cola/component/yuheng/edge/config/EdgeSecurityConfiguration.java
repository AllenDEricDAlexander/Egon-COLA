package top.egon.cola.component.yuheng.edge.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.WebSessionServerCsrfTokenRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.session.CookieWebSessionIdResolver;
import org.springframework.web.server.session.WebSessionIdResolver;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import org.springframework.security.web.server.DefaultServerRedirectStrategy;
import top.egon.cola.component.yuheng.edge.controller.EdgeSessionController;
import top.egon.cola.component.yuheng.edge.service.impl.EdgeAuthorizedClientRepository;
import top.egon.cola.component.yuheng.edge.service.impl.EdgeSingleFlightAuthorizedClientManager;
import top.egon.cola.component.yuheng.edge.service.impl.EdgeSessionBindingComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterAutoConfiguration;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TenantIssuerAuthenticationManagerComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.OAuthRevocationReader;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.VerifiedJwtBO;
import top.egon.cola.platform.tianquan.shoubing.starter.security.reactive.ReactiveOAuthRevocationReader;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.function.Function;

@Configuration(value = "edgeSecurityConfiguration", proxyBeanMethods = false)
@EnableWebFluxSecurity
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
@Import({IdpStarterAutoConfiguration.VerifiedPrincipalConverterConfiguration.class,
        IdpStarterAutoConfiguration.OAuthAuditConfiguration.class})
public class EdgeSecurityConfiguration {
    private static final String BFF_COOKIE = "__Host-egon_bff";

    @Qualifier("edgeOAuthProperties")
    private final EdgeOAuthProperties properties;

    @Bean(name = {"edgeClock", "idpClock"})
    @Qualifier("edgeClock")
    public Clock edgeClock() {
        return Clock.systemUTC();
    }

    @Bean(name = {"edgeIdpStarterProperties", "idpStarterProperties"}, initMethod = "validate")
    @ConfigurationProperties("egon.cola.platform.tianquan.shoubing")
    public IdpStarterProperties edgeIdpStarterProperties() {
        return new IdpStarterProperties();
    }

    @Bean(name = {"edgeTrustedIssuerRegistry", "trustedIssuerRegistry"})
    public TrustedIssuerRegistry edgeTrustedIssuerRegistry(
            @Qualifier("edgeIdpStarterProperties") IdpStarterProperties idpProperties) {
        return new TrustedIssuerRegistry(idpProperties);
    }

    public TenantIssuerAuthenticationManagerComponent edgeTenantIssuerAuthenticationManager(
            TrustedIssuerRegistry registry, IdpStarterProperties idpProperties, Clock clock) {
        var manager = new TenantIssuerAuthenticationManagerComponent(registry, idpProperties, clock);
        manager.initialize(); return manager;
    }

    @Bean(name = {"edgeTenantIssuerAuthenticationManager", "tenantIssuerAuthenticationManagerComponent"})
    public TenantIssuerAuthenticationManagerComponent edgeTenantIssuerAuthenticationManager(
            @Qualifier("edgeTrustedIssuerRegistry") TrustedIssuerRegistry issuerRegistry,
            @Qualifier("edgeIdpStarterProperties") IdpStarterProperties idpProperties,
            @Qualifier("edgeClock") Clock clock,
            @Qualifier("idpSecurityMetricsRecorder") top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder metrics) {
        TenantIssuerAuthenticationManagerComponent manager =
                new TenantIssuerAuthenticationManagerComponent(issuerRegistry, idpProperties, clock, metrics);
        manager.initialize();
        return manager;
    }

    @Bean(name = {"edgeReactiveOAuthRevocationReader", "reactiveOAuthRevocationReader"})
    public ReactiveOAuthRevocationReader edgeReactiveOAuthRevocationReader(
            @Qualifier("redisson") RedissonClient redisson,
            @Qualifier("edgeTrustedIssuerRegistry") TrustedIssuerRegistry issuerRegistry,
            @Qualifier("edgeIdpStarterProperties") IdpStarterProperties idpProperties) {
        return new ReactiveOAuthRevocationReader(redisson.reactive(), issuerRegistry, idpProperties);
    }

    @Bean(name = "edgeAccessTokenVerifier")
    public Function<String, VerifiedJwtBO> edgeAccessTokenVerifier(
            @Qualifier("edgeTenantIssuerAuthenticationManager")
            TenantIssuerAuthenticationManagerComponent issuerAuthentication) {
        return issuerAuthentication::authenticate;
    }

    @Bean(name = "edgeRevocationCheck")
    public Function<VerifiedJwtBO, Mono<OAuthRevocationReader.Result>> edgeRevocationCheck(
            @Qualifier("edgeReactiveOAuthRevocationReader")
            ReactiveOAuthRevocationReader revocationReader) {
        return revocationReader::check;
    }

    @Bean(name = {"idpAuditRedissonClient", "rbac3RuntimeRedissonClient"}, destroyMethod = "")
    public RedissonClient edgeSharedIdentityPrimary(@Qualifier("redisson") RedissonClient primary) { return primary; }

    @Bean(name = "verifiedPrincipalConverter")
    public top.egon.cola.platform.tianquan.shoubing.starter.security.VerifiedPrincipalConverter edgeVerifiedPrincipalConverter(
            @Qualifier("verifiedPrincipalConverterImpl") top.egon.cola.platform.tianquan.shoubing.starter.security.VerifiedPrincipalConverter converter) {
        return converter;
    }

    @Bean(name = "identityResourceServerStateReader")
    public top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerStateReader edgeResourceStates(
            @Qualifier("redisson") RedissonClient primary,
            @Qualifier("jacksonObjectMapper") com.fasterxml.jackson.databind.ObjectMapper mapper,
            @Qualifier("edgeIdpStarterProperties") IdpStarterProperties properties) {
        return new top.egon.cola.platform.tianquan.shoubing.starter.state.RedisIdentityResourceServerStateReader(primary, mapper, properties.getResourceStateKeyPrefix());
    }

    @Bean(name = "identityOAuthClientStateReader")
    public top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityOAuthClientStateReader edgeClientStates(
            @Qualifier("redisson") RedissonClient primary,
            @Qualifier("jacksonObjectMapper") com.fasterxml.jackson.databind.ObjectMapper mapper,
            @Qualifier("edgeIdpStarterProperties") IdpStarterProperties properties) {
        return new top.egon.cola.platform.tianquan.shoubing.starter.state.RedisIdentityOAuthClientStateReader(primary, mapper, properties.getOauthClientStateKeyPrefix());
    }

    @Bean(name = "oauthUsageObservationProperties")
    public top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties edgeUsageObservationProperties() {
        return new top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties();
    }

    @Bean(name = "idpObservedVerificationComponent")
    public top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent edgeObservedVerification(
            @Qualifier("tokenUsageAuditComponent") top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent producer,
            @Qualifier("tokenUsageAuditEventConverterImpl") top.egon.cola.platform.tianquan.shoubing.starter.security.domain.converter.TokenUsageAuditEventConverter converter,
            @Qualifier("oauthUsageObservationProperties") top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties usage,
            @Qualifier("idpSecurityMetricsRecorder") top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder metrics,
            @Qualifier("edgeClock") Clock clock,
            @Qualifier("defaultValidator") jakarta.validation.Validator validator,
            @Qualifier("edgeIdpStarterProperties") IdpStarterProperties receiver) {
        return new top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent(
                producer, converter, usage, metrics, clock, validator, receiver.getResourceUri());
    }

    @Bean(name = "edgeSecurityAuditHealthIndicator")
    public org.springframework.boot.actuate.health.HealthIndicator edgeSecurityAuditHealthIndicator(
            @Qualifier("idpSecurityAuditReady") java.util.function.BooleanSupplier ready) {
        if (!ready.getAsBoolean()) { throw new IllegalStateException("EDGE_SECURITY_AUDIT_NOT_READY"); }
        return () -> ready.getAsBoolean() ? org.springframework.boot.actuate.health.Health.up().build()
                : org.springframework.boot.actuate.health.Health.down().build();
    }

    @Bean(name = "edgeAuthorizationRequestRepository")
    public org.springframework.security.oauth2.client.web.server.ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> edgeAuthorizationRequestRepository() {
        return new org.springframework.security.oauth2.client.web.server.WebSessionOAuth2ServerAuthorizationRequestRepository();
    }

    @Bean(name = "redissonReactiveClient", destroyMethod = "")
    public org.redisson.api.RedissonReactiveClient edgeReactiveRedis(@Qualifier("redisson") RedissonClient primary) { return primary.reactive(); }

    @Bean(name = "edgeDefaultAuthorizedClientManager")
    public DefaultReactiveOAuth2AuthorizedClientManager edgeDefaultAuthorizedClientManager(
            @Qualifier("reactiveClientRegistrationRepository") ReactiveClientRegistrationRepository registrations,
            @Qualifier("edgeAuthorizedClientRepository") ServerOAuth2AuthorizedClientRepository clients) {
        DefaultReactiveOAuth2AuthorizedClientManager delegate = new DefaultReactiveOAuth2AuthorizedClientManager(registrations, clients);
        delegate.setAuthorizedClientProvider(ReactiveOAuth2AuthorizedClientProviderBuilder.builder().authorizationCode().refreshToken().build());
        return delegate;
    }

    public ReactiveOAuth2AuthorizedClientManager reactiveOAuth2AuthorizedClientManager(
            ReactiveClientRegistrationRepository registrations, ServerOAuth2AuthorizedClientRepository clients, RedissonClient primary) {
        if (!(clients instanceof EdgeAuthorizedClientRepository authoritative)) {
            throw EdgeSessionBindingComponent.unavailable("Edge authoritative client repository is required");
        }
        return reactiveOAuth2AuthorizedClientManager(registrations, clients, primary, authoritative.bindingComponent(), authoritative);
    }

    public ReactiveOAuth2AuthorizedClientManager reactiveOAuth2AuthorizedClientManager(
            @Qualifier("reactiveClientRegistrationRepository")
            ReactiveClientRegistrationRepository registrations,
            @Qualifier("edgeAuthorizedClientRepository") ServerOAuth2AuthorizedClientRepository repository,
            @Qualifier("redisson") RedissonClient redisson,
            @Qualifier("edgeSessionBindingComponent") EdgeSessionBindingComponent bindings,
            @Qualifier("edgeAuthorizedClientRepository") EdgeAuthorizedClientRepository clients) {
        return configuredEdgeAuthorizedClientManager(edgeDefaultAuthorizedClientManager(registrations, repository), registrations, redisson.reactive(), bindings, clients);
    }

    @Bean(name = "reactiveOAuth2AuthorizedClientManager")
    @org.springframework.context.annotation.Primary
    public ReactiveOAuth2AuthorizedClientManager configuredEdgeAuthorizedClientManager(
            @Qualifier("edgeDefaultAuthorizedClientManager") DefaultReactiveOAuth2AuthorizedClientManager delegate,
            @Qualifier("reactiveClientRegistrationRepository") ReactiveClientRegistrationRepository registrations,
            @Qualifier("redissonReactiveClient") org.redisson.api.RedissonReactiveClient redisson,
            @Qualifier("edgeSessionBindingComponent") EdgeSessionBindingComponent bindings,
            @Qualifier("edgeAuthorizedClientRepository") EdgeAuthorizedClientRepository clients) {
        return EdgeSingleFlightAuthorizedClientManager.createConfigured(delegate, registrations, redisson, properties, bindings, clients);
    }

    @Bean(name = "edgeWebSessionIdResolver")
    public WebSessionIdResolver edgeWebSessionIdResolver() {
        CookieWebSessionIdResolver resolver = new CookieWebSessionIdResolver();
        resolver.setCookieName(BFF_COOKIE);
        resolver.addCookieInitializer(cookie -> cookie
                .path("/")
                .secure(true)
                .httpOnly(true)
                .sameSite("Lax"));
        return resolver;
    }

    @Bean(name = "edgeCsrfTokenRepository")
    public ServerCsrfTokenRepository edgeCsrfTokenRepository() {
        WebSessionServerCsrfTokenRepository repository = new WebSessionServerCsrfTokenRepository();
        repository.setHeaderName("X-CSRF-TOKEN");
        repository.setParameterName("_csrf");
        return repository;
    }

    @Bean(name = "edgeCorsWebFilter")
    public CorsWebFilter edgeCorsWebFilter() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(properties.getPublicOrigin().toASCIIString()));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-CSRF-TOKEN"));
        cors.setAllowCredentials(true);
        cors.setMaxAge(600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return new CorsWebFilter(source);
    }

    @Bean(name = "edgeApiSecurityWebFilterChain")
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityWebFilterChain edgeApiSecurityWebFilterChain(
            ServerHttpSecurity http,
            @Qualifier("edgeCsrfTokenRepository")
            ServerCsrfTokenRepository csrfTokens) {
        return http
                .securityMatcher(exchange -> {
                    String path = exchange.getRequest().getPath().value();
                    boolean protectedPath = path.startsWith("/api/") || path.startsWith("/mcp/") || path.startsWith("/v1/")
                            || properties.getResources().values().stream().flatMap(resource -> resource.getRequestPaths().stream())
                                    .anyMatch(pattern -> new org.springframework.util.AntPathMatcher().match(pattern, path));
                    return protectedPath ? ServerWebExchangeMatcher.MatchResult.match() : ServerWebExchangeMatcher.MatchResult.notMatch();
                })
                .securityContextRepository(new WebSessionServerSecurityContextRepository())
                .addFilterAt((exchange, chain) -> {
                    var values = exchange.getRequest().getHeaders().getOrEmpty(HttpHeaders.AUTHORIZATION);
                    if (values.size() > 1 || values.size() == 1 && exchange.getRequest().getCookies().containsKey(BFF_COOKIE)
                            && values.getFirst().regionMatches(true, 0, "Bearer ", 0, 7)) {
                        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST); exchange.getResponse().getHeaders().setCacheControl("no-store");
                        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                        byte[] body = "{\"error\":\"invalid_request\"}".getBytes(StandardCharsets.UTF_8);
                        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
                    }
                    return chain.filter(exchange);
                }, SecurityWebFiltersOrder.FIRST)
                .authorizeExchange(authorize -> authorize.anyExchange().permitAll())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokens)
                        .requireCsrfProtectionMatcher(edgeCsrfMatcher()))
                .build();
    }

    /** Retains the existing entry point for hosts that already provide the authoritative repository. */
    public SecurityWebFilterChain edgeBrowserSecurityWebFilterChain(ServerHttpSecurity http,
            ReactiveClientRegistrationRepository registrations, ServerOAuth2AuthorizedClientRepository clients,
            ServerCsrfTokenRepository csrfTokens, Clock clock, IdpStarterProperties idpProperties,
            com.fasterxml.jackson.databind.ObjectMapper mapper) {
        if (!(clients instanceof EdgeAuthorizedClientRepository authoritative)) {
            throw EdgeSessionBindingComponent.unavailable("Edge authoritative client repository is required");
        }
        return edgeBrowserSecurityWebFilterChain(http, registrations, clients, csrfTokens, clock, idpProperties, mapper,
                authoritative.bindingComponent(), edgeAuthorizationRequestRepository());
    }

    @Bean(name = "edgeBrowserSecurityWebFilterChain")
    @Order(Ordered.LOWEST_PRECEDENCE)
    public SecurityWebFilterChain edgeBrowserSecurityWebFilterChain(
            ServerHttpSecurity http,
            @Qualifier("reactiveClientRegistrationRepository") ReactiveClientRegistrationRepository registrations,
            @Qualifier("edgeAuthorizedClientRepository") ServerOAuth2AuthorizedClientRepository clients,
            @Qualifier("edgeCsrfTokenRepository") ServerCsrfTokenRepository csrfTokens,
            @Qualifier("edgeClock") Clock clock,
            @Qualifier("edgeIdpStarterProperties") IdpStarterProperties idpProperties,
            @Qualifier("jacksonObjectMapper") com.fasterxml.jackson.databind.ObjectMapper mapper,
            @Qualifier("edgeSessionBindingComponent") EdgeSessionBindingComponent bindings,
            @Qualifier("edgeAuthorizationRequestRepository")
            org.springframework.security.oauth2.client.web.server.ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> requests) {
        DefaultServerRedirectStrategy redirects = new DefaultServerRedirectStrategy();
        redirects.setHttpStatus(HttpStatus.SEE_OTHER);
        ServerAuthenticationSuccessHandler success = (filterExchange, authentication) -> {
            ServerWebExchange exchange = filterExchange.getExchange();
            OAuth2AuthorizationRequest pending = exchange.getAttribute(EdgeAuthorizedClientRepository.PENDING_REQUEST);
            if (pending == null) { return Mono.error(EdgeSessionBindingComponent.invalid("Edge callback transaction is absent")); }
            String returnTo = pending.getAttribute("egon.bff.returnTo");
            if (!properties.getAllowedReturnPaths().contains(returnTo)) {
                return Mono.error(EdgeSessionBindingComponent.invalid("Edge callback return path is not allowlisted"));
            }
            return bindings.load(exchange).flatMap(binding -> {
                if (binding.getState() != top.egon.cola.component.yuheng.edge.service.domain.enums.EdgeSessionBindingStateEnum.ACTIVE
                        || !binding.getSubject().equals(authentication.getName())) {
                    return Mono.error(EdgeSessionBindingComponent.invalid("Edge canonical identity is not active"));
                }
                return exchange.getSession().flatMap(session -> {
                    session.getAttributes().put(EdgeSessionController.SESSION_CREATED_AT_ATTRIBUTE, binding.getOriginalCreatedAt());
                    return session.changeSessionId().then(bindings.rotatePointer(session, binding));
                });
            }).then(Mono.defer(() -> {
                exchange.getResponse().getHeaders().setCacheControl("no-store");
                return redirects.sendRedirect(exchange, properties.getPublicOrigin().resolve(returnTo));
            }));
        };
        RedirectServerAuthenticationEntryPoint login = new RedirectServerAuthenticationEntryPoint(
                "/oauth2/authorization/" + properties.getDefaultRegistrationId());
        login.setRequestCache(NoOpServerRequestCache.getInstance());
        login.setRedirectStrategy(redirects);
        return http.securityContextRepository(new WebSessionServerSecurityContextRepository())
                .addFilterAt(nativeTransactionFilter(bindings, requests, mapper, (EdgeAuthorizedClientRepository) clients), SecurityWebFiltersOrder.FIRST)
                .requestCache(cache -> cache.requestCache(NoOpServerRequestCache.getInstance()))
                .authorizeExchange(authorize -> authorize
                        .pathMatchers("/bff/csrf", "/actuator/health", "/actuator/health/**").permitAll()
                        .pathMatchers("/bff/session", "/bff/logout").authenticated()
                        .pathMatchers("/oauth2/authorization/**", "/login/oauth2/code/**", "/tenants/**", "/platform/**").permitAll()
                        .anyExchange().authenticated())
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(bffAuthenticationEntryPoint(login)))
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokens).requireCsrfProtectionMatcher(edgeCsrfMatcher()))
                .oauth2Login(oauth2 -> oauth2.clientRegistrationRepository(registrations).authorizedClientRepository(clients)
                        .authorizationRequestRepository(requests)
                        .authorizationRequestResolver(authorizationRequestResolver(registrations, bindings))
                        .authenticationSuccessHandler(success)
                        .authenticationFailureHandler((filterExchange, failure) -> nativeFailure(filterExchange.getExchange(), failure, mapper)))
                .oauth2Client(oauth2 -> oauth2.clientRegistrationRepository(registrations).authorizedClientRepository(clients))
                .logout(ServerHttpSecurity.LogoutSpec::disable).build();
    }

    private ServerOAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ReactiveClientRegistrationRepository registrations, EdgeSessionBindingComponent bindings) {
        DefaultServerOAuth2AuthorizationRequestResolver delegate = new DefaultServerOAuth2AuthorizationRequestResolver(registrations);
        delegate.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        return new ServerOAuth2AuthorizationRequestResolver() {
            @Override public Mono<OAuth2AuthorizationRequest> resolve(ServerWebExchange exchange) {
                return exchange.getRequest().getPath().value().startsWith("/oauth2/authorization/") ? resolve(exchange, null) : Mono.empty();
            }
            @Override public Mono<OAuth2AuthorizationRequest> resolve(ServerWebExchange exchange, String requested) {
                String input = requested == null ? exchange.getRequest().getPath().value().substring("/oauth2/authorization/".length()) : requested;
                return bindings.load(exchange).flatMap(binding -> {
                    String alias = properties.getResources().containsKey(input) ? input : bindings.aliasForRegistration(input);
                    String realId = properties.getResources().containsKey(input) ? bindings.resolveRegistration(alias, binding.getIssuer()) : input;
                    var resource = bindings.resource(alias);
                    return registrations.findByRegistrationId(realId).switchIfEmpty(Mono.error(EdgeSessionBindingComponent.invalid("Edge login registration is unknown")))
                            .flatMap(registration -> {
                                String issuer = registration.getProviderDetails().getIssuerUri();
                                if (binding.getIssuer() != null && !binding.getIssuer().toASCIIString().equals(issuer)) {
                                    return Mono.error(EdgeSessionBindingComponent.invalid("Edge login cannot change the canonical issuer"));
                                }
                                boolean stepUp = "true".equals(exchange.getRequest().getQueryParams().getFirst("stepUp"));
                                String returnTo = exchange.getRequest().getQueryParams().getFirst("returnTo");
                                String destination = returnTo == null ? resource.getReturnPath() : returnTo;
                                if (!properties.getAllowedReturnPaths().contains(destination)) {
                                    return Mono.error(EdgeSessionBindingComponent.invalid("Edge login return path is not allowlisted"));
                                }
                                return delegate.resolve(exchange, realId).map(request -> {
                                    var uri = UriComponentsBuilder.fromUriString(request.getAuthorizationRequestUri())
                                            .queryParam("resource", org.springframework.web.util.UriUtils.encodeQueryParam(resource.getResourceUri().toASCIIString(), StandardCharsets.UTF_8));
                                    if (stepUp) { uri.queryParam("prompt", "login").queryParam("max_age", "0"); }
                                    return OAuth2AuthorizationRequest.from(request).attributes(attributes -> {
                                        attributes.put(EdgeSessionBindingComponent.BINDING_ID, binding.getBindingId());
                                        attributes.put(EdgeSessionBindingComponent.GENERATION, binding.getGeneration());
                                        attributes.put(EdgeSessionBindingComponent.RESOURCE_ALIAS, alias);
                                        attributes.put(EdgeSessionBindingComponent.REGISTRATION_ID, realId);
                                        attributes.put(EdgeSessionBindingComponent.ISSUER, issuer);
                                        attributes.put(EdgeSessionBindingComponent.RESOURCE, resource.getResourceUri().toASCIIString());
                                        attributes.put(EdgeSessionBindingComponent.INTENT, stepUp ? "STEP_UP" : binding.getIssuer() == null ? "INITIAL" : "APPEND");
                                        attributes.put("egon.bff.returnTo", destination);
                                    }).additionalParameters(parameters -> {
                                        parameters.put("resource", resource.getResourceUri().toASCIIString());
                                        if (stepUp) { parameters.put("prompt", "login"); parameters.put("max_age", "0"); }
                                    }).authorizationRequestUri(uri.build(true).toUriString()).build();
                                });
                            });
                });
            }
        };
    }

    private WebFilter nativeTransactionFilter(EdgeSessionBindingComponent bindings,
            org.springframework.security.oauth2.client.web.server.ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> requests,
            com.fasterxml.jackson.databind.ObjectMapper mapper, EdgeAuthorizedClientRepository clients) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();
            boolean login = path.startsWith("/oauth2/authorization/");
            boolean callback = path.startsWith("/login/oauth2/code/");
            if (!login && !callback) { return chain.filter(exchange); }
            exchange.getResponse().getHeaders().setCacheControl("no-store");
            String id = path.substring((login ? "/oauth2/authorization/" : "/login/oauth2/code/").length());
            if (exchange.getRequest().getMethod() != HttpMethod.GET || !id.matches("[A-Za-z0-9_-]{1,128}")
                    || !validNativeQuery(exchange, login)) {
                return nativeFailure(exchange, EdgeSessionBindingComponent.invalid("Edge native request is invalid"), mapper);
            }
            return exchange.getRequest().getBody().doOnNext(org.springframework.core.io.buffer.DataBufferUtils::release).hasElements().flatMap(body -> {
                if (body) { return Mono.error(EdgeSessionBindingComponent.invalid("Edge native GET does not accept a body")); }
                if (login) {
                    if (!properties.getResources().containsKey(id)) {
                        try { bindings.aliasForRegistration(id); }
                        catch (org.springframework.security.oauth2.core.OAuth2AuthenticationException unknown) { return Mono.error(EdgeSessionBindingComponent.invalid("Edge login registration is unknown")); }
                    }
                    return bindings.getOrCreateForLogin(exchange).then(bindings.withLifecycle(exchange, lease -> chain.filter(exchange)));
                }
                return requests.loadAuthorizationRequest(exchange)
                        .switchIfEmpty(Mono.error(EdgeSessionBindingComponent.invalid("Edge callback state is absent")))
                        .flatMap(pending -> {
                            if (!id.equals(pending.getAttribute(EdgeSessionBindingComponent.REGISTRATION_ID))
                                    || !exchange.getRequest().getQueryParams().getFirst("iss").equals(pending.getAttribute(EdgeSessionBindingComponent.ISSUER))) {
                                return Mono.error(EdgeSessionBindingComponent.invalid("Edge callback issuer or registration is invalid"));
                            }
                            return bindings.withLifecycle(exchange, lease -> {
                                if (!lease.bindingId().equals(pending.getAttribute(EdgeSessionBindingComponent.BINDING_ID))
                                        || !(pending.getAttribute(EdgeSessionBindingComponent.GENERATION) instanceof Number generation)
                                        || generation.longValue() != lease.generation()) {
                                    return Mono.error(EdgeSessionBindingComponent.invalid("Edge callback generation is invalid"));
                                }
                                exchange.getAttributes().put(EdgeAuthorizedClientRepository.PENDING_REQUEST, pending);
                                return chain.filter(exchange).timeout(properties.getRefresh().getTokenTimeout())
                                        .onErrorResume(java.util.concurrent.TimeoutException.class, failure -> closeUnknownCallback(exchange, clients, lease)
                                                .then(Mono.error(EdgeSessionBindingComponent.unavailable("Edge callback token outcome is unknown"))));
                            }, lease -> closeUnknownCallback(exchange, clients, lease));
                        });
            }).doFinally(signal -> {
                exchange.getAttributes().remove(EdgeAuthorizedClientRepository.PENDING_REQUEST);
                clients.clearRequestVerification(exchange);
            }).onErrorResume(org.springframework.security.oauth2.core.OAuth2AuthenticationException.class,
                    failure -> nativeFailure(exchange, failure, mapper));
        };
    }

    private Mono<Void> closeUnknownCallback(ServerWebExchange exchange, EdgeAuthorizedClientRepository clients,
            EdgeSessionBindingComponent.LifecycleLease lease) {
        return exchange.getPrincipal().ofType(Authentication.class).map(java.util.Optional::of).defaultIfEmpty(java.util.Optional.empty())
                .flatMap(principal -> clients.closeUnknown(principal.orElse(null), exchange, lease));
    }

    private boolean validNativeQuery(ServerWebExchange exchange, boolean login) {
        var query = exchange.getRequest().getQueryParams();
        var allowed = login ? java.util.Set.of("returnTo", "stepUp") : java.util.Set.of("state", "code", "error", "error_description", "iss");
        if (!allowed.containsAll(query.keySet()) || query.values().stream().anyMatch(values -> values.size() != 1)) { return false; }
        if (login) {
            return (query.getFirst("stepUp") == null || "true".equals(query.getFirst("stepUp")))
                    && (query.getFirst("returnTo") == null || query.getFirst("returnTo").length() <= 2048 && properties.getAllowedReturnPaths().contains(query.getFirst("returnTo")));
        }
        String state = query.getFirst("state"), code = query.getFirst("code"), error = query.getFirst("error"), issuer = query.getFirst("iss");
        return bounded(state, 128) && bounded(issuer, 2048) && ((bounded(code, 512) && error == null) || (bounded(error, 128) && code == null))
                && (query.getFirst("error_description") == null || query.getFirst("error_description").length() <= 512 && error != null);
    }

    private static boolean bounded(String value, int maximum) { return value != null && !value.isBlank() && value.length() <= maximum; }

    private Mono<Void> nativeFailure(ServerWebExchange exchange, Throwable failure, com.fasterxml.jackson.databind.ObjectMapper mapper) {
        boolean unavailable = false;
        Throwable cause = failure;
        for (int depth = 0; cause != null && depth < 8; depth++, cause = cause.getCause()) {
            if (cause instanceof org.springframework.security.oauth2.core.OAuth2AuthenticationException oauth
                    && java.util.Set.of("temporarily_unavailable", "server_error", "invalid_token_response").contains(oauth.getError().getErrorCode())) {
                unavailable = true;
            }
        }
        exchange.getResponse().setStatusCode(unavailable ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_REQUEST);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().setCacheControl("no-store");
        String code = unavailable ? "temporarily_unavailable" : "invalid_request";
        return Mono.fromCallable(() -> mapper.writeValueAsBytes(java.util.Map.of("error", code)))
                .flatMap(body -> exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body))));
    }

    @Bean(name = "edgeOpenAPI")
    public OpenAPI edgeOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Yuheng OAuth BFF").version("1.0"))
                .paths(nativeLoginPaths())
                .components(new Components()
                        .addSecuritySchemes("bffSessionCookie", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name(BFF_COOKIE))
                        .addSecuritySchemes("bffCsrfHeader", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-CSRF-TOKEN"))
                        .addSchemas("EdgeSessionResult", new io.swagger.v3.oas.models.media.ObjectSchema()
                                .addProperty("authenticated", new io.swagger.v3.oas.models.media.BooleanSchema()._enum(List.of(true)))
                                .addProperty("tenantId", new io.swagger.v3.oas.models.media.StringSchema().pattern("[1-9][0-9]{0,18}").minLength(1).maxLength(19))
                                .addProperty("subject", new io.swagger.v3.oas.models.media.StringSchema().minLength(1).maxLength(256))
                                .addProperty("displayName", new io.swagger.v3.oas.models.media.StringSchema().minLength(1).maxLength(256))
                                .required(List.of("authenticated", "tenantId", "subject", "displayName")))
                        .addSchemas("EdgeCsrfResult", new io.swagger.v3.oas.models.media.ObjectSchema()
                                .addProperty("headerName", new io.swagger.v3.oas.models.media.StringSchema()._enum(List.of("X-CSRF-TOKEN")))
                                .addProperty("token", new io.swagger.v3.oas.models.media.StringSchema().minLength(1))
                                .required(List.of("headerName", "token")))
                        .addSchemas("EdgeLogoutFailureResult", new io.swagger.v3.oas.models.media.ObjectSchema()
                                .addProperty("code", new io.swagger.v3.oas.models.media.StringSchema()._enum(List.of("REMOTE_REVOCATION_PENDING")))
                                .addProperty("message", new io.swagger.v3.oas.models.media.StringSchema().minLength(1).maxLength(256))
                                .addProperty("localLoggedOut", new io.swagger.v3.oas.models.media.BooleanSchema()._enum(List.of(true)))
                                .addProperty("remoteState", new io.swagger.v3.oas.models.media.StringSchema()._enum(List.of("PENDING", "UNKNOWN")))
                                .required(List.of("code", "message", "localLoggedOut", "remoteState"))));
    }

    private static io.swagger.v3.oas.models.Paths nativeLoginPaths() {
        var registration = new io.swagger.v3.oas.models.parameters.Parameter().name("registrationId").in("path")
                .required(true).schema(new io.swagger.v3.oas.models.media.StringSchema().minLength(1).maxLength(128).pattern("[A-Za-z0-9_-]{1,128}"));
        var errors = new io.swagger.v3.oas.models.media.Content().addMediaType("application/json",
                new io.swagger.v3.oas.models.media.MediaType().schema(new io.swagger.v3.oas.models.media.ObjectSchema()
                        .addProperty("error", new io.swagger.v3.oas.models.media.StringSchema()).required(java.util.List.of("error"))));
        var bad = new io.swagger.v3.oas.models.responses.ApiResponse().description("Unknown registration or invalid state/request")
                .content(errors);
        var noStore = new io.swagger.v3.oas.models.headers.Header()
                .schema(new io.swagger.v3.oas.models.media.StringSchema()._enum(java.util.List.of("no-store")));
        var login = new io.swagger.v3.oas.models.Operation().operationId("oauthBffLogin")
                .summary("Start OIDC login for a configured registration").security(java.util.List.of())
                .description("Only trusted server-side registration; returnTo stays inside the allowlist. Exact stepUp=true starts real reauthentication.")
                .addParametersItem(registration)
                .addParametersItem(new io.swagger.v3.oas.models.parameters.Parameter().name("returnTo").in("query")
                        .schema(new io.swagger.v3.oas.models.media.StringSchema().minLength(1).maxLength(2048)))
                .addParametersItem(new io.swagger.v3.oas.models.parameters.Parameter().name("stepUp").in("query")
                        .schema(new io.swagger.v3.oas.models.media.StringSchema()._enum(java.util.List.of("true"))))
                .responses(new io.swagger.v3.oas.models.responses.ApiResponses()
                        .addApiResponse("302", new io.swagger.v3.oas.models.responses.ApiResponse()
                                .description("Redirect to configured issuer with PKCE/state/nonce").addHeaderObject("Cache-Control", noStore)
                                .addHeaderObject("Location", new io.swagger.v3.oas.models.headers.Header().required(true)
                                        .schema(new io.swagger.v3.oas.models.media.StringSchema().format("uri"))))
                        .addApiResponse("400", bad.addHeaderObject("Cache-Control", noStore))
                        .addApiResponse("503", new io.swagger.v3.oas.models.responses.ApiResponse().description("Shared security/session dependency unavailable").content(errors).addHeaderObject("Cache-Control", noStore)));
        var callback = new io.swagger.v3.oas.models.Operation().operationId("oauthBffCallback")
                .summary("Complete the stored OIDC transaction").security(java.util.List.of())
                .description("The stored request binds state, nonce, issuer and registration. Tokens stay on the server; only the opaque session reaches the browser.")
                .addParametersItem(registration);
        for (String name : java.util.List.of("code", "state", "error", "error_description", "iss")) {
            callback.addParametersItem(new io.swagger.v3.oas.models.parameters.Parameter().name(name).in("query")
                    .required(name.equals("state") || name.equals("iss"))
                    .schema(new io.swagger.v3.oas.models.media.StringSchema().minLength(1)
                            .maxLength(name.equals("iss") ? 2048 : name.equals("code") || name.equals("error_description") ? 512 : 128)));
        }
        callback.responses(new io.swagger.v3.oas.models.responses.ApiResponses()
                .addApiResponse("303", new io.swagger.v3.oas.models.responses.ApiResponse().description("Resume the validated local return path")
                        .addHeaderObject("Location", new io.swagger.v3.oas.models.headers.Header().required(true)
                                .schema(new io.swagger.v3.oas.models.media.StringSchema()))
                        .addHeaderObject("Cache-Control", noStore))
                .addApiResponse("400", bad)
                .addApiResponse("503", new io.swagger.v3.oas.models.responses.ApiResponse()
                        .description("Issuer/shared security/session dependency unavailable").content(errors).addHeaderObject("Cache-Control", noStore)));
        return new io.swagger.v3.oas.models.Paths()
                .addPathItem("/oauth2/authorization/{registrationId}", new io.swagger.v3.oas.models.PathItem().get(login))
                .addPathItem("/login/oauth2/code/{registrationId}", new io.swagger.v3.oas.models.PathItem().get(callback));
    }

    @Bean(name = "edgeBffOpenApiSecurityCustomizer")
    public org.springdoc.core.customizers.GlobalOpenApiCustomizer edgeBffOpenApiSecurityCustomizer() {
        return api -> {
            if (api.getComponents() != null && api.getComponents().getSchemas() != null) {
                var schemas = api.getComponents().getSchemas();
                io.swagger.v3.oas.models.media.Schema<?> session = schemas.get("EdgeSessionResult");
                if (session != null) {
                    session.setRequired(List.of("authenticated", "tenantId", "subject", "displayName"));
                    session.getProperties().get("displayName").setMaxLength(256);
                    session.getProperties().get("displayName").setMinLength(1);
                    session.getProperties().get("subject").setMaxLength(256);
                    session.getProperties().get("subject").setMinLength(1);
                }
                var csrf = schemas.get("EdgeCsrfResult"); if (csrf != null) { csrf.setRequired(List.of("headerName", "token")); }
                io.swagger.v3.oas.models.media.Schema<?> failure = schemas.get("EdgeLogoutFailureResult");
                if (failure != null) {
                    failure.setRequired(List.of("code", "message", "localLoggedOut", "remoteState"));
                    failure.addProperty("remoteState", new io.swagger.v3.oas.models.media.StringSchema()._enum(List.of("PENDING", "UNKNOWN")));
                }
            }
            if (api.getPaths() == null) { return; }
            api.getPaths().values().stream().flatMap(path -> path.readOperations().stream())
                    .filter(operation -> "oauthBffLogout".equals(operation.getOperationId()))
                    .forEach(operation -> {
                        operation.setSecurity(List.of(new io.swagger.v3.oas.models.security.SecurityRequirement().addList("bffSessionCookie").addList("bffCsrfHeader")));
                        if (operation.getResponses() != null) {
                            for (String status : List.of("204", "403")) {
                                var response = operation.getResponses().get(status); if (response != null) { response.setContent(null); }
                            }
                        }
                    });
        };
    }

    private ServerWebExchangeMatcher edgeCsrfMatcher() {
        return exchange -> {
            HttpMethod method = exchange.getRequest().getMethod();
            if (method == null || method == HttpMethod.GET || method == HttpMethod.HEAD
                    || method == HttpMethod.OPTIONS || method == HttpMethod.TRACE) {
                return ServerWebExchangeMatcher.MatchResult.notMatch();
            }
            if (exchange.getRequest().getCookies().containsKey(BFF_COOKIE)) {
                return ServerWebExchangeMatcher.MatchResult.match();
            }
            if (HttpMethod.POST.equals(method)
                    && "/bff/logout".equals(exchange.getRequest().getPath().value())) {
                String origin = exchange.getRequest().getHeaders().getFirst(HttpHeaders.ORIGIN);
                return sameOrigin(origin)
                        ? ServerWebExchangeMatcher.MatchResult.notMatch()
                        : ServerWebExchangeMatcher.MatchResult.match();
            }
            return ServerWebExchangeMatcher.MatchResult.notMatch();
        };
    }

    private ServerAuthenticationEntryPoint bffAuthenticationEntryPoint(
            RedirectServerAuthenticationEntryPoint loginEntryPoint) {
        return (exchange, exception) -> {
            if ("/bff/session".equals(exchange.getRequest().getPath().value())
                    || "/bff/logout".equals(exchange.getRequest().getPath().value())) {
                byte[] body = "{\"error\":\"invalid_token\"}".getBytes(StandardCharsets.UTF_8);
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                response.getHeaders().setCacheControl(CacheControl.noStore());
                return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
            }
            return loginEntryPoint.commence(exchange, exception);
        };
    }

    private boolean sameOrigin(String originValue) {
        if (originValue == null || originValue.isBlank()) {
            return false;
        }
        URI origin;
        try {
            origin = URI.create(originValue);
        } catch (IllegalArgumentException exception) {
            return false;
        }
        URI expected = properties.getPublicOrigin();
        return expected.getScheme().equalsIgnoreCase(origin.getScheme())
                && expected.getHost().equalsIgnoreCase(origin.getHost())
                && effectivePort(expected) == effectivePort(origin);
    }

    private static int effectivePort(URI uri) {
        if (uri.getPort() > 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }
}
