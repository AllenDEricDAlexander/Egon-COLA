package top.egon.cola.component.yuheng.edge.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
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
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
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
import org.springframework.security.web.server.ServerRedirectStrategy;
import top.egon.cola.component.yuheng.edge.controller.EdgeSessionController;
import top.egon.cola.component.yuheng.edge.service.impl.EdgeAuthorizedClientRepository;
import top.egon.cola.component.yuheng.edge.service.impl.EdgeSingleFlightAuthorizedClientManager;
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
public class EdgeSecurityConfiguration {
    private static final String BFF_COOKIE = "__Host-egon_bff";

    @Qualifier("edgeOAuthProperties")
    private final EdgeOAuthProperties properties;

    @Bean(name = "edgeClock")
    @Qualifier("edgeClock")
    public Clock edgeClock() {
        return Clock.systemUTC();
    }

    @Bean(name = "edgeIdpStarterProperties", initMethod = "validate")
    @ConfigurationProperties("egon.cola.platform.tianquan.shoubing")
    public IdpStarterProperties edgeIdpStarterProperties() {
        return new IdpStarterProperties();
    }

    @Bean(name = "edgeTrustedIssuerRegistry")
    public TrustedIssuerRegistry edgeTrustedIssuerRegistry(
            @Qualifier("edgeIdpStarterProperties") IdpStarterProperties idpProperties) {
        return new TrustedIssuerRegistry(idpProperties);
    }

    @Bean(name = "edgeTenantIssuerAuthenticationManager")
    public TenantIssuerAuthenticationManagerComponent edgeTenantIssuerAuthenticationManager(
            @Qualifier("edgeTrustedIssuerRegistry") TrustedIssuerRegistry issuerRegistry,
            @Qualifier("edgeIdpStarterProperties") IdpStarterProperties idpProperties,
            @Qualifier("edgeClock") Clock clock) {
        TenantIssuerAuthenticationManagerComponent manager =
                new TenantIssuerAuthenticationManagerComponent(issuerRegistry, idpProperties, clock);
        manager.initialize();
        return manager;
    }

    @Bean(name = "edgeReactiveOAuthRevocationReader")
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

