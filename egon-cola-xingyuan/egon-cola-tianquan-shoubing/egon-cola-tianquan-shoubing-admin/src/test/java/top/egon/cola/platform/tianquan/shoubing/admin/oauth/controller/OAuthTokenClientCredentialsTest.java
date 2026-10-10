package top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.config.AuthorizationServerConfiguration;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller.filter.LegacyOAuthRetirementFilter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthSecuritySnapshotBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.SasRegisteredClientConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.OAuthClientPurposeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.OAuthTokenIssueService;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.TenantIssuerService;
import top.egon.cola.platform.tianquan.shoubing.admin.support.security.IdpSecurityConfig;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.TokenRevocationService;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ClientCredentialsAccessPolicy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OAuthTokenClientCredentialsTest {

    private static final URI ISSUER = URI.create("https://idp.example/platform");
    private static final URI RESOURCE = URI.create("https://api.example/orders");
    private static final TrustedRealmBO REALM = TrustedRealmBO.builder().issuer(ISSUER).realmKey("platform")
            .realmType(RealmTypeEnum.PLATFORM).storagePartitionId(1L).trustVersion(0L).build();
    private static final TrustedRealmBO TENANT_REALM = TrustedRealmBO.builder()
            .issuer(URI.create("https://idp.example/tenants/1001")).realmKey("tenant:1001")
            .realmType(RealmTypeEnum.TENANT).storagePartitionId(1001L).businessTenantId(1001L).trustVersion(0L).build();
    private static RSAKey signingKey;
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mockMvc;

    @BeforeAll
    static void initializeLocalSigningFixture() throws Exception {
        SnowflakeIdGenerator.initialize(0L, Duration.ofMillis(5));
        signingKey = new RSAKeyGenerator(3072).keyID("test-key").algorithm(JWSAlgorithm.RS256).generate();
    }

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test",
                Map.of("egon.tianquan-shoubing.oauth.public-origin", "https://idp.example")));
        context.register(SecurityConfiguration.class);
        context.refresh();
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach
    void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void actualSasBasicProviderAcceptsTheStoredBcryptHashAndIssuesOnlyServiceAccessToken() throws Exception {
        var result = mockMvc.perform(post("/platform/oauth2/token")
                        .header("Authorization", basic("orders-service", "test-client-secret"))
                        .contentType("application/x-www-form-urlencoded")
                        .param("grant_type", "client_credentials").param("resource", RESOURCE.toString())
                        .param("scope", "orders:read"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.scope").value("orders:read"))
                .andExpect(jsonPath("$.refresh_token").doesNotExist())
                .andExpect(jsonPath("$.id_token").doesNotExist())
                .andExpect(header().doesNotExist("Set-Cookie")).andReturn();
        String token = new ObjectMapper().readTree(result.getResponse().getContentAsString())
                .path("access_token").asText();
        var signed = com.nimbusds.jwt.SignedJWT.parse(token);
        assertTrue(signed.verify(new com.nimbusds.jose.crypto.RSASSAVerifier(signingKey.toRSAPublicKey())));
        assertEquals(ISSUER.toString(), signed.getJWTClaimsSet().getIssuer());
        assertEquals("orders:read", signed.getJWTClaimsSet().getClaim("scope"));
        assertEquals("SERVICE", signed.getJWTClaimsSet().getStringClaim("principal_type"));
        assertEquals("PLATFORM", signed.getJWTClaimsSet().getStringClaim("scope_context"));
        assertEquals(java.util.List.of(RESOURCE.toString()), signed.getJWTClaimsSet().getAudience());
        assertNull(signed.getJWTClaimsSet().getClaim("tenant_id"));
        var saved = org.mockito.ArgumentCaptor.forClass(
                org.springframework.security.oauth2.server.authorization.OAuth2Authorization.class);
        verify(context.getBean(org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService.class))
                .save(saved.capture());
        var converter = (top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.SasAuthorizationConverter)
                Class.forName("top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.SasAuthorizationConverterImpl")
                        .getDeclaredConstructor().newInstance();
        var mapper = new ObjectMapper().findAndRegisterModules();
        var projection = new java.util.concurrent.atomic.AtomicReference<
                top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthAuthorizationBO>();
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                projection.set(converter.toSource(saved.getValue(), null, mapper)));
        assertEquals(java.util.Set.of("orders:read"), projection.get().getTokens().getFirst().getMetadata().getClaims().getScope());
        assertEquals(signed.getJWTClaimsSet().getStringClaim("app_id"),
                projection.get().getTokens().getFirst().getMetadata().getClaims().getAppId());

        verify(context.getBean(OAuthTokenIssueService.class)).recordClientCredentialsIssued(REALM, 500L, RESOURCE);
    }

    @Test
    void actualSasTenantServiceTokenCarriesItsExactBusinessTenant() throws Exception {
        var result = mockMvc.perform(post("/tenants/1001/oauth2/token")
                        .header("Authorization", basic("orders-service", "test-client-secret"))
                        .contentType("application/x-www-form-urlencoded")
                        .param("grant_type", "client_credentials").param("resource", RESOURCE.toString())
                        .param("scope", "orders:read"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.refresh_token").doesNotExist()).andReturn();
        var response = new ObjectMapper().readTree(result.getResponse().getContentAsString());
        var jwt = com.nimbusds.jwt.SignedJWT.parse(response.path("access_token").asText());
        assertTrue(jwt.verify(new com.nimbusds.jose.crypto.RSASSAVerifier(signingKey.toRSAPublicKey())));
        assertEquals(TENANT_REALM.getIssuer().toString(), jwt.getJWTClaimsSet().getIssuer());
        assertEquals("orders:read", jwt.getJWTClaimsSet().getClaim("scope"));
        assertEquals("TENANT", jwt.getJWTClaimsSet().getStringClaim("scope_context"));
        assertEquals("1001", String.valueOf(jwt.getJWTClaimsSet().getClaim("tenant_id")));
        verify(context.getBean(OAuthTokenIssueService.class))
                .recordClientCredentialsIssued(TENANT_REALM, 500L, RESOURCE);
    }

    @Test
    void actualSasClientAuthenticationRejectsMalformedDuplicateWrongAndBodyCredentials() throws Exception {
        for (String header : new String[]{"Basic %not-base64", basic("orders-service", "wrong-secret"),
                basic("unknown-client", "test-client-secret")}) {
            mockMvc.perform(post("/platform/oauth2/token").header("Authorization", header)
                            .contentType("application/x-www-form-urlencoded")
                            .param("grant_type", "client_credentials").param("resource", RESOURCE.toString()))
                    .andExpect(status().is4xxClientError())
                    .andExpect(jsonPath("$.access_token").doesNotExist())
                    .andExpect(content().string(org.hamcrest.Matchers.not(
                            org.hamcrest.Matchers.containsString("test-client-secret"))));
        }
        mockMvc.perform(post("/platform/oauth2/token")
                        .header("Authorization", basic("orders-service", "test-client-secret"),
                                basic("orders-service", "wrong-secret"))
                        .contentType("application/x-www-form-urlencoded")
                        .param("grant_type", "client_credentials").param("resource", RESOURCE.toString()))
                .andExpect(status().is4xxClientError()).andExpect(jsonPath("$.access_token").doesNotExist());
        mockMvc.perform(post("/platform/oauth2/token")
                        .header("Authorization", basic("orders-service", "test-client-secret"))
                        .contentType("application/x-www-form-urlencoded")
                        .param("grant_type", "client_credentials").param("resource", RESOURCE.toString())
                        .param("client_secret", "test-client-secret"))
                .andExpect(status().is4xxClientError()).andExpect(jsonPath("$.access_token").doesNotExist());
        verifyNoInteractions(context.getBean(OAuthTokenIssueService.class));
    }

    @Test
    void rootTokenRouteCannotReachSasIssuanceWithAnyCredentials() throws Exception {
        mockMvc.perform(post("/oauth2/token").header("Authorization", "Basic invalid")
                        .contentType("application/x-www-form-urlencoded").param("grant_type", "client_credentials")
                        .param("client_secret", "test-client-secret"))
                .andExpect(status().isGone()).andExpect(jsonPath("$.error").value("legacy_endpoint_retired"))
                .andExpect(jsonPath("$.access_token").doesNotExist()).andExpect(header().doesNotExist("Set-Cookie"));
        verifyNoInteractions(context.getBean(OAuthTokenIssueService.class));
    }

    private static String basic(String clientId, String secret) {
        return "Basic " + Base64.getEncoder().encodeToString((clientId + ":" + secret)
                .getBytes(StandardCharsets.UTF_8));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebSecurity
    @EnableWebMvc
    @Import({AuthorizationServerConfiguration.class, IdpSecurityConfig.class, LegacyOAuthRetirementFilter.class,
            org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class})
    static class SecurityConfiguration {

        @Bean("idpClock")
        java.time.Clock idpClock() {
            return java.time.Clock.systemUTC();
        }

        @Bean("jacksonObjectMapper")
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean("tenantRegisteredClientRepository")
        RegisteredClientRepository registeredClientRepository() {
            OAuthClientBO client = OAuthClientBO.builder().id(500L).version(0L).clientId("orders-service")
                    .clientName("Orders Service").clientType(OAuthClient.ClientType.CONFIDENTIAL)
                    .purpose(OAuthClientPurposeEnum.SERVICE).status(OAuthClient.Status.ACTIVE)
                    .secretHash(new BCryptPasswordEncoder(12).encode("test-client-secret"))
                    .credentialVersion(1L).allowedScopes(java.util.List.of("orders:read"))
                    .redirectUris(java.util.List.of()).resourceUris(java.util.List.of())
                    .accessTtlSeconds(300).refreshTtlSeconds(0).build();
            return new InMemoryRegisteredClientRepository(Mappers.getMapper(SasRegisteredClientConverter.class)
                    .toTarget(client));
        }

        @Bean("tenantAuthorizationService")
        OAuth2AuthorizationService authorizationService() {
            return mock(OAuth2AuthorizationService.class);
        }

        @Bean("tenantConsentService")
        OAuth2AuthorizationConsentService consentService() {
            return mock(OAuth2AuthorizationConsentService.class);
        }

        @Bean("tenantIssuerService")
        TenantIssuerService issuerService() {
            TenantIssuerService service = mock(TenantIssuerService.class);
            when(service.resolveExactIssuer(any())).thenAnswer(invocation -> {
                URI issuer = invocation.getArgument(0);
                if (ISSUER.equals(issuer)) {
                    return REALM;
                }
                if (TENANT_REALM.getIssuer().equals(issuer)) {
                    return TENANT_REALM;
                }
                throw new OAuth2AuthenticationException("invalid_request");
            });
            when(service.resolveCurrentIssuer()).thenAnswer(invocation -> service.resolveExactIssuer(URI.create(
                    org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder
                            .getContext().getIssuer())));
            return service;
        }

        @Bean("tenantJwkSourceComponent")
        JWKSource<SecurityContext> jwkSource() {
            return (selector, securityContext) -> selector.select(new JWKSet(signingKey));
        }

        @Bean("oAuthRealmRepository")
        OAuthRealmRepository realmRepository() {
            OAuthRealmRepository repository = mock(OAuthRealmRepository.class);
            when(repository.findByIssuer(any(TrustedRealmBO.class))).thenReturn(Optional.of(OAuthRealmBO.builder()
                    .status("ACTIVE").activeKid("test-key").build()));
            return repository;
        }

        @Bean("oauthTokenIssueService")
        OAuthTokenIssueService tokenIssueService() {
            OAuthTokenIssueService service = mock(OAuthTokenIssueService.class);
            var access = new ClientCredentialsAccessPolicy.ServiceResourceAccess("orders-service", "source-resource",
                    "business", "orders", "prod", "orders-resource", RESOURCE, 1L, null,
                    Set.of("orders:read"), 1L, ServiceTokenContext.PLATFORM);
            when(service.authorizeClientCredentials(eq(REALM), eq(500L), eq(RESOURCE), anySet()))
                    .thenReturn(new OAuthTokenIssueService.ClientCredentialsIssueContext(1002L,
                            OAuthSecuritySnapshotBO.builder().realmGeneration(0L).clientGeneration(0L)
                                    .readyEpoch("epoch").revision(0L).build(), access, "orders", "orders-service"));
            var tenantAccess = new ClientCredentialsAccessPolicy.ServiceResourceAccess("orders-service", "source-resource",
                    "business", "orders", "prod", "orders-resource", RESOURCE, 1L, "1001",
                    Set.of("orders:read"), 1L, ServiceTokenContext.TENANT);
            when(service.authorizeClientCredentials(eq(TENANT_REALM), eq(500L), eq(RESOURCE), anySet()))
                    .thenReturn(new OAuthTokenIssueService.ClientCredentialsIssueContext(1003L,
                            OAuthSecuritySnapshotBO.builder().realmGeneration(0L).clientGeneration(0L)
                                    .readyEpoch("epoch").revision(0L).build(), tenantAccess, "orders", "orders-service"));
            return service;
        }

        @Bean("tokenRevocationService")
        TokenRevocationService revocationService() {
            return mock(TokenRevocationService.class);
        }

        @Bean("transactionManager")
        PlatformTransactionManager transactionManager() {
            PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
            when(manager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
            return manager;
        }
    }
}
