package top.egon.cola.component.yuheng.openapi.webmvc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.redisson.api.RedissonClient;
import org.redisson.api.RStream;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.client.codec.StringCodec;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpJwtVerifier;
import top.egon.cola.platform.tianquan.shoubing.starter.security.UserAccessTokenVerifier;
import top.egon.cola.platform.tianquan.shoubing.starter.security.ServiceAccessTokenVerifier;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TenantIssuerAuthenticationManagerComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.OAuthRevocationReader;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.VerifiedPrincipalConverter;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerState;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerStateReader;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityOAuthClientStateReader;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthAuditProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.AuditDeliveryProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.observability.IdpSecurityObservationReceiverEnum;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.converter.TokenUsageAuditEventConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springdoc.webmvc.api.OpenApiResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class GatewayOpenApiWebMvcSecurityAutoConfigurationTest {

    private static final URI ISSUER = URI.create("https://idp.example/tenants/1001");
    private static final URI RESOURCE = URI.create("https://api.example/provider");
    private static final Instant NOW = Instant.parse("2026-10-12T00:00:00Z");
    private static final String SCOPE = "yuheng.openapi.read";
    private static final String STREAM = "egon:oauth:audit:v1";
    @TempDir Path temporary;
    private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    private final AtomicReference<OAuthRevocationReader.Result> revocation = new AtomicReference<>(OAuthRevocationReader.Result.valid());
    private final AtomicInteger documents = new AtomicInteger();
    @SuppressWarnings("unchecked") private final RStream<String, String> stream = mock(RStream.class);
    AnnotationConfigWebApplicationContext context;
    MockMvc mvc;
    private RSAKey key;

    @BeforeEach
    void setupPipeline() throws Exception {
        setupPipeline(RESOURCE, Map.of("egon.cola.component.yuheng.openapi.enabled", true), FixtureConfiguration.class);
    }

    void setupPipeline(URI resource, Map<String, Object> settings, Class<?> fixture) throws Exception {
        validator.afterPropertiesSet();
        key = new RSAKeyGenerator(2048).keyID("docs-key").algorithm(JWSAlgorithm.RS256).generate();
        var clock = Clock.fixed(NOW, ZoneOffset.UTC);
        var properties = new IdpStarterProperties(); properties.setResourceServerId("provider-resource"); properties.setResourceUri(resource);
        properties.getRevocation().setPrimaryEndpoint("rediss://redis.example:6379"); properties.getRevocation().setEpoch("epoch-1");
        var trusted = new IdpStarterProperties.TrustedIssuerSettings(); trusted.setIssuer(ISSUER.toASCIIString());
        trusted.setJwksUri(ISSUER + "/.well-known/jwks.json"); trusted.setRealmType("TENANT"); trusted.setRealmKey("tenant:1001");
        trusted.setTenantId("1001"); trusted.setStatus("ACTIVE"); trusted.setTrustVersion(1); properties.setTrustedIssuers(Map.of("tenant-1001", trusted));
        var managers = new TenantIssuerAuthenticationManagerComponent(new TrustedIssuerRegistry(properties), properties, clock); managers.initialize();
        var rest = TenantIssuerAuthenticationManagerComponent.class.getDeclaredField("jwksRestOperations"); rest.setAccessible(true);
        var jwks = MockRestServiceServer.bindTo((RestTemplate) rest.get(managers)).build();
        jwks.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(ISSUER + "/.well-known/jwks.json"))
                .andRespond(MockRestResponseCreators.withSuccess(new JWKSet(key.toPublicJWK()).toString(), MediaType.APPLICATION_JSON));
        var redis = mock(RedissonClient.class); doReturn(stream).when(redis).getStream(eq(STREAM), eq(StringCodec.INSTANCE));
        when(stream.add(any(StreamAddArgs.class))).thenReturn(new StreamMessageId(1, 1));
        var secret = temporary.resolve("audit.key"); Files.writeString(secret, "0123456789abcdef0123456789abcdef");
        Files.setPosixFilePermissions(secret, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        var mapper = new ObjectMapper().findAndRegisterModules();
        var producer = new TokenUsageAuditComponent(redis, mapper, new OAuthAuditProperties(STREAM, temporary.resolve("spool"), 1_048_576, secret),
                new AuditDeliveryProperties(), clock, validator);
        var eventConverter = (TokenUsageAuditEventConverter) Class.forName(TokenUsageAuditEventConverter.class.getName() + "Impl").getConstructor().newInstance();
        var observer = new IdpObservedVerificationComponent(producer, eventConverter,
                new OAuthUsageObservationProperties(true, ISSUER, false, IdpSecurityObservationReceiverEnum.APPLICATION), observation -> { }, clock, validator, resource);
        IdentityResourceServerStateReader resourceStates = id -> Optional.of(new IdentityResourceServerState("provider-resource", resource, "biz", "provider", "test", ResourceServerStatus.ACTIVE, 7));
        var converter = (VerifiedPrincipalConverter) Class.forName(VerifiedPrincipalConverter.class.getName() + "Impl").getConstructor().newInstance();
        var verifier = IdpJwtVerifier.createConfigured(null, resourceStates, mock(IdentityOAuthClientStateReader.class), "provider-resource", resource,
                "platform-audience", clock, managers, ignored -> revocation.get(), converter, null, observer);
        var decoder = NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).validateType(false).build();
        var timestamps = new JwtTimestampValidator(java.time.Duration.ZERO); timestamps.setClock(clock);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamps, new JwtIssuerValidator(ISSUER.toASCIIString())));
        context = new AnnotationConfigWebApplicationContext(); context.setServletContext(new MockServletContext());
        context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("docsFixture", settings));
        context.addBeanFactoryPostProcessor(factory -> {
            factory.registerSingleton("idpStarterProperties", properties); factory.registerSingleton("idpJwtVerifier", verifier);
            factory.registerSingleton("userAccessTokenVerifier", new UserAccessTokenVerifier(verifier)); factory.registerSingleton("serviceAccessTokenVerifier", new ServiceAccessTokenVerifier(verifier));
            factory.registerSingleton("idpObservedVerificationComponent", observer); factory.registerSingleton("jacksonObjectMapper", mapper);
            factory.registerSingleton("jwtDecoder", decoder); factory.registerSingleton("documentCalls", documents);
        });
        context.register(GatewayOpenApiWebMvcSecurityAutoConfiguration.class, fixture); context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(context.getBean(FilterChainProxy.class)).build();
    }

    @AfterEach
    void cleanup() {
        if (context != null) { context.close(); }
        validator.close(); org.springframework.security.core.context.SecurityContextHolder.clearContext(); org.slf4j.MDC.clear();
    }

    @Test
    void wrongAudienceIdTokenRevokedAndUnavailableNeverReturnADocument() throws Exception {
        assertRejected(token(URI.create("https://api.example/other"), "at+jwt", "SERVICE", SCOPE), 401);
    }

    @Test
    void idTokenWithADocumentScopeCannotBecomeAServiceCredential() throws Exception {
        assertRejected(token(RESOURCE, "JWT", "USER", SCOPE), 401);
    }

    @Test
    void revokedServiceCannotReturnADocument() throws Exception {
        revocation.set(OAuthRevocationReader.Result.revoked("TOKEN_REVOKED"));
        assertRejected(token(RESOURCE, "at+jwt", "SERVICE", SCOPE), 401);
    }

    @Test
    void unavailableSecurityStateMustBe503WithoutReturningADocument() throws Exception {
        revocation.set(OAuthRevocationReader.Result.unavailable("OAUTH_SECURITY_STATE_UNAVAILABLE"));
        assertRejected(token(RESOURCE, "at+jwt", "SERVICE", SCOPE), 503);
    }

    @Test
    void validServiceStillNeedsTheExactDocumentScopeAndAConfirmedAudit() throws Exception {
        assertRejected(token(RESOURCE, "at+jwt", "SERVICE", "provider:read"), 403);
    }

    @Test
    void validServiceWithTheExactScopePreservesTheDocumentResponse() throws Exception {
        var response = mvc.perform(get("/v3/api-docs/fixture").header("Authorization", "Bearer " + token(RESOURCE, "at+jwt", "SERVICE", SCOPE))).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(200); assertThat(documents.get()).isOne();
    }

    @Test
    void auditFailureMustBe503WithoutReturningADocument() throws Exception {
        when(stream.add(any(StreamAddArgs.class))).thenThrow(new org.redisson.client.RedisException("controlled unavailable"));
        assertRejected(token(RESOURCE, "at+jwt", "SERVICE", SCOPE), 503);
    }

    @Test
    void denialAuditFailureMustBe503AfterAConfirmedTokenUse() throws Exception {
        when(stream.add(any(StreamAddArgs.class))).thenReturn(new StreamMessageId(1, 1))
                .thenThrow(new org.redisson.client.RedisException("controlled denied delivery failure"));
        assertRejected(token(RESOURCE, "at+jwt", "SERVICE", "provider:read"), 503);
        org.mockito.Mockito.verify(stream, org.mockito.Mockito.times(2)).add(any(StreamAddArgs.class));
    }

    @Test
    void missingAndMalformedBearerNeedAnAnonymousAuditAndAChallenge() throws Exception {
        for (String credential : List.of("", "invalid", "Bearer invalid", "Bearer invalid,duplicate")) {
            var request = get("/v3/api-docs/fixture");
            if (!credential.isEmpty()) { request.header("Authorization", credential); }
            var response = mvc.perform(request).andReturn().getResponse();
            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
            assertThat(response.getHeader("Cache-Control")).contains("no-store");
        }
        assertThat(documents.get()).isZero();
        org.mockito.Mockito.verify(stream, org.mockito.Mockito.times(4)).add(any(StreamAddArgs.class));
    }

    @Test
    void userAccessTokenCannotBecomeADocumentServiceIdentity() throws Exception {
        assertRejected(token(RESOURCE, "at+jwt", "USER", SCOPE), 401);
    }

    @Test
    void staleResourceVersionCannotReturnADocument() throws Exception {
        assertRejected(changedClaim(token(RESOURCE, "at+jwt", "SERVICE", SCOPE), "resource_version", 6L), 401);
    }

    @Test
    void expiredAccessTokenCannotReturnADocument() throws Exception {
        assertRejected(changedClaim(token(RESOURCE, "at+jwt", "SERVICE", SCOPE), "exp", NOW.minusSeconds(120).getEpochSecond()), 401);
    }

    private String changedClaim(String credential, String name, Object value) throws Exception {
        var original = SignedJWT.parse(credential);
        var claims = new LinkedHashMap<>(original.getJWTClaimsSet().toJSONObject()); claims.put(name, value);
        var changed = new SignedJWT(original.getHeader(), JWTClaimsSet.parse(claims));
        changed.sign(new RSASSASigner(key)); return changed.serialize();
    }

    @Test
    void narrowDocumentChainDoesNotChangeTheBusinessSecurityChain() throws Exception {
        var response = mvc.perform(get("/business/ping")).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(200); assertThat(response.getContentAsString()).contains("business-original");
        assertThat(documents.get()).isZero();
    }

    @Test
    void disabledDocumentationDoesNotCreateAChainOrAPublishedDocument() throws Exception {
        var disabled = new AnnotationConfigWebApplicationContext(); disabled.setServletContext(new MockServletContext());
        disabled.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("docsDisabled", Map.of("egon.cola.component.yuheng.openapi.enabled", false)));
        disabled.register(GatewayOpenApiWebMvcSecurityAutoConfiguration.class, FixtureConfiguration.class); disabled.refresh();
        try {
            assertThat(disabled.containsBean("gatewayOpenApiWebMvcSecurityFilterChain")).isFalse();
            assertThat(disabled.containsBean("documentFixture")).isFalse();
            var client = MockMvcBuilders.webAppContextSetup(disabled).addFilters(disabled.getBean(FilterChainProxy.class)).build();
            assertThat(client.perform(get("/v3/api-docs/fixture")).andReturn().getResponse().getStatus()).isEqualTo(404);
            assertThat(client.perform(get("/business/ping")).andReturn().getResponse().getStatus()).isEqualTo(200);
        } finally { disabled.close(); }
    }

    private void assertRejected(String token, int status) throws Exception {
        documents.set(0);
        var response = mvc.perform(get("/v3/api-docs/fixture").header("Authorization", "Bearer " + token)).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(status); assertThat(documents.get()).isZero();
        assertThat(response.getHeader("Cache-Control")).contains("no-store");
        if (status == 401) { assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer"); }
    }

    String token(URI audience, String typ, String principalType, String scope) throws Exception {
        var claims = new LinkedHashMap<String, Object>(); claims.put("iss", ISSUER.toASCIIString()); claims.put("aud", List.of(audience.toASCIIString()));
        claims.put("sub", "docs-client"); claims.put("client_id", "docs-client"); claims.put("jti", "901"); claims.put("tenant_id", "1001");
        claims.put("iat", NOW.minusSeconds(1).getEpochSecond()); claims.put("nbf", NOW.minusSeconds(1).getEpochSecond()); claims.put("exp", NOW.plusSeconds(300).getEpochSecond());
        claims.put("principal_type", principalType); claims.put("scope_context", "TENANT"); claims.put("scope", scope); claims.put("authorization_id", "801");
        claims.put("client_registration_id", "501"); claims.put("security_version", Map.of("realm", 1L, "client", 2L));
        claims.put("resource_version", 7L); claims.put("source_biz", "biz"); claims.put("source_app", "provider"); claims.put("source_env", "test");
        claims.put("credential_id", "credential-1"); claims.put("app_id", "provider-app");
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).type(new JOSEObjectType(typ)).keyID("docs-key").build(), JWTClaimsSet.parse(claims));
        jwt.sign(new RSASSASigner(key)); return jwt.serialize();
    }

    @Slf4j
    @Configuration(value = "docsMvcFixtureConfiguration", proxyBeanMethods = false)
    @EnableWebSecurity
    @EnableWebMvc
    static class FixtureConfiguration {
        @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix = "egon.cola.component.yuheng.openapi", name = "enabled", havingValue = "true")
        @Bean("documentFixture") DocumentFixture documentFixture(@Qualifier("documentCalls") AtomicInteger documentCalls) { return new DocumentFixture(documentCalls); }
        @Bean("businessFixture") BusinessFixture businessFixture() { return new BusinessFixture(); }
        @Bean("fixtureBusinessSecurityChain")
        @org.springframework.core.annotation.Order(org.springframework.core.Ordered.LOWEST_PRECEDENCE)
        SecurityFilterChain businessChain(HttpSecurity http) throws Exception {
            return http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll()).build();
        }
    }

    @Slf4j
    @RequiredArgsConstructor
    @RestController("documentFixture")
    static class DocumentFixture {
        @Qualifier("documentCalls") private final AtomicInteger calls;
        @GetMapping("/v3/api-docs/fixture") Map<String, String> document() { calls.incrementAndGet(); return Map.of("openapi", "3.1.0"); }
    }

    @Slf4j
    @RestController("businessFixture")
    static class BusinessFixture {
        @GetMapping("/business/ping") Map<String, String> ping() { return Map.of("result", "business-original"); }
    }

    @Test
    void isConditionalServletAutoConfigurationForSpringdocAndSecurity() {
        Class<?> configuration =
                GatewayOpenApiWebMvcSecurityAutoConfiguration.class;

        assertThat(configuration.getAnnotation(AutoConfiguration.class))
                .isNotNull();
        assertThat(configuration.getAnnotation(ConditionalOnClass.class))
                .isNotNull();
        assertThat(configuration.getAnnotation(ConditionalOnWebApplication.class))
                .extracting(ConditionalOnWebApplication::type)
                .isEqualTo(ConditionalOnWebApplication.Type.SERVLET);
        assertThat(configuration.getAnnotation(ConditionalOnClass.class)
                .value())
                .contains(HttpSecurity.class, SecurityFilterChain.class,
                        OpenApiResource.class);
    }
}
