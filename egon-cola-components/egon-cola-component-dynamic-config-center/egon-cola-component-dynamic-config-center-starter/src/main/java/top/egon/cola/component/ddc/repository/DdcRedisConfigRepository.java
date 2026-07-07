package top.egon.cola.component.ddc.repository;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import top.egon.cola.component.ddc.common.DdcKeys;
import top.egon.cola.component.ddc.config.DdcProperties;

public class DdcRedisConfigRepository {

    private final RedissonClient redissonClient;

    private final DdcProperties properties;

    public DdcRedisConfigRepository(RedissonClient redissonClient, DdcProperties properties) {
        this.redissonClient = redissonClient;
        this.properties = properties;
    }

    public String readValue(String key) {
        RBucket<String> bucket = redissonClient.getBucket(DdcKeys.config(properties.getAppCode(), properties.getEnv(), properties.getNamespace(), key));
        return bucket.get();
    }

    public Long readVersion(String key) {
        RBucket<Long> bucket = redissonClient.getBucket(DdcKeys.version(properties.getAppCode(), properties.getEnv(), properties.getNamespace(), key));
        return bucket.get();
    }
}
