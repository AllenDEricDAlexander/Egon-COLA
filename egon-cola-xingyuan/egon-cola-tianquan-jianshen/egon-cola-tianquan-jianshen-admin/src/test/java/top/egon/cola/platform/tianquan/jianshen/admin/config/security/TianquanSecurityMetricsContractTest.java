package top.egon.cola.platform.tianquan.jianshen.admin.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.redisson.api.RStream;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.client.codec.StringCodec;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.client.RestTemplate;
import top.egon.cola.platform.tianquan.shoubing.core.audit.TokenUsageAuditEvent;
import top.egon.cola.platform.tianquan.shoubing.core.observability.SecurityMetricObservationBO;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.AuditDeliveryProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthAuditProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TenantIssuerAuthenticationManagerComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry;

/** Actual SDK emitters and host registry wiring, with controlled infrastructure and time. */
class TianquanSecurityMetricsContractTest {
    private static final String RECEIVER = "JIANSHEN";
    private static final URI ISSUER = URI.create("https://idp.example/tenants/1001");
    private static final URI RESOURCE = URI.create("https://api.example/orders");
    private static final Instant NOW = Instant.parse("2026-10-10T00:00:00Z");
    private static final String STREAM = "egon:oauth:audit:v1";
    @TempDir Path temporary;
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    private TianquanSecurityMetricsComponent metrics;

    @BeforeEach void prepare() { validator.afterPropertiesSet(); metrics = new TianquanSecurityMetricsComponent(() -> registry, validator); }

    @Test
    void realCryptoAndJwkCacheProduceTheHostMetersWithoutIdentityTags() throws Exception {
        var properties = new IdpStarterProperties();
        properties.setResourceServerId("orders-resource"); properties.setResourceUri(RESOURCE);
        properties.getRevocation().setPrimaryEndpoint("rediss://redis.example:6379"); properties.getRevocation().setEpoch("epoch-1");
        var trusted = new IdpStarterProperties.TrustedIssuerSettings();
        trusted.setIssuer(ISSUER.toASCIIString()); trusted.setJwksUri(ISSUER + "/.well-known/jwks.json");
        trusted.setRealmKey("tenant:1001"); trusted.setRealmType("TENANT"); trusted.setTenantId("1001"); trusted.setStatus("ACTIVE"); trusted.setTrustVersion(1);
        properties.setTrustedIssuers(Map.of("tenant-1001", trusted));
        var managers = new TenantIssuerAuthenticationManagerComponent(new TrustedIssuerRegistry(properties), properties,
                Clock.fixed(NOW, ZoneOffset.UTC), metrics);
        managers.initialize();
        var field = TenantIssuerAuthenticationManagerComponent.class.getDeclaredField("jwksRestOperations"); field.setAccessible(true);
        var server = MockRestServiceServer.bindTo((RestTemplate) field.get(managers)).build();
        var key = new RSAKeyGenerator(2048).keyID("key-1").algorithm(JWSAlgorithm.RS256).generate();
        server.expect(ExpectedCount.once(), MockRestRequestMatchers.requestTo(ISSUER + "/.well-known/jwks.json"))
                .andRespond(MockRestResponseCreators.withSuccess(new JWKSet(key.toPublicJWK()).toString(), MediaType.APPLICATION_JSON));
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", ISSUER.toASCIIString()); claims.put("sub", "subject-1001"); claims.put("aud", RESOURCE.toASCIIString());
        claims.put("iat", NOW.minusSeconds(1).getEpochSecond()); claims.put("nbf", NOW.minusSeconds(1).getEpochSecond());
        claims.put("exp", NOW.plusSeconds(300).getEpochSecond()); claims.put("jti", "901"); claims.put("tenant_id", "1001");
        claims.put("principal_type", "USER"); claims.put("scope_context", "TENANT");
        claims.put("authorization_id", "801"); claims.put("client_registration_id", "501");
        claims.put("security_version", Map.of("realm", 1L, "client", 2L, "subject", 3L));
        var token = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("key-1").type(new JOSEObjectType("at+jwt")).build(), JWTClaimsSet.parse(claims));
        token.sign(new RSASSASigner(key));
        managers.authenticate(token.serialize()); managers.authenticate(token.serialize());
        assertThat(registry.find("jwt.verify.duration").tags("receiver", RECEIVER, "result", "SUCCESS").timer()).isNotNull();
        assertThat(registry.get("jwt.verify.duration").tags("receiver", RECEIVER, "result", "SUCCESS").timer().count()).isEqualTo(2);
        assertThat(registry.get("jwks.cache.access").tags("receiver", RECEIVER, "result", "MISS").counter().count()).isGreaterThan(0);
        assertThat(registry.getMeters()).allSatisfy(meter -> assertThat(meter.getId().getTags())
                .allSatisfy(tag -> assertThat(tag.getKey()).isIn("receiver", "result")));
        server.verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    void realDurableHandoffAndReceiptEmitDeliveryAgeAndSpoolMeters() throws Exception {
        var redis = mock(RedissonClient.class);
        RStream<String, String> stream = mock(RStream.class); RBucket<String> receipt = mock(RBucket.class);
        doReturn(stream).when(redis).getStream(eq(STREAM), eq(StringCodec.INSTANCE));
        doReturn(receipt).when(redis).getBucket(anyString(), eq(StringCodec.INSTANCE));
        when(stream.add(any(StreamAddArgs.class))).thenReturn(new StreamMessageId(1, 1));
        var producer = producer(redis, metrics);
        var event = new TokenUsageAuditEvent("metric-event", 1, ISSUER, RESOURCE.toASCIIString(), null, null, null,
                "SUCCESS", "TOKEN_USE_VERIFIED", NOW.minusSeconds(5), null);
        producer.appendStrict(event);
        assertThat(registry.find("oauth.audit.delivery").tags("receiver", RECEIVER, "result", "SENT").counter()).isNotNull();
        assertThat(registry.get("oauth.audit.delivery").tags("receiver", RECEIVER, "result", "SENT").counter().count()).isEqualTo(1);
        assertThat(registry.get("spool.bytes").tags("receiver", RECEIVER, "result", "CURRENT").gauge().value()).isGreaterThan(0);
        when(receipt.get()).thenReturn(producer.encodeReceipt(producer.createReceipt(event, "STORED", NOW.minusSeconds(2))));
        producer.acknowledge(event.eventId());
        assertThat(registry.get("receipt.age").tags("receiver", RECEIVER, "result", "CONFIRMED").timer().count()).isEqualTo(1);
        assertThat(registry.get("spool.bytes").tags("receiver", RECEIVER, "result", "CURRENT").gauge().value()).isZero();
    }

