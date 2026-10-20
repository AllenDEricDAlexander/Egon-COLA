package top.egon.cola.platform.tianquan.shoubing.admin.oauth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import jakarta.validation.Validator;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.client.RedisException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.ClientSecretAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationValidator;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationConsentAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientCredentialsAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientCredentialsAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2RefreshTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2TokenRevocationAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2ErrorAuthenticationFailureHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriComponentsBuilder;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller.filter.TenantLoginAuthenticationFilter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthAuthorizationAttributesBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthAuthorizationBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthSecuritySnapshotBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.SasAuthorizationConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthClientRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.OAuthTokenIssueService;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.TenantIssuerService;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.TransactionalOAuthAuthenticationProvider;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.TransactionalOAuthRevocationAuthenticationProvider;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.TokenRevocationService;
import top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder;
import top.egon.cola.platform.tianquan.shoubing.core.observability.SecurityMetricObservationBO;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditContextBO;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.converter.TokenUsageAuditEventConverter;

@Slf4j
@Configuration(value = "authorizationServerConfiguration", proxyBeanMethods = false)
public class AuthorizationServerConfiguration {

    @Bean("oauthPublicOrigin")
    URI oauthPublicOrigin(
            @Value("${egon.tianquan-shoubing.oauth.public-origin}") URI configuredOrigin) {
        return validatePublicOrigin(configuredOrigin);
    }

