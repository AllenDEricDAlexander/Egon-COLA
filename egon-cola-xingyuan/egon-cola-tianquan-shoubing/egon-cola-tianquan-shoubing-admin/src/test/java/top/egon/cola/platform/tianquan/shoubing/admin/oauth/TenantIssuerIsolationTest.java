package top.egon.cola.platform.tianquan.shoubing.admin.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import top.egon.cola.component.common.crypto.digest.Digests;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.repo.IdentityDirectoryContext;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.config.AuthorizationServerConfiguration;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.config.OAuthProtocolOpenApiConfiguration;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller.TenantLoginController;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthAuthorizationAttributesBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthAuthorizationBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthConsentBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthSecuritySnapshotBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.SasAuthorizationConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.SasConsentConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.SasRegisteredClientConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.OAuthClientPurposeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthAuthorizationRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthClientRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthConsentRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.TenantRegisteredClientRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.TenantIssuerService;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.OAuthTokenIssueService;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.TenantAuthorizationServiceImpl;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.TenantConsentServiceImpl;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.TenantIssuerServiceImpl;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthSigningKeyBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthTokenBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthTokenMetadataBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.SignedTokenClaimsBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthKeyStatusEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthSigningKeyRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthTokenRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthKeyCipherComponent;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthSecurityProjectionComponent;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.TenantJwkSourceComponent;
import top.egon.cola.platform.tianquan.shoubing.contract.OAuthSecurityVersionBO;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        TenantIssuerIsolationTest.TestWebConfiguration.class,
        AuthorizationServerConfiguration.class,
        OAuthProtocolOpenApiConfiguration.class,
        TenantLoginController.class
})
@WebAppConfiguration
@TestPropertySource(properties = "egon.tianquan-shoubing.oauth.public-origin=https://idp.example.test")
class TenantIssuerIsolationTest {

    private static final String PUBLIC_ORIGIN_KEY = "egon.tianquan-shoubing.oauth.public-origin";
    private static final String PUBLIC_ORIGIN_PLACEHOLDER = "${EGON_TIANQUAN_SHOUBING_OAUTH_PUBLIC_ORIGIN}";
    private static final String PLATFORM_MANAGEMENT_TENANT_KEY =
            "egon.tianquan-shoubing.oauth.platform-management-tenant-id";
    private static final String PLATFORM_MANAGEMENT_TENANT_PLACEHOLDER =
            "${EGON_TIANQUAN_SHOUBING_PLATFORM_MANAGEMENT_TENANT_ID}";
    private static final String REVOCATION_EPOCH_KEY =
            "egon.cola.platform.tianquan.shoubing.revocation.epoch";
    private static final String REVOCATION_EPOCH_PLACEHOLDER = "${EGON_TIANQUAN_SHOUBING_REVOCATION_EPOCH}";
    private static final URI PUBLIC_ORIGIN = URI.create("https://idp.example.test");
    private static final String SHARED_CLIENT_ID = "shared-client";
    private static final String SHARED_KID = "shared-signing-key";
    private static final Instant NOW = Instant.parse("2026-10-08T08:00:00Z");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private static KeyPair tenantOneKeyPair;
    private static KeyPair tenantTwoKeyPair;

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private AuthorizationServerSettings authorizationServerSettings;
    @Autowired
    @Qualifier("tenantIssuerService")
    private TenantIssuerService requestIssuerService;
    @Autowired
    @Qualifier("tenantRegisteredClientRepository")
    private RegisteredClientRepository requestClientRepository;
    @Autowired
    @Qualifier("tenantAuthorizationService")
    private OAuth2AuthorizationService requestAuthorizationService;
    @Autowired
    @Qualifier("tenantConsentService")
    private OAuth2AuthorizationConsentService requestConsentService;
    @Autowired
    @Qualifier("oauthProtocolOpenApiCustomizer")
    private OpenApiCustomizer openApiCustomizer;

    private MockMvc mockMvc;

