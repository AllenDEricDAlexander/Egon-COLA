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

/**
 * 创建 Gateway 专用的 IdP 用户状态 Redis 客户端。
 * 独立客户端使 Gateway 能在不依赖 Servlet Starter 或其他业务 Redis Bean 的情况下读取身份控制面状态。
 *
 * <p>Creates the Gateway-specific Redis client for current IdP user state. The dedicated client
 * lets the Gateway read identity control-plane state without depending on the Servlet Starter or
 * unrelated application Redis beans.</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "egon.cola.platform.idp.gateway.runtime",
        name = "redis-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class IdpGatewayRedissonConfiguration {

    /**
     * 创建 Gateway IdP Redis 自动配置实例。
     *
     * <p>Creates the Gateway IdP Redis configuration instance.</p>
     */
    public IdpGatewayRedissonConfiguration() {
    }

    /**
     * 根据运行时配置创建单节点 Redisson 客户端。
     * 密码只从指定文件读取，Bean 销毁时由 Spring 调用 {@code shutdown}。
     *
     * <p>Creates a single-server Redisson client from runtime settings. The password is read only
     * from the configured file, and Spring invokes {@code shutdown} when the bean is destroyed.</p>
     *
     * @param properties Gateway IdP 适配器配置；Gateway IdP adapter settings
     * @return 名为 {@code idpGatewayRedissonClient} 的 Redisson 客户端；Redisson client named
     *         {@code idpGatewayRedissonClient}
     * @throws IllegalArgumentException 当地址协议或超时时间无效时；when the address scheme or
     *                                  timeout is invalid
     */
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

    /**
     * 从本地文件读取并规范化 Redis 密码。
     *
     * <p>Reads and normalizes the Redis password from a local file.</p>
     *
     * @param file 密码文件路径；password-file path
     * @return 密码；未配置文件时返回 {@code null}；password, or {@code null} when no file is
     *         configured
     * @throws IllegalArgumentException 当密码文件内容为空时；when the password file is empty
     * @throws IllegalStateException 当密码文件无法读取时；when the password file cannot be read
     */
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
