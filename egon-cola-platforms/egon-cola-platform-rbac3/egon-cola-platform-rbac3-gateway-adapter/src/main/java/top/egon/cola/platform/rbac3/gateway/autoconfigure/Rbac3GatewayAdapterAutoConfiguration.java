package top.egon.cola.platform.rbac3.gateway.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import top.egon.cola.platform.rbac3.core.runtime.Rbac3RuntimeKeyFactory;
import top.egon.cola.platform.rbac3.gateway.runtime.Rbac3GatewayRedissonConfiguration;
import top.egon.cola.platform.rbac3.gateway.runtime.Rbac3GatewayRuntimeSnapshotReader;
import top.egon.cola.platform.rbac3.gateway.security.Rbac3BearerCredentialExtractor;
import top.egon.cola.platform.rbac3.gateway.security.Rbac3GatewayJwtVerifier;
import top.egon.cola.platform.rbac3.gateway.security.Rbac3JwtSessionAuthenticationProvider;
import top.egon.cola.platform.rbac3.gateway.security.Rbac3PermissionAuthorizationProvider;
import top.egon.cola.platform.rbac3.gateway.security.Rbac3ReservedHeaderSanitizer;
import top.egon.cola.platform.rbac3.gateway.security.Rbac3TrustedIdentityMapper;

import java.time.Clock;

