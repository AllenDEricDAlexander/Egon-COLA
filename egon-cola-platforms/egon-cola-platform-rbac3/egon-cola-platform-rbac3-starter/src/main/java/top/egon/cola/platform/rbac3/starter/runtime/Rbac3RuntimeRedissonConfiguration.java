package top.egon.cola.platform.rbac3.starter.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.platform.rbac3.starter.autoconfigure.Rbac3StarterProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "egon.cola.platform.rbac3.runtime",
        name = "redis-enabled",
        havingValue = "true")
public class Rbac3RuntimeRedissonConfiguration {

    @Bean(name = "rbac3RuntimeRedissonClient", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "rbac3RuntimeRedissonClient")
    public RedissonClient rbac3RuntimeRedissonClient(
            Rbac3StarterProperties properties,
            ObjectMapper objectMapper
    ) {
        var runtime = properties.getRuntime();
        String address = runtime.getRedisAddress();
        if (address == null || (!address.startsWith("redis://")
                && !address.startsWith("rediss://"))) {
            throw new IllegalArgumentException(
                    "RBAC3 runtime Redis address must use redis:// or rediss://");
        }
        Config config = new Config();
        config.setCodec(new JsonJacksonCodec(objectMapper.copy()));
        var server = config.useSingleServer()
                .setAddress(address)
                .setDatabase(runtime.getDatabase())
                .setTimeout(Math.toIntExact(runtime.getTimeout().toMillis()));
        String password = readSecret(runtime.getPasswordFile());
        if (password != null) {
            server.setPassword(password);
        }
        return Redisson.create(config);
    }

    private String readSecret(String file) {
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
                    "Cannot read RBAC3 runtime Redis password file", exception);
        }
    }
}
