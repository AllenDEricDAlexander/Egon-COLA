package top.egon.cola.platform.idp.starter.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisIdentityUserStateReaderTest {

    @Test
    void readsTheSameStringCodecKeyWrittenByIdp() {
        RedissonClient redisson = mock(RedissonClient.class);
        @SuppressWarnings("unchecked")
        RBucket<String> bucket = mock(RBucket.class);
        when(redisson.<String>getBucket(
                "identity:v1:user:identity-1", StringCodec.INSTANCE))
                .thenReturn(bucket);
        when(bucket.get()).thenReturn("""
                {"subject":"identity-1","status":"ACTIVE",\
                "tokenVersion":7,"updatedAt":"2026-08-02T08:00:00Z"}
                """);
        var reader = new RedisIdentityUserStateReader(
                redisson,
                new ObjectMapper().findAndRegisterModules(),
                "identity:v1:user:");

        assertThat(reader.read("identity-1"))
                .get()
                .extracting(state -> state.tokenVersion())
                .isEqualTo(7L);
        verify(redisson).getBucket(
                "identity:v1:user:identity-1", StringCodec.INSTANCE);
    }
}
