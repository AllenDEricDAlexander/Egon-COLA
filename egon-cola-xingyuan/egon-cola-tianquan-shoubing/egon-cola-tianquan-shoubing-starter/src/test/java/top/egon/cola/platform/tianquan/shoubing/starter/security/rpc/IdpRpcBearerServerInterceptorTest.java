package top.egon.cola.platform.tianquan.shoubing.starter.security.rpc;

import io.grpc.Attributes;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import io.grpc.Context;
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
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;
import org.redisson.api.RedissonClient;
import org.redisson.api.RStream;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.api.stream.StreamAddParams;
import org.redisson.client.codec.StringCodec;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.component.rpc.context.invocation.RpcMetadataKeys;
import top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus;
import top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.AuditDeliveryProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthAuditProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.observability.IdpSecurityObservationReceiverEnum;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.RpcPrincipalKindEnum;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.RpcProviderMethodProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpJwtVerifier;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.OAuthRevocationReader;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TenantIssuerAuthenticationManagerComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry;
import top.egon.cola.platform.tianquan.shoubing.starter.security.VerifiedPrincipalConverter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.IdpAuthorizationObservationContextBO;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.converter.TokenUsageAuditEventConverter;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityOAuthClientStateReader;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerState;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerStateReader;
import top.egon.cola.platform.tianquan.shoubing.starter.security.UserAccessTokenVerifier;
import top.egon.cola.platform.tianquan.shoubing.starter.security.VerifiedUserTokenCarrier;

