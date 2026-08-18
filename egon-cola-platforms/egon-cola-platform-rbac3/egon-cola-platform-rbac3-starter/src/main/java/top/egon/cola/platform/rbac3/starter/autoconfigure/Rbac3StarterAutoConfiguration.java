package top.egon.cola.platform.rbac3.starter.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.platform.idp.starter.admission.OwnerOnlyPrivateKeyLoader;
import top.egon.cola.platform.idp.starter.admission.PrivateKeyJwtAssertionFactory;
import top.egon.cola.platform.idp.starter.autoconfigure.IdpStarterAutoConfiguration;
import top.egon.cola.platform.idp.starter.security.IdpJwtVerifier;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationBootstrapService;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationService;
import top.egon.cola.platform.rbac3.starter.authorization.DefaultAuthorizationService;
import top.egon.cola.platform.rbac3.starter.cache.AuthorizationSnapshotCache;
import top.egon.cola.platform.rbac3.starter.cache.RedisAuthorizationSnapshotCache;
import top.egon.cola.platform.rbac3.starter.cache.SingleFlightSnapshotLoader;
import top.egon.cola.platform.rbac3.starter.client.HttpRbac3AuthorizationClient;
import top.egon.cola.platform.rbac3.starter.client.HttpTenantServiceTokenSupplier;
import top.egon.cola.platform.rbac3.starter.client.Rbac3AuthorizationClient;
import top.egon.cola.platform.rbac3.starter.event.Rbac3AuthorizationInvalidationConsumer;
import top.egon.cola.platform.rbac3.starter.security.Rbac3BearerAuthenticationFilter;
import top.egon.cola.platform.rbac3.starter.security.Rbac3ContextAuthentication;
import top.egon.cola.platform.rbac3.starter.security.CurrentRbac3User;
import top.egon.cola.platform.rbac3.starter.security.Rbac3MethodAuthorizationManager;
import top.egon.cola.platform.rbac3.starter.web.Rbac3AuthorizationExceptionHandler;

import java.net.URI;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 类型 `Rbac3StarterAutoConfiguration` 位于当前包内，是类型，用于承载 `Rbac3 Starter Auto Configuration` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3StarterAutoConfiguration` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Starter Auto Configuration`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `Rbac3StarterAutoConfiguration` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `Rbac3StarterAutoConfiguration` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@AutoConfiguration
@AutoConfigureAfter(IdpStarterAutoConfiguration.class)
@EnableConfigurationProperties(Rbac3StarterProperties.class)
@ConditionalOnProperty(
        prefix = "egon.cola.platform.rbac3",
        name = "enabled",
        havingValue = "true")
public class Rbac3StarterAutoConfiguration {

    /** Provides the request-scoped current USER accessor used by services. */
    @Bean
    @ConditionalOnMissingBean
    public CurrentRbac3User currentRbac3User() {
        return new CurrentRbac3User();
    }

