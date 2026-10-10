package top.egon.cola.platform.tianquan.shoubing.starter.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.api.stream.StreamAddParams;
import org.redisson.client.codec.StringCodec;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.client.RestTemplate;
import top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.AuditDeliveryProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthAuditProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.converter.TokenUsageAuditEventConverter;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityOAuthClientStateReader;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerState;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerStateReader;

/** Real local JWT validation, generated projection and durable producer; infrastructure stays controlled. */
class IdpSecurityObservationFilterTest {
    private static final URI ISSUER = URI.create("https://idp.example/tenants/1001");
    private static final URI RESOURCE = URI.create("https://api.example/orders");
    private static final Instant NOW = Instant.parse("2026-10-10T00:00:00Z");
    private static final String STREAM = "egon:oauth:audit:v1";
    @TempDir Path temporary;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    private final List<Map<String, String>> events = new ArrayList<>();
    private final RedissonClient redis = mock(RedissonClient.class);
    @SuppressWarnings("unchecked") private final RStream<String, String> stream = mock(RStream.class);
    private TokenUsageAuditComponent producer;
    private IdpObservedVerificationComponent observations;
    private IdpJwtVerifier verifier;
    private MockRestServiceServer jwks;
    private RSAKey key;
    private OAuthUsageObservationProperties usageProperties;
    private TokenUsageAuditEventConverter eventConverter;

