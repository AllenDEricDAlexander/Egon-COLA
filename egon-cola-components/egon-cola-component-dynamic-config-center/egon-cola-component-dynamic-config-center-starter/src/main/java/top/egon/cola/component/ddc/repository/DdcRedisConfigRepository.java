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
        String value = redissonClient.<String>getBucket(DdcKeys.v2Config(
                properties.getAppCode(), properties.getEnv(), properties.getNamespace(), key
        )).get();
        if (value != null) {
            return value;
        }
        RBucket<String> legacy = redissonClient.getBucket(DdcKeys.config(
                properties.getAppCode(), properties.getEnv(), properties.getNamespace(), key
        ));
        return legacy.get();
    }

    public Long readVersion(String key) {
        Long version = redissonClient.<Long>getBucket(DdcKeys.v2Version(
                properties.getAppCode(), properties.getEnv(), properties.getNamespace(), key
        )).get();
        if (version != null) {
            return version;
        }
        RBucket<Long> legacy = redissonClient.getBucket(DdcKeys.version(
                properties.getAppCode(), properties.getEnv(), properties.getNamespace(), key
        ));
        return legacy.get();
    }
}
