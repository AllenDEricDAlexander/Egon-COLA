package top.egon.cola.platform.idp.gateway.runtime;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.platform.idp.gateway.autoconfigure.IdpGatewayAdapterProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "egon.cola.platform.idp.gateway.runtime",
        name = "redis-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class IdpGatewayRedissonConfiguration {

    @Bean(name = "idpGatewayRedissonClient", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "idpGatewayRedissonClient")
    public RedissonClient idpGatewayRedissonClient(
            IdpGatewayAdapterProperties properties
    ) {
        IdpGatewayAdapterProperties.Runtime runtime = properties.getRuntime();
        String address = runtime.getRedisAddress();
        if (address == null || (!address.startsWith("redis://")
                && !address.startsWith("rediss://"))) {
            throw new IllegalArgumentException(
                    "IdP Gateway Redis address must use redis:// or rediss://");
        }
        if (runtime.getTimeout() == null || runtime.getTimeout().isNegative()
                || runtime.getTimeout().isZero()) {
            throw new IllegalArgumentException(
                    "IdP Gateway Redis timeout must be positive");
        }
        Config config = new Config();
        config.setCodec(StringCodec.INSTANCE);
        var server = config.useSingleServer()
                .setAddress(address)
                .setDatabase(runtime.getDatabase())
                .setTimeout(Math.toIntExact(runtime.getTimeout().toMillis()));
        String password = secret(runtime.getPasswordFile());
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
                        "IdP Gateway Redis password file is empty");
            }
            return value;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Cannot read IdP Gateway Redis password file", exception);
        }
    }
}
