package top.egon.cola.platform.rbac3.admin.integration.gateway;

import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReportResult;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;
import top.egon.cola.component.gateway.starter.reporting.GatewayReportingState;
import top.egon.cola.platform.rbac3.admin.integration.runtime.GatewayDdcRuntimeStatusService;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 类型 `GatewayDefinitionStatusService` 位于当前包内，是类型，用于承载 `Gateway Definition Status Service` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `GatewayDefinitionStatusService` is a type in its package and carries the responsibility, state, or contract for `Gateway Definition Status Service`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Exposes the definition receipt without conflating it with provider or release state.
 */
public final class GatewayDefinitionStatusService {

    /**
     * 字段 `snapshot` 表示 `GatewayDefinitionStatusService` 中与 `snapshot` 相关的状态、依赖、配置或结果（声明类型 `Supplier&lt;GatewayReportingState.Snapshot&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `snapshot` stores the `snapshot`-related state, dependency, configuration, or result of `GatewayDefinitionStatusService` (declared type `Supplier&lt;GatewayReportingState.Snapshot&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `snapshot` 时应保持 `GatewayDefinitionStatusService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `snapshot`, preserve `GatewayDefinitionStatusService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Supplier<GatewayReportingState.Snapshot> snapshot;
    /**
     * 字段 `identity` 表示 `GatewayDefinitionStatusService` 中与 `identity` 相关的状态、依赖、配置或结果（声明类型 `GatewayDdcRuntimeStatusService.ServiceIdentity`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `identity` stores the `identity`-related state, dependency, configuration, or result of `GatewayDefinitionStatusService` (declared type `GatewayDdcRuntimeStatusService.ServiceIdentity`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `identity` 时应保持 `GatewayDefinitionStatusService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `identity`, preserve `GatewayDefinitionStatusService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final GatewayDdcRuntimeStatusService.ServiceIdentity identity;

    /**
     * 构造器 `GatewayDefinitionStatusService` 用于创建并初始化 `GatewayDefinitionStatusService` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `GatewayDefinitionStatusService` creates and initializes `GatewayDefinitionStatusService`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `GatewayDefinitionStatusService` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `GatewayDefinitionStatusService`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param state 输入参数 `state`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param properties 输入参数 `properties`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public GatewayDefinitionStatusService(
            GatewayReportingState state,
            GatewayReportingProperties properties) {
        this(state::snapshot, new GatewayDdcRuntimeStatusService.ServiceIdentity(
                properties.getBizCode(), properties.getApplicationCode(),
                properties.getEnv(), properties.getNamespace(), "HTTP_PROVIDER", "http",
                properties.getApplicationCode(), "default", properties.getArtifactVersion()));
    }

    /**
     * 构造器 `GatewayDefinitionStatusService` 用于创建并初始化 `GatewayDefinitionStatusService` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `GatewayDefinitionStatusService` creates and initializes `GatewayDefinitionStatusService`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `GatewayDefinitionStatusService` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `GatewayDefinitionStatusService`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param snapshot 输入参数 `snapshot`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param identity 输入参数 `identity`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public GatewayDefinitionStatusService(
            Supplier<GatewayReportingState.Snapshot> snapshot,
            GatewayDdcRuntimeStatusService.ServiceIdentity identity) {
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        this.identity = Objects.requireNonNull(identity, "identity");
    }

    /**
     * 方法 `status` 按照 `GatewayDefinitionStatusService` 的职责处理输入，完成 `status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `status` processes its inputs according to `GatewayDefinitionStatusService`'s responsibility, performs the `status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `status` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `status`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public DefinitionStatus status() {
        GatewayReportingState.Snapshot current = snapshot.get();
        GatewayInterfaceDefinitionReportResult result = current.result();
        if (result == null || !"SUCCESS".equals(current.status())) {
            return new DefinitionStatus(
                    "UNKNOWN", null, List.of(safe(current.lastError())), identity);
        }
        return new DefinitionStatus(
                result.status().name(), result.definitionSetId(),
                result.warnings().stream()
                        .map(GatewayInterfaceDefinitionReportResult.Warning::code)
                        .toList(), identity);
    }

    /**
     * 方法 `safe` 按照 `GatewayDefinitionStatusService` 的职责处理输入，完成 `safe` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `safe` processes its inputs according to `GatewayDefinitionStatusService`'s responsibility, performs the `safe` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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

    /**
     * 类型 `DefinitionStatus` 位于 `GatewayDefinitionStatusService` 内，是记录类型，用于承载 `Definition Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `DefinitionStatus` is a record inside `GatewayDefinitionStatusService` and carries the responsibility, state, or contract for `Definition Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `DefinitionStatus` 作为 `GatewayDefinitionStatusService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `DefinitionStatus` as the responsibility boundary of `GatewayDefinitionStatusService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param definitionSetId 记录组件 `definitionSetId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `definitionSetId` carries constructor data whose meaning is defined by the record contract.
     * @param warnings 记录组件 `warnings` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `warnings` carries constructor data whose meaning is defined by the record contract.
     * @param identity 记录组件 `identity` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `identity` carries constructor data whose meaning is defined by the record contract.
     */
    public record DefinitionStatus(
            /**
             * 字段 `status` 表示 `DefinitionStatus` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `DefinitionStatus` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `DefinitionStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `DefinitionStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status,
            /**
             * 字段 `definitionSetId` 表示 `DefinitionStatus` 中与 `definition Set Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `definitionSetId` stores the `definition Set Id`-related state, dependency, configuration, or result of `DefinitionStatus` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `definitionSetId` 时应保持 `DefinitionStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `definitionSetId`, preserve `DefinitionStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            String definitionSetId,
            /**
             * 字段 `warnings` 表示 `DefinitionStatus` 中与 `warnings` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `warnings` stores the `warnings`-related state, dependency, configuration, or result of `DefinitionStatus` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `warnings` 时应保持 `DefinitionStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `warnings`, preserve `DefinitionStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<String> warnings,
            /**
             * 字段 `identity` 表示 `DefinitionStatus` 中与 `identity` 相关的状态、依赖、配置或结果（声明类型 `GatewayDdcRuntimeStatusService.ServiceIdentity`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `identity` stores the `identity`-related state, dependency, configuration, or result of `DefinitionStatus` (declared type `GatewayDdcRuntimeStatusService.ServiceIdentity`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `identity` 时应保持 `DefinitionStatus` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `identity`, preserve `DefinitionStatus`'s lifecycle, immutability, and thread-safety constraints.
             */
            GatewayDdcRuntimeStatusService.ServiceIdentity identity) {

        /**
         * 构造器 `DefinitionStatus` 用于创建并初始化 `DefinitionStatus` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `DefinitionStatus` creates and initializes `DefinitionStatus`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `DefinitionStatus` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `DefinitionStatus`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param definitionSetId 输入参数 `definitionSetId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param warnings 输入参数 `warnings`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param identity 输入参数 `identity`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public DefinitionStatus {
            warnings = List.copyOf(warnings);
        }

        /**
         * 方法 `accepted` 按照 `DefinitionStatus` 的职责处理输入，完成 `accepted` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `accepted` processes its inputs according to `DefinitionStatus`'s responsibility, performs the `accepted` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `accepted` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `accepted`, then continue the business flow using its result, exception, or side effect.
         *
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        public boolean accepted() {
            return "ACCEPTED".equals(status)
                    || "ACCEPTED_WITH_WARNINGS".equals(status);
        }
    }
}
