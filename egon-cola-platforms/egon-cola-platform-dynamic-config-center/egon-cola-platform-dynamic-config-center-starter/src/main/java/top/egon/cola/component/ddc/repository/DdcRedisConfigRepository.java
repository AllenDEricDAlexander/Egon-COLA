package top.egon.cola.component.ddc.repository;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.ddc.common.DdcKeys;
import top.egon.cola.component.ddc.config.DdcProperties;

/**
 * 从 DDC Redis 键空间读取当前配置值和版本。
 * Reads current configuration values and versions from the DDC Redis keyspace.
 */
@Repository
@ConditionalOnProperty(
        prefix = "egon.cola.component.ddc.redis",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class DdcRedisConfigRepository {

    /**
     * DDC 专用 Redisson 客户端。 Redisson client dedicated to DDC.
     */
    private final RedissonClient redissonClient;

    /**
     * 用于构造作用域 Redis 键的 DDC 配置。 DDC settings used to build scoped Redis keys.
     */
    private final DdcProperties properties;

    /**
     * 创建 Redis 配置仓库。
     * Creates the Redis configuration repository.
     *
     * @param redissonClient DDC 专用 Redisson 客户端; Redisson client dedicated to DDC
     * @param properties     DDC 客户端配置; DDC client configuration
     */
    public DdcRedisConfigRepository(
            @Qualifier("ddcRedissonClient") RedissonClient redissonClient,
            DdcProperties properties) {
        this.redissonClient = redissonClient;
        this.properties = properties;
    }

    /**
     * 读取当前作用域中指定配置键的值。
     * Reads the value of a configuration key in the current scope.
     *
     * @param key 配置键; configuration key
     * @return 配置值，不存在时为 {@code null}; configuration value, or {@code null} when absent
     */
    public String readValue(String key) {
        return redissonClient.<String>getBucket(DdcKeys.config(
                properties.getBizCode(),
                properties.getEnv(),
                properties.getAppCode(),
                key
        )).get();
    }

    /**
     * 读取当前作用域中指定配置键的版本。
     * Reads the version of a configuration key in the current scope.
     *
     * @param key 配置键; configuration key
     * @return 配置版本，不存在时为 {@code null}; configuration version, or {@code null} when absent
     */
    public Long readVersion(String key) {
        return redissonClient.<Long>getBucket(DdcKeys.version(
                properties.getBizCode(),
                properties.getEnv(),
                properties.getAppCode(),
                key
        )).get();
    }
}
