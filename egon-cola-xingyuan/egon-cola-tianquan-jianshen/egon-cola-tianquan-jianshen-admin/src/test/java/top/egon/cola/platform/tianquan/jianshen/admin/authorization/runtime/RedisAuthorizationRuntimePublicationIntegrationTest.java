package top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.repository.InitialAuthorizationContextRepository.InitialAuthorizationContext;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.repository.redis.RedisAuthorizationRuntimeRepository;
import top.egon.cola.platform.tianquan.jianshen.core.runtime.Rbac3RuntimeKeyFactory;
import top.egon.cola.platform.tianquan.jianshen.starter.cache.AuthorizationSnapshotCache;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/** Uses isolated keys on an explicitly selected Redis; never starts or flushes a server. */
@EnabledIfEnvironmentVariable(named = "RBAC3_TEST_REDIS_ADDRESS", matches = ".+")
class RedisAuthorizationRuntimePublicationIntegrationTest {

    private final String tenant = "qa-publication-" + UUID.randomUUID();
    private final Rbac3RuntimeKeyFactory keys = new Rbac3RuntimeKeyFactory();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private RedissonClient redisson;

    @BeforeEach
    void connect() throws Exception {
        Config config = new Config();
        var server = config.useSingleServer().setAddress(System.getenv("RBAC3_TEST_REDIS_ADDRESS"))
                .setConnectionPoolSize(4).setConnectionMinimumIdleSize(1)
                .setDatabase(Integer.parseInt(System.getenv().getOrDefault("RBAC3_TEST_REDIS_DATABASE", "0")));
        String passwordFile = System.getenv("RBAC3_TEST_REDIS_PASSWORD_FILE");
        if (passwordFile != null) {
            server.setPassword(Files.readString(Path.of(passwordFile)).trim());
        }
        redisson = Redisson.create(config);
    }

    @AfterEach
    void cleanup() {
        if (redisson != null) {
            try {
                redisson.getKeys().delete(keys.user(tenant, "subject-a"), keys.authVersion(tenant, "101"),
                        keys.policyVersion(tenant), keys.authorizationPublicationGuard(tenant, "subject-a"),
                        keys.snapshot(tenant, "subject-a", 43L), keys.gatewayScope(tenant, "subject-a", 43L),
                        keys.snapshot(tenant, "subject-a", 44L), keys.gatewayScope(tenant, "subject-a", 44L));
            } finally {
                redisson.shutdown();
            }
        }
    }

    @Test
    void stalePolicyCannotOverwriteSnapshotsAtTheSameAuthVersion() throws Exception {
        publish(43L, 4L);
        assertThatThrownBy(() -> publish(43L, 3L)).hasMessage("RBAC3_RUNTIME_VERSION_CONFLICT");
        assertPublished(43L, 4L);
    }

    @Test
    void staleInvalidationCannotRemoveANewerPublication() throws Exception {
        publish(44L, 4L);
        var repository = new RedisAuthorizationRuntimeRepository(redisson, mapper, keys,
                Clock.fixed(RedisAuthorizationRuntimeRepositoryTest.NOW, ZoneOffset.UTC),
                (tenantId, subject) -> Optional.empty(), mock(AuthorizationSnapshotCache.class));
        assertThatThrownBy(() -> repository.invalidate(tenant, "subject-a", "101", 43L, 4L))
                .hasMessage("RBAC3_RUNTIME_VERSION_CONFLICT");
        assertPublished(44L, 4L);
        repository.invalidate(tenant, "subject-a", "101", 45L, 4L);
        assertThat(redisson.getBucket(keys.user(tenant, "subject-a")).isExists()).isFalse();
        assertThatThrownBy(() -> publish(44L, 4L)).hasMessage("RBAC3_RUNTIME_VERSION_CONFLICT");
    }

    @Test
    void watermarksSurviveSnapshotExpiryAndRejectOlderUserVersion() {
        publish(44L, 4L);
        redisson.getKeys().delete(keys.user(tenant, "subject-a"),
                keys.snapshot(tenant, "subject-a", 44L), keys.gatewayScope(tenant, "subject-a", 44L));
        assertThatThrownBy(() -> publish(43L, 4L)).hasMessage("RBAC3_RUNTIME_VERSION_CONFLICT");
        assertThat(redisson.getBucket(keys.authVersion(tenant, "101")).remainTimeToLive()).isEqualTo(-1L);
        assertThat(redisson.getBucket(keys.policyVersion(tenant)).remainTimeToLive()).isEqualTo(-1L);
    }

    @Test
    void decimalVersionsRemainExactAboveLuaIntegerPrecision() throws Exception {
        publish(43L, 9007199254740993L);
        assertThatThrownBy(() -> publish(43L, 9007199254740992L))
                .hasMessage("RBAC3_RUNTIME_VERSION_CONFLICT");
        assertPublished(43L, 9007199254740993L);
    }

    @Test
    void concurrentPolicyPublicationsKeepAllDocumentsAtTheNewestVersion() throws Exception {
        try (var executor = Executors.newFixedThreadPool(8)) {
            List<java.util.concurrent.Future<?>> writes = new ArrayList<>();
            for (long version = 1L; version <= 32L; version++) {
                long policyVersion = version;
                writes.add(executor.submit(() -> {
                    try {
                        publish(43L, policyVersion);
                    } catch (IllegalStateException conflict) {
                        assertThat(conflict).hasMessage("RBAC3_RUNTIME_VERSION_CONFLICT");
                    }
                }));
            }
            for (var write : writes) {
                write.get(20L, TimeUnit.SECONDS);
            }
        }
        assertPublished(43L, 32L);
    }

    private void publish(long authVersion, long policyVersion) {
        // Each publisher models facts read before a competing transaction committed.
        var repository = new RedisAuthorizationRuntimeRepository(redisson, mapper, keys,
                Clock.fixed(RedisAuthorizationRuntimeRepositoryTest.NOW, ZoneOffset.UTC),
                (tenantId, subject) -> Optional.of(new InitialAuthorizationContext("101", authVersion, policyVersion)),
                mock(AuthorizationSnapshotCache.class));
        repository.publish(RedisAuthorizationRuntimeRepositoryTest.command(tenant, authVersion, policyVersion));
    }

    private void assertPublished(long authVersion, long policyVersion) throws Exception {
        assertThat(redisson.getBucket(keys.authVersion(tenant, "101"), StringCodec.INSTANCE).get())
                .isEqualTo(Long.toString(authVersion));
        assertThat(redisson.getBucket(keys.policyVersion(tenant), StringCodec.INSTANCE).get())
                .isEqualTo(Long.toString(policyVersion));
        for (String key : List.of(keys.user(tenant, "subject-a"),
                keys.snapshot(tenant, "subject-a", authVersion), keys.gatewayScope(tenant, "subject-a", authVersion))) {
            String value = redisson.<String>getBucket(key, StringCodec.INSTANCE).get();
            var document = mapper.readTree(value);
            assertThat(document.path("authVersion").asLong()).isEqualTo(authVersion);
            assertThat(document.path("policyVersion").asLong()).isEqualTo(policyVersion);
        }
    }
}
