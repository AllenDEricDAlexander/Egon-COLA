package top.egon.cola.platform.rbac3.admin.runtime.repository.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.redisson.api.RScript;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import top.egon.cola.platform.rbac3.admin.runtime.service.SessionSnapshotProjector;
import top.egon.cola.platform.rbac3.contract.authorization.SessionAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.core.runtime.Rbac3RuntimeKeyFactory;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.RuntimeSessionVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.SessionSnapshotProjectionVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.dto.PublishCommandDTO;

class RedisAuthorizationRuntimeStoreIT {

    private static final Instant NOW = Instant.parse("2026-07-30T12:00:00Z");

    @Test
    void publishesAllRuntimeKeysAtomicallyInTheTenantHashSlot() {
        AtomicReference<List<Object>> keys = new AtomicReference<>();
        RScript script = mock(RScript.class);
        RedissonClient redisson = mock(RedissonClient.class);
        when(redisson.getScript(StringCodec.INSTANCE)).thenReturn(script);
        when(script.eval(
                eq(RScript.Mode.READ_WRITE), anyString(),
                eq(RScript.ReturnType.INTEGER), anyList(), any(Object[].class)))
                .thenAnswer(invocation -> {
                    keys.set(invocation.getArgument(3));
                    return 1L;
                });
        RedisAuthorizationRuntimeRepository store = new RedisAuthorizationRuntimeRepository(
                redisson,
                new ObjectMapper().findAndRegisterModules(),
                new Rbac3RuntimeKeyFactory());

        var result = store.publish(command());

        assertThat(result.changed()).isTrue();
        assertThat(keys.get()).hasSize(5)
                .allSatisfy(key -> assertThat(key.toString()).contains("rbac3:{7}:"));
        assertThat(keys.get().get(3).toString()).endsWith("snapshot:99:1");
        assertThat(keys.get().get(4).toString()).endsWith("fence:session:99");
    }

