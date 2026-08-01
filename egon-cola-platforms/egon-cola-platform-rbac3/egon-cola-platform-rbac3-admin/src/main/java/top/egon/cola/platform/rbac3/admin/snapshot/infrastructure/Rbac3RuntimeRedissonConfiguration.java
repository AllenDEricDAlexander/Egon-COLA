package top.egon.cola.platform.rbac3.admin.snapshot.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "egon.rbac3.runtime",
        name = "redis-enabled",
        havingValue = "true")
public class Rbac3RuntimeRedissonConfiguration {

    @Bean(name = "rbac3RuntimeRedissonClient", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "rbac3RuntimeRedissonClient")
    public RedissonClient rbac3RuntimeRedissonClient(
            ObjectMapper objectMapper,
            @Value("${egon.rbac3.runtime.redis-address}")
            String address,
            @Value("${egon.rbac3.runtime.redis-database:0}") int database,
            @Value("${egon.rbac3.runtime.redis-timeout:2s}") Duration timeout,
            @Value("${egon.rbac3.runtime.redis-password-file:}") String passwordFile
    ) {
        if (!address.startsWith("redis://") && !address.startsWith("rediss://")) {
            throw new IllegalArgumentException(
                    "RBAC3 runtime Redis address must use redis:// or rediss://");
        }
        Config config = new Config();
        config.setCodec(new JsonJacksonCodec(objectMapper.copy()));
        var server = config.useSingleServer()
                .setAddress(address)
                .setDatabase(database)
                .setTimeout(Math.toIntExact(timeout.toMillis()));
        String password = secret(passwordFile);
        if (password != null) {
            server.setPassword(password);
        }
        return Redisson.create(config);
    }

    private String secret(String file) {
        if (file == null || file.isBlank()) {
            return null;
        }
        try {
            String value = Files.readString(Path.of(file.trim())).trim();
            if (value.isEmpty()) {
                throw new IllegalArgumentException(
                        "RBAC3 runtime Redis password file is empty");
            }
            return value;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "cannot read RBAC3 runtime Redis password file", exception);
        }
    }
}