    @Test
    void rejectsUnsupportedMetricSchemaRatherThanRegisteringAnArbitraryMeter() {
        assertThatThrownBy(() -> metrics.record(SecurityMetricObservationBO.builder().signal("caller.unbounded")
                .result("tenant-1001").duration(Duration.ofNanos(-1)).value(-1).build()))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    void enabledHostRequiresARegistryAndUsesTheExistingPrimaryWithoutCreatingAnother() throws Exception {
        new ApplicationContextRunner().withUserConfiguration(TianquanSecurityObservationConfiguration.class)
                .withPropertyValues("egon.cola.platform.tianquan.shoubing.usage-observation.enabled=true")
                .withBean("defaultValidator", jakarta.validation.Validator.class, () -> validator)
                .run(context -> assertThat(context).hasFailed());
        var secondary = new SimpleMeterRegistry();
        var props = new OAuthUsageObservationProperties(true, ISSUER, false);
        var producer = producer(mock(RedissonClient.class), metrics);
        new ApplicationContextRunner().withUserConfiguration(TianquanSecurityObservationConfiguration.class)
                .withPropertyValues("egon.cola.platform.tianquan.shoubing.usage-observation.enabled=true")
                .withBean("primary", MeterRegistry.class, () -> registry, definition -> definition.setPrimary(true))
                .withBean("secondary", MeterRegistry.class, () -> secondary)
                .withBean("defaultValidator", jakarta.validation.Validator.class, () -> validator)
                .withBean("tokenUsageAuditComponent", TokenUsageAuditComponent.class, () -> producer)
                .withBean("oauthUsageObservationProperties", OAuthUsageObservationProperties.class, () -> props)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBeansOfType(MeterRegistry.class)).hasSize(2);
                    assertThat(context).hasSingleBean(TokenUsageAuditComponent.class);
                    @SuppressWarnings("unchecked") java.util.function.Supplier<MeterRegistry> selected =
                            (java.util.function.Supplier<MeterRegistry>) context.getBean("idpMeterRegistrySupplier");
                    assertThat(selected.get()).isSameAs(registry);
                });
    }

    @Test
    void fixedStrategyTableHandlesAllNineCanonicalSignals() {
        for (String signal : java.util.List.of("jwt.verify.duration", "token.acquire.duration", "receipt.age")) {
            metrics.record(SecurityMetricObservationBO.builder().signal(signal)
                    .result(signal.equals("receipt.age") ? "CONFIRMED" : "SUCCESS").duration(Duration.ofMillis(1)).build());
        }
        for (String signal : java.util.List.of("jwks.cache.access", "token.cache.access")) {
            metrics.record(SecurityMetricObservationBO.builder().signal(signal).result("HIT").build());
        }
        metrics.record(SecurityMetricObservationBO.builder().signal("oauth.authentication").result("VALID").build());
        metrics.record(SecurityMetricObservationBO.builder().signal("rbac.authorization").result("ALLOW").build());
        metrics.record(SecurityMetricObservationBO.builder().signal("oauth.audit.delivery").result("SENT").build());
        metrics.record(SecurityMetricObservationBO.builder().signal("spool.bytes").result("CURRENT").value(123).build());
        assertThat(registry.getMeters()).hasSize(9).allSatisfy(meter ->
                assertThat(meter.getId().getTag("receiver")).isEqualTo(RECEIVER));
    }

    @Test
    void disabledHostDoesNotRequireDeploymentInputsOrCreateAMetricsRegistry() {
        new ApplicationContextRunner().withUserConfiguration(TianquanSecurityObservationConfiguration.class)
                .withPropertyValues("egon.cola.platform.tianquan.shoubing.usage-observation.enabled=false")
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(MeterRegistry.class)
                        .doesNotHaveBean("idpSecurityMetricsRecorder"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void deadReceiptRetainsCapacityAndDoesNotRepeatedlyCountTheSameConfirmation() throws Exception {
        var redis = mock(RedissonClient.class);
        RStream<String, String> stream = mock(RStream.class); RBucket<String> receipt = mock(RBucket.class);
        doReturn(stream).when(redis).getStream(eq(STREAM), eq(StringCodec.INSTANCE));
        doReturn(receipt).when(redis).getBucket(anyString(), eq(StringCodec.INSTANCE));
        when(stream.add(any(StreamAddArgs.class))).thenReturn(new StreamMessageId(2, 1));
        var producer = producer(redis, metrics);
        var event = new TokenUsageAuditEvent("dead-metric", 1, ISSUER, RESOURCE.toASCIIString(), null, null, null,
                "SUCCESS", "TOKEN_USE_VERIFIED", NOW, null);
        producer.appendStrict(event);
        when(receipt.get()).thenReturn(producer.encodeReceipt(producer.createReceipt(event, "DEAD", NOW)));
        producer.acknowledge(event.eventId()); producer.acknowledge(event.eventId());
        assertThat(registry.get("oauth.audit.delivery").tags("receiver", RECEIVER, "result", "DEAD").counter().count()).isEqualTo(1);
        assertThat(registry.get("receipt.age").tags("receiver", RECEIVER, "result", "CONFIRMED").timer().count()).isEqualTo(1);
        assertThat(registry.get("spool.bytes").tags("receiver", RECEIVER, "result", "CURRENT").gauge().value()).isGreaterThan(0);
        assertThat(Files.exists(temporary.resolve("spool/dead-metric.evt"))).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void failedMetricRecordingNeverChangesTheConfirmedStrictHandoff() throws Exception {
        var redis = mock(RedissonClient.class);
        RStream<String, String> stream = mock(RStream.class);
        doReturn(stream).when(redis).getStream(eq(STREAM), eq(StringCodec.INSTANCE));
        when(stream.add(any(StreamAddArgs.class))).thenReturn(new StreamMessageId(3, 1));
        var producer = producer(redis, sample -> { throw new IllegalStateException("metrics unavailable"); });
        var event = new TokenUsageAuditEvent("metric-failure", 1, ISSUER, RESOURCE.toASCIIString(), null, null, null,
                "SUCCESS", "TOKEN_USE_VERIFIED", NOW, null);
        assertThat(producer.appendStrict(event)).isEqualTo(new StreamMessageId(3, 1));
        assertThat(Files.exists(temporary.resolve("spool/metric-failure.evt"))).isTrue();
        assertThat(Files.exists(temporary.resolve("spool/metric-failure.evt.sent"))).isTrue();
    }

    @Test
    void observedRealPdpPreservesResultsAndAuditsOnlyTokenBoundDenials() throws Exception {
        var identity = new top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal("identity-1", "10001", "901",
                Set.of(RESOURCE.toASCIIString()), NOW.minusSeconds(1), NOW.plusSeconds(300),
                top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext.password());
        var snapshot = new top.egon.cola.platform.tianquan.jianshen.contract.authorization.SystemAuthorizationSnapshot(
                "10001", "identity-1", "20001", "finance", 1, 2, java.util.List.of("50001"), null, null,
                Set.of("finance:payment:read"), Set.of("finance.api.payment.read"), Map.of(), Map.of(),
                "sha256:snapshot", NOW, NOW.plusSeconds(300));
        var context = new top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService.RuntimeAuthorizationContext(identity, snapshot, false);
        var delegate = new top.egon.cola.platform.tianquan.jianshen.starter.authorization.DefaultAuthorizationService(() -> context,
                request -> top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService.OperationSodResult.allowed(),
                request -> top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService.FenceResult.allowed(NOW),
                Clock.fixed(NOW, ZoneOffset.UTC));
        var redis = mock(RedissonClient.class);
        @SuppressWarnings("unchecked") RStream<String, String> stream = mock(RStream.class);
        doReturn(stream).when(redis).getStream(eq(STREAM), eq(StringCodec.INSTANCE));
        java.util.List<Map<String, String>> events = new java.util.ArrayList<>();
        when(stream.add(any(StreamAddArgs.class))).thenAnswer(call -> {
            org.redisson.api.stream.StreamAddParams<String, String> fields = call.getArgument(0); events.add(Map.copyOf(fields.getEntries()));
            return new StreamMessageId(events.size(), 1);
        });
        var converter = (top.egon.cola.platform.tianquan.shoubing.starter.security.domain.converter.TokenUsageAuditEventConverter)
                Class.forName("top.egon.cola.platform.tianquan.shoubing.starter.security.domain.converter.TokenUsageAuditEventConverterImpl").getConstructor().newInstance();
        var observer = new top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent(producer(redis, metrics),
                converter, new OAuthUsageObservationProperties(true, ISSUER, false), metrics, Clock.fixed(NOW, ZoneOffset.UTC), validator, RESOURCE);
        var observed = new top.egon.cola.platform.tianquan.jianshen.starter.authorization.Rbac3ObservedAuthorizationService(delegate, metrics, observer, validator);
        var request = top.egon.cola.platform.tianquan.jianshen.contract.authorization.PermissionRequest.of("finance:payment:approve");
        assertThat(observed.requirePermission(request)).isEqualTo(delegate.requirePermission(request));
        assertThat(events).isEmpty();
        var read = new top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService.DataScopeRequest("finance:payment:read");
        assertThat(observed.decideDataScope(read)).isEqualTo(delegate.decideDataScope(read));
        var fields = new top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService.FieldPolicyRequest("finance:payment:read", "finance", "payment");
        assertThat(observed.decideFields(fields)).isEqualTo(delegate.decideFields(fields));
        var operation = new top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService.OperationSodRequest(
                "finance:payment:read", "finance", "payment", "payment-1", "READ");
        assertThat(observed.checkParticipation(operation)).isEqualTo(delegate.checkParticipation(operation));
        var fence = new top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService.AuthorizationFenceRequest(
                "finance:payment:read", "payment", "payment-1", "pdp-trace");
        assertThat(observed.verifyFence(fence)).isEqualTo(delegate.verifyFence(fence));
        assertThat(events).isEmpty();
        assertThat(registry.find("rbac.authorization").tags("receiver", RECEIVER, "result", "DENY").counter()).isNotNull();
        var authentication = new top.egon.cola.platform.tianquan.shoubing.starter.security.IdpAuthenticationToken(identity);
        authentication.setDetails(new top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditProofBO(
                ISSUER, RESOURCE, "a".repeat(64), "b".repeat(64), "c".repeat(64)));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);
        org.slf4j.MDC.put("traceId", "pdp-trace");
        try {
            assertThat(observed.requirePermission(request)).isEqualTo(delegate.requirePermission(request));
            assertThat(events).hasSize(1);
            assertThat(events.getFirst()).containsEntry("reasonCode", "AUTHORIZATION_DENIED").containsEntry("traceId", "pdp-trace")
                    .containsEntry("tokenRef", "c".repeat(64));
            var servlet = new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/test");
            var response = new org.springframework.mock.web.MockHttpServletResponse();
            var policy = new top.egon.cola.platform.tianquan.shoubing.starter.security.IdpEndpointAuthenticationPolicy(java.util.List.of(), java.util.List.of());
            var filter = new top.egon.cola.platform.tianquan.shoubing.starter.security.IdpSecurityObservationFilter(observer, policy);
            org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
                    new org.springframework.web.context.request.ServletRequestAttributes(servlet));
            try {
                filter.doFilter(servlet, response, (incoming, outgoing) -> {
                    observed.requirePermission(request);
                    var rendered = new top.egon.cola.platform.tianquan.jianshen.starter.web.Rbac3AuthorizationExceptionHandler()
                            .fieldDenied(new org.springframework.security.authorization.AuthorizationDeniedException("FIELD_ACCESS_DENIED"));
                    assertThat(rendered.getStatusCode().value()).isEqualTo(403);
                });
                assertThat(events).hasSize(2);
                assertThat(events.get(0).get("eventId")).isNotEqualTo(events.get(1).get("eventId"));
                assertThat(events.get(1)).containsEntry("traceId", "pdp-trace");
            } finally { org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes(); }
            when(stream.add(any(StreamAddArgs.class))).thenThrow(new org.redisson.client.RedisException("strict audit offline"));
            assertThatThrownBy(() -> observed.requirePermission(request))
                    .isInstanceOf(top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService.RuntimeUnavailableException.class)
                    .hasMessage("OAUTH_AUDIT_DELIVERY_UNAVAILABLE");
        } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); org.slf4j.MDC.clear(); }
    }

    @Test
    void observerPreservesTheOriginalExceptionWhenMetricsFail() {
        var delegate = mock(top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService.class);
        var observer = mock(top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent.class);
        var identity = new top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal("identity-1", "1001", "901",
                Set.of(RESOURCE.toASCIIString()), NOW, NOW.plusSeconds(300),
                top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext.password());
        var failure = new top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService.RuntimeUnavailableException(
                "AUTHORIZATION_RUNTIME_UNAVAILABLE", identity);
        var request = top.egon.cola.platform.tianquan.jianshen.contract.authorization.PermissionRequest.of("finance:payment:read");
        when(delegate.requirePermission(request)).thenThrow(failure);
        var observed = new top.egon.cola.platform.tianquan.jianshen.starter.authorization.Rbac3ObservedAuthorizationService(delegate,
                sample -> { throw new IllegalStateException("metrics offline"); }, observer, validator);
        assertThatThrownBy(() -> observed.requirePermission(request)).isSameAs(failure);
        org.mockito.Mockito.verify(delegate, org.mockito.Mockito.times(1)).requirePermission(request);
        org.mockito.Mockito.verifyNoInteractions(observer);
    }

    @Test
    void actualStarterSelectsObservedDefaultAndRetainsConsumerOverride() {
        var runner = new ApplicationContextRunner().withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterAutoConfiguration.class))
                .withPropertyValues("egon.cola.platform.tianquan.jianshen.enabled=true")
                .withBean("defaultValidator", jakarta.validation.Validator.class, () -> validator)
                .withBean("oauthUsageObservationProperties", OAuthUsageObservationProperties.class,
                        () -> new OAuthUsageObservationProperties(true, ISSUER, false))
                .withBean("idpSecurityMetricsRecorder", top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder.class, () -> metrics)
                .withBean("idpObservedVerificationComponent", top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent.class,
                        () -> mock(top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent.class));
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService.class))
                    .isInstanceOf(top.egon.cola.platform.tianquan.jianshen.starter.authorization.Rbac3ObservedAuthorizationService.class);
            assertThat(context.getBean("rbac3AuthorizationDelegate"))
                    .isInstanceOf(top.egon.cola.platform.tianquan.jianshen.starter.authorization.DefaultAuthorizationService.class);
        });
        var custom = mock(top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService.class);
        runner.withBean("consumerAuthorization", top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService.class)).isSameAs(custom);
                    assertThat(context).doesNotHaveBean("rbac3AuthorizationDelegate");
                });
    }

    private TokenUsageAuditComponent producer(RedissonClient redis, top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder recorder) throws Exception {
        Path key = temporary.resolve("audit.key"); Files.writeString(key, "0123456789abcdef0123456789abcdef");
        Files.setPosixFilePermissions(key, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        return new TokenUsageAuditComponent(redis, new ObjectMapper().findAndRegisterModules(),
                new OAuthAuditProperties(STREAM, temporary.resolve("spool"), 1_048_576, key), new AuditDeliveryProperties(),
                Clock.fixed(NOW, ZoneOffset.UTC), validator, recorder);
    }
}