    /**
     * 方法 `rbac3Clock` 按照 `Rbac3StarterAutoConfiguration` 的职责处理输入，完成 `rbac3 Clock` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3Clock` processes its inputs according to `Rbac3StarterAutoConfiguration`'s responsibility, performs the `rbac3 Clock` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3Clock` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3Clock`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnMissingBean(name = "rbac3Clock")
    public Clock rbac3Clock() {
        return Clock.systemUTC();
    }

    /**
     * 方法 `redisAuthorizationSnapshotCache` 按照 `Rbac3StarterAutoConfiguration` 的职责处理输入，完成 `redis Authorization Snapshot Cache` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `redisAuthorizationSnapshotCache` processes its inputs according to `Rbac3StarterAutoConfiguration`'s responsibility, performs the `redis Authorization Snapshot Cache` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `redisAuthorizationSnapshotCache` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `redisAuthorizationSnapshotCache`, then continue the business flow using its result, exception, or side effect.
     *
     * @param redisson 输入参数 `redisson`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param objectMapper 输入参数 `objectMapper`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param properties 输入参数 `properties`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnBean(name = "rbac3RuntimeRedissonClient")
    @ConditionalOnMissingBean
    public RedisAuthorizationSnapshotCache redisAuthorizationSnapshotCache(
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            ObjectMapper objectMapper,
            Rbac3StarterProperties properties) {
        return new RedisAuthorizationSnapshotCache(
                redisson, objectMapper,
                properties.getAuthorization().getMaximumJitter());
    }

    /**
     * 方法 `authorizationSnapshotCache` 按照 `Rbac3StarterAutoConfiguration` 的职责处理输入，完成 `authorization Snapshot Cache` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `authorizationSnapshotCache` processes its inputs according to `Rbac3StarterAutoConfiguration`'s responsibility, performs the `authorization Snapshot Cache` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `authorizationSnapshotCache` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `authorizationSnapshotCache`, then continue the business flow using its result, exception, or side effect.
     *
     * @param store 输入参数 `store`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param properties 输入参数 `properties`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnBean(RedisAuthorizationSnapshotCache.class)
    @ConditionalOnMissingBean
    public AuthorizationSnapshotCache authorizationSnapshotCache(
            RedisAuthorizationSnapshotCache store,
            @Qualifier("rbac3Clock") Clock clock,
            Rbac3StarterProperties properties) {
        return new AuthorizationSnapshotCache(
                store, clock,
                properties.getAuthorization().getNearCacheTtl());
    }

    /**
     * 方法 `tenantAwareRbac3AuthorizationClient` 按照 `Rbac3StarterAutoConfiguration` 的职责处理输入，完成 `tenant Aware Rbac3 Authorization Client` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `tenantAwareRbac3AuthorizationClient` processes its inputs according to `Rbac3StarterAutoConfiguration`'s responsibility, performs the `tenant Aware Rbac3 Authorization Client` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `tenantAwareRbac3AuthorizationClient` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `tenantAwareRbac3AuthorizationClient`, then continue the business flow using its result, exception, or side effect.
     *
     * @param objectMapper 输入参数 `objectMapper`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param properties 输入参数 `properties`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "egon.cola.platform.rbac3.authorization.service-token",
            name = "enabled",
            havingValue = "true")
    @ConditionalOnMissingBean
    public Rbac3AuthorizationClient tenantAwareRbac3AuthorizationClient(
            ObjectMapper objectMapper,
            Rbac3StarterProperties properties) {
        var authorization = properties.getAuthorization();
        var serviceToken = authorization.getServiceToken();
        URI tokenEndpoint = URI.create(required(
                serviceToken.getTokenEndpoint(),
                "authorization.serviceToken.tokenEndpoint"
        ));
        Clock clock = Clock.systemUTC();
        PrivateKeyJwtAssertionFactory assertions =
                new PrivateKeyJwtAssertionFactory(
                        required(
                                serviceToken.getClientId(),
                                "authorization.serviceToken.clientId"
                        ),
                        required(
                                serviceToken.getKeyId(),
                                "authorization.serviceToken.keyId"
                        ),
                        tokenEndpoint,
                        new OwnerOnlyPrivateKeyLoader().load(Path.of(required(
                                serviceToken.getPrivateKeyFile(),
                                "authorization.serviceToken.privateKeyFile"
                        ))),
                        clock,
                        new SecureRandom()
                );
        HttpTenantServiceTokenSupplier credentials =
                new HttpTenantServiceTokenSupplier(
                        tokenEndpoint,
                        assertions,
                        objectMapper,
                        URI.create(required(
                                serviceToken.getResourceUri(),
                                "authorization.serviceToken.resourceUri"
                        )),
                        scopes(serviceToken.getScopes()),
                        serviceToken.getRenewalSkew(),
                        clock
                );
        return new HttpRbac3AuthorizationClient(
                URI.create(required(
                        authorization.getEndpoint(),
                        "authorization.endpoint"
                )),
                credentials,
                authorization.getFetchTimeout(),
                objectMapper
        );
    }

    /**
     * 方法 `singleFlightSnapshotLoader` 按照 `Rbac3StarterAutoConfiguration` 的职责处理输入，完成 `single Flight Snapshot Loader` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `singleFlightSnapshotLoader` processes its inputs according to `Rbac3StarterAutoConfiguration`'s responsibility, performs the `single Flight Snapshot Loader` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `singleFlightSnapshotLoader` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `singleFlightSnapshotLoader`, then continue the business flow using its result, exception, or side effect.
     *
     * @param cache 输入参数 `cache`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param client 输入参数 `client`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param properties 输入参数 `properties`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnBean({AuthorizationSnapshotCache.class,
            Rbac3AuthorizationClient.class})
    @ConditionalOnMissingBean
    public SingleFlightSnapshotLoader singleFlightSnapshotLoader(
            AuthorizationSnapshotCache cache,
            Rbac3AuthorizationClient client,
            Rbac3StarterProperties properties,
            @Qualifier("rbac3Clock") Clock clock) {
        return new SingleFlightSnapshotLoader(
                cache, client, required(properties.getSystemCode(), "systemCode"),
                properties.getAuthorization().getCacheTtl(), clock);
    }

    /**
     * 方法 `rbac3AuthorizationInvalidationConsumer` 按照 `Rbac3StarterAutoConfiguration` 的职责处理输入，完成 `rbac3 Authorization Invalidation Consumer` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3AuthorizationInvalidationConsumer` processes its inputs according to `Rbac3StarterAutoConfiguration`'s responsibility, performs the `rbac3 Authorization Invalidation Consumer` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3AuthorizationInvalidationConsumer` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3AuthorizationInvalidationConsumer`, then continue the business flow using its result, exception, or side effect.
     *
     * @param cache 输入参数 `cache`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param properties 输入参数 `properties`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnBean(AuthorizationSnapshotCache.class)
    @ConditionalOnMissingBean
    public Rbac3AuthorizationInvalidationConsumer
            rbac3AuthorizationInvalidationConsumer(
                    AuthorizationSnapshotCache cache,
                    Rbac3StarterProperties properties) {
        return new Rbac3AuthorizationInvalidationConsumer(
                required(properties.getSystemCode(), "systemCode"), cache);
    }

    /**
     * 方法 `rbac3RuntimeContextSource` 按照 `Rbac3StarterAutoConfiguration` 的职责处理输入，完成 `rbac3 Runtime Context Source` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3RuntimeContextSource` processes its inputs according to `Rbac3StarterAutoConfiguration`'s responsibility, performs the `rbac3 Runtime Context Source` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3RuntimeContextSource` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3RuntimeContextSource`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthorizationService.RuntimeContextSource rbac3RuntimeContextSource() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext()
                    .getAuthentication();
            if (authentication instanceof Rbac3ContextAuthentication token) {
                return token.context();
            }
            throw new IllegalStateException("RBAC3 authentication is required");
        };
    }

    /**
     * 方法 `rbac3OperationSodEvaluator` 按照 `Rbac3StarterAutoConfiguration` 的职责处理输入，完成 `rbac3 Operation Sod Evaluator` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3OperationSodEvaluator` processes its inputs according to `Rbac3StarterAutoConfiguration`'s responsibility, performs the `rbac3 Operation Sod Evaluator` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3OperationSodEvaluator` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3OperationSodEvaluator`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthorizationService.OperationSodEvaluator rbac3OperationSodEvaluator() {
        return request -> new AuthorizationService.OperationSodResult(
                false, "PARTICIPATION_VERIFIER_UNAVAILABLE", List.of(), List.of());
    }

    /**
     * 方法 `rbac3FenceVerifier` 按照 `Rbac3StarterAutoConfiguration` 的职责处理输入，完成 `rbac3 Fence Verifier` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3FenceVerifier` processes its inputs according to `Rbac3StarterAutoConfiguration`'s responsibility, performs the `rbac3 Fence Verifier` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3FenceVerifier` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3FenceVerifier`, then continue the business flow using its result, exception, or side effect.
     *
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthorizationService.FenceVerifier rbac3FenceVerifier(
            @Qualifier("rbac3Clock") Clock clock) {
        return request -> new AuthorizationService.FenceResult(
                false, "FENCE_VERIFIER_UNAVAILABLE", clock.instant(), List.of());
    }

    /**
     * 方法 `authorizationService` 按照 `Rbac3StarterAutoConfiguration` 的职责处理输入，完成 `authorization Service` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `authorizationService` processes its inputs according to `Rbac3StarterAutoConfiguration`'s responsibility, performs the `authorization Service` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `authorizationService` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `authorizationService`, then continue the business flow using its result, exception, or side effect.
     *
     * @param contextSource 输入参数 `contextSource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param operationSodEvaluator 输入参数 `operationSodEvaluator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param fenceVerifier 输入参数 `fenceVerifier`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthorizationService authorizationService(
            AuthorizationService.RuntimeContextSource contextSource,
            AuthorizationService.OperationSodEvaluator operationSodEvaluator,
            AuthorizationService.FenceVerifier fenceVerifier,
            @Qualifier("rbac3Clock") Clock clock) {
        return new DefaultAuthorizationService(
                contextSource, operationSodEvaluator, fenceVerifier, clock);
    }

    /**
     * 方法 `authorizationBootstrapService` 按照 `Rbac3StarterAutoConfiguration` 的职责处理输入，完成 `authorization Bootstrap Service` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `authorizationBootstrapService` processes its inputs according to `Rbac3StarterAutoConfiguration`'s responsibility, performs the `authorization Bootstrap Service` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `authorizationBootstrapService` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `authorizationBootstrapService`, then continue the business flow using its result, exception, or side effect.
     *
     * @param contextSource 输入参数 `contextSource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthorizationBootstrapService authorizationBootstrapService(
            AuthorizationService.RuntimeContextSource contextSource) {
        return new AuthorizationBootstrapService(contextSource);
    }

    /**
     * 方法 `rbac3MethodAuthorizationAspect` 按照 `Rbac3StarterAutoConfiguration` 的职责处理输入，完成 `rbac3 Method Authorization Aspect` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3MethodAuthorizationAspect` processes its inputs according to `Rbac3StarterAutoConfiguration`'s responsibility, performs the `rbac3 Method Authorization Aspect` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3MethodAuthorizationAspect` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3MethodAuthorizationAspect`, then continue the business flow using its result, exception, or side effect.
     *
     * @param authorizationService 输入参数 `authorizationService`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    /**
     * Registers the single Spring Method Security interceptor for RBAC3 declarations.
     */
    @Bean(name = "rbac3MethodAuthorizationInterceptor")
    @ConditionalOnMissingBean(name = "rbac3MethodAuthorizationInterceptor")
    public AuthorizationManagerBeforeMethodInterceptor
            rbac3MethodAuthorizationInterceptor(
                    AuthorizationService authorizationService) {
        Rbac3MethodAuthorizationManager manager =
                new Rbac3MethodAuthorizationManager(authorizationService);
        StaticMethodMatcherPointcut pointcut = new StaticMethodMatcherPointcut() {
            @Override
            public boolean matches(
                    java.lang.reflect.Method method,
                    Class<?> targetClass) {
                return manager.supports(method, targetClass);
            }
        };
        return new AuthorizationManagerBeforeMethodInterceptor(pointcut, manager);
    }

