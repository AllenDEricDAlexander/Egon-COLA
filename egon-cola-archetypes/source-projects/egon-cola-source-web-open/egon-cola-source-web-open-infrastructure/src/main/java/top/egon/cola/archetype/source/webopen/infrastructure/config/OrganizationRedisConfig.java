package top.egon.cola.archetype.source.webopen.infrastructure.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.common.cache.autoconfigure.EgonColaCacheProperties;

import java.time.Duration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 缓存连接装配：二级缓存是必需能力，缺省即开启，只在这里提供宿主侧的 Redisson 客户端。
 * 连接坐标全部来自显式配置，不提供隐藏地址；组件关闭时不创建任何连接。
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class OrganizationRedisConfig {

    @Bean("organizationRedisTemplate")
    @ConditionalOnProperty(prefix = "organization.integrations.redis", name = "enabled", havingValue = "true")
    RedisTemplate<String, Object> organizationRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean(name = "redissonClient", destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = EgonColaCacheProperties.PREFIX, name = "enabled",
            havingValue = "true", matchIfMissing = true)
    RedissonClient redissonClient(
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port:6379}") int port,
            @Value("${spring.data.redis.database:0}") int database,
            @Value("${spring.data.redis.username:}") String username,
            @Value("${spring.data.redis.password:}") String password,
            @Value("${spring.data.redis.ssl:false}") boolean ssl,
            @Value("${spring.data.redis.connect-timeout:3s}") Duration connectTimeout,
            @Value("${spring.data.redis.timeout:3s}") Duration timeout) {
        Config config = new Config();
        SingleServerConfig server = config.useSingleServer()
                .setAddress((ssl ? "rediss://" : "redis://") + host + ':' + port)
                .setDatabase(database)
                .setConnectTimeout((int) connectTimeout.toMillis())
                .setTimeout((int) timeout.toMillis());
        if (!username.isBlank()) {
            server.setUsername(username);
        }
        if (!password.isBlank()) {
            server.setPassword(password);
        }
        return Redisson.create(config);
    }
}
