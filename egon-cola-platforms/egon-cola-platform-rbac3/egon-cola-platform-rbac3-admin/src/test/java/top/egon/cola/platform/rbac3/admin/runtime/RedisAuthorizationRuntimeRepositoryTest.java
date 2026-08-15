package top.egon.cola.platform.rbac3.admin.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import top.egon.cola.platform.rbac3.admin.runtime.domain.dto.PublishCommandDTO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.RuntimeUserAuthorizationVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.UserSnapshotProjectionVO;
import top.egon.cola.platform.rbac3.admin.runtime.repository.redis.RedisAuthorizationRuntimeRepository;
import top.egon.cola.platform.rbac3.contract.authorization.ApplicationAccessScope;
import top.egon.cola.platform.rbac3.contract.authorization.BusinessAccessScope;
import top.egon.cola.platform.rbac3.contract.authorization.GatewayBizAppScopeSnapshot;
import top.egon.cola.platform.rbac3.contract.authorization.UserAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.core.runtime.Rbac3RuntimeKeyFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RedisAuthorizationRuntimeRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-08-15T00:00:00Z");
    private static final Duration TTL = Duration.ofHours(1);

    private final RedissonClient redisson = mock(RedissonClient.class);
    private final Rbac3RuntimeKeyFactory keyFactory = new Rbac3RuntimeKeyFactory();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    private final RBucket<String> userBucket = bucket();
    private final RBucket<String> fullSnapshotBucket = bucket();
    private final RBucket<String> gatewayScopeBucket = bucket();
    private final RBucket<String> authVersionBucket = bucket();
    private final RBucket<String> policyVersionBucket = bucket();
    private final RBucket<String> guardBucket = bucket();

    private RedisAuthorizationRuntimeRepository repository;

    @BeforeEach
    void setUp() {
        stubBucket(keyFactory.user("tenant-a", "subject-a"), userBucket);
        stubBucket(keyFactory.snapshot("tenant-a", "subject-a", 43L),
                fullSnapshotBucket);
        stubBucket(keyFactory.gatewayScope("tenant-a", "subject-a", 43L),
                gatewayScopeBucket);
        stubBucket(keyFactory.authVersion("tenant-a", "101"), authVersionBucket);
        stubBucket(keyFactory.policyVersion("tenant-a"), policyVersionBucket);
        stubBucket(keyFactory.authorizationPublicationGuard(
                "tenant-a", "subject-a"), guardBucket);
        when(userBucket.get()).thenReturn(null);
        repository = new RedisAuthorizationRuntimeRepository(
                redisson,
                objectMapper,
                keyFactory,
                Clock.fixed(NOW, ZoneOffset.UTC));
        clearInvocations(redisson);
    }

    @Test
    void publishesBothSnapshotsBeforePointerAndRemovesFenceLast() throws Exception {
        repository.publish(command(projection(43L, "tenant-a")));

        InOrder writes = inOrder(
                fullSnapshotBucket,
                gatewayScopeBucket,
                authVersionBucket,
                policyVersionBucket,
                userBucket,
                guardBucket);
        writes.verify(fullSnapshotBucket).set(anyString(), eq(TTL));
        writes.verify(gatewayScopeBucket).set(anyString(), eq(TTL));
        writes.verify(authVersionBucket).set("43", TTL);
        writes.verify(policyVersionBucket).set("3", TTL);
        writes.verify(userBucket).set(anyString(), eq(TTL));
        writes.verify(guardBucket).delete();

        ArgumentCaptor<String> scopeJson = ArgumentCaptor.forClass(String.class);
        verify(gatewayScopeBucket).set(scopeJson.capture(), any(Duration.class));
        GatewayBizAppScopeSnapshot written = objectMapper.readValue(
                scopeJson.getValue(), GatewayBizAppScopeSnapshot.class);
        assertThat(written.businesses().getFirst().businessCode())
                .isEqualTo("finance");
        assertThat(scopeJson.getValue()).doesNotContain("permissions");
    }

    @Test
    void scopeWriteFailureKeepsFenceAndDoesNotAdvancePointer() {
        doThrow(new IllegalStateException("scope write failed"))
                .when(gatewayScopeBucket).set(anyString(), any(Duration.class));

        assertThatThrownBy(() -> repository.publish(
                command(projection(43L, "tenant-a"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("scope write failed");

        verify(userBucket, never()).set(anyString(), any(Duration.class));
        verify(guardBucket, never()).delete();
    }

    @Test
    void rejectsScopeIdentityOrVersionMismatch() {
        assertThatThrownBy(() -> repository.publish(
                command(projection(44L, "tenant-a"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identity mismatch");
        assertThatThrownBy(() -> repository.publish(
                command(projection(43L, "tenant-b"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identity mismatch");

        verifyNoInteractions(redisson);
    }

    private PublishCommandDTO command(UserSnapshotProjectionVO projection) {
        return new PublishCommandDTO(
                "tenant-a", "subject-a", "101", 43L, 3L, projection);
    }

    private UserSnapshotProjectionVO projection(long scopeAuthVersion, String scopeTenant) {
        Instant expiresAt = NOW.plus(TTL);
        RuntimeUserAuthorizationVO user = new RuntimeUserAuthorizationVO(
                "tenant-a", "subject-a", "101", "ACTIVE", 43L, 3L, expiresAt);
        UserAuthorizationSnapshot snapshot = new UserAuthorizationSnapshot(
                "rbac3-admin", "tenant-a", "subject-a", "101", 43L, 3L,
                List.of(), "full-checksum", NOW, expiresAt);
        GatewayBizAppScopeSnapshot scope = new GatewayBizAppScopeSnapshot(
                scopeTenant,
                "subject-a",
                "101",
                scopeAuthVersion,
                3L,
                List.of(new BusinessAccessScope(
                        "business-1",
                        "finance",
                        List.of(new ApplicationAccessScope(
                                "application-1", "finance-web")))),
                "scope-checksum",
                NOW,
                expiresAt);
        return new UserSnapshotProjectionVO(user, snapshot, scope);
    }

    @SuppressWarnings("unchecked")
    private static RBucket<String> bucket() {
        return mock(RBucket.class);
    }

    private void stubBucket(String key, RBucket<String> value) {
        when(redisson.<String>getBucket(key, StringCodec.INSTANCE)).thenReturn(value);
    }
}
