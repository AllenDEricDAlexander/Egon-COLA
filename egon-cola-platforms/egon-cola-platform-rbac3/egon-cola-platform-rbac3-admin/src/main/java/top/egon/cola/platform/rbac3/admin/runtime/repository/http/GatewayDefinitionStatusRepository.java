package top.egon.cola.platform.rbac3.admin.runtime.repository.http;

import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReportResult;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;
import top.egon.cola.component.gateway.starter.reporting.GatewayReportingState;
import top.egon.cola.platform.rbac3.admin.runtime.service.GatewayDdcRuntimeStatusService;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.GatewayDefinitionStatusVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.ServiceIdentityVO;

/**
 * 类型 `GatewayDefinitionStatusRepository` 位于当前包内，是类型，用于承载 `Gateway Definition Status Service` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `GatewayDefinitionStatusRepository` is a type in its package and carries the responsibility, state, or contract for `Gateway Definition Status Service`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Exposes the definition receipt without conflating it with provider or release state.
 */
public final class GatewayDefinitionStatusRepository {

    /**
     * 字段 `snapshot` 表示 `GatewayDefinitionStatusRepository` 中与 `snapshot` 相关的状态、依赖、配置或结果（声明类型 `Supplier&lt;GatewayReportingState.Snapshot&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `snapshot` stores the `snapshot`-related state, dependency, configuration, or result of `GatewayDefinitionStatusRepository` (declared type `Supplier&lt;GatewayReportingState.Snapshot&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `snapshot` 时应保持 `GatewayDefinitionStatusRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `snapshot`, preserve `GatewayDefinitionStatusRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Supplier<GatewayReportingState.Snapshot> snapshot;
    /**
     * 字段 `identity` 表示 `GatewayDefinitionStatusRepository` 中与 `identity` 相关的状态、依赖、配置或结果（声明类型 `ServiceIdentityVO`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `identity` stores the `identity`-related state, dependency, configuration, or result of `GatewayDefinitionStatusRepository` (declared type `ServiceIdentityVO`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `identity` 时应保持 `GatewayDefinitionStatusRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `identity`, preserve `GatewayDefinitionStatusRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ServiceIdentityVO identity;

    /**
     * 构造器 `GatewayDefinitionStatusRepository` 用于创建并初始化 `GatewayDefinitionStatusRepository` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `GatewayDefinitionStatusRepository` creates and initializes `GatewayDefinitionStatusRepository`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `GatewayDefinitionStatusRepository` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `GatewayDefinitionStatusRepository`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param state 输入参数 `state`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param properties 输入参数 `properties`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public GatewayDefinitionStatusRepository(
            GatewayReportingState state,
            GatewayReportingProperties properties) {
        this(state::snapshot, new ServiceIdentityVO(
                properties.getBizCode(), properties.getApplicationCode(),
                properties.getEnv(), properties.getNamespace(), "HTTP_PROVIDER", "http",
                properties.getApplicationCode(), "default", properties.getArtifactVersion()));
    }

    /**
     * 构造器 `GatewayDefinitionStatusRepository` 用于创建并初始化 `GatewayDefinitionStatusRepository` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `GatewayDefinitionStatusRepository` creates and initializes `GatewayDefinitionStatusRepository`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `GatewayDefinitionStatusRepository` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `GatewayDefinitionStatusRepository`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param snapshot 输入参数 `snapshot`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param identity 输入参数 `identity`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public GatewayDefinitionStatusRepository(
            Supplier<GatewayReportingState.Snapshot> snapshot,
            ServiceIdentityVO identity) {
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        this.identity = Objects.requireNonNull(identity, "identity");
    }

    /**
     * 方法 `status` 按照 `GatewayDefinitionStatusRepository` 的职责处理输入，完成 `status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `status` processes its inputs according to `GatewayDefinitionStatusRepository`'s responsibility, performs the `status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `status` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `status`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public GatewayDefinitionStatusVO status() {
        GatewayReportingState.Snapshot current = snapshot.get();
        GatewayInterfaceDefinitionReportResult result = current.result();
        if (result == null || !"SUCCESS".equals(current.status())) {
            return new GatewayDefinitionStatusVO(
                    "UNKNOWN", null, List.of(safe(current.lastError())), identity);
        }
        return new GatewayDefinitionStatusVO(
                result.status().name(), result.definitionSetId(),
                result.warnings().stream()
                        .map(GatewayInterfaceDefinitionReportResult.Warning::code)
                        .toList(), identity);
    }

    /**
     * 方法 `safe` 按照 `GatewayDefinitionStatusRepository` 的职责处理输入，完成 `safe` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `safe` processes its inputs according to `GatewayDefinitionStatusRepository`'s responsibility, performs the `safe` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `safe` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `safe`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String safe(String value) {
        return value == null || value.isBlank() ? "DEFINITION_NOT_ACKNOWLEDGED"
                : "DEFINITION_REPORT_FAILED";
    }

    }