    @Test
    void rejectsAProjectionThatWouldDowngradeRuntimeVersions() {
        RScript script = mock(RScript.class);
        RedissonClient redisson = mock(RedissonClient.class);
        when(redisson.getScript(StringCodec.INSTANCE)).thenReturn(script);
        when(script.eval(
                eq(RScript.Mode.READ_WRITE), anyString(),
                eq(RScript.ReturnType.INTEGER), anyList(), any(Object[].class)))
                .thenReturn(-1L);
        RedisAuthorizationRuntimeRepository store = new RedisAuthorizationRuntimeRepository(
                redisson,
                new ObjectMapper().findAndRegisterModules(),
                new Rbac3RuntimeKeyFactory());

        assertThatThrownBy(() -> store.publish(command()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("RBAC3_RUNTIME_VERSION_CONFLICT");
    }

    @Test
    void treatsTheSameRuntimeVersionAsIdempotent() {
        RScript script = mock(RScript.class);
        RedissonClient redisson = mock(RedissonClient.class);
        when(redisson.getScript(StringCodec.INSTANCE)).thenReturn(script);
        when(script.eval(
                eq(RScript.Mode.READ_WRITE), anyString(),
                eq(RScript.ReturnType.INTEGER), anyList(), any(Object[].class)))
                .thenReturn(0L);
        RedisAuthorizationRuntimeRepository store = new RedisAuthorizationRuntimeRepository(
                redisson,
                new ObjectMapper().findAndRegisterModules(),
                new Rbac3RuntimeKeyFactory());

        var result = store.publish(command());

        assertThat(result.changed()).isFalse();
    }

    @Test
    void coldLoadsTheSessionAndExactImmutableSnapshot() {
        RBucket<String> sessionBucket = mock(RBucket.class);
        RBucket<String> snapshotBucket = mock(RBucket.class);
        RBucket<String> authVersionBucket = mock(RBucket.class);
        RBucket<String> policyVersionBucket = mock(RBucket.class);
        RBucket<String> fenceBucket = mock(RBucket.class);
        RedissonClient redisson = mock(RedissonClient.class);
        when(redisson.<String>getBucket(
                "rbac3:{7}:session:99", StringCodec.INSTANCE)).thenReturn(sessionBucket);
        when(redisson.<String>getBucket(
                "rbac3:{7}:snapshot:99:1", StringCodec.INSTANCE)).thenReturn(snapshotBucket);
        when(redisson.<String>getBucket(
                "rbac3:{7}:auth-version:9", StringCodec.INSTANCE))
                .thenReturn(authVersionBucket);
        when(redisson.<String>getBucket(
                "rbac3:{7}:policy-version", StringCodec.INSTANCE))
                .thenReturn(policyVersionBucket);
        when(redisson.<String>getBucket(
                "rbac3:{7}:fence:session:99", StringCodec.INSTANCE))
                .thenReturn(fenceBucket);
        when(sessionBucket.get()).thenReturn("""
                {"tenantId":"7","userId":"9","identitySub":"9","sessionId":"99","status":"ACTIVE",
                 "authVersion":3,"sessionVersion":1,"policyVersion":4,
                 "expiresAt":"2026-07-30T13:00:00Z"}
                """);
        when(snapshotBucket.get()).thenReturn("""
                {"sessionId":"99","authVersion":3,"sessionVersion":1,
                 "policyVersion":4,"appContexts":[],"checksum":"sha256:test",
                 "generatedAt":"2026-07-30T12:00:00Z"}
                """);
        when(authVersionBucket.get()).thenReturn("3");
        when(policyVersionBucket.get()).thenReturn("4");
        when(fenceBucket.isExists()).thenReturn(false);
        RedisAuthorizationRuntimeRepository store = new RedisAuthorizationRuntimeRepository(
                redisson,
                new ObjectMapper().findAndRegisterModules(),
                new Rbac3RuntimeKeyFactory(),
                Clock.fixed(NOW, ZoneOffset.UTC));

        var loaded = store.load("7", "99");

        assertThat(loaded.tenantId()).isEqualTo("7");
        assertThat(loaded.userId()).isEqualTo("9");
        assertThat(loaded.snapshot().checksum()).isEqualTo("sha256:test");
    }

    @Test
    void rejectsRuntimeWhenTheAuthoritativeVersionHasAdvanced() {
        RBucket<String> sessionBucket = mock(RBucket.class);
        RBucket<String> snapshotBucket = mock(RBucket.class);
        RBucket<String> authVersionBucket = mock(RBucket.class);
        RBucket<String> policyVersionBucket = mock(RBucket.class);
        RBucket<String> fenceBucket = mock(RBucket.class);
        RedissonClient redisson = mock(RedissonClient.class);
        when(redisson.<String>getBucket(
                "rbac3:{7}:session:99", StringCodec.INSTANCE)).thenReturn(sessionBucket);
        when(redisson.<String>getBucket(
                "rbac3:{7}:snapshot:99:1", StringCodec.INSTANCE)).thenReturn(snapshotBucket);
        when(redisson.<String>getBucket(
                "rbac3:{7}:auth-version:9", StringCodec.INSTANCE))
                .thenReturn(authVersionBucket);
        when(redisson.<String>getBucket(
                "rbac3:{7}:policy-version", StringCodec.INSTANCE))
                .thenReturn(policyVersionBucket);
        when(redisson.<String>getBucket(
                "rbac3:{7}:fence:session:99", StringCodec.INSTANCE))
                .thenReturn(fenceBucket);
        when(sessionBucket.get()).thenReturn("""
                {"tenantId":"7","userId":"9","identitySub":"9","sessionId":"99","status":"ACTIVE",
                 "authVersion":3,"sessionVersion":1,"policyVersion":4,
                 "expiresAt":"2026-07-30T13:00:00Z"}
                """);
        when(snapshotBucket.get()).thenReturn("""
                {"sessionId":"99","authVersion":3,"sessionVersion":1,
                 "policyVersion":4,"appContexts":[],"checksum":"sha256:test",
                 "generatedAt":"2026-07-30T12:00:00Z"}
                """);
        when(authVersionBucket.get()).thenReturn("4");
        when(policyVersionBucket.get()).thenReturn("4");
        when(fenceBucket.isExists()).thenReturn(false);
        RedisAuthorizationRuntimeRepository store = new RedisAuthorizationRuntimeRepository(
                redisson, new ObjectMapper().findAndRegisterModules(),
                new Rbac3RuntimeKeyFactory(), Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> store.load("7", "99"))
                .isInstanceOf(Rbac3RuleViolation.class)
                .hasMessageContaining("AUTH_VERSION_MISMATCH");
    }

    private PublishCommandDTO command() {
        RuntimeSessionVO session =
                new RuntimeSessionVO(
                        "7", "9", "9", "99", "ACTIVE", 3, 1, 4,
                        NOW.plusSeconds(3600));
        SessionAuthorizationSnapshot snapshot = new SessionAuthorizationSnapshot(
                "99", 3, 1, 4, List.of(), "sha256:test", NOW);
        return new PublishCommandDTO(
                "7", "9", "99", 3, 1, 4,
                new SessionSnapshotProjectionVO(session, snapshot));
    }
}