    @BeforeAll
    static void generateIssuerKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(3072);
        tenantOneKeyPair = generator.generateKeyPair();
        tenantTwoKeyPair = generator.generateKeyPair();
    }

    @BeforeEach
    void configureRequestIssuerRegistry() {
        AuthorizationServerContextHolder.resetContext();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        reset(requestIssuerService, requestClientRepository, requestAuthorizationService, requestConsentService);
        when(requestIssuerService.resolveExactIssuer(any(URI.class))).thenAnswer(invocation -> {
            URI issuer = invocation.getArgument(0);
            return switch (issuer.getRawPath()) {
                case "/tenants/1001" -> trustedTenant(1001L, issuer);
                case "/tenants/1002" -> trustedTenant(1002L, issuer);
                case "/platform" -> trustedPlatform(issuer);
                default -> throw new OAuth2AuthenticationException(
                        new OAuth2Error("invalid_request", "Request rejected", null));
            };
        });
        when(requestClientRepository.findByClientId(anyString())).thenReturn(null);
    }

    @AfterEach
    void clearAuthorizationServerContext() {
        AuthorizationServerContextHolder.resetContext();
    }

    @Test
    void requiresTheSamePublicOriginPlaceholderInBothProfiles() throws Exception {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        String baseOrigin = (String) loader.load("base", new ClassPathResource("application.yml"))
                .getFirst().getProperty(PUBLIC_ORIGIN_KEY);
        String localOrigin = (String) loader.load("local", new ClassPathResource("application-local.yml"))
                .getFirst().getProperty(PUBLIC_ORIGIN_KEY);
        String baseManagementTenant = (String) loader.load("base", new ClassPathResource("application.yml"))
                .getFirst().getProperty(PLATFORM_MANAGEMENT_TENANT_KEY);
        String localManagementTenant = (String) loader.load("local", new ClassPathResource("application-local.yml"))
                .getFirst().getProperty(PLATFORM_MANAGEMENT_TENANT_KEY);
        String baseEpoch = (String) loader.load("base", new ClassPathResource("application.yml"))
                .getFirst().getProperty(REVOCATION_EPOCH_KEY);
        String localEpoch = (String) loader.load("local", new ClassPathResource("application-local.yml"))
                .getFirst().getProperty(REVOCATION_EPOCH_KEY);

        assertEquals(PUBLIC_ORIGIN_PLACEHOLDER, baseOrigin);
        assertEquals(PUBLIC_ORIGIN_PLACEHOLDER, localOrigin);
        assertEquals(PLATFORM_MANAGEMENT_TENANT_PLACEHOLDER, baseManagementTenant);
        assertEquals(PLATFORM_MANAGEMENT_TENANT_PLACEHOLDER, localManagementTenant);
        assertEquals(REVOCATION_EPOCH_PLACEHOLDER, baseEpoch);
        assertEquals(REVOCATION_EPOCH_PLACEHOLDER, localEpoch);
        assertTrue(authorizationServerSettings.isMultipleIssuersAllowed());
        assertNull(authorizationServerSettings.getIssuer());
    }

    @Test
    void servesDiscoveryAndJwksPerTenantAndKeepsTokenErrorsOnTheSasRoute() throws Exception {
        MvcResult discovery = mockMvc.perform(tenantRequest(
                        get("/tenants/1001/.well-known/openid-configuration"), 1001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value("https://idp.example.test/tenants/1001"))
                .andExpect(jsonPath("$.jwks_uri").value("https://idp.example.test/tenants/1001/.well-known/jwks.json"))
                .andReturn();
        assertEquals("public, max-age=60", discovery.getResponse().getHeader("Cache-Control"));
        assertTrue(discovery.getResponse().getContentAsString().contains("openid"));

        MvcResult firstJwksResponse = mockMvc.perform(tenantRequest(get("/tenants/1001/.well-known/jwks.json"), 1001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kid").value(SHARED_KID))
                .andReturn();
        MvcResult secondJwksResponse = mockMvc.perform(tenantRequest(get("/tenants/1002/.well-known/jwks.json"), 1002L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kid").value(SHARED_KID))
                .andReturn();
        String firstJwks = firstJwksResponse.getResponse().getContentAsString();
        String secondJwks = secondJwksResponse.getResponse().getContentAsString();
        assertEquals("public, max-age=60", firstJwksResponse.getResponse().getHeader("Cache-Control"));
        assertEquals("public, max-age=60", secondJwksResponse.getResponse().getHeader("Cache-Control"));
        assertNotEquals(OBJECT_MAPPER.readTree(firstJwks).at("/keys/0/n"),
                OBJECT_MAPPER.readTree(secondJwks).at("/keys/0/n"));
        assertFalse(firstJwks.contains("\"d\""));
        assertFalse(secondJwks.contains("\"d\""));

        mockMvc.perform(tenantRequest(get("/platform/.well-known/openid-configuration"), 9000L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value("https://idp.example.test/platform"));

        for (String issuerPath : List.of("/tenants/1001", "/platform")) {
            boolean platform = issuerPath.equals("/platform");
            MvcResult metadata = mockMvc.perform(tenantRequest(
                            get("/.well-known/oauth-authorization-server" + issuerPath), platform ? 9000L : 1001L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.issuer").value("https://idp.example.test" + issuerPath))
                    .andExpect(jsonPath("$.jwks_uri").value("https://idp.example.test" + issuerPath + "/.well-known/jwks.json"))
                    .andReturn();
            assertEquals("public, max-age=60", metadata.getResponse().getHeader("Cache-Control"));
            JsonNode claims = OBJECT_MAPPER.readTree(metadata.getResponse().getContentAsString());
            assertEquals(platform ? "[\"client_secret_basic\"]" : "[\"client_secret_basic\",\"none\"]",
                    claims.get("token_endpoint_auth_methods_supported").toString());
            assertEquals(platform ? "[\"client_credentials\"]" : "[\"authorization_code\",\"refresh_token\",\"client_credentials\"]",
                    claims.get("grant_types_supported").toString());
        }

        mockMvc.perform(tenantRequest(post("/tenants/1001/oauth2/token")
                        .contentType("application/x-www-form-urlencoded")
                        .param("grant_type", "client_credentials")
                        .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                                "missing-client:invalid-secret".getBytes(StandardCharsets.UTF_8))), 1001L))
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.error").value("invalid_client"));

        mockMvc.perform(tenantRequest(get("/.well-known/openid-configuration"), 1001L))
                .andExpect(status().is4xxClientError());
        verify(requestIssuerService, atLeastOnce())
                .resolveExactIssuer(URI.create("https://idp.example.test/tenants/1001"));
        verify(requestIssuerService, atLeastOnce())
                .resolveExactIssuer(URI.create("https://idp.example.test/tenants/1002"));

        String apiDocs = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode discoveryOperation = OBJECT_MAPPER.readTree(apiDocs)
                .at("/paths/~1tenants~1{tenantId}~1.well-known~1openid-configuration/get");
        assertEquals("oauthTenantOidcDiscovery", discoveryOperation.path("operationId").asText());
        JsonNode loginPath = OBJECT_MAPPER.readTree(apiDocs).at("/paths/~1tenants~1{tenantId}~1login");
        assertEquals("oauthSasLoginPage", loginPath.path("get").path("operationId").asText());
        assertEquals("oauthSasLoginSubmit", loginPath.path("post").path("operationId").asText());
        assertTrue(loginPath.path("post").path("requestBody").path("required").asBoolean());
        assertTrue(loginPath.path("post").path("requestBody").path("content")
                .has("application/x-www-form-urlencoded"));
    }

    @Test
    void tenantLoginSubmitRemainsInsideTheIssuerSessionCsrfChain() throws Exception {
        mockMvc.perform(tenantRequest(post("/tenants/1001/login")
                        .contentType("application/x-www-form-urlencoded")
                        .param("username", "alice")
                        .param("password", "synthetic-password"), 1001L))
                .andExpect(status().isForbidden());

        mockMvc.perform(tenantRequest(post("/tenants/1001/login")
                        .with(csrf())
                        .contentType("application/x-www-form-urlencoded")
                        .param("username", "alice")
                        .param("password", "synthetic-password"), 1001L))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void unknownIssuerReturnsNotFoundBeforeProtocolEndpointHandling() throws Exception {
        mockMvc.perform(tenantRequest(get("/tenants/9999/.well-known/openid-configuration"), 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("invalid_request"));
    }

    @Test
    void exactIssuerRegistryRejectsHostSuffixAndDisabledRealmWithoutGuessing() {
        OAuthRealmRepository realmRepository = realmRepository("ACTIVE");
        TenantIssuerServiceImpl service = issuerService(realmRepository);

        TrustedRealmBO tenantOne = service.resolveExactIssuer(URI.create(
                "https://idp.example.test/tenants/1001"));
        TrustedRealmBO tenantTwo = service.resolveExactIssuer(URI.create(
                "https://idp.example.test/tenants/1002"));
        TrustedRealmBO platform = service.resolveExactIssuer(URI.create("https://idp.example.test/platform"));
        assertEquals(1001L, tenantOne.getStoragePartitionId());
        assertEquals(1002L, tenantTwo.getStoragePartitionId());
        assertEquals(9000L, platform.getStoragePartitionId());
        assertNull(platform.getBusinessTenantId());
        assertEquals(RealmTypeEnum.PLATFORM, platform.getRealmType());
        assertNotEquals(tenantOne.getIssuer(), tenantTwo.getIssuer());

        OAuthRealmRepository untouchedRepository = mock(OAuthRealmRepository.class);
        TenantIssuerServiceImpl originGuard = issuerService(untouchedRepository);
        assertThrows(OAuth2AuthenticationException.class,
                () -> originGuard.resolveExactIssuer(URI.create("https://idp.example.test.evil/tenants/1001")));
        assertThrows(OAuth2AuthenticationException.class,
                () -> originGuard.resolveExactIssuer(URI.create("https://idp.example.test/tenants/01001")));
        verify(untouchedRepository, never()).findByIssuer(any(TrustedRealmBO.class));

        TenantIssuerServiceImpl disabled = issuerService(realmRepository("SUSPENDED"));
        assertThrows(OAuth2AuthenticationException.class,
                () -> disabled.resolveExactIssuer(URI.create("https://idp.example.test/tenants/1001")));
    }

    @Test
    void registeredClientLookupSeparatesSameClientIdAcrossIssuerPartitions() {
        OAuthRealmRepository realmRepository = realmRepository("ACTIVE");
        TenantIssuerService issuerService = issuerService(realmRepository);
        OAuthClientRepository clients = mock(OAuthClientRepository.class);
        OAuthClientBO tenantOneClient = client(501L, "{bcrypt}tenant-one-hash", "orders.read");
        OAuthClientBO tenantTwoClient = client(502L, "{bcrypt}tenant-two-hash", "billing.read");
        when(clients.findById(any(TrustedRealmBO.class), eq(501L))).thenReturn(Optional.of(tenantOneClient));
        when(clients.findById(any(TrustedRealmBO.class), eq(502L))).thenReturn(Optional.of(tenantTwoClient));
        when(clients.findByClientId(any(TrustedRealmBO.class), eq(SHARED_CLIENT_ID))).thenAnswer(invocation -> {
            TrustedRealmBO realm = invocation.getArgument(0);
            return Optional.of(realm.getStoragePartitionId().equals(1001L) ? tenantOneClient : tenantTwoClient);
        });
        SasRegisteredClientConverter converter = org.mapstruct.factory.Mappers.getMapper(
                SasRegisteredClientConverter.class);
        TenantRegisteredClientRepository repository =
                new TenantRegisteredClientRepository(issuerService, clients, converter);
        OAuthClientRepository untouchedClients = mock(OAuthClientRepository.class);
        TenantRegisteredClientRepository noContextRepository =
                new TenantRegisteredClientRepository(issuerService, untouchedClients, converter);
        assertThrows(OAuth2AuthenticationException.class,
                () -> noContextRepository.findByClientId(SHARED_CLIENT_ID));
        verify(untouchedClients, never()).findByClientId(any(TrustedRealmBO.class), anyString());

        RegisteredClient first = withIssuer("https://idp.example.test/tenants/1001",
                () -> repository.findByClientId(SHARED_CLIENT_ID));
        RegisteredClient second = withIssuer("https://idp.example.test/tenants/1002",
                () -> repository.findByClientId(SHARED_CLIENT_ID));
        RegisteredClient firstById = withIssuer("https://idp.example.test/tenants/1001",
                () -> repository.findById("501"));
        RegisteredClient secondById = withIssuer("https://idp.example.test/tenants/1002",
                () -> repository.findById("502"));

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(SHARED_CLIENT_ID, first.getClientId());
        assertEquals(SHARED_CLIENT_ID, second.getClientId());
        assertEquals("501", first.getId());
        assertEquals("502", second.getId());
        assertEquals("501", firstById.getId());
        assertEquals("502", secondById.getId());
        assertNotEquals(first.getClientSecret(), second.getClientSecret());
        assertTrue(first.getClientSettings().isRequireProofKey());
        assertTrue(second.getClientSettings().isRequireProofKey());
        assertEquals(Set.of("openid", "orders.read"), first.getScopes());
        assertEquals(Set.of("openid", "billing.read"), second.getScopes());
    }

    @Test
    void authorizationTokenLookupAndConsentRemainBoundToTheCurrentIssuer() {
        OAuthRealmRepository realmRepository = realmRepository("ACTIVE");
        TenantIssuerService issuerService = issuerService(realmRepository);
        OAuthAuthorizationRepository authorizations = mock(OAuthAuthorizationRepository.class);
        OAuthTokenRepository tokens = mock(OAuthTokenRepository.class);
        OAuthClientRepository clients = mock(OAuthClientRepository.class);
        OAuthClientBO activeClient = client(501L, "{bcrypt}tenant-one-hash", "orders.read");
        OAuthTokenBO storedToken = token(801L, 601L, OAuthTokenTypeEnum.ACCESS, "issuer-bound-token", claims(), false);
        OAuthAuthorizationBO storedAuthorization = authorization(601L, 501L, "subject-1");
        String tokenDigest = Digests.sha256Hex("issuer-bound-token");

        when(tokens.findByTokenDigest(any(TrustedRealmBO.class), eq(tokenDigest))).thenAnswer(invocation -> {
            TrustedRealmBO realm = invocation.getArgument(0);
            return realm.getStoragePartitionId().equals(1001L) ? Optional.of(storedToken) : Optional.empty();
        });
        when(tokens.findByAuthorization(any(TrustedRealmBO.class), eq(601L), any(OAuthTokenTypeEnum.class),
                eq(0L), eq(500))).thenAnswer(invocation -> {
            TrustedRealmBO realm = invocation.getArgument(0);
            OAuthTokenTypeEnum type = invocation.getArgument(2);
            return realm.getStoragePartitionId().equals(1001L) && type == OAuthTokenTypeEnum.ACCESS
                    ? List.of(storedToken) : List.of();
        });
        when(authorizations.findById(any(TrustedRealmBO.class), eq(601L))).thenAnswer(invocation -> {
            TrustedRealmBO realm = invocation.getArgument(0);
            return realm.getStoragePartitionId().equals(1001L)
                    ? Optional.of(storedAuthorization) : Optional.empty();
        });
        when(clients.findById(any(TrustedRealmBO.class), eq(501L))).thenReturn(Optional.of(activeClient));

        TenantAuthorizationServiceImpl authorizationService = new TenantAuthorizationServiceImpl(
                issuerService,
                mock(OAuthRealmRepository.class),
                authorizations,
                tokens,
                clients,
                org.mapstruct.factory.Mappers.getMapper(SasAuthorizationConverter.class),
                org.mapstruct.factory.Mappers.getMapper(SasRegisteredClientConverter.class),
                OBJECT_MAPPER,
                Clock.fixed(NOW, ZoneOffset.UTC),
                mock(OAuthSecurityProjectionComponent.class),
                mock(OAuthTokenIssueService.class));
        OAuth2Authorization tenantOneAuthorization = withIssuer("https://idp.example.test/tenants/1001",
                () -> authorizationService.findByToken("issuer-bound-token", OAuth2TokenType.ACCESS_TOKEN));
        OAuth2Authorization tenantTwoAuthorization = withIssuer("https://idp.example.test/tenants/1002",
                () -> authorizationService.findByToken("issuer-bound-token", OAuth2TokenType.ACCESS_TOKEN));

        assertNotNull(tenantOneAuthorization);
        assertEquals("601", tenantOneAuthorization.getId());
        assertNull(tenantTwoAuthorization);
        verify(authorizations).findById(any(TrustedRealmBO.class), eq(601L));

        OAuthConsentRepository consents = mock(OAuthConsentRepository.class);
        OAuthConsentBO storedConsent = OAuthConsentBO.builder()
                .id(901L).version(0L).clientPk(501L).subject("subject-1")
                .authorities(List.of("SCOPE_openid", "SCOPE_orders.read"))
                .grantedAt(NOW).status("ACTIVE").build();
        when(consents.findConsent(any(TrustedRealmBO.class), eq(501L), eq("subject-1")))
                .thenAnswer(invocation -> {
                    TrustedRealmBO realm = invocation.getArgument(0);
                    return realm.getStoragePartitionId().equals(1001L)
                            ? Optional.of(storedConsent) : Optional.empty();
                });
        TenantConsentServiceImpl consentService = new TenantConsentServiceImpl(
                issuerService,
                consents,
                clients,
                org.mapstruct.factory.Mappers.getMapper(SasConsentConverter.class),
                Clock.fixed(NOW, ZoneOffset.UTC));
        OAuth2AuthorizationConsent tenantOneConsent = withIssuer("https://idp.example.test/tenants/1001",
                () -> consentService.findById("501", "subject-1"));
        OAuth2AuthorizationConsent tenantTwoConsent = withIssuer("https://idp.example.test/tenants/1002",
                () -> consentService.findById("501", "subject-1"));
        assertNotNull(tenantOneConsent);
        assertEquals(Set.of("openid", "orders.read"), tenantOneConsent.getScopes());
        assertNull(tenantTwoConsent);

        OAuthConsentBO existingConsent = OAuthConsentBO.builder()
                .id(901L).version(2L).clientPk(501L).subject("subject-1")
                .authorities(List.of("SCOPE_openid"))
                .grantedAt(NOW.minusSeconds(60)).status("ACTIVE").build();
        OAuthConsentRepository updatingConsents = mock(OAuthConsentRepository.class);
        when(updatingConsents.findConsentForUpdate(any(TrustedRealmBO.class), eq(501L), eq("subject-1")))
                .thenReturn(Optional.of(existingConsent));
        TenantConsentServiceImpl updatingConsentService = new TenantConsentServiceImpl(
                issuerService,
                updatingConsents,
                clients,
                org.mapstruct.factory.Mappers.getMapper(SasConsentConverter.class),
                Clock.fixed(NOW, ZoneOffset.UTC));
        OAuth2AuthorizationConsent updatedConsent = OAuth2AuthorizationConsent.withId("501", "subject-1")
                .scope("openid")
                .scope("orders.read")
                .build();
        withIssuer("https://idp.example.test/tenants/1001", () -> {
            updatingConsentService.save(updatedConsent);
            return null;
        });
        ArgumentCaptor<OAuthConsentBO> savedConsent = ArgumentCaptor.forClass(OAuthConsentBO.class);
        verify(updatingConsents).save(any(TrustedRealmBO.class), savedConsent.capture(), eq(2L));
        assertEquals(901L, savedConsent.getValue().getId());
        assertEquals(3L, savedConsent.getValue().getVersion());
        assertEquals(NOW, savedConsent.getValue().getGrantedAt());

        OAuthConsentRepository roleConsents = mock(OAuthConsentRepository.class);
        when(roleConsents.findConsent(any(TrustedRealmBO.class), eq(501L), eq("subject-1")))
                .thenReturn(Optional.of(OAuthConsentBO.builder()
                        .id(902L).version(0L).clientPk(501L).subject("subject-1")
                        .authorities(List.of("ROLE_ADMIN")).grantedAt(NOW).status("ACTIVE").build()));
        TenantConsentServiceImpl roleRejectingService = new TenantConsentServiceImpl(
                issuerService,
                roleConsents,
                clients,
                org.mapstruct.factory.Mappers.getMapper(SasConsentConverter.class),
                Clock.fixed(NOW, ZoneOffset.UTC));
        assertThrows(IllegalArgumentException.class, () -> withIssuer(
                "https://idp.example.test/tenants/1001",
                () -> roleRejectingService.findById("501", "subject-1")));
    }

    @Test
    void signingAndPublicJwksKeepSharedKidScopedByIssuer() throws Exception {
        OAuthRealmRepository realmRepository = realmRepository("ACTIVE");
        TenantIssuerService issuerService = issuerService(realmRepository);
        OAuthSigningKeyRepository keys = mock(OAuthSigningKeyRepository.class);
        OAuthKeyCipherComponent keyCipher = mock(OAuthKeyCipherComponent.class);
        when(keys.findByStatuses(any(TrustedRealmBO.class), anyCollection(), eq(0L), eq(8)))
                .thenAnswer(invocation -> {
                    TrustedRealmBO realm = invocation.getArgument(0);
                    return List.of(signingKey(1001L, tenantOneKeyPair), signingKey(1002L, tenantTwoKeyPair))
                            .stream()
                            .filter(key -> realm.getStoragePartitionId().equals(1001L)
                                    ? key.getId().equals(1001L) : key.getId().equals(1002L))
                            .toList();
                });
        when(keyCipher.decryptPrivateKey(any(TrustedRealmBO.class), eq(SHARED_KID), anyString()))
                .thenAnswer(invocation -> {
                    TrustedRealmBO realm = invocation.getArgument(0);
                    return pem(realm.getStoragePartitionId().equals(1001L)
                            ? tenantOneKeyPair : tenantTwoKeyPair);
                });
        TenantJwkSourceComponent jwks = new TenantJwkSourceComponent(issuerService, realmRepository, keys, keyCipher);
        JWKSelector publicSelector = new JWKSelector(new JWKMatcher.Builder().build());
        JWKSelector signingSelector = new JWKSelector(new JWKMatcher.Builder()
                .keyID(SHARED_KID).algorithm(JWSAlgorithm.RS256).keyUse(KeyUse.SIGNATURE).build());

        List<JWK> firstPublic = withIssuer("https://idp.example.test/tenants/1001",
                () -> jwks.get(publicSelector, null));
        List<JWK> secondPublic = withIssuer("https://idp.example.test/tenants/1002",
                () -> jwks.get(publicSelector, null));
        List<JWK> firstSigning = withIssuer("https://idp.example.test/tenants/1001",
                () -> jwks.get(signingSelector, null));
        List<JWK> secondSigning = withIssuer("https://idp.example.test/tenants/1002",
                () -> jwks.get(signingSelector, null));

        assertEquals(SHARED_KID, firstPublic.getFirst().getKeyID());
        assertEquals(SHARED_KID, secondPublic.getFirst().getKeyID());
        assertFalse(firstPublic.getFirst().isPrivate());
        assertFalse(secondPublic.getFirst().isPrivate());
        assertNotEquals(((RSAKey) firstPublic.getFirst()).getModulus(),
                ((RSAKey) secondPublic.getFirst()).getModulus());
        assertTrue(firstSigning.getFirst().isPrivate());
        assertTrue(secondSigning.getFirst().isPrivate());
        assertNotEquals(((RSAKey) firstSigning.getFirst()).getModulus(),
                ((RSAKey) secondSigning.getFirst()).getModulus());
    }

    @Test
    void sasConvertersPreserveAuthorizationTokenAndConsentContracts() {
        SasAuthorizationConverter authorizationConverter = org.mapstruct.factory.Mappers.getMapper(
                SasAuthorizationConverter.class);
        SasConsentConverter consentConverter = org.mapstruct.factory.Mappers.getMapper(SasConsentConverter.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        OAuthAuthorizationAttributesBO attributes = attributes();
        SignedTokenClaimsBO claims = claims();
        OAuthTokenBO code = token(801L, 601L, OAuthTokenTypeEnum.CODE, "one-time-code", null, false);
        OAuthTokenBO access = token(802L, 601L, OAuthTokenTypeEnum.ACCESS, "signed-access-token", claims, false);
        OAuthAuthorizationBO source = OAuthAuthorizationBO.builder()
                .id(601L)
                .version(0L)
                .clientPk(501L)
                .principalName("subject-1")
                .principalType("USER")
                .grantType("authorization_code")
                .authorizedScopes(List.of("openid", "orders.read"))
                .audiences(List.of("https://orders.example.test"))
                .status("ACTIVE")
                .authTime(NOW)
                .acr("urn:example:loa:2")
                .attributes(attributes)
                .tokens(List.of(code, access))
                .absoluteExpiresAt(NOW.plusSeconds(600))
                .build();
        RegisteredClient registeredClient = RegisteredClient.withId("501")
                .clientId(SHARED_CLIENT_ID)
                .clientName("Shared Client")
                .clientSecret("{bcrypt}encoded-test-hash")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://client.example.test/callback")
                .build();

        OAuth2Authorization sas = authorizationConverter.toTarget(source, registeredClient, objectMapper);
        OAuthAuthorizationBO restored = authorizationConverter.toSource(sas, null, objectMapper);
        assertEquals("601", sas.getId());
        assertEquals("one-time-code", sas.getToken(OAuth2AuthorizationCode.class).getToken().getTokenValue());
        assertEquals("signed-access-token", sas.getAccessToken().getToken().getTokenValue());
        assertEquals(claims.getSubject(), restored.getTokens().stream()
                .filter(value -> value.getTokenType() == OAuthTokenTypeEnum.ACCESS)
                .findFirst().orElseThrow().getMetadata().getClaims().getSubject());
        assertEquals(attributes, restored.getAttributes());

        OAuthConsentBO consent = OAuthConsentBO.builder()
                .id(901L)
                .version(0L)
                .clientPk(501L)
                .subject("subject-1")
                .authorities(List.of("SCOPE_openid", "SCOPE_orders.read"))
                .grantedAt(NOW)
                .status("ACTIVE")
                .build();
        OAuth2AuthorizationConsent sasConsent = consentConverter.toTarget(consent);
        assertEquals(Set.of("SCOPE_openid", "SCOPE_orders.read"), sasConsent.getAuthorities().stream()
                .map(authority -> authority.getAuthority()).collect(java.util.stream.Collectors.toSet()));
        assertEquals(consent.getAuthorities(), consentConverter.toSource(sasConsent).getAuthorities());
    }

    @Test
    void discoveryOpenApiDocumentsTheRealSasFilterPath() {
        OpenAPI openApi = new OpenAPI();
        openApiCustomizer.customise(openApi);

        var operation = openApi.getPaths().get("/tenants/{tenantId}/.well-known/openid-configuration").getGet();
        assertEquals("oauthTenantOidcDiscovery", operation.getOperationId());
        assertEquals(Boolean.TRUE, operation.getParameters().getFirst().getRequired());
        assertTrue(operation.getSecurity().isEmpty());
        assertNotNull(operation.getResponses().get("200"));
        assertNotNull(operation.getResponses().get("404"));
        var loginPath = openApi.getPaths().get("/tenants/{tenantId}/login");
        assertEquals("oauthSasLoginSubmit", loginPath.getPost().getOperationId());
        assertTrue(loginPath.getPost().getRequestBody().getRequired());
        assertNotNull(loginPath.getPost().getResponses().get("303").getHeaders().get("Location"));
    }

    @Test
    void clientByIdMapperParsesWithTenantAndSoftDeletePredicates() throws Exception {
        String resource = "mapper/oauth/OAuthClientMapper.xml";
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        try (InputStream input = new ClassPathResource(resource).getInputStream()) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        String statementId = "top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthClientMapper.selectActiveById";
        assertTrue(configuration.hasStatement(statementId));
        BoundSql boundSql = configuration.getMappedStatement(statementId)
                .getBoundSql(java.util.Map.of("tenantId", 1001L, "id", 501L));
        String sql = boundSql.getSql().replaceAll("\\s+", " ").toLowerCase();
        assertTrue(sql.contains("tenant_id = ?"));
        assertTrue(sql.contains("id = ?"));
        assertTrue(sql.contains("deleted_at is null"));
    }

    private static TenantIssuerServiceImpl issuerService(OAuthRealmRepository repository) {
        return new TenantIssuerServiceImpl(PUBLIC_ORIGIN, repository, new IdentityDirectoryContext(9000L, "tenantId"));
    }

    private static OAuthRealmRepository realmRepository(String status) {
        OAuthRealmRepository repository = mock(OAuthRealmRepository.class);
        when(repository.findByIssuer(any(TrustedRealmBO.class))).thenAnswer(invocation -> {
            TrustedRealmBO trustedRealm = invocation.getArgument(0);
            return Optional.of(realm(trustedRealm, status));
        });
        return repository;
    }

    private static OAuthRealmBO realm(TrustedRealmBO trustedRealm, String status) {
        return OAuthRealmBO.builder()
                .id(trustedRealm.getStoragePartitionId())
                .version(0L)
                .issuer(trustedRealm.getIssuer())
                .realmKey(trustedRealm.getRealmKey())
                .realmType(trustedRealm.getRealmType())
                .businessTenantId(trustedRealm.getBusinessTenantId())
                .storagePartitionId(trustedRealm.getStoragePartitionId())
                .status(status)
                .activeKid(SHARED_KID)
                .trustVersion(4L)
                .build();
    }

    private static OAuthClientBO client(long id, String secretHash, String scope) {
        return OAuthClientBO.builder()
                .id(id)
                .version(0L)
                .clientId(SHARED_CLIENT_ID)
                .clientName("Shared Client")
                .clientType(OAuthClient.ClientType.CONFIDENTIAL)
                .purpose(OAuthClientPurposeEnum.USER)
                .status(OAuthClient.Status.ACTIVE)
                .secretHash(secretHash)
                .secretHint("hint")
                .credentialVersion(1L)
                .redirectUris(List.of("https://client.example.test/callback"))
                .resourceUris(List.of("https://api.example.test"))
                .allowedScopes(List.of("openid", scope))
                .accessTtlSeconds(300)
                .refreshTtlSeconds(3600)
                .build();
    }

    private static OAuthAuthorizationBO authorization(long id, long clientPk, String principalName) {
        return OAuthAuthorizationBO.builder()
                .id(id)
                .version(0L)
                .clientPk(clientPk)
                .principalName(principalName)
                .principalType("USER")
                .grantType("authorization_code")
                .authorizedScopes(List.of("openid", "orders.read"))
                .audiences(List.of("https://orders.example.test"))
                .status("ACTIVE")
                .authTime(NOW)
                .acr("urn:example:loa:2")
                .attributes(attributes())
                .absoluteExpiresAt(NOW.plusSeconds(600))
                .build();
    }

    private static OAuthSigningKeyBO signingKey(long id, KeyPair pair) {
        RSAKey publicKey = new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                .keyID(SHARED_KID)
                .algorithm(JWSAlgorithm.RS256)
                .keyUse(KeyUse.SIGNATURE)
                .build();
        JsonNode publicJwk = OBJECT_MAPPER.valueToTree(publicKey.toPublicJWK().toJSONObject());
        return OAuthSigningKeyBO.builder()
                .id(id)
                .version(0L)
                .kid(SHARED_KID)
                .algorithm("RS256")
                .publicJwk(publicJwk)
                .privateKeyCiphertext("ciphertext-" + id)
                .status(OAuthKeyStatusEnum.ACTIVE)
                .publishedAt(NOW.minusSeconds(60))
                .activatedAt(NOW.minusSeconds(30))
                .build();
    }

    private static String pem(KeyPair pair) {
        String encoded = Base64.getMimeEncoder(64, new byte[] {'\n'})
                .encodeToString(((RSAPrivateCrtKey) pair.getPrivate()).getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + encoded + "\n-----END PRIVATE KEY-----";
    }

    private static OAuthAuthorizationAttributesBO attributes() {
        return OAuthAuthorizationAttributesBO.builder()
                .schemaVersion(1)
                .nonce("nonce-value")
                .codeChallenge("challenge-value")
                .codeChallengeMethod("S256")
                .redirectUri(URI.create("https://client.example.test/callback"))
                .stateDigest("a".repeat(64))
                .securitySnapshot(OAuthSecuritySnapshotBO.builder()
                        .realmGeneration(1L)
                        .clientGeneration(2L)
                        .subjectGeneration(3L)
                        .readyEpoch("ready-epoch-1")
                        .revision(4L)
                        .build())
                .acr("urn:example:loa:2")
                .authTime(NOW)
                .build();
    }

    private static SignedTokenClaimsBO claims() {
        return SignedTokenClaimsBO.builder()
                .issuer(URI.create("https://idp.example.test/tenants/1001"))
                .audience(Set.of(URI.create("https://orders.example.test")))
                .subject("subject-1")
                .scope(Set.of("openid", "orders.read"))
                .tenantId(1001L)
                .principalType("USER")
                .jti("123456789")
                .issuedAt(NOW)
                .notBefore(NOW)
                .expiresAt(NOW.plusSeconds(300))
                .clientId(SHARED_CLIENT_ID)
                .authorizationId("601")
                .clientRegistrationId("501")
                .scopeContext("tenant")
                .securityVersion(OAuthSecurityVersionBO.builder()
                        .realm(1L).client(2L).subject(3L).build())
                .build();
    }

    private static OAuthTokenBO token(
            long id,
            long authorizationPk,
            OAuthTokenTypeEnum type,
            String value,
            SignedTokenClaimsBO tokenClaims,
            boolean invalidated) {
        return OAuthTokenBO.builder()
                .id(id)
                .version(0L)
                .authorizationPk(authorizationPk)
                .tokenType(type)
                .tokenDigest(Digests.sha256Hex(value))
                .tokenValue(value)
                .jti(tokenClaims == null ? null : tokenClaims.getJti())
                .issuedAt(NOW)
                .expiresAt(NOW.plusSeconds(type == OAuthTokenTypeEnum.CODE ? 120 : 300))
                .metadata(OAuthTokenMetadataBO.builder()
                        .schemaVersion(1)
                        .claims(tokenClaims)
                        .invalidated(invalidated)
                        .codeConsumed(false)
                        .build())
                .build();
    }

    private static TrustedRealmBO trustedTenant(long tenantId, URI issuer) {
        return TrustedRealmBO.builder()
                .issuer(issuer)
                .realmKey("tenant:" + tenantId)
                .realmType(RealmTypeEnum.TENANT)
                .storagePartitionId(tenantId)
                .businessTenantId(tenantId)
                .trustVersion(4L)
                .build();
    }

    private static TrustedRealmBO trustedPlatform(URI issuer) {
        return TrustedRealmBO.builder()
                .issuer(issuer)
                .realmKey("platform")
                .realmType(RealmTypeEnum.PLATFORM)
                .storagePartitionId(9000L)
                .businessTenantId(null)
                .trustVersion(4L)
                .build();
    }

    private static MockHttpServletRequestBuilder tenantRequest(MockHttpServletRequestBuilder request, long tenantId) {
        return request.secure(true);
    }

    private static <T> T withIssuer(String issuer, java.util.concurrent.Callable<T> operation) {
        AuthorizationServerContextHolder.setContext(new AuthorizationServerContext() {
            @Override
            public String getIssuer() {
                return issuer;
            }

            @Override
            public AuthorizationServerSettings getAuthorizationServerSettings() {
                return AuthorizationServerSettings.builder().multipleIssuersAllowed(true).build();
            }
        });
        try {
            return operation.call();
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IllegalStateException("TENANT_ISSUER_TEST_FAILED", failure);
        } finally {
            AuthorizationServerContextHolder.resetContext();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableWebSecurity
    @EnableConfigurationProperties(SpringDocConfigProperties.class)
    @Import({SpringDocConfiguration.class, SpringDocWebMvcConfiguration.class})
    static class TestWebConfiguration {

        @Bean("jacksonObjectMapper")
        ObjectMapper objectMapper() {
            return OBJECT_MAPPER;
        }

        @Bean("oauthTokenIssueService")
        OAuthTokenIssueService oauthTokenIssueService() {
            return mock(OAuthTokenIssueService.class);
        }

        @Bean("oAuthRealmRepository")
        OAuthRealmRepository oauthRealmRepository() {
            return mock(OAuthRealmRepository.class);
        }

        @Bean("tokenRevocationService")
        top.egon.cola.platform.tianquan.shoubing.admin.token.service.TokenRevocationService tokenRevocationService() {
            return mock(top.egon.cola.platform.tianquan.shoubing.admin.token.service.TokenRevocationService.class);
        }

        @Bean("transactionManager")
        org.springframework.transaction.PlatformTransactionManager transactionManager() {
            return mock(org.springframework.transaction.PlatformTransactionManager.class);
        }

        @Bean("tenantIssuerService")
        TenantIssuerService tenantIssuerService() {
            return mock(TenantIssuerService.class);
        }

        @Bean("idpClock")
        java.time.Clock idpClock() {
            return java.time.Clock.systemUTC();
        }

        @Bean("tenantRegisteredClientRepository")
        RegisteredClientRepository registeredClientRepository() {
            return mock(RegisteredClientRepository.class);
        }

        @Bean("tenantAuthorizationService")
        OAuth2AuthorizationService authorizationService() {
            return mock(OAuth2AuthorizationService.class);
        }

        @Bean("tenantConsentService")
        OAuth2AuthorizationConsentService consentService() {
            return mock(OAuth2AuthorizationConsentService.class);
        }

        @Bean("tenantJwkSourceComponent")
        JWKSource<SecurityContext> jwkSource() {
            return (selector, context) -> {
                AuthorizationServerContext serverContext = AuthorizationServerContextHolder.getContext();
                if (serverContext == null || serverContext.getIssuer() == null) {
                    return List.of();
                }
                RSAKey key = serverContext.getIssuer().endsWith("/tenants/1001")
                        ? rsaPublicKey(tenantOneKeyPair, SHARED_KID)
                        : rsaPublicKey(tenantTwoKeyPair, SHARED_KID);
                return selector.select(new JWKSet(List.of(key.toPublicJWK())));
            };
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }
    }

    private static RSAKey rsaPublicKey(KeyPair pair, String kid) {
        return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                .keyID(kid)
                .algorithm(JWSAlgorithm.RS256)
                .keyUse(KeyUse.SIGNATURE)
                .build();
    }
}
