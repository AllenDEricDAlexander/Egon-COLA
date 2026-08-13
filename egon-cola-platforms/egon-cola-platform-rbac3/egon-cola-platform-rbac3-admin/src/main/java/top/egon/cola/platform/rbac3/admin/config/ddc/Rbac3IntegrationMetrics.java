package top.egon.cola.platform.rbac3.admin.config.ddc;

import top.egon.cola.platform.rbac3.admin.integration.ddc.AtomicRbac3RuntimePolicy;
import top.egon.cola.platform.rbac3.admin.integration.ddc.Rbac3DdcPolicyApplier;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import top.egon.cola.component.ddc.model.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.service.lifecycle.DdcRuntimeCoordinator;
import top.egon.cola.component.ddc.model.instance.DdcRuntimeState;
import top.egon.cola.component.gateway.starter.reporting.GatewayReportingState;

import java.util.Objects;
import java.util.Set;

/**
 * 类型 `Rbac3IntegrationMetrics` 位于当前包内，是类型，用于承载 `Rbac3 Integration Metrics` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3IntegrationMetrics` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Integration Metrics`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Registers the bounded RBAC3 DDC and Gateway integration metrics.
 */
public final class Rbac3IntegrationMetrics
        implements Rbac3DdcPolicyApplier.ApplyObserver {

    /**
     * 字段 `APPLY_STATUSES` 表示 `Rbac3IntegrationMetrics` 中与 `APPLY STATUSES` 相关的状态、依赖、配置或结果（声明类型 `Set&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `APPLY_STATUSES` stores the `APPLY STATUSES`-related state, dependency, configuration, or result of `Rbac3IntegrationMetrics` (declared type `Set&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `APPLY_STATUSES` 时应保持 `Rbac3IntegrationMetrics` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `APPLY_STATUSES`, preserve `Rbac3IntegrationMetrics`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final Set<String> APPLY_STATUSES = Set.of("success", "failed");

    /**
     * 字段 `registry` 表示 `Rbac3IntegrationMetrics` 中与 `registry` 相关的状态、依赖、配置或结果（声明类型 `MeterRegistry`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `registry` stores the `registry`-related state, dependency, configuration, or result of `Rbac3IntegrationMetrics` (declared type `MeterRegistry`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `registry` 时应保持 `Rbac3IntegrationMetrics` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `registry`, preserve `Rbac3IntegrationMetrics`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final MeterRegistry registry;
    /**
     * 字段 `policy` 表示 `Rbac3IntegrationMetrics` 中与 `policy` 相关的状态、依赖、配置或结果（声明类型 `AtomicRbac3RuntimePolicy`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `policy` stores the `policy`-related state, dependency, configuration, or result of `Rbac3IntegrationMetrics` (declared type `AtomicRbac3RuntimePolicy`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `policy` 时应保持 `Rbac3IntegrationMetrics` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `policy`, preserve `Rbac3IntegrationMetrics`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AtomicRbac3RuntimePolicy policy;
    /**
     * 字段 `coordinator` 表示 `Rbac3IntegrationMetrics` 中与 `coordinator` 相关的状态、依赖、配置或结果（声明类型 `ObjectProvider&lt;DdcRuntimeCoordinator&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `coordinator` stores the `coordinator`-related state, dependency, configuration, or result of `Rbac3IntegrationMetrics` (declared type `ObjectProvider&lt;DdcRuntimeCoordinator&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `coordinator` 时应保持 `Rbac3IntegrationMetrics` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `coordinator`, preserve `Rbac3IntegrationMetrics`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ObjectProvider<DdcRuntimeCoordinator> coordinator;
    /**
     * 字段 `reportingState` 表示 `Rbac3IntegrationMetrics` 中与 `reporting State` 相关的状态、依赖、配置或结果（声明类型 `ObjectProvider&lt;GatewayReportingState&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `reportingState` stores the `reporting State`-related state, dependency, configuration, or result of `Rbac3IntegrationMetrics` (declared type `ObjectProvider&lt;GatewayReportingState&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `reportingState` 时应保持 `Rbac3IntegrationMetrics` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `reportingState`, preserve `Rbac3IntegrationMetrics`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ObjectProvider<GatewayReportingState> reportingState;

    /**
     * 构造器 `Rbac3IntegrationMetrics` 用于创建并初始化 `Rbac3IntegrationMetrics` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `Rbac3IntegrationMetrics` creates and initializes `Rbac3IntegrationMetrics`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `Rbac3IntegrationMetrics` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `Rbac3IntegrationMetrics`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param registry 输入参数 `registry`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policy 输入参数 `policy`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param coordinator 输入参数 `coordinator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param reportingState 输入参数 `reportingState`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public Rbac3IntegrationMetrics(
            MeterRegistry registry,
            AtomicRbac3RuntimePolicy policy,
            ObjectProvider<DdcRuntimeCoordinator> coordinator,
            ObjectProvider<GatewayReportingState> reportingState) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.policy = Objects.requireNonNull(policy, "policy");
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator");
        this.reportingState = Objects.requireNonNull(reportingState, "reportingState");
        AtomicRbac3RuntimePolicy.CONFIG_KEYS.stream().sorted().forEach(this::registerVersionGauge);
        Gauge.builder("rbac3_ddc_config_ready", this, Rbac3IntegrationMetrics::ready)
                .register(registry);
        Gauge.builder("rbac3_gateway_definition_operation_count", this,
                        Rbac3IntegrationMetrics::operationCount)
                .register(registry);
    }

    /**
     * 方法 `recordApply` 按照 `Rbac3IntegrationMetrics` 的职责处理输入，完成 `record Apply` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `recordApply` processes its inputs according to `Rbac3IntegrationMetrics`'s responsibility, performs the `record Apply` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `recordApply` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `recordApply`, then continue the business flow using its result, exception, or side effect.
     *
     * @param key 输入参数 `key`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    public void recordApply(String key, String status) {
        if (!AtomicRbac3RuntimePolicy.CONFIG_KEYS.contains(key)) {
            throw new IllegalArgumentException("unknown RBAC3 metric key");
        }
        if (!APPLY_STATUSES.contains(status)) {
            throw new IllegalArgumentException("unknown RBAC3 metric status");
        }
        Counter.builder("rbac3_ddc_config_apply_total")
                .tag("key", key)
                .tag("status", status)
                .register(registry)
                .increment();
    }

    /**
     * 方法 `registerVersionGauge` 按照 `Rbac3IntegrationMetrics` 的职责处理输入，完成 `register Version Gauge` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `registerVersionGauge` processes its inputs according to `Rbac3IntegrationMetrics`'s responsibility, performs the `register Version Gauge` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `registerVersionGauge` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `registerVersionGauge`, then continue the business flow using its result, exception, or side effect.
     *
     * @param key 输入参数 `key`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void registerVersionGauge(String key) {
        Gauge.builder("rbac3_ddc_config_snapshot_version", policy,
                        value -> value.current().configVersions().getOrDefault(key, 0L))
                .tag("key", key)
                .register(registry);
    }

    /**
     * 方法 `ready` 按照 `Rbac3IntegrationMetrics` 的职责处理输入，完成 `ready` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `ready` processes its inputs according to `Rbac3IntegrationMetrics`'s responsibility, performs the `ready` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `ready` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `ready`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private double ready() {
        try {
            DdcRuntimeCoordinator runtime = coordinator.getIfAvailable();
            return runtime != null
                    && runtime.state() == DdcRuntimeState.READY
                    && runtime.currentSession()
                    .filter(session -> session.role() == DdcLeaseRole.CONFIG_CLIENT)
                    .isPresent() ? 1.0d : 0.0d;
        } catch (RuntimeException unavailable) {
            return 0.0d;
        }
    }

    /**
     * 方法 `operationCount` 按照 `Rbac3IntegrationMetrics` 的职责处理输入，完成 `operation Count` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `operationCount` processes its inputs according to `Rbac3IntegrationMetrics`'s responsibility, performs the `operation Count` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `operationCount` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `operationCount`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private double operationCount() {
        try {
            GatewayReportingState state = reportingState.getIfAvailable();
            GatewayReportingState.Snapshot snapshot = state == null
                    ? null : state.snapshot();
            if (snapshot == null || snapshot.result() == null) {
                return 0.0d;
            }
            return snapshot.result().counts().operations();
        } catch (RuntimeException unavailable) {
            return 0.0d;
        }
    }
}
