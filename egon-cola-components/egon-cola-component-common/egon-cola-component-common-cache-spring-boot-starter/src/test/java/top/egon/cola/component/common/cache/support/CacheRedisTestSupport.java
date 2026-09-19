package top.egon.cola.component.common.cache.support;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.convert.ApplicationConversionService;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import top.egon.cola.component.common.cache.autoconfigure.EgonColaCacheProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Testcontainers Redis 共享夹具：单容器、每节点独立客户端构造原语与属性绑定原语。
 * 门控属性 {@code egon.cola.cache.redis.it} 与镜像版本镜像 access-guard 既有先例。
 */
@EnabledIfSystemProperty(named = "egon.cola.cache.redis.it", matches = "true")
public abstract class CacheRedisTestSupport {

    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379);
    private static final List<RedissonClient> CLIENTS = new ArrayList<>();

    @AfterEach
    void flushRedis() {
        if (!CLIENTS.isEmpty()) {
            CLIENTS.get(CLIENTS.size() - 1).getKeys().flushdb();
        }
    }

    @AfterAll
    static void stopRedis() {
        for (RedissonClient client : CLIENTS) {
            client.shutdown();
        }
        CLIENTS.clear();
        if (REDIS.isRunning()) {
            REDIS.stop();
        }
    }

    protected static RedissonClient newClient() {
        ensureRedis();
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + REDIS.getHost() + ':' + REDIS.getMappedPort(6379))
                .setConnectTimeout(1000)
                .setTimeout(500)
                .setRetryAttempts(0);
        RedissonClient client = Redisson.create(config);
        CLIENTS.add(client);
        return client;
    }

    /**
     * 容器惰性拉起：Docker 缺失只逐用例 assumption 跳过，纯单测段（如抖动采样）照常执行。
     */
    private static synchronized void ensureRedis() {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker is unavailable");
        if (!REDIS.isRunning()) {
            REDIS.start();
        }
    }

    /**
     * 默认键树 + 测试 keyPrefix，overrides 走与宿主一致的 Boot relaxed 绑定路径。
     */
    protected static EgonColaCacheProperties props(Map<String, Object> overrides) {
        EgonColaCacheProperties defaults = new EgonColaCacheProperties();
        defaults.setKeyPrefix("egon:cola:cache:it");
        Binder binder = new Binder(List.of(new MapConfigurationPropertySource(overrides)), null,
                ApplicationConversionService.getSharedInstance());
        return binder.bind("egon.cola.component.cache", Bindable.ofInstance(defaults)).orElse(defaults);
    }
}
