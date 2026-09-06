package top.egon.cola.platform.rbac3.admin.authorization.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.domain.dto.PublishCommandDTO;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.domain.vo.RuntimeUserAuthorizationVO;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.domain.vo.UserSnapshotProjectionVO;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.repository.InitialAuthorizationContextRepository;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.repository.InitialAuthorizationContextRepository.InitialAuthorizationContext;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.repository.redis.RedisAuthorizationRuntimeRepository;
import top.egon.cola.platform.rbac3.contract.authorization.GatewayBizAppScopeSnapshot;
import top.egon.cola.platform.rbac3.contract.authorization.UserAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.core.runtime.Rbac3RuntimeKeyFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RedisAuthorizationRuntimeRepositoryTest {

    static final Instant NOW = Instant.parse("2026-08-15T00:00:00Z");
    private final RedissonClient redisson = mock(RedissonClient.class);
    private final RScript script = mock(RScript.class);
    private final Rbac3RuntimeKeyFactory keys = new Rbac3RuntimeKeyFactory();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final InitialAuthorizationContextRepository context = mock(InitialAuthorizationContextRepository.class);
    private RedisAuthorizationRuntimeRepository repository;

    @BeforeEach
    void setUp() {
        when(redisson.getScript(StringCodec.INSTANCE)).thenReturn(script);
        when(script.<Long>eval(any(RScript.Mode.class), anyString(), any(RScript.ReturnType.class),
                anyList(), any(Object[].class))).thenReturn(1L);
        current(43L, 3L);
        repository = new RedisAuthorizationRuntimeRepository(redisson, mapper, keys,
                Clock.fixed(NOW, ZoneOffset.UTC), context);
    }

    @Test
    void publishesBothSnapshotsAndVersionsInOneAtomicOperation() {
        var result = repository.publish(command("tenant-a", 43L, 3L));
        assertThat(result).isNotNull();
        verify(script).eval(any(RScript.Mode.class), anyString(), any(RScript.ReturnType.class),
                anyList(), any(Object[].class));
    }

    @Test
    void rejectsRedisVersionRegression() {
        when(script.<Long>eval(any(RScript.Mode.class), anyString(), any(RScript.ReturnType.class),
                anyList(), any(Object[].class))).thenReturn(-1L);
        assertThatThrownBy(() -> repository.publish(command("tenant-a", 43L, 3L)))
                .hasMessage("RBAC3_RUNTIME_VERSION_CONFLICT");
    }

    @Test
    void doesNotPublishAnAlreadyRevokedPolicy() {
        current(43L, 4L);
        assertThatThrownBy(() -> repository.publish(command("tenant-a", 43L, 3L)))
                .hasMessageContaining("POLICY_VERSION_MISMATCH");
        verifyNoInteractions(script);
    }

    @Test
    void rejectsPublicationIdentityMismatch() {
        var projection = command("tenant-b", 43L, 3L).projection();
        assertThatThrownBy(() -> repository.publish(new PublishCommandDTO(
                "tenant-a", "subject-a", "101", 43L, 3L, projection)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identity mismatch");
        verifyNoInteractions(script);
    }

    @Test
    void loadRejectsCommittedRevocationBeforeOutboxHasUpdatedRedis() throws Exception {
        storedSnapshot();
        current(43L, 4L);
        assertThatThrownBy(() -> repository.load("tenant-a", "subject-a"))
                .hasMessageContaining("POLICY_VERSION_MISMATCH");
    }

    @Test
    void loadRejectsChangedUserVersionAndInactiveMembership() throws Exception {
        storedSnapshot();
        current(44L, 3L);
        assertThatThrownBy(() -> repository.load("tenant-a", "subject-a"))
                .hasMessageContaining("AUTH_VERSION_MISMATCH");
        when(context.find("tenant-a", "subject-a")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> repository.load("tenant-a", "subject-a"))
                .hasMessageContaining("IDENTITY_INACTIVE");
    }

    @Test
    void loadAcceptsMatchingAuthoritativeVersions() throws Exception {
        storedSnapshot();
        assertThat(repository.load("tenant-a", "subject-a").snapshot().policyVersion()).isEqualTo(3L);
    }

    private void current(long authVersion, long policyVersion) {
        when(context.find("tenant-a", "subject-a")).thenReturn(Optional.of(
                new InitialAuthorizationContext("101", authVersion, policyVersion)));
    }

    private void storedSnapshot() throws Exception {
        var projection = command("tenant-a", 43L, 3L).projection();
        stored(keys.user("tenant-a", "subject-a"), mapper.writeValueAsString(projection.user()));
        stored(keys.snapshot("tenant-a", "subject-a", 43L), mapper.writeValueAsString(projection.snapshot()));
        stored(keys.authVersion("tenant-a", "101"), "43");
        stored(keys.policyVersion("tenant-a"), "3");
        stored(keys.authorizationPublicationGuard("tenant-a", "subject-a"), null);
    }

    private void stored(String key, String value) {
        @SuppressWarnings("unchecked") RBucket<String> bucket = mock(RBucket.class);
        when(redisson.<String>getBucket(key, StringCodec.INSTANCE)).thenReturn(bucket);
        when(bucket.get()).thenReturn(value);
    }

    static PublishCommandDTO command(String tenant, long authVersion, long policyVersion) {
        Instant expiresAt = NOW.plus(Duration.ofHours(1));
        var user = new RuntimeUserAuthorizationVO(tenant, "subject-a", "101", "ACTIVE",
                authVersion, policyVersion, expiresAt);
        var snapshot = new UserAuthorizationSnapshot("rbac3-admin", tenant, "subject-a", "101",
                authVersion, policyVersion, List.of(), "full-" + policyVersion, NOW, expiresAt);
        var scope = new GatewayBizAppScopeSnapshot(tenant, "subject-a", "101", authVersion,
                policyVersion, List.of(), "scope-" + policyVersion, NOW, expiresAt);
        return new PublishCommandDTO(tenant, "subject-a", "101", authVersion, policyVersion,
                new UserSnapshotProjectionVO(user, snapshot, scope));
    }
}
