package top.egon.cola.platform.rbac3.gateway.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.core.context.GatewayPrincipal;
import top.egon.cola.component.gateway.core.security.AuthorizationDecision;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.platform.rbac3.contract.authorization.ApplicationAccessScope;
import top.egon.cola.platform.rbac3.contract.authorization.BusinessAccessScope;
import top.egon.cola.platform.rbac3.contract.authorization.GatewayBizAppScopeSnapshot;
import top.egon.cola.platform.rbac3.core.runtime.Rbac3RuntimeKeyFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Rbac3GatewayScopeSnapshotReaderTest {

    private static final Instant NOW = Instant.parse("2026-08-15T12:00:00Z");
    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().findAndRegisterModules();

    private final Rbac3RuntimeKeyFactory keys = new Rbac3RuntimeKeyFactory();

    @Test
    void deniesBusinessWithoutLookingUpApplication() {
        Fixture fixture = fixture(scope(List.of(new BusinessAccessScope(
                "biz-2", "finance", List.of(
                new ApplicationAccessScope("app-1", "console"))))));

        AuthorizationDecision decision = fixture.reader().authorize(context(
                Map.of("idp.biz-code", "orders", "idp.app-code", "console")));

        assertThat(decision).isEqualTo(AuthorizationDecision.deny(
                "RBAC3_BUSINESS_SCOPE_DENIED"));
        assertThat(fixture.requestedKeys()).containsExactly(
                keys.user("7", "9"),
                keys.authorizationPublicationGuard("7", "9"),
                keys.authVersion("7", "9"),
                keys.policyVersion("7"),
                keys.gatewayScope("7", "9", 3L));
    }

    @Test
    void deniesApplicationWithinMatchedBusiness() {
        Fixture fixture = fixture(scope(List.of(new BusinessAccessScope(
                "biz-1", "orders", List.of()))));

        AuthorizationDecision decision = fixture.reader().authorize(context(
                Map.of("idp.biz-code", "orders", "idp.app-code", "console")));

        assertThat(decision).isEqualTo(AuthorizationDecision.deny(
                "RBAC3_APPLICATION_SCOPE_DENIED"));
    }

    @Test
    void allowsNestedScopeAndIgnoresLegacyPermissionMetadata() {
        Fixture fixture = fixture(validScope());

        AuthorizationDecision decision = fixture.reader().authorize(context(Map.of(
                "idp.biz-code", "orders",
                "idp.app-code", "console",
                "rbac3.definition-set-id", "definition-7",
                "rbac3.mapping-version", "5")));

        assertThat(decision).isEqualTo(AuthorizationDecision.allow());
        assertThat(fixture.requestedKeys())
                .noneMatch(key -> key.contains("operation-mapping"))
                .noneMatch(key -> key.contains(":snapshot:"));
    }

    @Test
    void deniesMissingPrincipalOrRouteScopeBeforeReadingRuntime() {
        Fixture fixture = fixture(validScope());

        assertThat(fixture.reader().authorize(context(Map.of()).withPrincipal(
                GatewayPrincipal.anonymous())))
                .isEqualTo(AuthorizationDecision.deny(
                        "RBAC3_PRINCIPAL_REQUIRED"));
        assertThat(fixture.reader().authorize(context(Map.of()).withPrincipal(
                new GatewayPrincipal(
                        "service", "SERVICE", "7", null, true, Map.of()))))
                .isEqualTo(AuthorizationDecision.deny(
                        "RBAC3_USER_PRINCIPAL_REQUIRED"));
        assertThat(fixture.reader().authorize(context(
                Map.of("idp.app-code", "console"))))
                .isEqualTo(AuthorizationDecision.deny(
                        "RBAC3_BUSINESS_SCOPE_REQUIRED"));
        assertThat(fixture.reader().authorize(context(
                Map.of("idp.biz-code", "orders"))))
                .isEqualTo(AuthorizationDecision.deny(
                        "RBAC3_APPLICATION_SCOPE_REQUIRED"));
        assertThat(fixture.requestedKeys()).isEmpty();
    }

    @Test
    void rejectsFenceMissingExpiryVersionTenantAndRedisFailures() {
        Fixture fenced = fixture(validScope());
        fenced.existingKeys().add(keys.authorizationPublicationGuard("7", "9"));
        assertUnavailable(fenced, "RBAC3_AUTHORIZATION_PUBLICATION_PENDING");

        Fixture missing = fixture(validScope());
        missing.values().remove(keys.gatewayScope("7", "9", 3L));
        assertUnavailable(missing, "RBAC3_SCOPE_RUNTIME_UNAVAILABLE");

        Fixture expired = fixture(scope(List.of(), NOW.minusSeconds(1)));
        assertUnavailable(expired, "RBAC3_SCOPE_VERSION_MISMATCH");

        Fixture versionDrift = fixture(validScope());
        versionDrift.values().put(keys.authVersion("7", "9"), "4");
        assertUnavailable(versionDrift, "RBAC3_RUNTIME_VERSION_MISMATCH");

        Fixture tenantMismatch = fixture(new GatewayBizAppScopeSnapshot(
                "8", "9", "9", 3L, 5L,
                validScope().businesses(), "scope-checksum",
                NOW.minusSeconds(1), NOW.plusSeconds(300)));
        assertUnavailable(tenantMismatch, "RBAC3_SCOPE_VERSION_MISMATCH");

        Fixture redisFailure = fixture(validScope());
        redisFailure.failures().put(keys.user("7", "9"),
                new IllegalStateException("redis timeout"));
        assertUnavailable(redisFailure, "RBAC3_SCOPE_RUNTIME_UNAVAILABLE");
    }

    private void assertUnavailable(Fixture fixture, String reason) {
        assertThatThrownBy(() -> fixture.reader().authorize(context(Map.of(
                "idp.biz-code", "orders", "idp.app-code", "console"))))
                .isInstanceOf(
                        Rbac3GatewayScopeSnapshotReader.RuntimeUnavailableException.class)
                .hasMessage(reason);
    }

    private Fixture fixture(GatewayBizAppScopeSnapshot scope) {
        Map<String, String> values = new HashMap<>();
        Map<String, RuntimeException> failures = new HashMap<>();
        Set<String> existingKeys = new HashSet<>();
        List<String> requestedKeys = new ArrayList<>();
        values.put(keys.user("7", "9"), json(
                new Rbac3GatewayScopeSnapshotReader.RuntimeUserAuthorization(
                        "7", "9", "9", "ACTIVE", 3L, 5L,
                        NOW.plusSeconds(300))));
        values.put(keys.authVersion("7", "9"), "3");
        values.put(keys.policyVersion("7"), "5");
        values.put(keys.gatewayScope("7", "9", 3L), json(scope));

        RedissonClient redisson = mock(RedissonClient.class);
        when(redisson.<String>getBucket(
                anyString(), eq(StringCodec.INSTANCE))).thenAnswer(invocation -> {
                    String key = invocation.getArgument(0);
                    if (key.contains("operation-mapping")
                            || key.contains(":snapshot:")) {
                        throw new AssertionError("forbidden runtime key: " + key);
                    }
                    requestedKeys.add(key);
                    @SuppressWarnings("unchecked")
                    RBucket<String> bucket = mock(RBucket.class);
                    when(bucket.get()).thenAnswer(ignored -> {
                        RuntimeException failure = failures.get(key);
                        if (failure != null) {
                            throw failure;
                        }
                        return values.get(key);
                    });
                    when(bucket.isExists()).thenReturn(existingKeys.contains(key));
                    return bucket;
                });
        return new Fixture(
                new Rbac3GatewayScopeSnapshotReader(
                        redisson, OBJECT_MAPPER, keys,
                        Clock.fixed(NOW, ZoneOffset.UTC)),
                values, failures, existingKeys, requestedKeys);
    }

    private GatewayBizAppScopeSnapshot validScope() {
        return scope(List.of(new BusinessAccessScope(
                "biz-1", "orders", List.of(
                new ApplicationAccessScope("app-1", "console")))));
    }

    private GatewayBizAppScopeSnapshot scope(
            List<BusinessAccessScope> businesses) {
        return scope(businesses, NOW.plusSeconds(300));
    }

    private GatewayBizAppScopeSnapshot scope(
            List<BusinessAccessScope> businesses,
            Instant expiresAt) {
        return new GatewayBizAppScopeSnapshot(
                "7", "9", "9", 3L, 5L,
                businesses, "scope-checksum", NOW.minusSeconds(60), expiresAt);
    }

    private GatewayAuthContext context(Map<String, String> attributes) {
        return new GatewayAuthContext(
                AccessZone.INTERNAL, GatewayProtocol.HTTP,
                "operation-9", "route-1", "business-protected",
                "/orders", "GET", Set.of("bearer"),
                new GatewayPrincipal(
                        "9", "USER", "7", null, true, Map.of()),
                "127.0.0.1", "trace-1", "request-1",
                NOW.plusSeconds(3), "release-1", attributes);
    }

    private String json(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new AssertionError(exception);
        }
    }

    private record Fixture(
            Rbac3GatewayScopeSnapshotReader reader,
            Map<String, String> values,
            Map<String, RuntimeException> failures,
            Set<String> existingKeys,
            List<String> requestedKeys
    ) {
    }
}
