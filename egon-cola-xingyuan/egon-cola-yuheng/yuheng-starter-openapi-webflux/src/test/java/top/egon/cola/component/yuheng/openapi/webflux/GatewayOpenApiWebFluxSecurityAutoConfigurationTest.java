package top.egon.cola.component.yuheng.openapi.webflux;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.redisson.api.RedissonClient;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.api.RScript;
import org.redisson.api.RScriptReactive;
import org.redisson.api.RStream;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.reactive.context.StandardReactiveWebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springdoc.webflux.api.OpenApiResource;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.AuditDeliveryProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthAuditProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.observability.IdpSecurityObservationReceiverEnum;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TenantIssuerAuthenticationManagerComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry;
import top.egon.cola.platform.tianquan.shoubing.starter.security.VerifiedPrincipalConverter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.IdpAuthorizationObservationContextBO;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.converter.TokenUsageAuditEventConverter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.impl.RedisOAuthRevocationReader;
import top.egon.cola.platform.tianquan.shoubing.starter.security.reactive.ReactiveOAuthRevocationReader;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityOAuthClientStateReader;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerState;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerStateReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GatewayOpenApiWebFluxSecurityAutoConfigurationTest {
    private static final URI ISSUER = URI.create("https://idp.example/tenants/1001");
    private static final URI RESOURCE = URI.create("https://api.example/provider");
    private static final Instant NOW = Instant.parse("2026-10-12T00:00:00Z");
    private static final String SCOPE = "yuheng.openapi.read";
    private static final String STREAM = "egon:oauth:audit:v1";
    @TempDir Path temporary;
    private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    private final AtomicReference<Mono<List<Object>>> snapshot = new AtomicReference<>(Mono.just(List.of("V", "ACTIVE")));
    private final AtomicInteger documents = new AtomicInteger();
    private final AtomicInteger snapshots = new AtomicInteger();
    private final AtomicInteger auditEvents = new AtomicInteger();
    private final AtomicBoolean blockingPortOnEventThread = new AtomicBoolean();
    @SuppressWarnings("unchecked") private final RStream<String, String> stream = mock(RStream.class);
    AnnotationConfigApplicationContext context;
    WebTestClient client;
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
        var registry = new TrustedIssuerRegistry(properties);
        var managers = new TenantIssuerAuthenticationManagerComponent(registry, properties, clock); managers.initialize();
        var rest = TenantIssuerAuthenticationManagerComponent.class.getDeclaredField("jwksRestOperations"); rest.setAccessible(true);
        var jwks = MockRestServiceServer.bindTo((RestTemplate) rest.get(managers)).build();
        jwks.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(ISSUER + "/.well-known/jwks.json"))
                .andRespond(MockRestResponseCreators.withSuccess(new JWKSet(key.toPublicJWK()).toString(), MediaType.APPLICATION_JSON));
        var redis = mock(RedissonClient.class); doReturn(stream).when(redis).getStream(eq(STREAM), eq(StringCodec.INSTANCE));
        when(stream.add(any(StreamAddArgs.class))).thenAnswer(invocation -> {
            blockingPortOnEventThread.compareAndSet(false, Schedulers.isInNonBlockingThread());
            auditEvents.incrementAndGet(); return new StreamMessageId(1, auditEvents.get());
        });
        var reactiveRedis = mock(RedissonReactiveClient.class);
        var script = mock(RScriptReactive.class); when(reactiveRedis.getScript(StringCodec.INSTANCE)).thenReturn(script);
        doAnswer(invocation -> Mono.defer(() -> {
            snapshots.incrementAndGet();
            assertThat((String) invocation.getArgument(1)).isEqualTo(RedisOAuthRevocationReader.READ_SECURITY_SNAPSHOT_SCRIPT);
            assertThat((List<?>) invocation.getArgument(3)).hasSize(7);
            return snapshot.get();
        })).when(script).eval(eq(RScript.Mode.READ_ONLY), anyString(), eq(RScript.ReturnType.MULTI), anyList(), any(Object[].class));
        var revocation = new ReactiveOAuthRevocationReader(reactiveRedis, registry, properties);
        var secret = temporary.resolve("audit.key"); Files.writeString(secret, "0123456789abcdef0123456789abcdef");
        Files.setPosixFilePermissions(secret, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        var mapper = new ObjectMapper().findAndRegisterModules();
        var producer = new TokenUsageAuditComponent(redis, mapper, new OAuthAuditProperties(STREAM, temporary.resolve("spool"), 1_048_576, secret),
                new AuditDeliveryProperties(), clock, validator);
        var events = (TokenUsageAuditEventConverter) Class.forName(TokenUsageAuditEventConverter.class.getName() + "Impl").getConstructor().newInstance();
        var observer = new IdpObservedVerificationComponent(producer, events,
                new OAuthUsageObservationProperties(true, ISSUER, false, IdpSecurityObservationReceiverEnum.APPLICATION), observation -> { }, clock, validator, resource);
        IdentityResourceServerStateReader states = id -> {
            blockingPortOnEventThread.compareAndSet(false, Schedulers.isInNonBlockingThread());
            return Optional.of(new IdentityResourceServerState("provider-resource", resource, "biz", "provider", "test", ResourceServerStatus.ACTIVE, 7));
        };
        var converter = (VerifiedPrincipalConverter) Class.forName(VerifiedPrincipalConverter.class.getName() + "Impl").getConstructor().newInstance();
        var decoder = NimbusReactiveJwtDecoder.withPublicKey(key.toRSAPublicKey()).validateType(false).build();
        var timestamps = new JwtTimestampValidator(Duration.ZERO); timestamps.setClock(clock);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamps, new JwtIssuerValidator(ISSUER.toASCIIString())));
        context = new AnnotationConfigApplicationContext(); context.setEnvironment(new StandardReactiveWebEnvironment());
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("docsFixture", settings));
        context.getBeanFactory().registerSingleton("idpStarterProperties", properties);
        context.getBeanFactory().registerSingleton("tenantIssuerAuthenticationManagerComponent", managers);
        context.getBeanFactory().registerSingleton("identityResourceServerStateReader", states);
        context.getBeanFactory().registerSingleton("identityOAuthClientStateReader", mock(IdentityOAuthClientStateReader.class));
        context.getBeanFactory().registerSingleton("verifiedPrincipalConverter", converter);
        context.getBeanFactory().registerSingleton("reactiveOAuthRevocationReader", revocation);
        context.getBeanFactory().registerSingleton("idpClock", clock);
        context.getBeanFactory().registerSingleton("idpObservedVerificationComponent", observer);
        context.getBeanFactory().registerSingleton("jacksonObjectMapper", mapper);
        context.getBeanFactory().registerSingleton("reactiveJwtDecoder", decoder);
        context.getBeanFactory().registerSingleton("documentCalls", documents);
        context.register(GatewayOpenApiWebFluxSecurityAutoConfiguration.class, fixture); context.refresh();
        client = WebTestClient.bindToApplicationContext(context).configureClient().responseTimeout(Duration.ofSeconds(5)).build();
    }

    @AfterEach
    void cleanup() { if (context != null) { context.close(); } validator.close(); org.slf4j.MDC.clear(); }

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
        snapshot.set(Mono.just(List.of("R", "TOKEN_REVOKED")));
        assertRejected(token(RESOURCE, "at+jwt", "SERVICE", SCOPE), 401);
    }

    @Test
    void unavailableSecurityStateMustBe503WithoutReturningADocument() throws Exception {
        snapshot.set(Mono.error(new IllegalStateException("controlled state failure")));
        assertRejected(token(RESOURCE, "at+jwt", "SERVICE", SCOPE), 503);
    }

    @Test
    void validServiceStillNeedsTheExactDocumentScopeAndAConfirmedDenial() throws Exception {
        assertRejected(token(RESOURCE, "at+jwt", "SERVICE", "provider:read"), 403);
        assertThat(auditEvents.get()).isEqualTo(2);
    }

    @Test
    void validServiceWithTheExactScopePreservesTheDocumentResponse() throws Exception {
        client.get().uri("/v3/api-docs/fixture").header("Authorization", "Bearer " + token(RESOURCE, "at+jwt", "SERVICE", SCOPE))
                .exchange().expectStatus().isOk().expectBody().jsonPath("$.openapi").isEqualTo("3.1.0");
        assertThat(documents.get()).isOne(); assertThat(snapshots.get()).isOne(); assertThat(auditEvents.get()).isOne();
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
    void malformedOrMissingBearerCannotEnterTheDocumentHandler() {
        assertRejected("invalid", 401);
        client.get().uri("/v3/api-docs/fixture").exchange().expectStatus().isUnauthorized()
                .expectHeader().valueEquals("WWW-Authenticate", "Bearer");
        assertThat(documents.get()).isZero();
    }

    @Test
    void verificationStateAndAuditStayOffTheNonBlockingRequestThread() throws Exception {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/v3/api-docs/fixture")
                .header("Authorization", "Bearer " + token(RESOURCE, "at+jwt", "SERVICE", SCOPE)));
        context.getBean(WebFilterChainProxy.class).filter(exchange, current -> { documents.incrementAndGet(); return current.getResponse().setComplete(); })
                .subscribeOn(Schedulers.parallel()).block(Duration.ofSeconds(5));
        assertThat(documents.get()).isOne(); assertThat(snapshots.get()).isOne(); assertThat(auditEvents.get()).isOne();
        assertThat(blockingPortOnEventThread.get()).isFalse();
        assertThat(exchange.getAttributes().values()).noneMatch(IdpAuthorizationObservationContextBO.class::isInstance);
    }

    @Test
    void cancellationCannotInvokeAHandlerOrRetainRequestObservation() throws Exception {
        var subscribed = new CountDownLatch(1);
        var pending = Sinks.<List<Object>>one();
        snapshot.set(pending.asMono().doOnSubscribe(subscription -> subscribed.countDown()));
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/v3/api-docs/fixture")
                .header("Authorization", "Bearer " + token(RESOURCE, "at+jwt", "SERVICE", SCOPE)));
        var request = context.getBean(WebFilterChainProxy.class).filter(exchange, current -> { documents.incrementAndGet(); return current.getResponse().setComplete(); });
        var subscription = request.subscribeOn(Schedulers.parallel()).subscribe();
        try { assertThat(subscribed.await(2, TimeUnit.SECONDS)).isTrue(); }
        finally { subscription.dispose(); }
        assertThat(documents.get()).isZero();
        assertThat(exchange.getAttributes().values()).noneMatch(IdpAuthorizationObservationContextBO.class::isInstance);
        pending.tryEmitValue(List.of("V", "ACTIVE"));
    }

    @Test
    void cancellationDuringDenialDeliveryMustNotBlockTheEventThread() throws Exception {
        var entered = new CountDownLatch(1); var release = new java.util.concurrent.CompletableFuture<Void>(); var cancelled = new CountDownLatch(1);
        when(stream.add(any(StreamAddArgs.class))).thenAnswer(invocation -> {
            int count = auditEvents.incrementAndGet();
            if (count == 2) { entered.countDown(); release.join(); }
            return new StreamMessageId(1, count);
        });
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/v3/api-docs/fixture")
                .header("Authorization", "Bearer " + token(RESOURCE, "at+jwt", "SERVICE", "provider:read")));
        var subscription = context.getBean(WebFilterChainProxy.class).filter(exchange,
                current -> { documents.incrementAndGet(); return current.getResponse().setComplete(); })
                .subscribeOn(Schedulers.parallel()).subscribe();
        try {
            assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
            Schedulers.parallel().schedule(() -> { subscription.dispose(); cancelled.countDown(); });
            assertThat(cancelled.await(300, TimeUnit.MILLISECONDS)).isTrue();
            assertThat(exchange.getAttributes().values()).noneMatch(IdpAuthorizationObservationContextBO.class::isInstance);
            assertThat(documents.get()).isZero();
        } finally { release.complete(null); subscription.dispose(); }
    }

    @Test
    void narrowDocumentChainDoesNotChangeTheBusinessSecurityChain() {
        client.get().uri("/business/ping").exchange().expectStatus().isOk().expectBody().jsonPath("$.result").isEqualTo("business-original");
        assertThat(documents.get()).isZero(); assertThat(snapshots.get()).isZero(); assertThat(auditEvents.get()).isZero();
    }

    @Test
    void disabledDocumentationDoesNotCreateAChainOrAPublishedDocument() {
        var disabled = new AnnotationConfigApplicationContext(); disabled.setEnvironment(new StandardReactiveWebEnvironment());
        disabled.getEnvironment().getPropertySources().addFirst(new MapPropertySource("docsDisabled", Map.of("egon.cola.component.yuheng.openapi.enabled", false)));
        disabled.register(GatewayOpenApiWebFluxSecurityAutoConfiguration.class, FixtureConfiguration.class); disabled.refresh();
        try {
            assertThat(disabled.containsBean("gatewayOpenApiWebFluxSecurityWebFilterChain")).isFalse();
            assertThat(disabled.containsBean("documentFixture")).isFalse();
            var disabledClient = WebTestClient.bindToApplicationContext(disabled).build();
            disabledClient.get().uri("/v3/api-docs/fixture").exchange().expectStatus().isNotFound();
            disabledClient.get().uri("/business/ping").exchange().expectStatus().isOk();
        } finally { disabled.close(); }
    }

    private void assertRejected(String token, int status) {
        client.get().uri("/v3/api-docs/fixture").header("Authorization", "Bearer " + token).exchange()
                .expectStatus().isEqualTo(status).expectHeader().value("Cache-Control", value -> assertThat(value).contains("no-store"));
        assertThat(documents.get()).isZero();
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
    @Configuration(value = "docsFluxFixtureConfiguration", proxyBeanMethods = false)
    @EnableWebFluxSecurity
    @EnableWebFlux
    static class FixtureConfiguration {
        @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix = "egon.cola.component.yuheng.openapi", name = "enabled", havingValue = "true")
        @Bean("documentFixture") DocumentFixture documentFixture(@Qualifier("documentCalls") AtomicInteger documentCalls) { return new DocumentFixture(documentCalls); }
        @Bean("businessFixture") BusinessFixture businessFixture() { return new BusinessFixture(); }
        @Bean("fixtureBusinessSecurityChain")
        @Order(Ordered.LOWEST_PRECEDENCE)
        SecurityWebFilterChain businessChain(ServerHttpSecurity http) {
            return http.csrf(ServerHttpSecurity.CsrfSpec::disable).authorizeExchange(authorize -> authorize.anyExchange().permitAll()).build();
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
    void isConditionalReactiveAutoConfigurationForSpringdocAndSecurity() {
        Class<?> configuration = GatewayOpenApiWebFluxSecurityAutoConfiguration.class;
        assertThat(configuration.getAnnotation(AutoConfiguration.class)).isNotNull();
        assertThat(configuration.getAnnotation(ConditionalOnClass.class)).isNotNull();
        assertThat(configuration.getAnnotation(ConditionalOnWebApplication.class)).extracting(ConditionalOnWebApplication::type)
                .isEqualTo(ConditionalOnWebApplication.Type.REACTIVE);
        assertThat(configuration.getAnnotation(ConditionalOnClass.class).value())
                .contains(ServerHttpSecurity.class, SecurityWebFilterChain.class, OpenApiResource.class);
    }
}
