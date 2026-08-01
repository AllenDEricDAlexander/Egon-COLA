package top.egon.cola.component.ddc.repository;

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
        return redissonClient.<String>getBucket(DdcKeys.v3Config(
                properties.getBizCode(),
                properties.getEnv(),
                properties.getAppCode(),
                key
        )).get();
    }

    public Long readVersion(String key) {
        return redissonClient.<Long>getBucket(DdcKeys.v3Version(
                properties.getBizCode(),
                properties.getEnv(),
                properties.getAppCode(),
                key
        )).get();
    }
}