import java.time.Instant;
import java.lang.reflect.Constructor;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doReturn;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IdpRpcBearerServerInterceptorTest {

    @Test
    void requiredMissingNoCallback() {
        try (var validator = new LocalValidatorFactoryBean()) {
            validator.afterPropertiesSet();
            var verifier = mock(UserAccessTokenVerifier.class);
            var interceptor = IdpRpcBearerServerInterceptor.createConfigured(verifier, null, providerProperties(), null, validator);
            var call = new RecordingServerCall();
            var next = new RecordingHandler();
            interceptor.interceptCall(call, new Metadata(), next);
            assertThat(next.calls).as("required RPC must reject before provider entry").isZero();
            assertThat(call.status.getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
            verifyNoInteractions(verifier);
        }
    }

    @Test
    void unknownActualMethodCannotBorrowPolicyFromIncomingMetadata() {
        try (var validator = new LocalValidatorFactoryBean()) {
            validator.afterPropertiesSet();
            var properties = providerProperties();
            properties.getRpc().getProvider().getMethods().clear();
            properties.getRpc().getProvider().getMethods().put("example.Other/Read", providerPolicy());
            var interceptor = IdpRpcBearerServerInterceptor.createConfigured(mock(UserAccessTokenVerifier.class), null, properties, null, validator);
            var call = new RecordingServerCall();
            var next = new RecordingHandler();
            interceptor.interceptCall(call, new Metadata(), next);
            assertThat(next.calls).isZero();
            assertThat(call.status.getCode()).isEqualTo(Status.Code.PERMISSION_DENIED);
        }
    }

    private static IdpStarterProperties providerProperties() {
        var properties = new IdpStarterProperties();
        properties.getRpc().setEnabled(true);
        properties.getRpc().getProvider().setEnabled(true);
        properties.getRpc().getProvider().getMethods().put(IdpRpcBindingPropertiesTest.METHOD, providerPolicy());
        return properties;
    }

    private static RpcProviderMethodProperties providerPolicy() {
        return RpcProviderMethodProperties.builder().required(true).principalKind(RpcPrincipalKindEnum.SERVICE)
                .scopeContext(ServiceTokenContext.TENANT).requiredScopes(Set.of("inventory:read"))
                .expectedResourceUri(IdpRpcBindingPropertiesTest.RESOURCE).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Nested
    class ConfiguredPipelineTest {
        private static final URI ISSUER = URI.create("https://idp.example/tenants/1001");
        private static final URI PLATFORM = URI.create("https://idp.example/platform");
        private static final Instant NOW = Instant.parse("2026-10-11T00:00:00Z");
        private static final String STREAM = "egon:oauth:audit:v1";
        @TempDir Path temporary;
        private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        private final List<Map<String, String>> events = new CopyOnWriteArrayList<>();
        private final RedissonClient redis = mock(RedissonClient.class);
        @SuppressWarnings("unchecked") private final RStream<String, String> stream = mock(RStream.class);
        private final OAuthRevocationReader revocations = mock(OAuthRevocationReader.class);
        private IdpStarterProperties properties;
        private TenantIssuerAuthenticationManagerComponent managers;
        private IdpRpcBearerServerInterceptor interceptor;
        private RSAKey key;

        @BeforeEach
        void prepare() throws Exception {
            validator.afterPropertiesSet();
            var keyFile = temporary.resolve("audit.key");
            Files.writeString(keyFile, "0123456789abcdef0123456789abcdef");
            Files.setPosixFilePermissions(keyFile, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
            doReturn(stream).when(redis).getStream(eq(STREAM), eq(StringCodec.INSTANCE));
            when(stream.add(any(StreamAddArgs.class))).thenAnswer(call -> {
                StreamAddParams<String, String> values = call.getArgument(0);
                events.add(Map.copyOf(values.getEntries()));
                return new StreamMessageId(events.size(), 1);
            });
            var clock = Clock.fixed(NOW, ZoneOffset.UTC);
            var producer = new TokenUsageAuditComponent(redis, new ObjectMapper().findAndRegisterModules(),
                    new OAuthAuditProperties(STREAM, temporary.resolve("spool"), 1_048_576, keyFile), new AuditDeliveryProperties(), clock, validator);
            var converter = (TokenUsageAuditEventConverter) Class.forName(TokenUsageAuditEventConverter.class.getName() + "Impl").getConstructor().newInstance();
            var observations = new IdpObservedVerificationComponent(producer, converter,
                    new OAuthUsageObservationProperties(true, ISSUER, false, IdpSecurityObservationReceiverEnum.APPLICATION),
                    observation -> { }, clock, validator, IdpRpcBindingPropertiesTest.RESOURCE);
            properties = providerProperties();
            properties.setResourceServerId("inventory-resource");
            properties.setResourceUri(IdpRpcBindingPropertiesTest.RESOURCE);
            properties.getRevocation().setPrimaryEndpoint("rediss://redis.example:6379");
            properties.getRevocation().setEpoch("epoch-1");
            properties.setTrustedIssuers(Map.of("tenant-1001", trusted(ISSUER, "TENANT", "1001"), "platform", trusted(PLATFORM, "PLATFORM", null)));
            managers = spy(new TenantIssuerAuthenticationManagerComponent(new TrustedIssuerRegistry(properties), properties, clock));
            managers.initialize();
            var rest = TenantIssuerAuthenticationManagerComponent.class.getDeclaredField("jwksRestOperations");
            rest.setAccessible(true);
            var jwks = MockRestServiceServer.bindTo((RestTemplate) rest.get(managers)).build();
            key = new RSAKeyGenerator(2048).keyID("rpc-key").algorithm(JWSAlgorithm.RS256).generate();
            for (URI issuer : List.of(ISSUER, PLATFORM)) {
                jwks.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(issuer + "/.well-known/jwks.json"))
                        .andRespond(MockRestResponseCreators.withSuccess(new JWKSet(key.toPublicJWK()).toString(), MediaType.APPLICATION_JSON));
            }
            when(revocations.check(any())).thenReturn(OAuthRevocationReader.Result.valid());
            IdentityResourceServerStateReader resources = id -> Optional.of(new IdentityResourceServerState("inventory-resource",
                    IdpRpcBindingPropertiesTest.RESOURCE, "biz", "inventory", "test", ResourceServerStatus.ACTIVE, 7));
            var principalConverter = (VerifiedPrincipalConverter) Class.forName(VerifiedPrincipalConverter.class.getName() + "Impl").getConstructor().newInstance();
            var verifier = IdpJwtVerifier.createConfigured(null, resources, mock(IdentityOAuthClientStateReader.class), "inventory-resource",
                    IdpRpcBindingPropertiesTest.RESOURCE, "platform-audience", clock, managers, revocations, principalConverter, null, observations);
            interceptor = IdpRpcBearerServerInterceptor.createConfigured(new UserAccessTokenVerifier(verifier), verifier, properties, observations, validator);
            interceptor.validateConfiguration();
        }

        @AfterEach void closeValidator() { validator.close(); }

        @Test
        void serviceScopeAndContextStrictWithOneRealRs256Verification() throws Exception {
            String token = token(false, ServiceTokenContext.TENANT, Set.of("inventory:read"), IdpRpcBindingPropertiesTest.RESOURCE, NOW.plusSeconds(300));
            var observed = new AtomicReference<Authentication>();
            var marker = new AtomicReference<IdpAuthorizationObservationContextBO>();
            var entered = new java.util.concurrent.atomic.AtomicInteger();
            var previous = SecurityContextHolder.createEmptyContext();
            SecurityContextHolder.setContext(previous);
            MDC.put("tenantId", "previous-tenant");
            MDC.put("userId", "previous-user");
            MDC.put("egon.auth.actor-kind", "previous-kind");
            var previousAttributes = new ServletRequestAttributes(new MockHttpServletRequest());
            RequestContextHolder.setRequestAttributes(previousAttributes);
            Runnable record = () -> {
                entered.incrementAndGet();
                observed.set(SecurityContextHolder.getContext().getAuthentication());
                marker.set(IdpRpcSecurityContext.currentAuthorizationObservationContext());
                assertThat(IdpRpcSecurityContext.currentTokenOrNull()).isNull();
                assertThat(VerifiedUserTokenCarrier.currentOrNull()).isNull();
                assertThat(RequestContextHolder.getRequestAttributes()).isNull();
                assertThat(MDC.get("tenantId")).isEqualTo("1001");
                assertThat(MDC.get("userId")).isEqualTo("orders-client");
                assertThat(MDC.get("egon.auth.actor-kind")).isEqualTo("SERVICE");
                assertThat(IdpRpcSecurityContext.currentProofOrNull()).isNotNull();
            };
            var listener = interceptor.interceptCall(new RecordingServerCall(), bearer(token), (call, headers) -> {
                record.run();
                return new ServerCall.Listener<String>() {
                    @Override public void onMessage(String message) { record.run(); }
                    @Override public void onHalfClose() { record.run(); }
                    @Override public void onReady() { record.run(); }
                    @Override public void onComplete() { record.run(); }
                };
            });
            listener.onMessage("payload"); listener.onHalfClose(); listener.onReady(); listener.onComplete(); listener.onReady();
            assertThat(entered.get()).isEqualTo(5);
            assertThat(observed.get().getPrincipal()).isInstanceOf(ServiceIdentityPrincipal.class);
            assertThat(observed.get().getCredentials()).isEqualTo("");
            assertThat(observed.get().getAuthorities()).extracting("authority").containsExactly("SCOPE_inventory:read");
            assertThat(marker.get().isActive()).isFalse();
            assertThat(SecurityContextHolder.getContext()).isSameAs(previous);
            assertThat(RequestContextHolder.getRequestAttributes()).isSameAs(previousAttributes);
            assertThat(MDC.get("tenantId")).isEqualTo("previous-tenant");
            assertThat(MDC.get("userId")).isEqualTo("previous-user");
            assertThat(MDC.get("egon.auth.actor-kind")).isEqualTo("previous-kind");
            assertThat(IdpRpcSecurityContext.currentPrincipalOrNull()).isNull();
            assertThat(events).hasSize(1).allSatisfy(event -> assertThat(event).containsEntry("result", "SUCCESS"));
            verify(managers, times(1)).authenticate(token); verify(revocations, times(1)).check(any());
        }

        @Test
        void authenticatedUserCallbackBindsItsOwnActorKindAndRestoresThePreviousValue() throws Exception {
            String token = token(true, ServiceTokenContext.TENANT, Set.of("inventory:read"),
                    IdpRpcBindingPropertiesTest.RESOURCE, NOW.plusSeconds(300));
            properties.getRpc().getProvider().getMethods().get(IdpRpcBindingPropertiesTest.METHOD)
                    .setPrincipalKind(RpcPrincipalKindEnum.USER);
            MDC.put("egon.auth.actor-kind", "previous-kind");
            var listener = interceptor.interceptCall(new RecordingServerCall(), bearer(token), (call, headers) -> {
                assertThat(MDC.get("egon.auth.actor-kind")).isEqualTo("USER");
                return new ServerCall.Listener<String>() {
                    @Override public void onHalfClose() {
                        assertThat(MDC.get("egon.auth.actor-kind")).isEqualTo("USER");
                    }
                };
            });
            listener.onHalfClose(); listener.onCancel();
            assertThat(MDC.get("egon.auth.actor-kind")).isEqualTo("previous-kind");
        }

        @Test
        void principalKindScopeContextAndExpectedResourceEachRequireTheirOwnGrant() throws Exception {
            String user = token(true, ServiceTokenContext.TENANT, Set.of("inventory:read"), IdpRpcBindingPropertiesTest.RESOURCE, NOW.plusSeconds(300));
            String missingScope = token(false, ServiceTokenContext.TENANT, Set.of("inventory:other"), IdpRpcBindingPropertiesTest.RESOURCE, NOW.plusSeconds(300));
            String platform = token(false, ServiceTokenContext.PLATFORM, Set.of("inventory:read"), IdpRpcBindingPropertiesTest.RESOURCE, NOW.plusSeconds(300));
            for (String rejected : List.of(user, missingScope, platform)) { assertStrictRejected(rejected, Status.Code.PERMISSION_DENIED); }
            properties.getRpc().getProvider().getMethods().get(IdpRpcBindingPropertiesTest.METHOD).setExpectedResourceUri(URI.create("https://api.example/other"));
            assertStrictRejected(token(false, ServiceTokenContext.TENANT, Set.of("inventory:read"), IdpRpcBindingPropertiesTest.RESOURCE, NOW.plusSeconds(300)), Status.Code.PERMISSION_DENIED);
            assertThat(events.stream().filter(event -> "DENIED".equals(event.get("result")))).hasSize(4);
        }

        @Test
        void wrongAudienceExpiredAndSignatureInvalidNeverReachTheProvider() throws Exception {
            assertStrictRejected(token(false, ServiceTokenContext.TENANT, Set.of("inventory:read"), URI.create("https://api.example/other"), NOW.plusSeconds(300)), Status.Code.UNAUTHENTICATED);
            assertStrictRejected(token(false, ServiceTokenContext.TENANT, Set.of("inventory:read"), IdpRpcBindingPropertiesTest.RESOURCE, NOW.minusSeconds(60)), Status.Code.UNAUTHENTICATED);
            String valid = token(false, ServiceTokenContext.TENANT, Set.of("inventory:read"), IdpRpcBindingPropertiesTest.RESOURCE, NOW.plusSeconds(300));
            var parsed = SignedJWT.parse(valid);
            var forged = new SignedJWT(parsed.getHeader(), parsed.getJWTClaimsSet());
            forged.sign(new RSASSASigner(new RSAKeyGenerator(2048).keyID("rpc-key").generate()));
            assertStrictRejected(forged.serialize(), Status.Code.UNAUTHENTICATED);
            assertThat(events).hasSize(3).allSatisfy(event -> assertThat(event).containsEntry("result", "FAILED").doesNotContainKeys("subjectRef", "clientRef", "tokenRef"));
        }

        @Test
        void stateAndAuthenticationAuditUnavailableUseUnavailableWithZeroCallbacks() throws Exception {
            when(revocations.check(any())).thenReturn(new OAuthRevocationReader.Result(OAuthRevocationReader.Status.UNAVAILABLE, "OAUTH_SECURITY_STATE_UNAVAILABLE"));
            assertStrictRejected(token(false, ServiceTokenContext.TENANT, Set.of("inventory:read"), IdpRpcBindingPropertiesTest.RESOURCE, NOW.plusSeconds(300)), Status.Code.UNAVAILABLE);
            when(revocations.check(any())).thenReturn(OAuthRevocationReader.Result.valid());
            when(stream.add(any(StreamAddArgs.class))).thenThrow(new org.redisson.client.RedisException("sensitive-broker-address"));
            assertStrictRejected(token(false, ServiceTokenContext.TENANT, Set.of("inventory:read"), IdpRpcBindingPropertiesTest.RESOURCE, NOW.plusSeconds(300)), Status.Code.UNAVAILABLE);
            var missing = new RecordingServerCall(); var next = new RecordingHandler();
            interceptor.interceptCall(missing, new Metadata(), next);
            assertThat(missing.status.getCode()).isEqualTo(Status.Code.UNAVAILABLE); assertThat(next.calls).isZero();
        }

        @Test
        void deniedAuditFailureCannotBecomeOrdinaryPermissionDenied() throws Exception {
            when(stream.add(any(StreamAddArgs.class))).thenAnswer(call -> {
                StreamAddParams<String, String> values = call.getArgument(0);
                if ("DENIED".equals(values.getEntries().get("result"))) { throw new org.redisson.client.RedisException("sensitive-broker-address"); }
                events.add(Map.copyOf(values.getEntries())); return new StreamMessageId(events.size(), 1);
            });
            assertStrictRejected(token(false, ServiceTokenContext.TENANT, Set.of("inventory:other"), IdpRpcBindingPropertiesTest.RESOURCE, NOW.plusSeconds(300)), Status.Code.UNAVAILABLE);
            assertThat(events).hasSize(1).allSatisfy(event -> assertThat(event).containsEntry("result", "SUCCESS"));
        }

        @Test
        void userTokenRelaysOnlyInAUserPolicyAndPlatformServiceHasNoTenant() throws Exception {
            var policy = properties.getRpc().getProvider().getMethods().get(IdpRpcBindingPropertiesTest.METHOD);
            policy.setPrincipalKind(RpcPrincipalKindEnum.USER);
            String user = token(true, ServiceTokenContext.TENANT, Set.of("inventory:read"), IdpRpcBindingPropertiesTest.RESOURCE, NOW.plusSeconds(300));
            var listener = interceptor.interceptCall(new RecordingServerCall(), bearer(user), (call, headers) -> {
                assertThat(IdpRpcSecurityContext.currentTokenOrNull()).isEqualTo(user);
                var principal = (IdentityPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                IdpRpcSecurityContext.runWithSpringSecurity(principal, () ->
                        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                                .extracting("authority").containsExactly("SCOPE_inventory:read"));
                return new ServerCall.Listener<String>() { };
            });
            listener.onComplete();
            policy.setPrincipalKind(RpcPrincipalKindEnum.SERVICE).setScopeContext(ServiceTokenContext.PLATFORM);
            var service = interceptor.interceptCall(new RecordingServerCall(), bearer(token(false, ServiceTokenContext.PLATFORM, Set.of("inventory:read"), IdpRpcBindingPropertiesTest.RESOURCE, NOW.plusSeconds(300))),
                    (call, headers) -> { assertThat(MDC.get("tenantId")).isNull(); assertThat(MDC.get("userId")).isEqualTo("orders-client"); return new ServerCall.Listener<String>() { }; });
            service.onCancel();
            assertThat(IdpRpcSecurityContext.currentTokenOrNull()).isNull();
        }

        @Test
        void cancelAndThrowingCallbacksCloseMarkerAndRestoreTheExactPriorThreadState() throws Exception {
            for (boolean cancellation : List.of(false, true)) {
                var marker = new AtomicReference<IdpAuthorizationObservationContextBO>();
                var previous = SecurityContextHolder.getContext();
                MDC.put("tenantId", "original");
                var listener = interceptor.interceptCall(new RecordingServerCall(), bearer(token(false, ServiceTokenContext.TENANT, Set.of("inventory:read"), IdpRpcBindingPropertiesTest.RESOURCE, NOW.plusSeconds(300))),
                        (call, headers) -> { marker.set(IdpRpcSecurityContext.currentAuthorizationObservationContext()); return new ServerCall.Listener<String>() {
                            @Override public void onHalfClose() { throw new IllegalStateException("controlled provider failure"); }
                        }; });
                if (cancellation) { listener.onCancel(); }
                else { assertThatThrownBy(listener::onHalfClose).isInstanceOf(IllegalStateException.class).hasMessage("controlled provider failure"); }
                assertThat(marker.get().isActive()).isFalse();
                assertThat(marker.get().denyOnce(() -> { throw new AssertionError("late callback"); })).isFalse();
                assertThat(SecurityContextHolder.getContext()).isSameAs(previous);
                assertThat(MDC.get("tenantId")).isEqualTo("original");
                assertThat(IdpRpcSecurityContext.currentPrincipalOrNull()).isNull();
            }
        }

        private void assertStrictRejected(String token, Status.Code expected) {
            var call = new RecordingServerCall(); var next = new RecordingHandler();
            interceptor.interceptCall(call, bearer(token), next);
            assertThat(next.calls).isZero(); assertThat(call.status.getCode()).isEqualTo(expected);
            assertThat(call.status.getCause()).isNull(); assertThat(call.status.getDescription()).doesNotContain("sensitive");
        }

        private IdpStarterProperties.TrustedIssuerSettings trusted(URI issuer, String realm, String tenant) {
            var trusted = new IdpStarterProperties.TrustedIssuerSettings();
            trusted.setIssuer(issuer.toASCIIString()); trusted.setJwksUri(issuer + "/.well-known/jwks.json");
            trusted.setRealmKey(tenant == null ? "platform" : "tenant:" + tenant); trusted.setRealmType(realm);
            trusted.setTenantId(tenant); trusted.setStatus("ACTIVE"); trusted.setTrustVersion(1); return trusted;
        }

        private String token(boolean user, ServiceTokenContext context, Set<String> scopes, URI audience, Instant expiry) throws Exception {
            var claims = new java.util.LinkedHashMap<String, Object>();
            claims.put("iss", (context == ServiceTokenContext.TENANT ? ISSUER : PLATFORM).toASCIIString());
            claims.put("aud", List.of(audience.toASCIIString())); claims.put("sub", user ? "10001" : "orders-client");
            claims.put("client_id", user ? "browser-client" : "orders-client"); claims.put("jti", "901");
            claims.put("iat", NOW.minusSeconds(120).getEpochSecond()); claims.put("nbf", NOW.minusSeconds(120).getEpochSecond());
            claims.put("exp", expiry.getEpochSecond()); claims.put("principal_type", user ? "USER" : "SERVICE");
            claims.put("scope", String.join(" ", scopes)); if (context == ServiceTokenContext.TENANT) { claims.put("tenant_id", "1001"); }
            claims.put("scope_context", context.name()); claims.put("authorization_id", "801"); claims.put("client_registration_id", "501");
            claims.put("security_version", user ? Map.of("realm", 1L, "client", 2L, "subject", 3L) : Map.of("realm", 1L, "client", 2L));
            if (user) { claims.put("acr", "PASSWORD"); claims.put("amr", List.of("pwd")); claims.put("auth_time", NOW.minusSeconds(120).getEpochSecond()); }
            else {
                claims.put("scope_context", context.name()); claims.put("app_id", "orders-app"); claims.put("resource_version", 7);
                claims.put("source_biz", "biz"); claims.put("source_app", "orders"); claims.put("source_env", "test"); claims.put("credential_id", "key-1");
            }
            var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).type(new JOSEObjectType("at+jwt")).keyID("rpc-key").build(), JWTClaimsSet.parse(claims));
            jwt.sign(new RSASSASigner(key)); return jwt.serialize();
        }
    }

    @Test
    void passesAnonymousCallsThroughWithoutCreatingAuthentication() {
        UserAccessTokenVerifier verifier = mock(UserAccessTokenVerifier.class);
        RecordingHandler next = new RecordingHandler();

        ServerCall.Listener<String> listener =
                new IdpRpcBearerServerInterceptor(verifier).interceptCall(
                        new RecordingServerCall(),
                        new Metadata(),
                        next
                );

        assertThat(listener).isSameAs(next.listener);
        assertThat(next.calls).isOne();
        verifyNoInteractions(verifier);
    }

    @Test
    void verifiesUserAndRestoresSecurityContextForEveryCallback() {
        UserAccessTokenVerifier verifier = mock(UserAccessTokenVerifier.class);
        IdentityPrincipal principal = principal();
        when(verifier.verify("verified-token"))
                .thenReturn(new AccessTokenVerification.Valid<>(principal));
        Metadata headers = bearer("verified-token");
        Authentication[] observed = new Authentication[1];
        String[] relayedToken = new String[1];
        ServerCallHandler<String, String> next = (call, metadata) ->
                new ServerCall.Listener<>() {
                    @Override
                    public void onHalfClose() {
                        observed[0] = SecurityContextHolder.getContext()
                                .getAuthentication();
                        relayedToken[0] = VerifiedUserTokenCarrier.currentOrNull();
                    }
                };

        ServerCall.Listener<String> listener =
                new IdpRpcBearerServerInterceptor(verifier).interceptCall(
                        new RecordingServerCall(),
                        headers,
                        next
                );
        listener.onHalfClose();

        assertThat(observed[0].getPrincipal()).isEqualTo(principal);
        assertThat(observed[0].getCredentials()).isEqualTo("");
        assertThat(relayedToken[0]).isEqualTo("verified-token");
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNull();
        assertThat(VerifiedUserTokenCarrier.currentOrNull()).isNull();
    }

    @Test
    void rejectsMalformedAndDuplicateAuthorizationBeforeTheHandler() {
        UserAccessTokenVerifier verifier = mock(UserAccessTokenVerifier.class);
        assertRejected(verifier, headers("Basic credential"));
        assertRejected(verifier, bearer("a".repeat(8193)));
        Metadata duplicate = bearer("first");
        duplicate.put(RpcMetadataKeys.AUTHORIZATION, "Bearer second");
        assertRejected(verifier, duplicate);

        verifyNoInteractions(verifier);
    }

    @Test
    void rejectsExpiredAndInvalidTokensBeforeTheHandler() {
        UserAccessTokenVerifier verifier = mock(UserAccessTokenVerifier.class);
        when(verifier.verify("expired"))
                .thenReturn(new AccessTokenVerification.Expired<>());
        when(verifier.verify("invalid"))
                .thenReturn(new AccessTokenVerification.Invalid<>("bad_signature"));

        assertRejected(verifier, bearer("expired"));
        assertRejected(verifier, bearer("invalid"));
    }

    @Test
    void sharedRevocationUnavailableUsesGrpcUnavailableInsteadOfUnauthenticated() {
        UserAccessTokenVerifier verifier = mock(UserAccessTokenVerifier.class);
        when(verifier.verify("state-unavailable"))
                .thenReturn(unavailableOrCurrentInvalidResult());
        RecordingServerCall call = new RecordingServerCall();
        RecordingHandler next = new RecordingHandler();

        new IdpRpcBearerServerInterceptor(verifier).interceptCall(
                call,
                bearer("state-unavailable"),
                next);

        assertThat(call.status.getCode()).isEqualTo(Status.Code.UNAVAILABLE);
        assertThat(call.status.getDescription())
                .isEqualTo("oauth_security_state_unavailable");
        assertThat(next.calls).isZero();
    }

    @SuppressWarnings("unchecked")
    private AccessTokenVerification<IdentityPrincipal> unavailableOrCurrentInvalidResult() {
        try {
            Class<?> unavailableType = Class.forName(
                    "top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification$Unavailable");
            Constructor<?> constructor = unavailableType.getConstructor(String.class);
            return (AccessTokenVerification<IdentityPrincipal>) constructor
                    .newInstance("OAUTH_SECURITY_STATE_UNAVAILABLE");
        } catch (ClassNotFoundException | NoSuchMethodException compatibilityBaseline) {
            return new AccessTokenVerification.Invalid<>("OAUTH_SECURITY_STATE_UNAVAILABLE");
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("UNAVAILABLE result must be constructible", failure);
        }
    }

    private void assertRejected(
            UserAccessTokenVerifier verifier,
            Metadata headers
    ) {
        RecordingServerCall call = new RecordingServerCall();
        RecordingHandler next = new RecordingHandler();

        ServerCall.Listener<String> listener =
                new IdpRpcBearerServerInterceptor(verifier).interceptCall(
                        call,
                        headers,
                        next
                );

        assertThat(call.status.getCode())
                .isEqualTo(Status.Code.UNAUTHENTICATED);
        assertThat(next.calls).isZero();
        assertThat(listener).isNotNull();
    }

    private Metadata bearer(String token) {
        return headers("Bearer " + token);
    }

    private Metadata headers(String authorization) {
        Metadata headers = new Metadata();
        headers.put(RpcMetadataKeys.AUTHORIZATION, authorization);
        return headers;
    }

    private IdentityPrincipal principal() {
        Instant issuedAt = Instant.parse("2026-08-15T00:00:00Z");
        return new IdentityPrincipal(
                "user-1",
                "tenant-1",
                "token-1",
                Set.of("resource-1"),
                issuedAt,
                issuedAt.plusSeconds(300),
                AuthenticationContext.of("PASSWORD", issuedAt)
        );
    }

    private static final class RecordingHandler
            implements ServerCallHandler<String, String> {

        private final ServerCall.Listener<String> listener =
                new ServerCall.Listener<>() {
                };
        private int calls;

        @Override
        public ServerCall.Listener<String> startCall(
                ServerCall<String, String> call,
                Metadata headers
        ) {
            calls++;
            return listener;
        }
    }

    private static final class RecordingServerCall
            extends ServerCall<String, String> {

        private Status status;

        @Override
        public void request(int numMessages) {
        }

        @Override
        public void sendHeaders(Metadata headers) {
        }

        @Override
        public void sendMessage(String message) {
        }

        @Override
        public void close(Status status, Metadata trailers) {
            this.status = status;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public MethodDescriptor<String, String> getMethodDescriptor() {
            return MethodDescriptor.<String, String>newBuilder().setType(MethodDescriptor.MethodType.UNARY)
                    .setFullMethodName(IdpRpcBindingPropertiesTest.METHOD).setRequestMarshaller(mock(MethodDescriptor.Marshaller.class))
                    .setResponseMarshaller(mock(MethodDescriptor.Marshaller.class)).build();
        }

        @Override
        public Attributes getAttributes() {
            return Attributes.EMPTY;
        }
    }
}
