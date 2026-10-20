package top.egon.cola.component.yuheng.edge.config;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.StringUtils;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;

import java.net.URI;
import java.time.Duration;

@Configuration(value = "edgeSessionRedisConfiguration", proxyBeanMethods = false)
public class EdgeSessionRedisConfiguration {
    @Bean(name = "redisson", destroyMethod = "shutdown")
    public RedissonClient edgePrimaryRedisson(
            RedisProperties redisProperties,
            @Value("${egon.cola.platform.tianquan.shoubing.revocation.primary-endpoint}") String primaryEndpoint,
            Environment environment) {
        URI primary = requireSharedPrimary(redisProperties, primaryEndpoint, environment);
        Config config = new Config();
        config.setCodec(StringCodec.INSTANCE);
        var server = config.useSingleServer()
                .setAddress(primary.toString())
                .setDatabase(redisProperties.getDatabase());
        if (StringUtils.hasText(redisProperties.getUsername())) {
            server.setUsername(redisProperties.getUsername());
        }
        if (StringUtils.hasText(redisProperties.getPassword())) {
            server.setPassword(redisProperties.getPassword());
        }
        Duration timeout = redisProperties.getTimeout();
        if (timeout != null && !timeout.isNegative() && !timeout.isZero()) {
            server.setTimeout(Math.toIntExact(timeout.toMillis()));
        }
        return Redisson.create(config);
    }

    @Bean(name = "edgeSessionRedisConnectionFactory")
    @Primary
    public RedisConnectionFactory edgeSessionRedisConnectionFactory(
            RedisProperties redisProperties,
            @Value("${egon.cola.platform.tianquan.shoubing.revocation.primary-endpoint}") String primaryEndpoint,
            Environment environment) {
        URI primary = requireSharedPrimary(redisProperties, primaryEndpoint, environment);

        RedisStandaloneConfiguration server = new RedisStandaloneConfiguration(
                primary.getHost(), primary.getPort() > 0 ? primary.getPort() : 6379);
        server.setDatabase(redisProperties.getDatabase());
        if (StringUtils.hasText(redisProperties.getUsername())) {
            server.setUsername(redisProperties.getUsername());
        }
        if (StringUtils.hasText(redisProperties.getPassword())) {
            server.setPassword(RedisPassword.of(redisProperties.getPassword()));
        }

        LettuceClientConfiguration.LettuceClientConfigurationBuilder client =
                LettuceClientConfiguration.builder();
        if ("rediss".equalsIgnoreCase(primary.getScheme())) {
            client.useSsl();
        }
        Duration timeout = redisProperties.getTimeout();
        if (timeout != null && !timeout.isNegative() && !timeout.isZero()) {
            client.commandTimeout(timeout);
        }
        if (StringUtils.hasText(redisProperties.getClientName())) {
            client.clientName(redisProperties.getClientName());
        }
        return new LettuceConnectionFactory(server, client.build());
    }

    @Bean(name = "springSessionDefaultRedisSerializer")
    public RedisSerializer<Object> springSessionDefaultRedisSerializer(
            @Qualifier("edgeSessionRedisSerializer")
            EdgeSessionRedisSerializer serializer) {
        return serializer;
    }

    private static URI requireSharedPrimary(RedisProperties redisProperties, String primaryEndpoint,
            Environment environment) {
        URI configured = parseConfiguredRedisUrl(redisProperties.getUrl());
        URI primary = parseConfiguredRedisUrl(primaryEndpoint);
        if (!sameEndpoint(configured, primary)) {
            throw new IllegalStateException("Spring Session and Shoubing Redis must use the same primary endpoint");
        }
        if ("redis".equalsIgnoreCase(primary.getScheme())
                && (!environment.acceptsProfiles("local")
                || !"127.0.0.1".equals(primary.getHost()))) {
            throw new IllegalStateException("Plaintext Edge Redis is allowed only on 127.0.0.1 in the local profile");
        }
        return primary;
    }

    private static URI parseConfiguredRedisUrl(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Redis primary endpoint URL is required");
        }
        URI uri = URI.create(value);
        String scheme = uri.getScheme();
        if (!"rediss".equalsIgnoreCase(scheme)
                && !"redis".equalsIgnoreCase(scheme)) {
            throw new IllegalStateException("Redis endpoint must use redis or rediss");
        }
        if (!StringUtils.hasText(uri.getHost())
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null) {
            throw new IllegalStateException("Redis endpoint must not contain credentials or options");
        }
        return uri;
    }

    private static boolean sameEndpoint(URI left, URI right) {
        return left.getScheme().equalsIgnoreCase(right.getScheme())
                && left.getHost().equalsIgnoreCase(right.getHost())
                && effectivePort(left) == effectivePort(right);
    }

    private static int effectivePort(URI uri) {
        return uri.getPort() > 0 ? uri.getPort() : 6379;
    }
}
