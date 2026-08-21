package top.egon.cola.component.accessguard.store.redisson;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import top.egon.cola.component.accessguard.core.plan.AdmissionConfig;
import top.egon.cola.component.accessguard.store.PenaltyKey;
import top.egon.cola.component.accessguard.store.RateLimitDecision;
import top.egon.cola.component.accessguard.store.RateLimitRequest;
import top.egon.cola.component.accessguard.store.StoreOperationException;
import top.egon.cola.component.accessguard.store.local.LocalRateLimitBackend;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledIfSystemProperty(named = "egon.access.guard.redis.it", matches = "true")
class RedissonStoreIntegrationTest {

    private static final String HASH = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379);

    private static RedissonClient firstClient;
    private static RedissonClient secondClient;
    private static AccessGuardRedisKeyFactory keyFactory;

    @BeforeAll
    static void startRedis() {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker is unavailable");
        REDIS.start();
        firstClient = client();
        secondClient = client();
        keyFactory = new AccessGuardRedisKeyFactory("egon:access-guard:it", "test");
    }

    @AfterEach
    void flushRedis() {
        firstClient.getKeys().flushdb();
    }

    @AfterAll
    static void stopRedis() {
        if (firstClient != null) {
            firstClient.shutdown();
        }
        if (secondClient != null) {
            secondClient.shutdown();
        }
        if (REDIS.isRunning()) {
            REDIS.stop();
        }
    }

    @Test
    void twoClientsShareOneAtomicTokenBucket() {
        RedissonRateLimitBackend first = backend(firstClient);
        RedissonRateLimitBackend second = backend(secondClient);

        assertThat(first.acquire(request("shared", 1)).allowed()).isTrue();
        RateLimitDecision rejected = second.acquire(request("shared", 1));

        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.retryAfter()).isPositive();
    }

    @ParameterizedTest
    @EnumSource(AdmissionConfig.RateLimitAlgorithm.class)
    void twoClientsShareOneAtomicQuotaForEveryAlgorithm(
            AdmissionConfig.RateLimitAlgorithm algorithm) {
        RedissonRateLimitBackend first = backend(firstClient);
        RedissonRateLimitBackend second = backend(secondClient);

        assertThat(first.acquire(request("shared-" + algorithm, algorithm, 1)).allowed()).isTrue();
        RateLimitDecision rejected = second.acquire(request("shared-" + algorithm, algorithm, 1));

        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.retryAfter()).isPositive();
    }

    @Test
    void algorithmSwitchUsesIndependentSuffixedStateWithoutWrongType() {
        RedissonRateLimitBackend first = backend(firstClient);

        assertThat(first.acquire(request("switch", AdmissionConfig.RateLimitAlgorithm.TOKEN_BUCKET, 1))
                .allowed()).isTrue();
        assertThat(first.acquire(request("switch", AdmissionConfig.RateLimitAlgorithm.LEAKY_BUCKET, 1))
                .allowed()).isTrue();
        assertThat(first.acquire(request("switch", AdmissionConfig.RateLimitAlgorithm.SLIDING_WINDOW, 1))
                .allowed()).isTrue();
    }

    @Test
    void penaltyThresholdAndTtlAreSharedAcrossClients() throws Exception {
        RedissonPenaltyStore first = new RedissonPenaltyStore(firstClient, keyFactory);
        RedissonPenaltyStore second = new RedissonPenaltyStore(secondClient, keyFactory);
        PenaltyKey key = new PenaltyKey("draw", "penalty", HASH);

        assertThat(first.recordViolation(key, 2, Duration.ofSeconds(2), Duration.ofMillis(200)).active())
                .isFalse();
        assertThat(second.recordViolation(key, 2, Duration.ofSeconds(2), Duration.ofMillis(200)).active())
                .isTrue();
        Thread.sleep(250);

        assertThat(first.current(key)).isEmpty();
    }

    @Test
    void outageUsesLocalFallbackAndTheNextHealthyCallRetriesRedis() throws Exception {
        RedissonRateLimitBackend primary = backend(firstClient);
        LocalRateLimitBackend fallback =
                new LocalRateLimitBackend(System::nanoTime, 10, Duration.ofMinutes(1));
        DockerClientFactory.instance().client().pauseContainerCmd(REDIS.getContainerId()).exec();
        try {
            assertThatThrownBy(() -> primary.acquire(request("outage", 1)))
                    .isInstanceOf(StoreOperationException.class);
            assertThat(fallback.acquire(request("outage", 1)).allowed()).isTrue();
        } finally {
            DockerClientFactory.instance().client().unpauseContainerCmd(REDIS.getContainerId()).exec();
        }

        RateLimitDecision recovered = retryUntilRedisRecovers(primary);
        assertThat(recovered.allowed()).isTrue();
    }

    private static RateLimitDecision retryUntilRedisRecovers(RedissonRateLimitBackend backend) throws Exception {
        StoreOperationException lastFailure = null;
        for (int attempt = 0; attempt < 50; attempt++) {
            try {
                return backend.acquire(request("recovered", 1));
            } catch (StoreOperationException exception) {
                lastFailure = exception;
                Thread.sleep(100);
            }
        }
        throw lastFailure;
    }

    private static RedissonRateLimitBackend backend(RedissonClient client) {
        return new RedissonRateLimitBackend(client, keyFactory, Duration.ofMinutes(1));
    }

    private static RateLimitRequest request(String version, long requested) {
        return new RateLimitRequest(
                "draw", version, HASH, 1, 1, Duration.ofSeconds(1), requested);
    }

    private static RateLimitRequest request(
            String version,
            AdmissionConfig.RateLimitAlgorithm algorithm,
            long requested) {
        return new RateLimitRequest(
                "draw", version, HASH, algorithm, 1, 1,
                Duration.ofSeconds(1), requested);
    }

    private static RedissonClient client() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + REDIS.getHost() + ':' + REDIS.getMappedPort(6379))
                .setConnectTimeout(500)
                .setTimeout(500)
                .setRetryAttempts(0);
        return Redisson.create(config);
    }
}
