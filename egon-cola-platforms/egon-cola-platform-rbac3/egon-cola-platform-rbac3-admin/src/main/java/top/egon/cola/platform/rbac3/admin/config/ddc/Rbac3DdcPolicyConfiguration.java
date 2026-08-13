package top.egon.cola.platform.rbac3.admin.config.ddc;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.ddc.api.refresh.DdcConfigApplierRegistry;
import top.egon.cola.component.ddc.service.lifecycle.DdcRuntimeCoordinator;
import top.egon.cola.component.gateway.starter.reporting.GatewayReportingState;
import top.egon.cola.platform.rbac3.admin.runtime.repository.ddc.AtomicRbac3RuntimePolicy;
import top.egon.cola.platform.rbac3.admin.runtime.repository.ddc.DdcConfigClientStatusRepository;
import top.egon.cola.platform.rbac3.admin.runtime.repository.ddc.Rbac3DdcPolicyApplier;
import top.egon.cola.platform.rbac3.admin.runtime.repository.ApplyObserver;

/**
 * 类型 `Rbac3DdcPolicyConfiguration` 位于当前包内，是类型，用于承载 `Rbac3 Ddc Policy Configuration` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3DdcPolicyConfiguration` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Ddc Policy Configuration`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Registers RBAC3 policy adapters before the DDC applier registry is frozen.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "egon.cola.component.ddc",
        name = "enabled",
        havingValue = "true")
public class Rbac3DdcPolicyConfiguration {

    /**
     * 方法 `rbac3DdcValueDeclarations` 按照 `Rbac3DdcPolicyConfiguration` 的职责处理输入，完成 `rbac3 Ddc Value Declarations` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3DdcValueDeclarations` processes its inputs according to `Rbac3DdcPolicyConfiguration`'s responsibility, performs the `rbac3 Ddc Value Declarations` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3DdcValueDeclarations` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3DdcValueDeclarations`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    public Rbac3DdcValueDeclarations rbac3DdcValueDeclarations() {
        return new Rbac3DdcValueDeclarations();
    }

    /**
     * 方法 `rbac3DdcPolicyRegistrar` 按照 `Rbac3DdcPolicyConfiguration` 的职责处理输入，完成 `rbac3 Ddc Policy Registrar` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3DdcPolicyRegistrar` processes its inputs according to `Rbac3DdcPolicyConfiguration`'s responsibility, performs the `rbac3 Ddc Policy Registrar` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3DdcPolicyRegistrar` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3DdcPolicyRegistrar`, then continue the business flow using its result, exception, or side effect.
     *
     * @param registry 输入参数 `registry`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policy 输入参数 `policy`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param metrics 输入参数 `metrics`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    public InitializingBean rbac3DdcPolicyRegistrar(
            DdcConfigApplierRegistry registry,
            AtomicRbac3RuntimePolicy policy,
            ObjectProvider<Rbac3IntegrationMetrics> metrics) {
        Rbac3IntegrationMetrics available = metrics.getIfAvailable();
        ApplyObserver observer = available == null
                ? ApplyObserver.noop()
                : available;
        return registrar(registry, policy, observer);
    }

    /**
     * 方法 `rbac3DdcPolicyRegistrar` 按照 `Rbac3DdcPolicyConfiguration` 的职责处理输入，完成 `rbac3 Ddc Policy Registrar` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3DdcPolicyRegistrar` processes its inputs according to `Rbac3DdcPolicyConfiguration`'s responsibility, performs the `rbac3 Ddc Policy Registrar` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3DdcPolicyRegistrar` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3DdcPolicyRegistrar`, then continue the business flow using its result, exception, or side effect.
     *
     * @param registry 输入参数 `registry`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policy 输入参数 `policy`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    InitializingBean rbac3DdcPolicyRegistrar(
            DdcConfigApplierRegistry registry,
            AtomicRbac3RuntimePolicy policy) {
        return registrar(registry, policy, ApplyObserver.noop());
    }

    /**
     * 方法 `ddcConfigClientStatusService` 按照 `Rbac3DdcPolicyConfiguration` 的职责处理输入，完成 `ddc Config Client Status Service` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `ddcConfigClientStatusService` processes its inputs according to `Rbac3DdcPolicyConfiguration`'s responsibility, performs the `ddc Config Client Status Service` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `ddcConfigClientStatusService` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `ddcConfigClientStatusService`, then continue the business flow using its result, exception, or side effect.
     *
     * @param coordinator 输入参数 `coordinator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policy 输入参数 `policy`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    DdcConfigClientStatusRepository ddcConfigClientStatusService(
            ObjectProvider<DdcRuntimeCoordinator> coordinator,
            AtomicRbac3RuntimePolicy policy) {
        return new DdcConfigClientStatusRepository(coordinator, policy);
    }

    /**
     * 方法 `rbac3IntegrationMetrics` 按照 `Rbac3DdcPolicyConfiguration` 的职责处理输入，完成 `rbac3 Integration Metrics` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3IntegrationMetrics` processes its inputs according to `Rbac3DdcPolicyConfiguration`'s responsibility, performs the `rbac3 Integration Metrics` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3IntegrationMetrics` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3IntegrationMetrics`, then continue the business flow using its result, exception, or side effect.
     *
     * @param registry 输入参数 `registry`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policy 输入参数 `policy`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param coordinator 输入参数 `coordinator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param reportingState 输入参数 `reportingState`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    Rbac3IntegrationMetrics rbac3IntegrationMetrics(
            MeterRegistry registry,
            AtomicRbac3RuntimePolicy policy,
            ObjectProvider<DdcRuntimeCoordinator> coordinator,
            ObjectProvider<GatewayReportingState> reportingState) {
        return new Rbac3IntegrationMetrics(
                registry, policy, coordinator, reportingState);
    }

    /**
     * 方法 `registrar` 按照 `Rbac3DdcPolicyConfiguration` 的职责处理输入，完成 `registrar` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `registrar` processes its inputs according to `Rbac3DdcPolicyConfiguration`'s responsibility, performs the `registrar` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `registrar` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `registrar`, then continue the business flow using its result, exception, or side effect.
     *
     * @param registry 输入参数 `registry`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policy 输入参数 `policy`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param observer 输入参数 `observer`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private InitializingBean registrar(
            DdcConfigApplierRegistry registry,
            AtomicRbac3RuntimePolicy policy,
            ApplyObserver observer) {
        return () -> {
            register(registry, policy, observer,
                    AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY, 0);
            register(registry, policy, observer,
                    AtomicRbac3RuntimePolicy.MAXIMUM_ACTIVE_ROOTS_KEY, 0);
            register(registry, policy, observer,
                    AtomicRbac3RuntimePolicy.REFRESH_TOKEN_TTL_KEY, 10);
            register(registry, policy, observer,
                    AtomicRbac3RuntimePolicy.SESSION_ABSOLUTE_TIMEOUT_KEY, 20);
            register(registry, policy, observer,
                    AtomicRbac3RuntimePolicy.SESSION_IDLE_TIMEOUT_KEY, 30);
        };
    }

    /**
     * 方法 `register` 按照 `Rbac3DdcPolicyConfiguration` 的职责处理输入，完成 `register` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `register` processes its inputs according to `Rbac3DdcPolicyConfiguration`'s responsibility, performs the `register` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `register` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `register`, then continue the business flow using its result, exception, or side effect.
     *
     * @param registry 输入参数 `registry`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policy 输入参数 `policy`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param observer 输入参数 `observer`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param key 输入参数 `key`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param priority 输入参数 `priority`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void register(
            DdcConfigApplierRegistry registry,
            AtomicRbac3RuntimePolicy policy,
            ApplyObserver observer,
            String key,
            int priority) {
        registry.registerExact(
                key, new Rbac3DdcPolicyApplier(key, priority, policy, observer));
    }
}
