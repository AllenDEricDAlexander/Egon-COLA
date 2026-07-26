package top.egon.cola.component.ddc.repository;

import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import top.egon.cola.component.ddc.common.DdcKeys;
import top.egon.cola.component.ddc.config.DdcProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DdcRedisConfigRepositoryTest {

    @Test
    void readsV2ValueAndVersionWithoutTouchingLegacyKeys() {
        RedissonClient redisson = mock(RedissonClient.class);
        RBucket<String> v2Value = bucket("v2");
        RBucket<Long> v2Version = bucket(2L);
        RBucket<String> legacyValue = bucket("legacy");
        RBucket<Long> legacyVersion = bucket(1L);
        stubBuckets(redisson, v2Value, v2Version, legacyValue, legacyVersion);

        DdcRedisConfigRepository repository = repository(redisson);

        assertThat(repository.readValue("switch")).isEqualTo("v2");
        assertThat(repository.readVersion("switch")).isEqualTo(2L);
        verifyNoInteractions(legacyValue, legacyVersion);
    }

    @Test
    void fallsBackToLegacyOnlyWhenV2ValueOrVersionIsAbsent() {
        RedissonClient redisson = mock(RedissonClient.class);
        RBucket<String> v2Value = bucket(null);
        RBucket<Long> v2Version = bucket(null);
        RBucket<String> legacyValue = bucket("legacy");
        RBucket<Long> legacyVersion = bucket(1L);
        stubBuckets(redisson, v2Value, v2Version, legacyValue, legacyVersion);

        DdcRedisConfigRepository repository = repository(redisson);

        assertThat(repository.readValue("switch")).isEqualTo("legacy");
        assertThat(repository.readVersion("switch")).isEqualTo(1L);
        verify(legacyValue).get();
        verify(legacyVersion).get();
    }

    @SuppressWarnings("unchecked")
    private <T> RBucket<T> bucket(T value) {
        RBucket<T> bucket = mock(RBucket.class);
        when(bucket.get()).thenReturn(value);
        return bucket;
    }

    private void stubBuckets(RedissonClient redisson,
                             RBucket<String> v2Value,
                             RBucket<Long> v2Version,
                             RBucket<String> legacyValue,
                             RBucket<Long> legacyVersion) {
        when(redisson.<String>getBucket(DdcKeys.v2Config(
                "demo", "dev", "default", "switch"
        ))).thenReturn(v2Value);
        when(redisson.<Long>getBucket(DdcKeys.v2Version(
                "demo", "dev", "default", "switch"
        ))).thenReturn(v2Version);
        when(redisson.<String>getBucket(DdcKeys.config(
                "demo", "dev", "default", "switch"
        ))).thenReturn(legacyValue);
        when(redisson.<Long>getBucket(DdcKeys.version(
                "demo", "dev", "default", "switch"
        ))).thenReturn(legacyVersion);
    }

    private DdcRedisConfigRepository repository(RedissonClient redisson) {
        DdcProperties properties = new DdcProperties();
        properties.setAppCode("demo");
        properties.setEnv("dev");
        properties.setNamespace("default");
        return new DdcRedisConfigRepository(redisson, properties);
    }
}
