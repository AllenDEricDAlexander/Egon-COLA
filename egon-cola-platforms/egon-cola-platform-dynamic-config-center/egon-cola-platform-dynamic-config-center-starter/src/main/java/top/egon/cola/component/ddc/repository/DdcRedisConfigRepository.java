package top.egon.cola.component.ddc.repository;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.ddc.common.DdcKeys;
import top.egon.cola.component.ddc.config.DdcProperties;

@Repository
@ConditionalOnProperty(
        prefix = "egon.cola.component.ddc.redis",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class DdcRedisConfigRepository {

    private final RedissonClient redissonClient;

    private final DdcProperties properties;

    public DdcRedisConfigRepository(
            @Qualifier("ddcRedissonClient") RedissonClient redissonClient,
            DdcProperties properties) {
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
