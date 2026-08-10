package top.egon.cola.platform.idp.admin.token.repo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import top.egon.cola.platform.idp.core.port.RefreshTokenStore;
import top.egon.cola.platform.idp.core.token.RefreshFamily;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnabledIfSystemProperty(
        named = "idp.test.redis.password-file",
        matches = ".+"
)
class RedisRefreshRotationLiveIT {

    private static final String DIGEST_A =
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final String DIGEST_B =
            "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB";
    private static final String DIGEST_C =
            "CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC";

    @Test
    void hostRedisExecutesSingleWinnerRotationAndReplayDetection()
            throws Exception {
        String address = System.getProperty(
                "idp.test.redis.address",
                "redis://127.0.0.1:6379"
        );
        String password = Files.readString(Path.of(System.getProperty(
                "idp.test.redis.password-file"
        ))).trim();
        Config config = new Config();
        config.useSingleServer()
                .setAddress(address)
                .setPassword(password);
        RedissonClient redisson = Redisson.create(config);
        String prefix = "identity:test:"
                + UUID.randomUUID().toString().replace("-", "") + ':';
        try {
            RedisRefreshTokenStore store = new RedisRefreshTokenStore(
                    redisson,
                    prefix
            );
            Instant now = Instant.now();
            Instant expiresAt = now.plusSeconds(3_600);
            store.create(new RefreshFamily(
                    "family-live-1",
                    "alice-sub",
                    "tenant-a",
                    "family-live-1",
                    "gateway-admin-web",
                    7L,
                    0L,
                    DIGEST_A,
                    RefreshFamily.Status.ACTIVE,
                    now,
                    now,
                    expiresAt
            ));

            assertEquals(
                    RefreshTokenStore.RotationOutcome.ROTATED,
                    store.rotate(command(
                            DIGEST_A,
                            DIGEST_B,
                            1L,
                            expiresAt,
                            now.plusMillis(1)
                    )).outcome()
            );
            assertEquals(
                    RefreshTokenStore.RotationOutcome.REPLAY,
                    store.rotate(command(
                            DIGEST_A,
                            DIGEST_C,
                            1L,
                            expiresAt,
                            now.plusMillis(2)
                    )).outcome()
            );
            assertEquals(
                    RefreshTokenStore.RotationOutcome.REVOKED,
                    store.rotate(command(
                            DIGEST_B,
                            DIGEST_C,
                            2L,
                            expiresAt,
                            now.plusMillis(3)
                    )).outcome()
            );
        } finally {
            redisson.getKeys().deleteByPattern(prefix + '*');
            redisson.shutdown();
        }
    }

    private RefreshTokenStore.RotationCommand command(
            String currentDigest,
            String successorDigest,
            long generation,
            Instant expiresAt,
            Instant now
    ) {
        return new RefreshTokenStore.RotationCommand(
                "family-live-1",
                currentDigest,
                successorDigest,
                generation,
                "alice-sub",
                7L,
                expiresAt,
                now
        );
    }
}
