package top.egon.cola.platform.idp.admin.oauth.repo;

import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class RedisClientAssertionReplayStoreTest {

    @Test
    @SuppressWarnings("unchecked")
    void atomicallyStoresClientAndAssertionUntilExpiration() {
        Instant now = Instant.parse("2026-08-10T00:00:00Z");
        RedissonClient redisson = mock(RedissonClient.class);
        RBucket<String> bucket = mock(RBucket.class);
        when(redisson.<String>getBucket(
                "identity:v1:client-assertion-replay:"
                        + "idp-service:assertion-1"
        )).thenReturn(bucket);
        when(bucket.setIfAbsent("1", Duration.ofSeconds(60)))
                .thenReturn(true, false);
        RedisClientAssertionReplayStore store =
                new RedisClientAssertionReplayStore(
                        redisson,
                        "identity:v1:client-assertion-replay:",
                        Clock.fixed(now, ZoneOffset.UTC)
                );

        assertThat(store.markIfAbsent(
                "idp-service",
                "assertion-1",
                now.plusSeconds(60)
        )).isTrue();
        assertThat(store.markIfAbsent(
                "idp-service",
                "assertion-1",
                now.plusSeconds(60)
        )).isFalse();

        verify(bucket, times(2)).setIfAbsent("1", Duration.ofSeconds(60));
    }
}