/**
 * 类型 `Rbac3GatewayAdapterAutoConfiguration` 位于当前包内，是类型，用于承载 `Rbac3 Gateway Adapter Auto Configuration` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3GatewayAdapterAutoConfiguration` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Gateway Adapter Auto Configuration`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `Rbac3GatewayAdapterAutoConfiguration` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `Rbac3GatewayAdapterAutoConfiguration` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@AutoConfiguration
@EnableConfigurationProperties(Rbac3GatewayAdapterProperties.class)
@ConditionalOnProperty(
        prefix = "egon.cola.platform.rbac3.gateway",
        name = "enabled",
        havingValue = "true")
@Import(Rbac3GatewayRedissonConfiguration.class)
public class Rbac3GatewayAdapterAutoConfiguration {

    /**
     * 方法 `rbac3GatewayClock` 按照 `Rbac3GatewayAdapterAutoConfiguration` 的职责处理输入，完成 `rbac3 Gateway Clock` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3GatewayClock` processes its inputs according to `Rbac3GatewayAdapterAutoConfiguration`'s responsibility, performs the `rbac3 Gateway Clock` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3GatewayClock` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3GatewayClock`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnMissingBean(name = "rbac3GatewayClock")
    public Clock rbac3GatewayClock() {
        return Clock.systemUTC();
    }

    /**
     * 方法 `rbac3GatewayRuntimeKeyFactory` 按照 `Rbac3GatewayAdapterAutoConfiguration` 的职责处理输入，完成 `rbac3 Gateway Runtime Key Factory` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3GatewayRuntimeKeyFactory` processes its inputs according to `Rbac3GatewayAdapterAutoConfiguration`'s responsibility, performs the `rbac3 Gateway Runtime Key Factory` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3GatewayRuntimeKeyFactory` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3GatewayRuntimeKeyFactory`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnMissingBean
    public Rbac3RuntimeKeyFactory rbac3GatewayRuntimeKeyFactory() {
        return new Rbac3RuntimeKeyFactory();
    }

    /**
     * 方法 `rbac3ReservedHeaderSanitizer` 按照 `Rbac3GatewayAdapterAutoConfiguration` 的职责处理输入，完成 `rbac3 Reserved Header Sanitizer` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3ReservedHeaderSanitizer` processes its inputs according to `Rbac3GatewayAdapterAutoConfiguration`'s responsibility, performs the `rbac3 Reserved Header Sanitizer` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3ReservedHeaderSanitizer` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3ReservedHeaderSanitizer`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnMissingBean
    public Rbac3ReservedHeaderSanitizer rbac3ReservedHeaderSanitizer() {
        return new Rbac3ReservedHeaderSanitizer();
    }

    /**
     * 方法 `rbac3BearerCredentialExtractor` 按照 `Rbac3GatewayAdapterAutoConfiguration` 的职责处理输入，完成 `rbac3 Bearer Credential Extractor` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3BearerCredentialExtractor` processes its inputs according to `Rbac3GatewayAdapterAutoConfiguration`'s responsibility, performs the `rbac3 Bearer Credential Extractor` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3BearerCredentialExtractor` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3BearerCredentialExtractor`, then continue the business flow using its result, exception, or side effect.
     *
     * @param sanitizer 输入参数 `sanitizer`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnMissingBean
    public Rbac3BearerCredentialExtractor rbac3BearerCredentialExtractor(
            Rbac3ReservedHeaderSanitizer sanitizer
    ) {
        return new Rbac3BearerCredentialExtractor(sanitizer);
    }

    /**
     * 方法 `rbac3GatewayRuntimeSnapshotReader` 按照 `Rbac3GatewayAdapterAutoConfiguration` 的职责处理输入，完成 `rbac3 Gateway Runtime Snapshot Reader` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3GatewayRuntimeSnapshotReader` processes its inputs according to `Rbac3GatewayAdapterAutoConfiguration`'s responsibility, performs the `rbac3 Gateway Runtime Snapshot Reader` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3GatewayRuntimeSnapshotReader` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3GatewayRuntimeSnapshotReader`, then continue the business flow using its result, exception, or side effect.
     *
     * @param redisson 输入参数 `redisson`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param objectMapper 输入参数 `objectMapper`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param keyFactory 输入参数 `keyFactory`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnBean(name = "rbac3RuntimeRedissonClient")
    @ConditionalOnMissingBean
    public Rbac3GatewayRuntimeSnapshotReader rbac3GatewayRuntimeSnapshotReader(
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            ObjectMapper objectMapper,
            Rbac3RuntimeKeyFactory keyFactory,
            @Qualifier("rbac3GatewayClock") Clock clock
    ) {
        return new Rbac3GatewayRuntimeSnapshotReader(
                redisson, objectMapper, keyFactory, clock);
    }

    /**
     * 方法 `rbac3GatewayJwtVerifier` 按照 `Rbac3GatewayAdapterAutoConfiguration` 的职责处理输入，完成 `rbac3 Gateway Jwt Verifier` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3GatewayJwtVerifier` processes its inputs according to `Rbac3GatewayAdapterAutoConfiguration`'s responsibility, performs the `rbac3 Gateway Jwt Verifier` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3GatewayJwtVerifier` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3GatewayJwtVerifier`, then continue the business flow using its result, exception, or side effect.
     *
     * @param redisson 输入参数 `redisson`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param objectMapper 输入参数 `objectMapper`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param keyFactory 输入参数 `keyFactory`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param properties 输入参数 `properties`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnBean(name = "rbac3RuntimeRedissonClient")
    @ConditionalOnMissingBean
    public Rbac3GatewayJwtVerifier rbac3GatewayJwtVerifier(
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            ObjectMapper objectMapper,
            Rbac3RuntimeKeyFactory keyFactory,
            @Qualifier("rbac3GatewayClock") Clock clock,
            Rbac3GatewayAdapterProperties properties
    ) {
        return new Rbac3GatewayJwtVerifier(
                redisson, objectMapper, keyFactory, clock,
                properties.getIssuer(), properties.getAudience(),
                properties.getClockSkew(), properties.getPublicKeyLkgTtl());
    }

    /**
     * 方法 `rbac3JwtSessionAuthenticationProvider` 按照 `Rbac3GatewayAdapterAutoConfiguration` 的职责处理输入，完成 `rbac3 Jwt Session Authentication Provider` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3JwtSessionAuthenticationProvider` processes its inputs according to `Rbac3GatewayAdapterAutoConfiguration`'s responsibility, performs the `rbac3 Jwt Session Authentication Provider` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3JwtSessionAuthenticationProvider` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3JwtSessionAuthenticationProvider`, then continue the business flow using its result, exception, or side effect.
     *
     * @param verifier 输入参数 `verifier`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param runtime 输入参数 `runtime`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnBean({Rbac3GatewayJwtVerifier.class,
            Rbac3GatewayRuntimeSnapshotReader.class})
    @ConditionalOnMissingBean
    public Rbac3JwtSessionAuthenticationProvider
            rbac3JwtSessionAuthenticationProvider(
                    Rbac3GatewayJwtVerifier verifier,
                    Rbac3GatewayRuntimeSnapshotReader runtime
            ) {
        return new Rbac3JwtSessionAuthenticationProvider(
                verifier, runtime::verifySession);
    }

    /**
     * 方法 `rbac3PermissionAuthorizationProvider` 按照 `Rbac3GatewayAdapterAutoConfiguration` 的职责处理输入，完成 `rbac3 Permission Authorization Provider` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3PermissionAuthorizationProvider` processes its inputs according to `Rbac3GatewayAdapterAutoConfiguration`'s responsibility, performs the `rbac3 Permission Authorization Provider` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3PermissionAuthorizationProvider` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3PermissionAuthorizationProvider`, then continue the business flow using its result, exception, or side effect.
     *
     * @param runtime 输入参数 `runtime`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnBean(Rbac3GatewayRuntimeSnapshotReader.class)
    @ConditionalOnMissingBean
    public Rbac3PermissionAuthorizationProvider
            rbac3PermissionAuthorizationProvider(
                    Rbac3GatewayRuntimeSnapshotReader runtime
            ) {
        return new Rbac3PermissionAuthorizationProvider(runtime::authorize);
    }

    /**
     * 方法 `rbac3TrustedIdentityMapper` 按照 `Rbac3GatewayAdapterAutoConfiguration` 的职责处理输入，完成 `rbac3 Trusted Identity Mapper` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3TrustedIdentityMapper` processes its inputs according to `Rbac3GatewayAdapterAutoConfiguration`'s responsibility, performs the `rbac3 Trusted Identity Mapper` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3TrustedIdentityMapper` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3TrustedIdentityMapper`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnMissingBean
    public Rbac3TrustedIdentityMapper rbac3TrustedIdentityMapper() {
        return new Rbac3TrustedIdentityMapper();
    }
}