    @BeforeEach
    void prepare() throws Exception {
        validator.afterPropertiesSet();
        Path keyFile = temporary.resolve("audit.key");
        Files.writeString(keyFile, "0123456789abcdef0123456789abcdef");
        Files.setPosixFilePermissions(keyFile, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        doReturn(stream).when(redis).getStream(eq(STREAM), eq(StringCodec.INSTANCE));
        when(stream.add(any(StreamAddArgs.class))).thenAnswer(call -> {
            StreamAddParams<String, String> fields = call.getArgument(0);
            events.add(Map.copyOf(fields.getEntries()));
            return new StreamMessageId(events.size(), 1);
        });
        producer = new TokenUsageAuditComponent(redis, mapper,
                new OAuthAuditProperties(STREAM, temporary.resolve("spool"), 1_048_576, keyFile),
                new AuditDeliveryProperties(), clock, validator);
        eventConverter = (TokenUsageAuditEventConverter) Class.forName(TokenUsageAuditEventConverter.class.getName() + "Impl")
                .getConstructor().newInstance();
        usageProperties = new OAuthUsageObservationProperties(true, ISSUER, false);
        observations = new IdpObservedVerificationComponent(producer, eventConverter,
                usageProperties, IdpSecurityMetricsRecorder.NOOP,
                clock, validator, RESOURCE);
        var properties = new IdpStarterProperties();
        properties.setResourceServerId("orders-resource");
        properties.setResourceUri(RESOURCE);
        properties.getRevocation().setPrimaryEndpoint("rediss://redis.example:6379");
        properties.getRevocation().setEpoch("epoch-1");
        var trusted = new IdpStarterProperties.TrustedIssuerSettings();
        trusted.setIssuer(ISSUER.toASCIIString());
        trusted.setJwksUri(ISSUER + "/.well-known/jwks.json");
        trusted.setRealmKey("tenant:1001");
        trusted.setRealmType("TENANT");
        trusted.setTenantId("1001");
        trusted.setStatus("ACTIVE");
        trusted.setTrustVersion(1);
        properties.setTrustedIssuers(Map.of("tenant-1001", trusted));
        var managers = new TenantIssuerAuthenticationManagerComponent(new TrustedIssuerRegistry(properties), properties, clock);
        managers.initialize();
        var rest = TenantIssuerAuthenticationManagerComponent.class.getDeclaredField("jwksRestOperations");
        rest.setAccessible(true);
        jwks = MockRestServiceServer.bindTo((RestTemplate) rest.get(managers)).build();
        key = new RSAKeyGenerator(2048).keyID("same-kid").algorithm(JWSAlgorithm.RS256).generate();
        jwks.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(ISSUER + "/.well-known/jwks.json"))
                .andRespond(MockRestResponseCreators.withSuccess(new JWKSet(key.toPublicJWK()).toString(), MediaType.APPLICATION_JSON));
        var revocations = mock(OAuthRevocationReader.class);
        when(revocations.check(any())).thenReturn(OAuthRevocationReader.Result.valid());
        var resourceStates = mock(IdentityResourceServerStateReader.class);
        when(resourceStates.read("orders-resource")).thenReturn(Optional.of(new IdentityResourceServerState(
                "orders-resource", RESOURCE, "biz", "orders", "test", ResourceServerStatus.ACTIVE, 7)));
        var principalConverter = (VerifiedPrincipalConverter) Class.forName(VerifiedPrincipalConverter.class.getName() + "Impl")
                .getConstructor().newInstance();
        verifier = IdpJwtVerifier.createConfigured(null, resourceStates, mock(IdentityOAuthClientStateReader.class),
                "orders-resource", RESOURCE, "platform-audience", clock, managers, revocations, principalConverter, null, observations);
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); org.slf4j.MDC.clear(); validator.close(); }

    @Test
    void httpUserAndServiceHandoffBeforeTheBusinessChainWithSecretFreeProof() throws Exception {
        var policy = new IdpEndpointAuthenticationPolicy(List.of("/public/**"), List.of("/service/**"));
        var bearer = IdpBearerAuthenticationFilter.create(new UserAccessTokenVerifier(verifier), new ServiceAccessTokenVerifier(verifier),
                policy, mapper, new VerifiedTenantContextFilter(), verifier);
        for (boolean service : List.of(false, true)) {
            String token = token(service);
            var request = new MockHttpServletRequest("GET", service ? "/service/orders" : "/user/orders");
            request.addHeader("Authorization", "Bearer " + token);
            var response = new MockHttpServletResponse();
            int expected = events.size() + 1;
            bearer.doFilter(request, response, (incoming, outgoing) -> {
                assertThat(events).as("strict handoff must precede the business chain").hasSize(expected);
                assertThat(SecurityContextHolder.getContext().getAuthentication().getDetails())
                        .isInstanceOf(top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditProofBO.class);
                assertThat(SecurityContextHolder.getContext().getAuthentication().getCredentials()).isEqualTo("");
            });
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(mapper.writeValueAsString(events)).doesNotContain(token, "subject-1001", "orders-service");
        }
        assertThat(events).hasSize(2).allSatisfy(event -> {
            assertThat(event.get("reasonCode")).isEqualTo("TOKEN_USE_VERIFIED");
            assertThat(event.get("tokenRef")).matches("[0-9a-f]{64}");
        });
        jwks.verify();
    }

    @Test
    void invalidTokenHandoffIsAnonymousAndUsesTheConfiguredReceiver() {
        assertThat(verifier.verifyAny("unverified.jwt.input")).isInstanceOf(AccessTokenVerification.Invalid.class);
        assertThat(events).hasSize(1);
        assertThat(events.getFirst()).containsEntry("issuer", ISSUER.toASCIIString()).containsEntry("result", "FAILED")
                .containsEntry("reasonCode", "AUTHENTICATION_FAILED")
                .doesNotContainKeys("subjectRef", "clientRef", "tokenRef");
    }

    @Test
    void referenceUsesTheExistingKeyAndDoesNotExposeItsInput() throws Exception {
        var mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec("0123456789abcdef0123456789abcdef".getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
        String expected = java.util.HexFormat.of().formatHex(mac.doFinal((ISSUER + "\n901").getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertThat(producer.reference(ISSUER, "901")).isEqualTo(expected);
    }

    @Test
    void deliveryFailureMapsTo503WithoutCallingBusinessAndKeepsTheDurableFrame() throws Exception {
        when(stream.add(any(StreamAddArgs.class))).thenThrow(new org.redisson.client.RedisException("broker unavailable"));
        var request = new MockHttpServletRequest("GET", "/user/orders");
        request.addHeader("Authorization", "Bearer " + token(false));
        var response = new MockHttpServletResponse();
        new IdpBearerAuthenticationFilter(verifier, mapper).doFilter(request, response,
                (incoming, outgoing) -> { throw new AssertionError("business must not run"); });
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getContentAsString()).contains("OAUTH_AUDIT_DELIVERY_UNAVAILABLE").doesNotContain("broker unavailable");
        try (var frames = Files.list(temporary.resolve("spool"))) {
            assertThat(frames.filter(path -> path.toString().endsWith(".evt")).count()).isEqualTo(1);
        }
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void missingAndRejectedPartialCaptureNeverPublishIdentityReferences() throws Exception {
        String token = token(false);
        var validWithoutCapture = observations.observe(capture -> verifier.verifyAny(token, ignored -> { }));
        assertThat(validWithoutCapture).isInstanceOf(AccessTokenVerification.Unavailable.class);
        var captured = new java.util.concurrent.atomic.AtomicReference<top.egon.cola.platform.tianquan.shoubing.starter.security.domain.VerifiedJwtBO>();
        verifier.verifyAny(token, captured::set);
        assertThat(captured.get()).isNotNull();
        var rejected = observations.observe(capture -> {
            capture.accept(captured.get());
            return new AccessTokenVerification.Invalid<>("JWT_AUDIENCE_INVALID");
        });
        assertThat(rejected).isInstanceOf(AccessTokenVerification.Invalid.class);
        assertThat(events).hasSize(2).allSatisfy(event -> assertThat(event)
                .containsEntry("issuer", ISSUER.toASCIIString()).containsEntry("result", "FAILED")
                .doesNotContainKeys("subjectRef", "clientRef", "tokenRef"));
    }

    @Test
    void malformedHeadersAreAuditedBeforeRenderingAndFailureReplaces401With503() throws Exception {
        var policy = new IdpEndpointAuthenticationPolicy();
        var observationFilter = new IdpSecurityObservationFilter(observations, policy);
        var bearer = new IdpBearerAuthenticationFilter(verifier, mapper);
        for (boolean unavailable : List.of(false, true)) {
            if (unavailable) { when(stream.add(any(StreamAddArgs.class))).thenThrow(new org.redisson.client.RedisException("offline")); }
            var request = new MockHttpServletRequest("GET", "/user/orders");
            request.addHeader("Authorization", "Basic invalid-credential");
            var response = new MockHttpServletResponse();
            observationFilter.doFilter(request, response, (incoming, outgoing) -> bearer.doFilter(incoming, outgoing,
                    (businessRequest, businessResponse) -> { throw new AssertionError("business must not run"); }));
            assertThat(response.getStatus()).isEqualTo(unavailable ? 503 : 401);
            assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
            assertThat(IdpSecurityObservationFilter.beforeAuthenticationFailure(request)).isTrue();
        }
        assertThat(events).hasSize(1);
        assertThat(events.getFirst()).doesNotContainKeys("subjectRef", "clientRef", "tokenRef");
    }

    @Test
    void publicPreflightDisabledAndBusiness500DoNotGenerateFalseJwtFailures() throws Exception {
        var policy = new IdpEndpointAuthenticationPolicy(List.of("/public/**"), List.of());
        var filter = new IdpSecurityObservationFilter(observations, policy);
        var publicRequest = new MockHttpServletRequest("GET", "/public/health");
        filter.doFilter(publicRequest, new MockHttpServletResponse(), (incoming, outgoing) ->
                assertThat(IdpSecurityObservationFilter.beforeAuthenticationFailure(publicRequest)).isTrue());
        var preflight = new MockHttpServletRequest("OPTIONS", "/user/orders");
        preflight.addHeader("Origin", "https://browser.example");
        preflight.addHeader("Access-Control-Request-Method", "GET");
        filter.doFilter(preflight, new MockHttpServletResponse(), (incoming, outgoing) ->
                assertThat(IdpSecurityObservationFilter.beforeAuthenticationFailure(preflight)).isTrue());
        usageProperties.setEnabled(false);
        verifier.verifyUser(token(false));
        assertThat(events).isEmpty();
        usageProperties.setEnabled(true);
        org.slf4j.MDC.put("traceId", "previous-trace");
        var request = new MockHttpServletRequest("GET", "/user/orders");
        request.addHeader("Authorization", "Bearer " + token(false));
        var bearer = new IdpBearerAuthenticationFilter(verifier, mapper);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(),
                (incoming, outgoing) -> bearer.doFilter(incoming, outgoing, (businessRequest, businessResponse) -> {
                    throw new IllegalStateException("business fixture");
                }))).hasMessage("business fixture");
        assertThat(events).hasSize(1);
        assertThat(events.getFirst()).containsEntry("reasonCode", "TOKEN_USE_VERIFIED").containsEntry("traceId", "previous-trace");
        assertThat(org.slf4j.MDC.get("traceId")).isEqualTo("previous-trace");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        org.slf4j.MDC.clear();
    }

    @Test
    @SuppressWarnings("unchecked")
    void realRpcVerificationHandsOffBeforeCallbacksAndRestoresProofOnCancel() throws Exception {
        String token = token(false);
        var headers = new io.grpc.Metadata();
        headers.put(top.egon.cola.component.rpc.context.invocation.RpcMetadataKeys.AUTHORIZATION, "Bearer " + token);
        io.grpc.ServerCall<String, String> call = mock(io.grpc.ServerCall.class);
        var previous = SecurityContextHolder.createEmptyContext();
        previous.setAuthentication(new org.springframework.security.authentication.TestingAuthenticationToken("previous", ""));
        SecurityContextHolder.setContext(previous);
        var callbacks = new java.util.concurrent.atomic.AtomicInteger();
        io.grpc.ServerCallHandler<String, String> handler = (incoming, metadata) -> {
            assertThat(events).hasSize(1);
            assertThat(top.egon.cola.platform.tianquan.shoubing.starter.security.rpc.IdpRpcSecurityContext.currentProofOrNull()).isNotNull();
            return new io.grpc.ServerCall.Listener<>() {
                private void check() {
                    callbacks.incrementAndGet();
                    assertThat(SecurityContextHolder.getContext().getAuthentication().getDetails())
                            .isSameAs(top.egon.cola.platform.tianquan.shoubing.starter.security.rpc.IdpRpcSecurityContext.currentProofOrNull());
                    assertThat(SecurityContextHolder.getContext().getAuthentication().getCredentials()).isEqualTo("");
                    assertThat(VerifiedUserTokenCarrier.currentOrNull()).isEqualTo(token);
                }
                @Override public void onMessage(String message) { check(); }
                @Override public void onHalfClose() { check(); }
                @Override public void onReady() { check(); }
                @Override public void onComplete() { check(); }
                @Override public void onCancel() { check(); throw new IllegalStateException("cancel fixture"); }
            };
        };
        var listener = new top.egon.cola.platform.tianquan.shoubing.starter.security.rpc.IdpRpcBearerServerInterceptor(
                new UserAccessTokenVerifier(verifier)).interceptCall(call, headers, handler);
        listener.onMessage("message"); listener.onHalfClose(); listener.onReady(); listener.onComplete();
        org.assertj.core.api.Assertions.assertThatThrownBy(listener::onCancel).hasMessage("cancel fixture");
        assertThat(callbacks.get()).isEqualTo(5);
        assertThat(events).hasSize(1);
        assertThat(SecurityContextHolder.getContext()).isSameAs(previous);
        assertThat(top.egon.cola.platform.tianquan.shoubing.starter.security.rpc.IdpRpcSecurityContext.currentProofOrNull()).isNull();
        assertThat(VerifiedUserTokenCarrier.currentOrNull()).isNull();
    }

    @Test
    void proofAndRawVerifiedJwtAreExcludedFromPublishedJsonAndContextLogging() throws Exception {
        String token = token(false);
        var valid = (AccessTokenVerification.Valid<?>) verifier.verifyAny(token);
        var json = mapper.readTree(mapper.writeValueAsString(valid));
        assertThat(json.size()).isEqualTo(2);
        assertThat(json.has("proof")).isFalse();
        assertThat(valid.proof()).isNotNull();
        var captured = new java.util.concurrent.atomic.AtomicReference<top.egon.cola.platform.tianquan.shoubing.starter.security.domain.VerifiedJwtBO>();
        verifier.verifyAny(token, captured::set);
        var context = top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditContextBO.builder()
                .verifiedJwt(captured.get()).proof(valid.proof()).build();
        assertThat(mapper.writeValueAsString(context)).doesNotContain(token, "verifiedJwt");
        assertThat(context.toString()).doesNotContain(token, "verifiedJwt");
    }

    @Test
    void enabledObservationRequiresItsDependenciesAndDisablesServletDoubleRegistration() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withUserConfiguration(top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterAutoConfiguration.UsageObservationConfiguration.class)
                .withPropertyValues("egon.cola.platform.tianquan.shoubing.usage-observation.enabled=true")
                .run(context -> assertThat(context).hasFailed());
        var receiver = new IdpStarterProperties(); receiver.setResourceUri(RESOURCE);
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withUserConfiguration(top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterAutoConfiguration.UsageObservationConfiguration.class)
                .withPropertyValues("egon.cola.platform.tianquan.shoubing.usage-observation.enabled=true")
                .withBean("tokenUsageAuditComponent", TokenUsageAuditComponent.class, () -> producer)
                .withBean("tokenUsageAuditEventConverterImpl", TokenUsageAuditEventConverter.class, () -> eventConverter)
                .withBean("oauthUsageObservationProperties", OAuthUsageObservationProperties.class, () -> usageProperties)
                .withBean("idpSecurityMetricsRecorder", IdpSecurityMetricsRecorder.class, () -> observation -> { })
                .withBean("idpClock", Clock.class, () -> clock)
                .withBean("defaultValidator", jakarta.validation.Validator.class, () -> validator)
                .withBean("idpStarterProperties", IdpStarterProperties.class, () -> receiver)
                .withBean("idpEndpointAuthenticationPolicy", IdpEndpointAuthenticationPolicy.class, IdpEndpointAuthenticationPolicy::new)
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(IdpObservedVerificationComponent.class).hasSingleBean(IdpSecurityObservationFilter.class);
                    assertThat(context.getBean("idpSecurityObservationFilterRegistration", org.springframework.boot.web.servlet.FilterRegistrationBean.class)
                            .isEnabled()).isFalse();
                });
    }

    @Test
    void missingHeaderDecisionIsHandedOffOnceBeforeTheOwning401Renderer() throws Exception {
        var request = new MockHttpServletRequest("GET", "/user/orders");
        var response = new MockHttpServletResponse();
        var filter = new IdpSecurityObservationFilter(observations, new IdpEndpointAuthenticationPolicy());
        filter.doFilter(request, response, (incoming, outgoing) -> {
            assertThat(IdpSecurityObservationFilter.beforeAuthenticationFailure(request)).isTrue();
            assertThat(IdpSecurityObservationFilter.beforeAuthenticationFailure(request)).isTrue();
            assertThat(events).hasSize(1);
            response.setStatus(401);
        });
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(events.getFirst()).containsEntry("reasonCode", "AUTHENTICATION_FAILED")
                .doesNotContainKeys("subjectRef", "clientRef", "tokenRef");
    }

    @Test
    void authorizationDenialHasADistinctEventWithTheSameTraceAndRendersOnlyAfterHandoff() throws Exception {
        var request = new MockHttpServletRequest("GET", "/user/orders");
        request.addHeader("Authorization", "Bearer " + token(false));
        var response = new MockHttpServletResponse();
        var filter = new IdpSecurityObservationFilter(observations, new IdpEndpointAuthenticationPolicy());
        var bearer = new IdpBearerAuthenticationFilter(verifier, mapper);
        filter.doFilter(request, response, (incoming, outgoing) -> bearer.doFilter(incoming, outgoing, (businessRequest, businessResponse) -> {
            assertThat(IdpSecurityObservationFilter.beforeAuthorizationDenied(request)).isTrue();
            assertThat(IdpSecurityObservationFilter.beforeAuthorizationDenied(request)).isTrue();
            assertThat(events).hasSize(2);
            response.setStatus(403);
        }));
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(events.get(0).get("eventId")).isNotEqualTo(events.get(1).get("eventId"));
        assertThat(events.get(0).get("traceId")).isEqualTo(events.get(1).get("traceId")).isNotBlank();
        assertThat(events.get(1)).containsEntry("reasonCode", "AUTHORIZATION_DENIED").containsEntry("result", "DENIED");
        assertThat(events.get(0).get("tokenRef")).isEqualTo(events.get(1).get("tokenRef"));
        assertThat(org.slf4j.MDC.get("traceId")).isNull();
    }

    @Test
    void expiredSignedJwtProducesOnlyTheAnonymousFailureFact() throws Exception {
        var claims = new LinkedHashMap<>(SignedJWT.parse(token(false)).getJWTClaimsSet().toJSONObject());
        claims.put("iat", NOW.minusSeconds(360).getEpochSecond());
        claims.put("nbf", NOW.minusSeconds(360).getEpochSecond());
        claims.put("exp", NOW.minusSeconds(60).getEpochSecond());
        var expired = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("same-kid")
                .type(new JOSEObjectType("at+jwt")).build(), JWTClaimsSet.parse(claims));
        expired.sign(new RSASSASigner(key));
        assertThat(verifier.verifyUser(expired.serialize())).isInstanceOf(AccessTokenVerification.Expired.class);
        assertThat(events).hasSize(1);
        assertThat(events.getFirst()).containsEntry("result", "FAILED").doesNotContainKeys("subjectRef", "clientRef", "tokenRef");
    }

    @Test
    void incompleteFullValidCaptureFailsClosedAndOriginalExceptionsRemainOriginal() throws Exception {
        var rawValid = verifier.verifyAny(token(false), ignored -> { });
        var result = observations.observe(capture -> {
            capture.accept(new top.egon.cola.platform.tianquan.shoubing.starter.security.domain.VerifiedJwtBO());
            return rawValid;
        });
        assertThat(result).isInstanceOf(AccessTokenVerification.Unavailable.class);
        assertThat(events).hasSize(1);
        assertThat(events.getFirst()).doesNotContainKeys("subjectRef", "clientRef", "tokenRef");
        var failure = new IllegalStateException("original boundary");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> observations.observe(capture -> { throw failure; }))
                .isSameAs(failure);
        assertThat(events).hasSize(1);
    }

    private String token(boolean service) throws Exception {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", ISSUER.toASCIIString());
        claims.put("sub", service ? "orders-service" : "subject-1001");
        claims.put("aud", List.of(RESOURCE.toASCIIString()));
        claims.put("iat", NOW.minusSeconds(1).getEpochSecond());
        claims.put("nbf", NOW.minusSeconds(1).getEpochSecond());
        claims.put("exp", NOW.plusSeconds(300).getEpochSecond());
        claims.put("jti", "901");
        claims.put("tenant_id", "1001");
        claims.put("client_id", service ? "orders-service" : "browser-client");
        claims.put("principal_type", service ? "SERVICE" : "USER");
        claims.put("scope_context", "TENANT");
        claims.put("authorization_id", "801");
        claims.put("client_registration_id", "501");
        claims.put("security_version", service ? Map.of("realm", 1L, "client", 2L) : Map.of("realm", 1L, "client", 2L, "subject", 3L));
        claims.put("scope", List.of("orders:read"));
        if (service) {
            claims.put("resource_version", 7L);
            claims.put("source_biz", "biz");
            claims.put("source_app", "orders");
            claims.put("source_env", "test");
            claims.put("credential_id", "credential-1");
            claims.put("app_id", "orders-app");
        }
        else { claims.put("acr", "PASSWORD"); claims.put("auth_time", NOW.minusSeconds(1).getEpochSecond()); }
        var token = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("same-kid").type(new JOSEObjectType("at+jwt")).build(), JWTClaimsSet.parse(claims));
        token.sign(new RSASSASigner(key));
        return token.serialize();
    }
}