    @Bean(name = "reactiveOAuth2AuthorizedClientManager")
    @org.springframework.context.annotation.Primary
    public ReactiveOAuth2AuthorizedClientManager reactiveOAuth2AuthorizedClientManager(
            @Qualifier("clientRegistrationRepository")
            ReactiveClientRegistrationRepository registrations,
            @Qualifier("edgeAuthorizedClientRepository") ServerOAuth2AuthorizedClientRepository repository,
            @Qualifier("redisson") RedissonClient redisson) {
        DefaultReactiveOAuth2AuthorizedClientManager delegate =
                new DefaultReactiveOAuth2AuthorizedClientManager(registrations, repository);
        delegate.setAuthorizedClientProvider(ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode()
                .refreshToken()
                .build());
        return new EdgeSingleFlightAuthorizedClientManager(
                delegate, registrations, redisson.reactive(), properties);
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
                .securityMatcher(ServerWebExchangeMatchers.pathMatchers("/api/**", "/mcp/**"))
                .securityContextRepository(new WebSessionServerSecurityContextRepository())
                .authorizeExchange(authorize -> authorize.anyExchange().permitAll())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokens)
                        .requireCsrfProtectionMatcher(edgeCsrfMatcher()))
                .build();
    }

    @Bean(name = "edgeBrowserSecurityWebFilterChain")
    @Order(Ordered.LOWEST_PRECEDENCE)
    public SecurityWebFilterChain edgeBrowserSecurityWebFilterChain(
            ServerHttpSecurity http,
            @Qualifier("clientRegistrationRepository")
            ReactiveClientRegistrationRepository registrations,
            @Qualifier("edgeAuthorizedClientRepository") ServerOAuth2AuthorizedClientRepository authorizedClients,
            @Qualifier("edgeCsrfTokenRepository")
            ServerCsrfTokenRepository csrfTokens,
            @Qualifier("edgeClock") Clock clock,
            @Qualifier("edgeIdpStarterProperties") IdpStarterProperties idpProperties,
            @Qualifier("jacksonObjectMapper") com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        ServerOAuth2AuthorizationRequestResolver requestResolver = authorizationRequestResolver(registrations,
                java.util.Objects.requireNonNull(idpProperties.getResourceUri(), "resourceUri"));

        ServerRedirectStrategy redirectStrategy = new DefaultServerRedirectStrategy();
        ((DefaultServerRedirectStrategy) redirectStrategy).setHttpStatus(HttpStatus.SEE_OTHER);
        ServerAuthenticationSuccessHandler successHandler = (webFilterExchange, authentication) -> {
            ServerWebExchange exchange = webFilterExchange.getExchange();
            URI destination = properties.getPublicOrigin()
                    .resolve(properties.getAllowedReturnPaths().getFirst());
            return exchange.getSession().flatMap(session -> {
                session.getAttributes().put(EdgeSessionController.SESSION_CREATED_AT_ATTRIBUTE, clock.instant());
                if (authentication instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken oauth) {
                    session.getAttributes().put(
                            EdgeSessionController.SESSION_REGISTRATION_ATTRIBUTE,
                            oauth.getAuthorizedClientRegistrationId());
                }
                return session.changeSessionId();
            }).then(redirectStrategy.sendRedirect(exchange, destination));
        };
        RedirectServerAuthenticationEntryPoint loginEntryPoint = new RedirectServerAuthenticationEntryPoint(
                "/oauth2/authorization/" + properties.getDefaultRegistrationId());
        loginEntryPoint.setRequestCache(NoOpServerRequestCache.getInstance());
        loginEntryPoint.setRedirectStrategy(redirectStrategy);

        return http
                .securityContextRepository(new WebSessionServerSecurityContextRepository())
                .addFilterAt(stepUpIntentFilter(), SecurityWebFiltersOrder.FIRST)
                .requestCache(cache -> cache.requestCache(NoOpServerRequestCache.getInstance()))
                .authorizeExchange(authorize -> authorize
                        .pathMatchers("/bff/csrf", "/actuator/health").permitAll()
                        .pathMatchers("/bff/session", "/bff/logout").authenticated()
                        .pathMatchers("/oauth2/authorization/**", "/login/oauth2/code/**")
                        .permitAll()
                        .pathMatchers("/tenants/**", "/platform/**").permitAll()
                        .anyExchange().authenticated())
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(
                        bffAuthenticationEntryPoint(loginEntryPoint)))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokens)
                        .requireCsrfProtectionMatcher(edgeCsrfMatcher()))
                .oauth2Login(oauth2 -> oauth2
                        .clientRegistrationRepository(registrations)
                        .authorizedClientRepository(authorizedClients)
                        .authorizationRequestResolver(requestResolver)
                        .authenticationSuccessHandler(successHandler)
                        .authenticationFailureHandler((filterExchange, failure) -> {
                            boolean unavailable = false;
                            Throwable cause = failure;
                            for (int depth = 0; cause != null && depth < 8; depth++, cause = cause.getCause()) {
                                if (cause instanceof org.springframework.security.oauth2.core.OAuth2AuthenticationException oauth
                                        && java.util.Set.of("temporarily_unavailable", "server_error", "invalid_token_response")
                                                .contains(oauth.getError().getErrorCode())) {
                                    unavailable = true;
                                }
                            }
                            ServerWebExchange exchange = filterExchange.getExchange();
                            exchange.getResponse().setStatusCode(unavailable
                                    ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_REQUEST);
                            exchange.getResponse().getHeaders().setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                            exchange.getResponse().getHeaders().setCacheControl("no-store");
                            String code = unavailable ? "temporarily_unavailable" : "invalid_request";
                            return Mono.fromCallable(() -> objectMapper.writeValueAsBytes(java.util.Map.of("error", code)))
                                    .flatMap(body -> exchange.getResponse().writeWith(Mono.just(
                                            exchange.getResponse().bufferFactory().wrap(body))));
                        }))
                .oauth2Client(oauth2 -> oauth2
                        .clientRegistrationRepository(registrations)
                        .authorizedClientRepository(authorizedClients))
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .build();
    }

    private ServerOAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ReactiveClientRegistrationRepository registrations, URI resource) {
        DefaultServerOAuth2AuthorizationRequestResolver delegate =
                new DefaultServerOAuth2AuthorizationRequestResolver(registrations);
        delegate.setAuthorizationRequestCustomizer(
                OAuth2AuthorizationRequestCustomizers.withPkce());
        return new ServerOAuth2AuthorizationRequestResolver() {
            @Override
            public Mono<OAuth2AuthorizationRequest> resolve(ServerWebExchange exchange) {
                return resolve(exchange, null);
            }

            @Override
            public Mono<OAuth2AuthorizationRequest> resolve(
                    ServerWebExchange exchange,
                    String registrationId) {
                List<String> stepUpValues = exchange.getRequest()
                        .getQueryParams()
                        .get("stepUp");
                boolean stepUp = stepUpValues != null
                        && stepUpValues.size() == 1
                        && "true".equals(stepUpValues.getFirst());
                Mono<OAuth2AuthorizationRequest> resolved = registrationId == null
                        ? delegate.resolve(exchange)
                        : delegate.resolve(exchange, registrationId);
                return resolved.map(request -> withRegisteredResource(request, stepUp));
            }

            private OAuth2AuthorizationRequest withRegisteredResource(
                    OAuth2AuthorizationRequest request, boolean stepUp) {
                var target = UriComponentsBuilder.fromUriString(request.getAuthorizationRequestUri())
                        .queryParam("resource", org.springframework.web.util.UriUtils.encodeQueryParam(
                                resource.toASCIIString(), StandardCharsets.UTF_8));
                if (stepUp) { target.queryParam("prompt", "login").queryParam("max_age", "0"); }
                return OAuth2AuthorizationRequest.from(request)
                        .additionalParameters(values -> values.put("resource", resource.toASCIIString()))
                        .authorizationRequestUri(target.build(true).toUriString())
                        .build();
            }
        };
    }

    private WebFilter stepUpIntentFilter() {
        return (exchange, chain) -> {
            if (!exchange.getRequest().getPath().value().startsWith("/oauth2/authorization/")) {
                return chain.filter(exchange);
            }
            List<String> values = exchange.getRequest().getQueryParams().get("stepUp");
            boolean known = java.util.Set.of("returnTo", "stepUp").containsAll(exchange.getRequest().getQueryParams().keySet())
                    && exchange.getRequest().getQueryParams().values().stream()
                            .allMatch(entries -> entries.size() == 1 && entries.getFirst().length() <= 2048);
            if (known && (values == null || (values.size() == 1 && "true".equals(values.getFirst())))) {
                return chain.filter(exchange);
            }
            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            return exchange.getResponse().setComplete();
        };
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
                                .name("X-CSRF-TOKEN")));
    }

    private static io.swagger.v3.oas.models.Paths nativeLoginPaths() {
        var registration = new io.swagger.v3.oas.models.parameters.Parameter().name("registrationId").in("path")
                .required(true).schema(new io.swagger.v3.oas.models.media.StringSchema().minLength(1).maxLength(128));
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
                        .schema(new io.swagger.v3.oas.models.media.StringSchema()))
                .addParametersItem(new io.swagger.v3.oas.models.parameters.Parameter().name("stepUp").in("query")
                        .schema(new io.swagger.v3.oas.models.media.StringSchema()._enum(java.util.List.of("true"))))
                .responses(new io.swagger.v3.oas.models.responses.ApiResponses()
                        .addApiResponse("302", new io.swagger.v3.oas.models.responses.ApiResponse()
                                .description("Redirect to configured issuer with PKCE/state/nonce")
                                .addHeaderObject("Location", new io.swagger.v3.oas.models.headers.Header().required(true)
                                        .schema(new io.swagger.v3.oas.models.media.StringSchema().format("uri"))))
                        .addApiResponse("400", bad));
        var callback = new io.swagger.v3.oas.models.Operation().operationId("oauthBffCallback")
                .summary("Complete the stored OIDC transaction").security(java.util.List.of())
                .description("The stored request binds state, nonce, issuer and registration. Tokens stay on the server; only the opaque session reaches the browser.")
                .addParametersItem(registration);
        for (String name : java.util.List.of("code", "state", "error", "error_description", "iss")) {
            callback.addParametersItem(new io.swagger.v3.oas.models.parameters.Parameter().name(name).in("query")
                    .required(name.equals("state")).schema(new io.swagger.v3.oas.models.media.StringSchema()));
        }
        callback.responses(new io.swagger.v3.oas.models.responses.ApiResponses()
                .addApiResponse("303", new io.swagger.v3.oas.models.responses.ApiResponse().description("Resume the validated local return path")
                        .addHeaderObject("Location", new io.swagger.v3.oas.models.headers.Header().required(true)
                                .schema(new io.swagger.v3.oas.models.media.StringSchema()))
                        .addHeaderObject("Cache-Control", noStore))
                .addApiResponse("400", bad)
                .addApiResponse("503", new io.swagger.v3.oas.models.responses.ApiResponse()
                        .description("Issuer/shared security/session dependency unavailable").content(errors)));
        return new io.swagger.v3.oas.models.Paths()
                .addPathItem("/oauth2/authorization/{registrationId}", new io.swagger.v3.oas.models.PathItem().get(login))
                .addPathItem("/login/oauth2/code/{registrationId}", new io.swagger.v3.oas.models.PathItem().get(callback));
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
