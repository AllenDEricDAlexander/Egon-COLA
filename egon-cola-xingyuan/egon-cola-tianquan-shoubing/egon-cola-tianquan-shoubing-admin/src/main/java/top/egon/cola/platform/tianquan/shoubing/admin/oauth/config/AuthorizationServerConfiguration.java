package top.egon.cola.platform.tianquan.shoubing.admin.oauth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.time.Instant;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationValidator;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientCredentialsAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientCredentialsAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2RefreshTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2TokenRevocationAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2ErrorAuthenticationFailureHandler;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.server.authorization.authentication.ClientSecretAuthenticationProvider;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.filter.OncePerRequestFilter;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.jwk.source.JWKSource;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthAuthorizationBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthAuthorizationAttributesBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthSecuritySnapshotBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.SasAuthorizationConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.OAuthTokenIssueService;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.TenantIssuerService;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.TransactionalOAuthAuthenticationProvider;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.TransactionalOAuthRevocationAuthenticationProvider;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller.filter.TenantLoginAuthenticationFilter;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.TokenRevocationService;

@Configuration(proxyBeanMethods = false)
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
            @Qualifier("tenantIssuerService") TenantIssuerService tenantIssuerService) {
        JwtDecoder delegate = org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration
                .OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
        return token -> {
            Jwt jwt = delegate.decode(token);
            TrustedRealmBO trustedRealm = tenantIssuerService.resolveCurrentIssuer();
            if (!trustedRealm.getIssuer().equals(jwt.getIssuer())) {
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
        return new DelegatingOAuth2TokenGenerator(
                jwtGenerator,
                new OAuth2AccessTokenGenerator(),
                new OAuth2RefreshTokenGenerator());
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
            @Qualifier("oauthLoginCsrfTokenRepository") CsrfTokenRepository csrfTokenRepository,
            @Qualifier("jacksonObjectMapper") ObjectMapper objectMapper) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServer =
                OAuth2AuthorizationServerConfigurer.authorizationServer();
        RequestMatcher endpoints = authorizationServer.getEndpointsMatcher();
        RequestMatcher loginRequests = request -> request.getRequestURI().substring(
                request.getContextPath() == null ? 0 : request.getContextPath().length())
                .matches("/tenants/[1-9][0-9]{0,18}/login");
        RequestMatcher canonicalIssuerPaths = request -> request.getRequestURI().substring(
                request.getContextPath().length()).matches("/(platform|tenants/[1-9][0-9]{0,18})/.*"
                        + "|/\\.well-known/oauth-authorization-server/(platform|tenants/[1-9][0-9]{0,18})");
        RequestMatcher protocolAndLogin = new AndRequestMatcher(canonicalIssuerPaths,
                new OrRequestMatcher(endpoints, loginRequests));
        OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator = tokenGenerators.getIfAvailable();
        if (tokenGenerator == null) {
            throw new IllegalStateException("OAUTH_TOKEN_GENERATOR_REQUIRED");
        }
        http.securityMatcher(protocolAndLogin)
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository).ignoringRequestMatchers(endpoints))
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
                        .authorizationEndpoint(endpoint -> endpoint.authenticationProviders(providers ->
                                configureAuthorizationProviders(providers)))
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
                        .oidc(oidc -> oidc.providerConfigurationEndpoint(metadata -> metadata
                                .providerConfigurationCustomizer(builder -> {
                                    builder.tokenEndpointAuthenticationMethods(AuthorizationServerConfiguration::metadataAuthenticationMethods);
                                    builder.tokenRevocationEndpointAuthenticationMethods(AuthorizationServerConfiguration::metadataAuthenticationMethods);
                                    builder.grantTypes(AuthorizationServerConfiguration::metadataGrantTypes);
                                }))))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(loginRequests).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception.authenticationEntryPoint((request, response, failure) -> {
                    TrustedRealmBO trustedRealm = tenantIssuerService.resolveCurrentIssuer();
                    response.setStatus(HttpServletResponse.SC_SEE_OTHER);
                    response.setHeader("Cache-Control", "no-store");
                    response.setHeader("Location", trustedRealm.getIssuer().getRawPath() + "/login");
                }));

        // SAS installs its protocol endpoint filters at this anchor after the issuer context filter.
        http.addFilterBefore(new ExactIssuerContextFilter(tenantIssuerService, publicOrigin, objectMapper),
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

    private static void configureAuthorizationProviders(List<AuthenticationProvider> providers) {
        OAuth2AuthorizationCodeRequestAuthenticationValidator standardValidator =
                new OAuth2AuthorizationCodeRequestAuthenticationValidator();
        for (AuthenticationProvider provider : providers) {
            if (provider instanceof OAuth2AuthorizationCodeRequestAuthenticationProvider codeProvider) {
                codeProvider.setAuthenticationValidator(context -> {
                    standardValidator.accept(context);
                    String method = text(context.getAuthorizationRequest()
                            .getAdditionalParameters().get("code_challenge_method"));
                    String challenge = text(context.getAuthorizationRequest()
                            .getAdditionalParameters().get("code_challenge"));
                    requireS256Challenge(method, challenge);
                });
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
                    .claim("scope", context.getAuthorizedScopes())
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
            context.getClaims().claim("aud", List.of(registeredClient.getClientId()));
            if (attributes.getNonce() != null) {
                context.getClaims().claim("nonce", attributes.getNonce());
            }
            if (attributes.getAuthTime() != null) {
                context.getClaims().claim("auth_time", attributes.getAuthTime());
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
                    .claim("scope", context.getAuthorizedScopes())
                    .claim("jti", SnowflakeIdGenerator.nextId())
                    .claim("client_id", registeredClient.getClientId())
                    .claim("authorization_id", Long.toString(issue.authorizationId()))
                    .claim("client_registration_id", registeredClient.getId())
                    .claim("principal_type", "USER")
                    .claim("scope_context", trustedRealm.getBusinessTenantId() == null ? "PLATFORM" : "TENANT")
                    .claim("security_version", securityVersion(issue.securitySnapshot()));
            if (trustedRealm.getBusinessTenantId() != null) {
                context.getClaims().claim("tenant_id", trustedRealm.getBusinessTenantId().toString());
            }
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

    private static final class ExactIssuerContextFilter extends OncePerRequestFilter {

        private final TenantIssuerService tenantIssuerService;
        private final String publicOriginRoot;
        private final ObjectMapper objectMapper;

        private ExactIssuerContextFilter(TenantIssuerService tenantIssuerService, URI publicOrigin, ObjectMapper objectMapper) {
            this.tenantIssuerService = tenantIssuerService;
            this.objectMapper = objectMapper;
            String origin = publicOrigin.toASCIIString();
            this.publicOriginRoot = origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin;
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
                filterChain.doFilter(request, response);
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
