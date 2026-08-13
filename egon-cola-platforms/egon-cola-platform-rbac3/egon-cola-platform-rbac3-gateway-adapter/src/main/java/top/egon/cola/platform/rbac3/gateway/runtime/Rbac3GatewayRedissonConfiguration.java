package top.egon.cola.platform.rbac3.gateway.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.platform.rbac3.gateway.autoconfigure.Rbac3GatewayAdapterProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 类型 `Rbac3GatewayRedissonConfiguration` 位于当前包内，是类型，用于承载 `Rbac3 Gateway Redisson Configuration` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3GatewayRedissonConfiguration` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Gateway Redisson Configuration`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `Rbac3GatewayRedissonConfiguration` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `Rbac3GatewayRedissonConfiguration` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "egon.cola.platform.rbac3.gateway.runtime",
        name = "redis-enabled",
        havingValue = "true")
public class Rbac3GatewayRedissonConfiguration {

    /**
     * 方法 `rbac3RuntimeRedissonClient` 按照 `Rbac3GatewayRedissonConfiguration` 的职责处理输入，完成 `rbac3 Runtime Redisson Client` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3RuntimeRedissonClient` processes its inputs according to `Rbac3GatewayRedissonConfiguration`'s responsibility, performs the `rbac3 Runtime Redisson Client` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3RuntimeRedissonClient` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3RuntimeRedissonClient`, then continue the business flow using its result, exception, or side effect.
     *
     * @param properties 输入参数 `properties`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param objectMapper 输入参数 `objectMapper`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean(name = "rbac3RuntimeRedissonClient", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "rbac3RuntimeRedissonClient")
    public RedissonClient rbac3RuntimeRedissonClient(
            Rbac3GatewayAdapterProperties properties,
            ObjectMapper objectMapper
    ) {
        Rbac3GatewayAdapterProperties.Runtime runtime = properties.getRuntime();
        String address = runtime.getRedisAddress();
        if (address == null || (!address.startsWith("redis://")
                && !address.startsWith("rediss://"))) {
            throw new IllegalArgumentException(
                    "RBAC3 Gateway Redis address must use redis:// or rediss://");
        }
        if (runtime.getTimeout() == null || runtime.getTimeout().isNegative()
                || runtime.getTimeout().isZero()) {
            throw new IllegalArgumentException(
                    "RBAC3 Gateway Redis timeout must be positive");
        }
        Config config = new Config();
        config.setCodec(new JsonJacksonCodec(objectMapper.copy()));
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
     * 方法 `secret` 按照 `Rbac3GatewayRedissonConfiguration` 的职责处理输入，完成 `secret` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `secret` processes its inputs according to `Rbac3GatewayRedissonConfiguration`'s responsibility, performs the `secret` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `secret` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `secret`, then continue the business flow using its result, exception, or side effect.
     *
     * @param file 输入参数 `file`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String secret(String file) {
        if (file == null || file.isBlank()) {
            return null;
        }
        try {
            String value = Files.readString(Path.of(file.trim())).trim();
            if (value.isEmpty()) {
                throw new IllegalArgumentException(
                        "RBAC3 Gateway Redis password file is empty");
            }
            return value;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Cannot read RBAC3 Gateway Redis password file", exception);
        }
    }
}
