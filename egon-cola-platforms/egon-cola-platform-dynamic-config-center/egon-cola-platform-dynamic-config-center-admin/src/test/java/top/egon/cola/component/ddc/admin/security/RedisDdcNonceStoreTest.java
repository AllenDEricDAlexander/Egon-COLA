package top.egon.cola.component.ddc.admin.security;

import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisDdcNonceStoreTest {

    @Test
    void sharesReplayStateWithoutStoringRawCredentialOrNonce() {
        RedissonClient redisson = mock(RedissonClient.class);
        @SuppressWarnings("unchecked")
        RBucket<String> bucket = mock(RBucket.class);
        AtomicBoolean present = new AtomicBoolean();
        AtomicReference<String> key = new AtomicReference<>();
        when(redisson.<String>getBucket(anyString())).thenAnswer(invocation -> {
            key.set(invocation.getArgument(0));
            return bucket;
        });
        when(bucket.trySet(
                "1",
                Duration.ofMinutes(5).toMillis(),
                TimeUnit.MILLISECONDS
        ))
                .thenAnswer(ignored -> present.compareAndSet(false, true));
        DdcNonceStore nodeOne = new RedisDdcNonceStore(redisson);
        DdcNonceStore nodeTwo = new RedisDdcNonceStore(redisson);

        assertThat(nodeOne.markIfAbsent(
                "sdk-access-key",
                "request-nonce",
                Duration.ofMinutes(5)
        )).isTrue();
        assertThat(nodeTwo.markIfAbsent(
                "sdk-access-key",
                "request-nonce",
                Duration.ofMinutes(5)
        )).isFalse();
        assertThat(key.get())
                .startsWith("ddc:security:nonce:{")
                .doesNotContain("sdk-access-key")
                .doesNotContain("request-nonce");
    }

    @Test
    void propagatesRedisOutageForFailClosedHandling() {
        RedissonClient redisson = mock(RedissonClient.class);
        when(redisson.getBucket(anyString()))
                .thenThrow(new IllegalStateException("redis unavailable"));
        DdcNonceStore store = new RedisDdcNonceStore(redisson);

        assertThatThrownBy(() -> store.markIfAbsent(
                "sdk-a",
                "nonce-a",
                Duration.ofMinutes(5)
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("redis unavailable");
    }
}