    @Bean("authorizationServerSettings")
    AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .multipleIssuersAllowed(true)
                .jwkSetEndpoint("/.well-known/jwks.json")
                .build();
    }

    @Bean("oauthAuthorizationRequestCache")
    RequestCache oauthAuthorizationRequestCache() {
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        requestCache.setCreateSessionAllowed(true);
        requestCache.setMatchingRequestParameterName(null);
        return requestCache;
    }

    @Bean("oauthLoginCsrfTokenRepository")
    CsrfTokenRepository oauthLoginCsrfTokenRepository() {
        return new HttpSessionCsrfTokenRepository();
    }

    @Bean("oauthLoginSessionAuthenticationStrategy")
    SessionAuthenticationStrategy oauthLoginSessionAuthenticationStrategy(
            @Qualifier("oauthLoginCsrfTokenRepository") CsrfTokenRepository csrfTokenRepository) {
        return new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(),
                new CsrfAuthenticationStrategy(csrfTokenRepository)));
    }

    @Bean("tenantAuthorizationServerJwtDecoder")
    @Primary
    JwtDecoder tenantAuthorizationServerJwtDecoder(
            @Qualifier("tenantJwkSourceComponent") JWKSource<SecurityContext> jwkSource,
            @Qualifier("tenantIssuerService") TenantIssuerService tenantIssuerService,
            @Qualifier("idpSecurityMetricsRecorder") ObjectProvider<IdpSecurityMetricsRecorder> metricProviders) {
        IdpSecurityMetricsRecorder metrics = metricProviders.getIfAvailable(() -> IdpSecurityMetricsRecorder.NOOP);
        DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource));
        processor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(new JOSEObjectType("at+jwt")));
        processor.setJWTClaimsSetVerifier((claims, context) -> { });
        var verifierFactory = processor.getJWSVerifierFactory();
        var metricFailureLogged = new java.util.concurrent.atomic.AtomicBoolean();
        processor.setJWSVerifierFactory(new com.nimbusds.jose.proc.JWSVerifierFactory() {
            @Override
            public com.nimbusds.jose.JWSVerifier createJWSVerifier(com.nimbusds.jose.JWSHeader header, java.security.Key key)
                    throws com.nimbusds.jose.JOSEException {
                var original = verifierFactory.createJWSVerifier(header, key);
                return new com.nimbusds.jose.JWSVerifier() {
                    @Override
                    public boolean verify(com.nimbusds.jose.JWSHeader signatureHeader, byte[] input, com.nimbusds.jose.util.Base64URL signature)
                            throws com.nimbusds.jose.JOSEException {
                        long started = System.nanoTime(); String result = "FAILURE";
                        try { boolean valid = original.verify(signatureHeader, input, signature); result = valid ? "SUCCESS" : "FAILURE"; return valid; }
                        finally {
                            java.time.Duration elapsed = java.time.Duration.ofNanos(Math.max(0, System.nanoTime() - started));
                            try { metrics.record(SecurityMetricObservationBO.builder().signal("jwt.verify.duration").result(result).duration(elapsed).build()); }
                            catch (RuntimeException metricFailure) {
                                if (metricFailureLogged.compareAndSet(false, true)) {
                                    log.warn("Native JWT metric unavailable; failureType={}", metricFailure.getClass().getSimpleName());
                                }
                            }
                        }
                    }
                    @Override public java.util.Set<JWSAlgorithm> supportedJWSAlgorithms() { return original.supportedJWSAlgorithms(); }
                    @Override public com.nimbusds.jose.jca.JCAContext getJCAContext() { return original.getJCAContext(); }
                };
            }
            @Override public java.util.Set<JWSAlgorithm> supportedJWSAlgorithms() { return verifierFactory.supportedJWSAlgorithms(); }
            @Override public com.nimbusds.jose.jca.JCAContext getJCAContext() { return verifierFactory.getJCAContext(); }
        });
        JwtDecoder delegate = new NimbusJwtDecoder(processor);
        return token -> {
            Jwt jwt;
            try {
                jwt = delegate.decode(token);
            } catch (JwtException failure) {
                if (!isInfrastructureFailure(failure)) { throw failure; }
                throw new OAuth2AuthenticationException(new OAuth2Error("temporarily_unavailable", "Request temporarily unavailable", null), failure);
            } catch (DataAccessException | RedisException failure) {
                throw new OAuth2AuthenticationException(new OAuth2Error("temporarily_unavailable", "Request temporarily unavailable", null), failure);
            }
            TrustedRealmBO trustedRealm = tenantIssuerService.resolveCurrentIssuer();
            if (!trustedRealm.getIssuer().toASCIIString().equals(jwt.getClaimAsString("iss"))) {
                throw new JwtValidationException("Token issuer does not match the current issuer.",
                        List.of(new OAuth2Error("invalid_token", "Request rejected", null)));
            }
            return jwt;
        };
    }

    @Bean("tenantOAuthTokenGenerator")
    OAuth2TokenGenerator<? extends OAuth2Token> tenantOAuthTokenGenerator(
            @Qualifier("tenantJwkSourceComponent") JWKSource<SecurityContext> jwkSource,
            @Qualifier("oauthTokenIssueService") OAuthTokenIssueService tokenIssueService,
            @Qualifier("tenantIssuerService") TenantIssuerService tenantIssuerService,
            @Qualifier("oAuthRealmRepository") OAuthRealmRepository realmRepository) {
        JwtGenerator jwtGenerator = new JwtGenerator(new NimbusJwtEncoder(jwkSource));
        jwtGenerator.setJwtCustomizer(context -> customizeJwt(
                context, tokenIssueService, tenantIssuerService, realmRepository));
        OAuth2RefreshTokenGenerator refreshGenerator = new OAuth2RefreshTokenGenerator();
        OAuth2TokenGenerator<OAuth2RefreshToken> boundedRefreshGenerator = context -> {
            OAuth2RefreshToken refresh = refreshGenerator.generate(context);
            if (refresh == null || context.getAuthorization() == null) {
                return refresh;
            }
            Instant deadline = context.getAuthorization().getAttribute(SasAuthorizationConverter.ABSOLUTE_EXPIRES_AT_ATTRIBUTE);
            if (deadline == null || !deadline.isAfter(refresh.getIssuedAt())) {
                throw new OAuth2AuthenticationException(new OAuth2Error("invalid_grant", "Request rejected", null));
            }
            return new OAuth2RefreshToken(refresh.getTokenValue(), refresh.getIssuedAt(),
                    refresh.getExpiresAt().isAfter(deadline) ? deadline : refresh.getExpiresAt());
        };
        return new DelegatingOAuth2TokenGenerator(jwtGenerator, new OAuth2AccessTokenGenerator(), boundedRefreshGenerator);
    }

    @Bean("authorizationServerSecurityFilterChain")
    @Order(1)
    SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            @Qualifier("authorizationServerSettings") AuthorizationServerSettings settings,
            @Qualifier("tenantRegisteredClientRepository") RegisteredClientRepository registeredClientRepository,
            @Qualifier("tenantAuthorizationService") OAuth2AuthorizationService authorizationService,
            @Qualifier("tenantConsentService") OAuth2AuthorizationConsentService authorizationConsentService,
            @Qualifier("tenantIssuerService") TenantIssuerService tenantIssuerService,
            @Qualifier("oauthPublicOrigin") URI publicOrigin,
            @Qualifier("oauthAuthorizationRequestCache") RequestCache requestCache,
            ObjectProvider<OAuthTokenIssueService> tokenIssueServices,
            ObjectProvider<OAuth2TokenGenerator<? extends OAuth2Token>> tokenGenerators,
            ObjectProvider<PlatformTransactionManager> transactionManagers,
            ObjectProvider<TenantLoginAuthenticationFilter> loginFilters,
            ObjectProvider<TokenRevocationService> tokenRevocationServices,
            @Qualifier("oAuthClientRepository") ObjectProvider<OAuthClientRepository> clientRepositories,
            @Qualifier("oauthLoginCsrfTokenRepository") CsrfTokenRepository csrfTokenRepository,
            @Qualifier("idpClock") Clock clock,
            @Qualifier("jacksonObjectMapper") ObjectMapper objectMapper,
            @Qualifier("corsConfigurationSource") ObjectProvider<org.springframework.web.cors.CorsConfigurationSource> corsSources,
            @Qualifier("oauthNativeCorsProcessor") ObjectProvider<OAuthNativeCorsProcessor> corsProcessors,
            @Qualifier("oauthUsageObservationProperties") ObjectProvider<OAuthUsageObservationProperties> usageProperties,
            @Qualifier("tokenUsageAuditComponent") ObjectProvider<TokenUsageAuditComponent> auditProducers,
            @Qualifier("tokenUsageAuditEventConverterImpl") ObjectProvider<TokenUsageAuditEventConverter> auditConverters,
            @Qualifier("idpStarterProperties") ObjectProvider<IdpStarterProperties> receiverProperties,
            @Qualifier("defaultValidator") ObjectProvider<Validator> schemaValidators) throws Exception {
        OAuthUsageObservationProperties usage = usageProperties.getIfAvailable(OAuthUsageObservationProperties::new);
        TokenUsageAuditComponent producer = usage.isEnabled() ? auditProducers.getObject() : null;
        TokenUsageAuditEventConverter converter = usage.isEnabled() ? auditConverters.getObject() : null;
        IdpStarterProperties receiver = usage.isEnabled() ? receiverProperties.getObject() : null;
        Validator validator = usage.isEnabled() ? schemaValidators.getObject() : null;
        AuthenticationFailureHandler userInfoErrors = userInfoFailureHandler(objectMapper, producer, converter, usage,
                receiver == null ? null : receiver.getResourceUri(), clock, validator);
        OAuth2AuthorizationServerConfigurer authorizationServer =
                OAuth2AuthorizationServerConfigurer.authorizationServer();
        RequestMatcher endpoints = authorizationServer.getEndpointsMatcher();
        RequestMatcher loginRequests = request -> request.getRequestURI().substring(
                request.getContextPath() == null ? 0 : request.getContextPath().length())
                .matches("/tenants/[1-9][0-9]{0,18}/login");
        RequestMatcher canonicalIssuerPaths = request -> request.getRequestURI().substring(
                request.getContextPath().length()).matches("/(platform|tenants/[1-9][0-9]{0,18})/.*"
                        + "|/\\.well-known/oauth-authorization-server/(platform|tenants/[1-9][0-9]{0,18})");
        RequestMatcher consentPage = request -> contextPath(request).matches("/tenants/[1-9][0-9]{0,18}/consent");
        RequestMatcher protocolAndLogin = new AndRequestMatcher(canonicalIssuerPaths,
                new OrRequestMatcher(endpoints, loginRequests, consentPage));
        OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator = tokenGenerators.getIfAvailable();
        if (tokenGenerator == null) {
            throw new IllegalStateException("OAUTH_TOKEN_GENERATOR_REQUIRED");
        }
        org.springframework.web.filter.CorsFilter nativeCors = new org.springframework.web.filter.CorsFilter(
                corsSources.getIfAvailable(() -> request -> null));
        nativeCors.setCorsProcessor(corsProcessors.getIfAvailable(() -> new OAuthNativeCorsProcessor(
                new org.springframework.web.cors.DefaultCorsProcessor())));
        http.addFilter(nativeCors);
        http.securityMatcher(new OrRequestMatcher(protocolAndLogin, OAuthNativeCorsProcessor::isNativePreflight))
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository)
                        .withObjectPostProcessor(new ObjectPostProcessor<CsrfFilter>() {
                            @Override
                            public <O extends CsrfFilter> O postProcess(O filter) {
                                // SAS exempts all endpoints during init; the native browser POST must remain protected.
                                filter.setRequireCsrfProtectionMatcher(request -> CsrfFilter.DEFAULT_CSRF_MATCHER.matches(request)
                                        && (!endpoints.matches(request) || isConsentSubmit(request)));
                                var rejected = new org.springframework.security.web.access.AccessDeniedHandlerImpl();
                                filter.setAccessDeniedHandler((request, response, failure) -> {
                                    log.warn("OAuth browser CSRF rejected; method={} path={} tokenPresent={} sessionValid={} failureType={}",
                                            request.getMethod(), contextPath(request), request.getParameter("_csrf") != null,
                                            request.isRequestedSessionIdValid(), failure.getClass().getSimpleName());
                                    rejected.handle(request, response, failure);
                                });
                                return filter;
                            }
                        }))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .requestCache(cache -> cache.requestCache(requestCache))
                .with(authorizationServer, server -> server
                        .tokenGenerator(tokenGenerator)
                        .authorizationServerSettings(settings)
                        .registeredClientRepository(registeredClientRepository)
                        .authorizationService(authorizationService)
                        .authorizationConsentService(authorizationConsentService)
                        .clientAuthentication(client -> {
                            client.authenticationConverters(converters -> converters.add(0, request -> {
                                var headers = request.getHeaders("Authorization");
                                boolean hasAuthorization = headers.hasMoreElements();
                                if (hasAuthorization) {
                                    headers.nextElement();
                                }
                                if (headers.hasMoreElements() || request.getParameter("client_secret") != null
                                        || (hasAuthorization && request.getParameter("client_id") != null)) {
                                    throw new OAuth2AuthenticationException(new OAuth2Error(
                                            "invalid_client", "Request rejected", null));
                                }
                                return null;
                            }));
                            client.authenticationProviders(providers -> providers.forEach(provider -> {
                                if (provider instanceof ClientSecretAuthenticationProvider secretProvider) {
                                    secretProvider.setPasswordEncoder(new BCryptPasswordEncoder(12));
                                }
                            }));
                        })
                        .authorizationEndpoint(endpoint -> endpoint.consentPage("/consent")
                                .authenticationProviders(providers -> configureAuthorizationProviders(providers,
                                        authorizationService, tokenIssueServices, transactionManagers, tenantIssuerService, clock, clientRepositories))
                                .errorResponseHandler(authorizationFailureHandler(objectMapper)))
                        .tokenEndpoint(endpoint -> {
                            endpoint.authenticationProviders(providers -> configureTokenProviders(
                                    providers, tokenIssueServices, transactionManagers, tenantIssuerService));
                            endpoint.errorResponseHandler(tokenEndpointFailureHandler(objectMapper));
                        })
                        .tokenRevocationEndpoint(endpoint -> {
                            endpoint.authenticationProviders(providers -> configureRevocationProviders(
                                    providers, authorizationService, tokenRevocationServices.getIfAvailable(),
                                    tenantIssuerService, transactionManagers.getIfAvailable()));
                            endpoint.errorResponseHandler(tokenEndpointFailureHandler(objectMapper));
                        })
                        .authorizationServerMetadataEndpoint(metadata -> metadata
                                .authorizationServerMetadataCustomizer(builder -> {
                                    builder.tokenEndpointAuthenticationMethods(AuthorizationServerConfiguration::metadataAuthenticationMethods);
                                    builder.tokenRevocationEndpointAuthenticationMethods(AuthorizationServerConfiguration::metadataAuthenticationMethods);
                                    builder.grantTypes(AuthorizationServerConfiguration::metadataGrantTypes);
                                }))
                        .oidc(oidc -> oidc.userInfoEndpoint(userinfo -> userinfo
                                .authenticationProviders(providers -> configureUserInfoProviders(providers,
                                        tokenIssueServices, transactionManagers, tenantIssuerService, producer, converter, usage,
                                        receiver == null ? null : receiver.getResourceUri(), clock, validator))
                                .errorResponseHandler(userInfoErrors))
                                .providerConfigurationEndpoint(metadata -> metadata
                                .providerConfigurationCustomizer(builder -> {
                                    builder.tokenEndpointAuthenticationMethods(AuthorizationServerConfiguration::metadataAuthenticationMethods);
                                    builder.tokenRevocationEndpointAuthenticationMethods(AuthorizationServerConfiguration::metadataAuthenticationMethods);
                                    builder.grantTypes(AuthorizationServerConfiguration::metadataGrantTypes);
                                }))))
                .oauth2ResourceServer(resource -> resource.jwt(Customizer.withDefaults())
                        .authenticationEntryPoint((request, response, failure) ->
                                userInfoErrors.onAuthenticationFailure(request, response, failure)))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(loginRequests).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception.authenticationEntryPoint((request, response, failure) -> {
                    if ("GET".equals(request.getMethod()) && contextPath(request).matches("/tenants/[1-9][0-9]{0,18}/userinfo")) {
                        userInfoErrors.onAuthenticationFailure(request, response, failure);
                        return;
                    }
                    TrustedRealmBO trustedRealm = tenantIssuerService.resolveCurrentIssuer();
                    if (consentPage.matches(request)) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("text/html;charset=UTF-8");
                        response.setHeader("Cache-Control", "no-store");
                        response.getWriter().write("<!doctype html><html><body><p>SAS session required</p></body></html>");
                        return;
                    }
                    response.setStatus(HttpServletResponse.SC_SEE_OTHER);
                    response.setHeader("Cache-Control", "no-store");
                    response.setHeader("Location", trustedRealm.getIssuer().getRawPath() + "/login");
                }));

        // SAS installs its protocol endpoint filters at this anchor after the issuer context filter.
        http.addFilterBefore(ExactIssuerContextFilter.create(tenantIssuerService, publicOrigin, objectMapper),
                AbstractPreAuthenticatedProcessingFilter.class);
        TenantLoginAuthenticationFilter loginFilter = loginFilters.getIfAvailable();
        if (loginFilter != null) {
            http.addFilterAfter(loginFilter, ExactIssuerContextFilter.class);
        }
        return http.build();
    }

    private static void metadataAuthenticationMethods(List<String> methods) {
        methods.clear();
        methods.add("client_secret_basic");
        if (!"/platform".equals(URI.create(
                AuthorizationServerContextHolder.getContext().getIssuer()).getRawPath())) {
            methods.add("none");
        }
    }

    private static void metadataGrantTypes(List<String> grants) {
        grants.clear();
        if (!"/platform".equals(URI.create(
                AuthorizationServerContextHolder.getContext().getIssuer()).getRawPath())) {
            grants.add("authorization_code");
            grants.add("refresh_token");
        }
        grants.add("client_credentials");
    }

    private static void configureAuthorizationProviders(
            List<AuthenticationProvider> providers,
            OAuth2AuthorizationService authorizations,
            ObjectProvider<OAuthTokenIssueService> issueServices,
            ObjectProvider<PlatformTransactionManager> transactionManagers,
            TenantIssuerService issuers, Clock clock, ObjectProvider<OAuthClientRepository> clientRepositories) {
        OAuthTokenIssueService issueService = issueServices.getIfAvailable();
        PlatformTransactionManager transactionManager = transactionManagers.getIfAvailable();
        if (issueService == null || transactionManager == null) {
            throw new IllegalStateException("OAUTH_AUTHORIZATION_TRANSACTION_REQUIRED");
        }
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        OAuth2AuthorizationCodeRequestAuthenticationValidator standardValidator =
                new OAuth2AuthorizationCodeRequestAuthenticationValidator();
        for (int index = 0; index < providers.size(); index++) {
            AuthenticationProvider provider = providers.get(index);
            TransactionalOAuthAuthenticationProvider.GrantTransaction strategy = null;
            if (provider instanceof OAuth2AuthorizationCodeRequestAuthenticationProvider codeProvider) {
                codeProvider.setAuthenticationValidator(context -> {
                    standardValidator.accept(context);
                    OAuth2AuthorizationCodeRequestAuthenticationToken request = context.getAuthentication();
                    try {
                        requireS256Challenge(text(request.getAdditionalParameters().get("code_challenge_method")),
                                text(request.getAdditionalParameters().get("code_challenge")));
                        requireAuthorizationTransaction(request, context.getRegisteredClient(),
                                issuers.resolveCurrentIssuer(), clientRepositories.getObject());
                    } catch (OAuth2AuthenticationException rejected) {
                        throw new OAuth2AuthorizationCodeRequestAuthenticationException(rejected.getError(), request);
                    }
                });
                strategy = TransactionalOAuthAuthenticationProvider.authorizationRequest(clock);
            } else if (provider instanceof OAuth2AuthorizationConsentAuthenticationProvider) {
                strategy = TransactionalOAuthAuthenticationProvider.authorizationConsent(authorizations);
            }
            if (strategy != null) {
                providers.set(index, new TransactionalOAuthAuthenticationProvider(provider, transaction,
                        issueService, issuers, strategy));
            }
        }
    }

    static void requireS256Challenge(String method, String challenge) {
        if (!"S256".equals(method) || challenge == null
                || !challenge.matches("[A-Za-z0-9_-]{43}")) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    "invalid_request", "Request rejected", null));
        }
    }

    private static void requireAuthorizationTransaction(OAuth2AuthorizationCodeRequestAuthenticationToken request,
            RegisteredClient registeredClient, TrustedRealmBO realm, OAuthClientRepository clients) {
        String state = request.getState();
        String nonce = text(request.getAdditionalParameters().get("nonce"));
        String resource = text(request.getAdditionalParameters().get("resource"));
        if (state == null || state.isBlank() || state.length() < 32 || state.length() > 512
                || nonce == null || nonce.isBlank() || nonce.length() < 32 || nonce.length() > 512
                || resource == null || resource.isBlank() || resource.length() > 2048
                || request.getScopes().isEmpty() || request.getScopes().size() > 64
                || request.getScopes().stream().anyMatch(scope -> !scope.matches("[A-Za-z0-9._:-]{1,128}"))) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_request", "Request rejected", null));
        }
        URI target;
        try { target = URI.create(resource); }
        catch (IllegalArgumentException failure) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_target", "Request rejected", null));
        }
        if (target.getHost() == null || target.getUserInfo() != null || target.getFragment() != null
                || !("https".equals(target.getScheme()) || "http".equals(target.getScheme()) && "127.0.0.1".equals(target.getHost()))) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_target", "Request rejected", null));
        }
        OAuthClientBO client = clients.findById(realm, parsePositiveId(registeredClient.getId(), "invalid_request"))
                .orElseThrow(() -> new OAuth2AuthenticationException(new OAuth2Error("invalid_request", "Request rejected", null)));
        if (client.getResourceUris() == null || !client.getResourceUris().contains(target.toASCIIString())) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_target", "Request rejected", null));
        }
    }

    private static void configureUserInfoProviders(List<AuthenticationProvider> providers,
            ObjectProvider<OAuthTokenIssueService> issueServices,
            ObjectProvider<PlatformTransactionManager> transactionManagers, TenantIssuerService issuers,
            TokenUsageAuditComponent producer, TokenUsageAuditEventConverter converter, OAuthUsageObservationProperties properties,
            URI resource, Clock clock, Validator validator) {
        OAuthTokenIssueService issueService = issueServices.getIfAvailable();
        PlatformTransactionManager transactionManager = transactionManagers.getIfAvailable();
        if (issueService == null || transactionManager == null) {
            throw new IllegalStateException("OAUTH_USERINFO_TRANSACTION_REQUIRED");
        }
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setReadOnly(true);
        for (int index = 0; index < providers.size(); index++) {
            AuthenticationProvider provider = providers.get(index);
            if (provider instanceof OidcUserInfoAuthenticationProvider) {
                providers.set(index, new TransactionalOAuthAuthenticationProvider(provider, transaction,
                        issueService, issuers, TransactionalOAuthAuthenticationProvider.userInfo(producer, converter, properties, resource, clock, validator)));
            }
        }
    }

    private static boolean isInfrastructureFailure(Throwable failure) {
        Throwable current = failure;
        for (int depth = 0; current != null && depth < 16; depth++, current = current.getCause()) {
            if (current instanceof DataAccessException || current instanceof RedisException || current instanceof KeySourceException) {
                return true;
            }
        }
        return false;
    }

    private static void configureTokenProviders(
            List<AuthenticationProvider> providers,
            ObjectProvider<OAuthTokenIssueService> tokenIssueServices,
            ObjectProvider<PlatformTransactionManager> transactionManagers,
            TenantIssuerService tenantIssuerService) {
        OAuthTokenIssueService issueService = tokenIssueServices.getIfAvailable();
        PlatformTransactionManager transactionManager = transactionManagers.getIfAvailable();
        if (issueService == null || transactionManager == null) {
            throw new IllegalStateException("OAUTH_TOKEN_TRANSACTION_SERVICE_REQUIRED");
        }
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        for (int index = 0; index < providers.size(); index++) {
            AuthenticationProvider provider = providers.get(index);
            TransactionalOAuthAuthenticationProvider.GrantTransaction grantTransaction = null;
            if (provider instanceof OAuth2AuthorizationCodeAuthenticationProvider) {
                grantTransaction = TransactionalOAuthAuthenticationProvider.authorizationCode();
            } else if (provider instanceof OAuth2RefreshTokenAuthenticationProvider) {
                grantTransaction = TransactionalOAuthAuthenticationProvider.refreshToken();
            } else if (provider instanceof OAuth2ClientCredentialsAuthenticationProvider) {
                grantTransaction = TransactionalOAuthAuthenticationProvider.clientCredentials();
            }
            if (grantTransaction != null) {
                providers.set(index, new TransactionalOAuthAuthenticationProvider(
                        provider, transactionTemplate, issueService,
                        tenantIssuerService, grantTransaction));
            }
        }
    }

    static void configureRevocationProviders(
            List<AuthenticationProvider> providers,
            OAuth2AuthorizationService authorizationService,
            TokenRevocationService revocationService,
            TenantIssuerService tenantIssuerService,
            PlatformTransactionManager transactionManager) {
        Objects.requireNonNull(providers, "providers");
        Objects.requireNonNull(authorizationService, "authorizationService");
        Objects.requireNonNull(revocationService, "revocationService");
        Objects.requireNonNull(tenantIssuerService, "tenantIssuerService");
        Objects.requireNonNull(transactionManager, "transactionManager");
        boolean found = false;
        for (int index = 0; index < providers.size(); index++) {
            AuthenticationProvider provider = providers.get(index);
            if (provider instanceof OAuth2TokenRevocationAuthenticationProvider) {
                providers.set(index, new TransactionalOAuthRevocationAuthenticationProvider(
                        authorizationService, revocationService, tenantIssuerService, transactionManager));
                found = true;
            } else if (provider instanceof TransactionalOAuthRevocationAuthenticationProvider) {
                found = true;
            }
        }
        if (!found) {
            throw new IllegalStateException("OAUTH_SAS_REVOCATION_PROVIDER_MISSING");
        }
    }

    private static void customizeJwt(
            JwtEncodingContext context,
            OAuthTokenIssueService tokenIssueService,
            TenantIssuerService tenantIssuerService,
            OAuthRealmRepository realmRepository) {
        RegisteredClient registeredClient = context.getRegisteredClient();
        TrustedRealmBO trustedRealm = tenantIssuerService.resolveCurrentIssuer();
        OAuth2Authorization authorization = context.getAuthorization();
        if (registeredClient == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_grant", "Request rejected", null));
        }
        long clientPk = parsePositiveId(registeredClient.getId(), "OAUTH_CLIENT_REGISTRATION_ID_INVALID");
        boolean idToken = OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue());
        OAuthRealmBO signingRealm = realmRepository.findByIssuer(trustedRealm)
                .filter(value -> "ACTIVE".equals(value.getStatus()) && value.getActiveKid() != null)
                .orElseThrow(() -> new OAuth2AuthenticationException(
                        new OAuth2Error("temporarily_unavailable", "Request temporarily unavailable", null)));
        context.getJwsHeader()
                .algorithm(SignatureAlgorithm.RS256)
                .keyId(signingRealm.getActiveKid())
                .type(idToken ? "JWT" : "at+jwt");
        if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())) {
            if (idToken) {
                throw new OAuth2AuthenticationException(new OAuth2Error("invalid_grant", "Request rejected", null));
            }
            Object requestedResource = ((OAuth2ClientCredentialsAuthenticationToken) context.getAuthorizationGrant())
                    .getAdditionalParameters().get("resource");
            URI resource = requestedResource instanceof URI uri
                    ? uri.normalize()
                    : requestedResource instanceof String value ? URI.create(value).normalize() : null;
            if (resource == null) {
                throw new OAuth2AuthenticationException(new OAuth2Error("invalid_target", "Request rejected", null));
            }
            OAuthTokenIssueService.ClientCredentialsIssueContext issue = tokenIssueService
                    .authorizeClientCredentials(trustedRealm, clientPk, resource, context.getAuthorizedScopes());
            var access = issue.resourceAccess();
            if (access.scopeContext() == top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext.TENANT) {
                if (trustedRealm.getRealmType() != top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum.TENANT
                        || trustedRealm.getBusinessTenantId() == null
                        || !Long.toString(trustedRealm.getBusinessTenantId()).equals(access.tenantId())) {
                    throw new OAuth2AuthenticationException(new OAuth2Error("invalid_grant", "Request rejected", null));
                }
                context.getClaims().claim("tenant_id", trustedRealm.getBusinessTenantId());
            } else if (trustedRealm.getRealmType()
                    != top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum.PLATFORM) {
                throw new OAuth2AuthenticationException(new OAuth2Error("invalid_grant", "Request rejected", null));
            }
            context.getClaims()
                    .claim("iss", trustedRealm.getIssuer().toASCIIString())
                    .claim("sub", registeredClient.getClientId())
                    .claim("aud", List.of(access.targetResourceUri().toASCIIString()))
                    .claim("scope", String.join(" ", context.getAuthorizedScopes()))
                    .claim("jti", SnowflakeIdGenerator.nextId())
                    .claim("client_id", registeredClient.getClientId())
                    .claim("authorization_id", Long.toString(issue.authorizationId()))
                    .claim("client_registration_id", registeredClient.getId())
                    .claims(values -> values.putAll(
                            serviceIdentityClaims(issue.appId(), issue.credentialId())))
                    .claim("principal_type", "SERVICE")
                    .claim("scope_context", access.scopeContext().name())
                    .claim("source_biz", access.sourceBizCode())
                    .claim("source_app", access.sourceAppCode())
                    .claim("source_env", access.sourceEnvironment())
                    .claim("resource_version", access.targetResourceVersion())
                    .claim("security_version", securityVersion(issue.securitySnapshot()));
            return;
        }

        if (authorization == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_grant", "Request rejected", null));
        }
        OAuthAuthorizationAttributesBO attributes = authorization.getAttribute(
                SasAuthorizationConverter.ATTRIBUTES_ATTRIBUTE);
        if (attributes == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_grant", "Request rejected", null));
        }
        List<String> audiences = authorization.getAttribute(SasAuthorizationConverter.REQUESTED_AUDIENCES_ATTRIBUTE);
        if (audiences == null) {
            audiences = List.of();
        }
        Instant absoluteExpiresAt = authorization.getAttribute(SasAuthorizationConverter.ABSOLUTE_EXPIRES_AT_ATTRIBUTE);
        if (absoluteExpiresAt == null) {
            absoluteExpiresAt = context.getAuthorization().getToken(org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode.class)
                    .getToken().getExpiresAt();
        }
        OAuthAuthorizationBO authorizationBO = OAuthAuthorizationBO.builder()
                .id(parsePositiveId(authorization.getId(), "OAUTH_AUTHORIZATION_ID_INVALID"))
                .version(0L)
                .clientPk(clientPk)
                .principalName(authorization.getPrincipalName())
                .principalType("USER")
                .grantType(authorization.getAuthorizationGrantType().getValue())
                .authorizedScopes(List.copyOf(authorization.getAuthorizedScopes()))
                .audiences(audiences)
                .status("ACTIVE")
                .authTime(attributes.getAuthTime())
                .acr(attributes.getAcr())
                .attributes(attributes)
                .absoluteExpiresAt(absoluteExpiresAt)
                .build();
        OAuthTokenIssueService.UserAuthorizationIssueContext issue = tokenIssueService
                .authorizeUserToken(trustedRealm, authorizationBO);
        context.getClaims()
                .claim("iss", trustedRealm.getIssuer().toASCIIString())
                .claim("sub", issue.subject());
        if (idToken) {
            context.getClaims().claim("aud", List.of(registeredClient.getClientId()))
                    .claim("jti", SnowflakeIdGenerator.nextId());
            if (attributes.getNonce() != null) {
                context.getClaims().claim("nonce", attributes.getNonce());
            }
            if (attributes.getAuthTime() != null) {
                context.getClaims().claim("auth_time", attributes.getAuthTime().getEpochSecond());
            }
            if (attributes.getAcr() != null) {
                context.getClaims().claim("acr", attributes.getAcr());
            }
        } else {
            if (issue.audiences().isEmpty()) {
                throw new OAuth2AuthenticationException(new OAuth2Error("invalid_target", "Request rejected", null));
            }
            context.getClaims()
                    .claim("aud", issue.audiences().stream().map(URI::toASCIIString).toList())
                    .claim("scope", String.join(" ", context.getAuthorizedScopes()))
                    .claim("jti", SnowflakeIdGenerator.nextId())
                    .claim("client_id", registeredClient.getClientId())
                    .claim("authorization_id", Long.toString(issue.authorizationId()))
                    .claim("client_registration_id", registeredClient.getId())
                    .claim("principal_type", "USER")
                    .claim("scope_context", trustedRealm.getBusinessTenantId() == null ? "PLATFORM" : "TENANT")
                    .claim("security_version", securityVersion(issue.securitySnapshot()));
            if (attributes.getAuthTime() != null) {
                context.getClaims().claim("auth_time", attributes.getAuthTime().getEpochSecond());
            }
            if (attributes.getAcr() != null) {
                context.getClaims().claim("acr", attributes.getAcr());
            }
            if (trustedRealm.getBusinessTenantId() != null) {
                context.getClaims().claim("tenant_id", trustedRealm.getBusinessTenantId().toString());
            }
        }
        Map<String, Object> currentClaims = new java.util.HashMap<>();
        context.getClaims().claims(currentClaims::putAll);
        Instant generatedExpiry = (Instant) currentClaims.get("exp");
        if (generatedExpiry != null && generatedExpiry.isAfter(absoluteExpiresAt)) {
            context.getClaims().expiresAt(absoluteExpiresAt);
        }
    }

    private static Map<String, Object> securityVersion(OAuthSecuritySnapshotBO snapshot) {
        Map<String, Object> version = new java.util.LinkedHashMap<>();
        version.put("realm", snapshot.getRealmGeneration());
        version.put("client", snapshot.getClientGeneration());
        if (snapshot.getSubjectGeneration() != null) {
            version.put("subject", snapshot.getSubjectGeneration());
        }
        return version;
    }

    static Map<String, Object> serviceIdentityClaims(String appId, String credentialId) {
        if (appId == null || appId.isBlank() || !appId.equals(appId.trim())
                || credentialId == null || credentialId.isBlank()
                || !credentialId.equals(credentialId.trim())
                || appId.length() > 128 || credentialId.length() > 128) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_grant", "Request rejected", null));
        }
        return Map.of("app_id", appId, "credential_id", credentialId);
    }

    private static long parsePositiveId(String value, String errorCode) {
        try {
            long id = Long.parseLong(value);
            if (id <= 0L) {
                throw new NumberFormatException();
            }
            return id;
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(errorCode, failure);
        }
    }

    private static String text(Object value) {
        return value instanceof String text ? text : null;
    }

    private static AuthenticationFailureHandler tokenEndpointFailureHandler(ObjectMapper objectMapper) {
        OAuth2ErrorAuthenticationFailureHandler defaultHandler = new OAuth2ErrorAuthenticationFailureHandler();
        return (request, response, failure) -> {
            if (failure instanceof OAuth2AuthenticationException authenticationFailure
                    && "temporarily_unavailable".equals(authenticationFailure.getError().getErrorCode())) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setHeader("Cache-Control", "no-store");
                objectMapper.writeValue(response.getWriter(), Map.of("error", "temporarily_unavailable",
                        "error_description", "Request temporarily unavailable"));
                return;
            }
            defaultHandler.onAuthenticationFailure(request, response, failure);
        };
    }

    private static String contextPath(HttpServletRequest request) {
        String context = request.getContextPath();
        return context == null || context.isEmpty() ? request.getRequestURI()
                : request.getRequestURI().substring(context.length());
    }

    private static boolean isConsentSubmit(HttpServletRequest request) {
        return "POST".equals(request.getMethod())
                && contextPath(request).matches("/tenants/[1-9][0-9]{0,18}/oauth2/authorize");
    }

    private static boolean isAuthorizationRequest(HttpServletRequest request) {
        return "GET".equals(request.getMethod())
                && contextPath(request).matches("/tenants/[1-9][0-9]{0,18}/oauth2/authorize");
    }

    private static boolean boundedAuthorizationQuery(HttpServletRequest request) {
        if (request.getHeader("Authorization") != null
                || request.getQueryString() != null && request.getQueryString().length() > 16 * 1024
                || !java.util.Set.of("client_id", "response_type", "redirect_uri", "scope", "resource", "state", "nonce",
                        "code_challenge", "code_challenge_method", "prompt", "max_age").containsAll(request.getParameterMap().keySet())) {
            return false;
        }
        for (var entry : request.getParameterMap().entrySet()) {
            int maximum = switch (entry.getKey()) {
                case "scope" -> 8320;
                case "redirect_uri", "resource" -> 2048;
                case "state", "nonce" -> 512;
                default -> 128;
            };
            String[] values = entry.getValue();
            if (values == null || values.length != 1 || values[0] == null || values[0].length() > maximum) { return false; }
            if (entry.getKey().equals("scope")) {
                List<String> scopes = java.util.Arrays.asList(values[0].split(" ", -1));
                if (scopes.size() > 64 || scopes.stream().anyMatch(String::isBlank)
                        || java.util.Set.copyOf(scopes).size() != scopes.size()) { return false; }
            }
        }
        return true;
    }

    private static boolean boundedConsentForm(HttpServletRequest request) {
        if (request.getContentLengthLong() > 16 * 1024 || request.getQueryString() != null
                || !java.util.Set.of("client_id", "state", "scope", "_csrf", "consent_action")
                        .containsAll(request.getParameterMap().keySet())) {
            return false;
        }
        for (var entry : request.getParameterMap().entrySet()) {
            String[] values = entry.getValue();
            if (values == null || values.length == 0 || values.length > ("scope".equals(entry.getKey()) ? 64 : 1)
                    || java.util.Arrays.stream(values).anyMatch(value -> value == null || value.length() > 256)) {
                return false;
            }
        }
        return request.getParameter("consent_action") == null || "deny".equals(request.getParameter("consent_action"));
    }

    private static String nativeRedirect(HttpServletRequest request, String location, TrustedRealmBO realm) {
        if (!contextPath(request).equals(realm.getIssuer().getRawPath() + "/oauth2/authorize")) {
            return location;
        }
        URI target = URI.create(location);
        String query = target.getRawQuery();
        boolean callback = query != null && java.util.Arrays.stream(query.split("&"))
                .anyMatch(value -> value.startsWith("code=") || value.startsWith("error="));
        if (callback) {
            return UriComponentsBuilder.fromUriString(location)
                    .queryParam("iss", URLEncoder.encode(realm.getIssuer().toASCIIString(), StandardCharsets.UTF_8))
                    .build(true).toUriString();
        }
        if (target.getRawPath().equals(request.getContextPath() + "/consent") && query != null
                && target.getUserInfo() == null && target.getFragment() == null
                && java.util.Arrays.stream(query.split("&")).anyMatch(value -> value.startsWith("client_id="))) {
            return realm.getIssuer().toASCIIString() + "/consent?" + query;
        }
        return location;
    }

    private static AuthenticationFailureHandler authorizationFailureHandler(ObjectMapper mapper) {
        return (request, response, failure) -> {
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Referrer-Policy", "no-referrer");
            if (failure instanceof OAuth2AuthenticationException oauth
                    && "temporarily_unavailable".equals(oauth.getError().getErrorCode())) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                mapper.writeValue(response.getWriter(), Map.of("error", "temporarily_unavailable", "error_description", "Request temporarily unavailable"));
                return;
            }
            if (failure instanceof OAuth2AuthorizationCodeRequestAuthenticationException oauth
                    && oauth.getAuthorizationCodeRequestAuthentication() != null
                    && oauth.getAuthorizationCodeRequestAuthentication().getRedirectUri() != null) {
                OAuth2AuthorizationCodeRequestAuthenticationToken source = oauth.getAuthorizationCodeRequestAuthentication();
                UriComponentsBuilder redirect = UriComponentsBuilder.fromUriString(source.getRedirectUri())
                        .queryParam("error", URLEncoder.encode(oauth.getError().getErrorCode(), StandardCharsets.UTF_8));
                if (source.getState() != null) {
                    redirect.queryParam("state", URLEncoder.encode(source.getState(), StandardCharsets.UTF_8));
                }
                response.sendRedirect(redirect.build(true).toUriString());
                return;
            }
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write("<!doctype html><html><body><p>Request rejected</p></body></html>");
        };
    }

    private static AuthenticationFailureHandler userInfoFailureHandler(ObjectMapper mapper) {
        return (request, response, failure) -> {
            String code = failure instanceof OAuth2AuthenticationException oauth ? oauth.getError().getErrorCode()
                    : failure instanceof AuthenticationServiceException ? "temporarily_unavailable" : "invalid_token";
            int status = "temporarily_unavailable".equals(code) ? 503 : "insufficient_scope".equals(code) ? 403 : 401;
            response.setStatus(status);
            response.setHeader("Cache-Control", "no-store");
            if (status == 401) { response.setHeader("WWW-Authenticate", "Bearer"); }
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getWriter(), Map.of("error", code, "error_description", "Request rejected"));
        };
    }

    private static AuthenticationFailureHandler userInfoFailureHandler(ObjectMapper mapper, TokenUsageAuditComponent producer,
            TokenUsageAuditEventConverter converter, OAuthUsageObservationProperties properties, URI resource, Clock clock, Validator validator) {
        return (request, response, failure) -> {
            Exception outcome = failure;
            if (properties.isEnabled() && !(failure instanceof TransactionalOAuthAuthenticationProvider.UserInfoUsageUnavailableException)
                    && request.getAttribute("oauth.userinfo.audit.attempted") == null) {
                request.setAttribute("oauth.userinfo.audit.attempted", Boolean.TRUE);
                try {
                    String trace = org.slf4j.MDC.get("traceId");
                    if (trace != null && !trace.matches("[A-Za-z0-9._~-]{1,128}")) { trace = null; }
                    var context = TokenAuditContextBO.builder().eventId(java.util.UUID.randomUUID().toString().replace("-", ""))
                            .fallbackIssuer(properties.getFallbackIssuer()).resource(resource).result("FAILED")
                            .reasonCode("AUTHENTICATION_FAILED").occurredAt(clock.instant()).traceId(trace).build();
                    ValidationUtils schema = new ValidationUtils(validator);
                    producer.appendStrict(schema.validate(converter.toTarget(schema.validate(context))));
                } catch (RuntimeException unavailable) {
                    outcome = new OAuth2AuthenticationException(new OAuth2Error("temporarily_unavailable", "Request temporarily unavailable", null), unavailable);
                }
            }
            userInfoFailureHandler(mapper).onAuthenticationFailure(request, response,
                    outcome instanceof org.springframework.security.core.AuthenticationException authentication ? authentication
                            : new AuthenticationServiceException("Request temporarily unavailable", outcome));
        };
    }

    private static URI validatePublicOrigin(URI origin) {
        if (origin == null || !origin.isAbsolute() || !"https".equalsIgnoreCase(origin.getScheme())
                || origin.getHost() == null || origin.getUserInfo() != null
                || origin.getQuery() != null || origin.getFragment() != null
                || (origin.getRawPath() != null && !origin.getRawPath().isEmpty()
                        && !"/".equals(origin.getRawPath()))) {
            throw new IllegalArgumentException("OAUTH_PUBLIC_ORIGIN_INVALID");
        }
        return origin;
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    @Slf4j
    private static final class ExactIssuerContextFilter extends OncePerRequestFilter {

        @NonNull
        @Qualifier("tenantIssuerService")
        private final TenantIssuerService tenantIssuerService;
        @NonNull
        private final String publicOriginRoot;
        @NonNull
        @Qualifier("jacksonObjectMapper")
        private final ObjectMapper objectMapper;

        private static ExactIssuerContextFilter create(
                TenantIssuerService issuers, URI publicOrigin, ObjectMapper mapper) {
            String origin = publicOrigin.toASCIIString();
            return new ExactIssuerContextFilter(issuers,
                    origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin, mapper);
        }

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain) throws ServletException, IOException {
            AuthorizationServerContext current = AuthorizationServerContextHolder.getContext();
            if (current == null || current.getIssuer() == null) {
                writeError(response, HttpServletResponse.SC_NOT_FOUND, "invalid_request", "Request rejected");
                return;
            }

            TrustedRealmBO trustedRealm;
            try {
                URI requestIssuer = URI.create(current.getIssuer());
                String path = requestIssuer.getRawPath();
                String pagePath = contextPath(request);
                if (pagePath.matches("/tenants/[1-9][0-9]{0,18}/(login|consent)")) {
                    path = pagePath.substring(0, pagePath.lastIndexOf('/'));
                }
                if (path == null || path.isEmpty()) {
                    throw new OAuth2AuthenticationException("invalid_request");
                }
                URI configuredIssuer = URI.create(publicOriginRoot + path);
                trustedRealm = tenantIssuerService.resolveExactIssuer(configuredIssuer);
            } catch (OAuth2AuthenticationException | IllegalArgumentException failure) {
                writeError(response, HttpServletResponse.SC_NOT_FOUND, "invalid_request", "Request rejected");
                return;
            } catch (RuntimeException failure) {
                writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                        "temporarily_unavailable", "Request temporarily unavailable");
                return;
            }

            response.setHeader("Cache-Control", isPublicMetadataRequest(request, trustedRealm)
                    ? "public, max-age=60" : "no-store");

            AuthorizationServerContext resolvedContext = new AuthorizationServerContext() {
                @Override
                public String getIssuer() {
                    return trustedRealm.getIssuer().toASCIIString();
                }

                @Override
                public AuthorizationServerSettings getAuthorizationServerSettings() {
                    return current.getAuthorizationServerSettings();
                }
            };
            AuthorizationServerContextHolder.setContext(resolvedContext);
            try {
                HttpServletResponse forwarded = new HttpServletResponseWrapper(response) {
                    @Override
                    public void sendRedirect(String location) throws IOException {
                        super.sendRedirect(nativeRedirect(request, location, trustedRealm));
                    }
                };
                if (isConsentSubmit(request) && !boundedConsentForm(request)) {
                    writeError(response, HttpServletResponse.SC_BAD_REQUEST, "invalid_request", "Request rejected");
                    return;
                }
                if (isAuthorizationRequest(request) && !boundedAuthorizationQuery(request)) {
                    writeError(response, HttpServletResponse.SC_BAD_REQUEST, "invalid_request", "Request rejected");
                    return;
                }
                filterChain.doFilter(request, forwarded);
            } finally {
                AuthorizationServerContextHolder.setContext(current);
            }
        }

        private void writeError(
                HttpServletResponse response,
                int status,
                String error,
                String description) throws IOException {
            response.setStatus(status);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Cache-Control", "no-store");
            objectMapper.writeValue(response.getWriter(), Map.of("error", error, "error_description", description));
        }

        private static boolean isPublicMetadataRequest(HttpServletRequest request, TrustedRealmBO trustedRealm) {
            if (!"GET".equals(request.getMethod())) {
                return false;
            }
            String path = request.getRequestURI();
            String contextPath = request.getContextPath();
            if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
                path = path.substring(contextPath.length());
            }
            String issuerPath = trustedRealm.getIssuer().getRawPath();
            return path.equals(issuerPath + "/.well-known/openid-configuration")
                    || path.equals(issuerPath + "/.well-known/jwks.json")
                    || path.equals("/.well-known/oauth-authorization-server" + issuerPath);
        }
    }
}
