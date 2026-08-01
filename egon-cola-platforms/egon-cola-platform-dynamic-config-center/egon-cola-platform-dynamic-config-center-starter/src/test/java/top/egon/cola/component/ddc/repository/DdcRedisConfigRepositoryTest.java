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
    void readsOnlyThePhysicalV3ValueAndVersion() {
        RedissonClient redisson = mock(RedissonClient.class);
        RBucket<String> value = bucket("v3");
        RBucket<Long> version = bucket(3L);
        stubBuckets(redisson, value, version);

        DdcRedisConfigRepository repository = repository(redisson);

        assertThat(repository.readValue("switch")).isEqualTo("v3");
        assertThat(repository.readVersion("switch")).isEqualTo(3L);
    }

    @Test
    void deprecatedNamespaceDoesNotChangeThePhysicalConfigKey() {
        RedissonClient redisson = mock(RedissonClient.class);
        RBucket<String> value = bucket("same");
        RBucket<Long> version = bucket(3L);
        stubBuckets(redisson, value, version);
        DdcProperties properties = properties();
        DdcRedisConfigRepository repository =
                new DdcRedisConfigRepository(redisson, properties);

        properties.setNamespace("namespace-a");
        assertThat(repository.readValue("switch")).isEqualTo("same");
        properties.setNamespace("namespace-b");
        assertThat(repository.readValue("switch")).isEqualTo("same");

        verify(value, org.mockito.Mockito.times(2)).get();
    }

    @SuppressWarnings("unchecked")
    private <T> RBucket<T> bucket(T value) {
        RBucket<T> bucket = mock(RBucket.class);
        when(bucket.get()).thenReturn(value);
        return bucket;
    }

    private void stubBuckets(RedissonClient redisson,
                             RBucket<String> value,
                             RBucket<Long> version) {
        when(redisson.<String>getBucket(DdcKeys.v3Config(
                "retail", "dev", "demo", "switch"
        ))).thenReturn(value);
        when(redisson.<Long>getBucket(DdcKeys.v3Version(
                "retail", "dev", "demo", "switch"
        ))).thenReturn(version);
    }

    private DdcRedisConfigRepository repository(RedissonClient redisson) {
        return new DdcRedisConfigRepository(redisson, properties());
    }

    private DdcProperties properties() {
        DdcProperties properties = new DdcProperties();
        properties.setBizCode("retail");
        properties.setAppCode("demo");
        properties.setEnv("dev");
        properties.setNamespace("default");
        return properties;
    }
}
