package top.egon.cola.platform.rbac3.gateway.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.core.context.GatewayPrincipal;
import top.egon.cola.component.gateway.core.security.AuthorizationDecision;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.platform.rbac3.contract.auth.Rbac3TokenClaims;
import top.egon.cola.platform.rbac3.contract.authorization.AppAuthorizationContext;
import top.egon.cola.platform.rbac3.contract.authorization.SessionAuthorizationSnapshot;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Rbac3GatewayRuntimeSnapshotReaderTest {

    private static final Instant NOW = Instant.parse("2026-07-30T12:00:00Z");
    private final Rbac3RuntimeKeyFactory keys = new Rbac3RuntimeKeyFactory();

    @Test
    void authorizesOnlyOneExactVersionedOperationMapping() {
        Fixture fixture = fixture();
        fixture.values().put(mappingKey(), mapping());

        fixture.reader().verifySession(tokenClaims());
        AuthorizationDecision decision = fixture.reader().authorize(context());

        assertThat(decision).isEqualTo(AuthorizationDecision.allow());
        assertThat(fixture.requestedKeys()).contains(mappingKey());
    }

    @Test
    void failsClosedForMissingAndConflictingOperationMappings() {
        Fixture missing = fixture();
        assertThat(missing.reader().authorize(context()))
                .isEqualTo(AuthorizationDecision.deny(
                        "RBAC3_OPERATION_MAPPING_MISSING"));

        Fixture conflict = fixture();
        conflict.values().put(mappingKey(), List.of(mapping(), mapping()));
        assertThat(conflict.reader().authorize(context()))
                .isEqualTo(AuthorizationDecision.deny(
                        "RBAC3_OPERATION_MAPPING_CONFLICT"));
    }

    @Test
    void rejectsVersionDriftFencesAndRedisFailures() {
        Fixture versionDrift = fixture();
        versionDrift.values().put(keys.authVersion("7", "9"), 4L);
        assertThatThrownBy(() -> versionDrift.reader().verifySession(tokenClaims()))
                .isInstanceOf(
                        Rbac3GatewayRuntimeSnapshotReader.RuntimeUnavailableException.class)
                .hasMessage("RBAC3_RUNTIME_VERSION_MISMATCH");

        Fixture fenced = fixture();
        fenced.existingKeys().add(keys.sessionFence("7", "99"));
        assertThatThrownBy(() -> fenced.reader().verifySession(tokenClaims()))
                .isInstanceOf(
                        Rbac3GatewayRuntimeSnapshotReader.RuntimeUnavailableException.class)
                .hasMessage("RBAC3_SESSION_FENCED");

        Fixture unavailable = fixture();
        unavailable.values().put(keys.session("7", "99"),
                new IllegalStateException("redis timeout"));
        assertThatThrownBy(() -> unavailable.reader().verifySession(tokenClaims()))
                .isInstanceOf(
                        Rbac3GatewayRuntimeSnapshotReader.RuntimeUnavailableException.class)
                .hasMessage("RBAC3_AUTHORIZATION_RUNTIME_UNAVAILABLE");
    }

    private Fixture fixture() {
        Map<String, Object> values = new HashMap<>();
        Set<String> existingKeys = new HashSet<>();
        List<String> requestedKeys = new ArrayList<>();
        values.put(keys.session("7", "99"),
                new Rbac3GatewayRuntimeSnapshotReader.RuntimeSession(
                        "7", "9", "99", "ACTIVE", 3, 4, 5,
                        NOW.plusSeconds(300)));
        values.put(keys.authVersion("7", "9"), 3L);
        values.put(keys.policyVersion("7"), 5L);
        values.put(keys.snapshot("7", "99", 4), snapshot());

        RedissonClient redisson = mock(RedissonClient.class);
        when(redisson.<Object>getBucket(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            requestedKeys.add(key);
            @SuppressWarnings("unchecked")
            RBucket<Object> bucket = mock(RBucket.class);
            when(bucket.get()).thenAnswer(ignored -> {
                Object value = values.get(key);
                if (value instanceof RuntimeException exception) {
                    throw exception;
                }
                return value;
            });
            when(bucket.isExists()).thenReturn(existingKeys.contains(key));
            return bucket;
        });
        return new Fixture(
                new Rbac3GatewayRuntimeSnapshotReader(
                        redisson,
                        new ObjectMapper().findAndRegisterModules(),
                        keys,
                        Clock.fixed(NOW, ZoneOffset.UTC)),
                values, existingKeys, requestedKeys);
    }

    private SessionAuthorizationSnapshot snapshot() {
        AppAuthorizationContext app = new AppAuthorizationContext(
                "11", "orders", List.of("root"), List.of("assignment"),
                List.of("root", "child"), Set.of("order:read"),
                Map.of(), Map.of(), List.of(), "/orders");
        return new SessionAuthorizationSnapshot(
                "99", 3, 4, 5, List.of(app), "checksum", NOW);
    }

    private Rbac3GatewayRuntimeSnapshotReader.OperationPermissionMapping mapping() {
        return new Rbac3GatewayRuntimeSnapshotReader.OperationPermissionMapping(
                "7", "orders", "definition-7", "operation-9", 5,
                "order:read", true, "rbac3-required-v1", true);
    }

    private String mappingKey() {
        return keys.operationMapping("7", "definition-7", "operation-9", 5);
    }

    private Rbac3TokenClaims tokenClaims() {
        return new Rbac3TokenClaims(
                "issuer", List.of("orders"), "9", "7", "99",
                3, 4, 5, "jti-1", NOW.minusSeconds(1),
                NOW.minusSeconds(1), NOW.plusSeconds(300), "kid-1");
    }

    private GatewayAuthContext context() {
        GatewayPrincipal principal = new GatewayPrincipal(
                "9", "USER", "7", null, true, Map.of(
                "rbac3.session-id", "99",
                "rbac3.auth-version", "3",
                "rbac3.session-version", "4",
                "rbac3.policy-version", "5"));
        return new GatewayAuthContext(
                AccessZone.PUBLIC, GatewayProtocol.HTTP,
                "operation-9", "route-1", "rbac3-required-v1",
                "/orders", "GET", Set.of("bearer"), principal,
                "127.0.0.1", "trace-1", "request-1",
                NOW.plusSeconds(3), "release-1", Map.of(
                "rbac3.definition-set-id", "definition-7",
                "rbac3.mapping-version", "5"));
    }

    private record Fixture(
            Rbac3GatewayRuntimeSnapshotReader reader,
            Map<String, Object> values,
            Set<String> existingKeys,
            List<String> requestedKeys
    ) {
    }
}