    /**
     * 方法 `rbac3AuthorizationExceptionHandler` 按照 `Rbac3StarterAutoConfiguration` 的职责处理输入，完成 `rbac3 Authorization Exception Handler` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3AuthorizationExceptionHandler` processes its inputs according to `Rbac3StarterAutoConfiguration`'s responsibility, performs the `rbac3 Authorization Exception Handler` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3AuthorizationExceptionHandler` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3AuthorizationExceptionHandler`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnMissingBean
    public Rbac3AuthorizationExceptionHandler rbac3AuthorizationExceptionHandler() {
        return new Rbac3AuthorizationExceptionHandler();
    }

    /**
     * 方法 `rbac3BearerAuthenticationFilter` 按照 `Rbac3StarterAutoConfiguration` 的职责处理输入，完成 `rbac3 Bearer Authentication Filter` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3BearerAuthenticationFilter` processes its inputs according to `Rbac3StarterAutoConfiguration`'s responsibility, performs the `rbac3 Bearer Authentication Filter` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3BearerAuthenticationFilter` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3BearerAuthenticationFilter`, then continue the business flow using its result, exception, or side effect.
     *
     * @param snapshotLoader 输入参数 `snapshotLoader`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param objectMapper 输入参数 `objectMapper`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnBean({IdpJwtVerifier.class, SingleFlightSnapshotLoader.class})
    @ConditionalOnMissingBean
    public Rbac3BearerAuthenticationFilter rbac3BearerAuthenticationFilter(
            SingleFlightSnapshotLoader snapshotLoader,
            ObjectMapper objectMapper) {
        return new Rbac3BearerAuthenticationFilter(snapshotLoader, objectMapper);
    }

    /**
     * 方法 `rbac3BearerFilterRegistration` 按照 `Rbac3StarterAutoConfiguration` 的职责处理输入，完成 `rbac3 Bearer Filter Registration` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3BearerFilterRegistration` processes its inputs according to `Rbac3StarterAutoConfiguration`'s responsibility, performs the `rbac3 Bearer Filter Registration` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3BearerFilterRegistration` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3BearerFilterRegistration`, then continue the business flow using its result, exception, or side effect.
     *
     * @param filter 输入参数 `filter`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param properties 输入参数 `properties`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnBean(Rbac3BearerAuthenticationFilter.class)
    @ConditionalOnMissingBean(name = "rbac3BearerFilterRegistration")
    public FilterRegistrationBean<Rbac3BearerAuthenticationFilter>
            rbac3BearerFilterRegistration(
                    Rbac3BearerAuthenticationFilter filter,
                    Rbac3StarterProperties properties) {
        FilterRegistrationBean<Rbac3BearerAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setName("rbac3BearerAuthenticationFilter");
        registration.setOrder(-101);
        registration.setEnabled(properties.isRegisterFilter());
        return registration;
    }

    /**
     * 方法 `required` 按照 `Rbac3StarterAutoConfiguration` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `Rbac3StarterAutoConfiguration`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }

    /**
     * 方法 `scopes` 按照 `Rbac3StarterAutoConfiguration` 的职责处理输入，完成 `scopes` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `scopes` processes its inputs according to `Rbac3StarterAutoConfiguration`'s responsibility, performs the `scopes` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `scopes` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `scopes`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static Set<String> scopes(String value) {
        return Arrays.stream(required(
                        value,
                        "authorization.serviceToken.scopes"
                ).split("[,\\s]+"))
                .filter(scope -> !scope.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }
}
