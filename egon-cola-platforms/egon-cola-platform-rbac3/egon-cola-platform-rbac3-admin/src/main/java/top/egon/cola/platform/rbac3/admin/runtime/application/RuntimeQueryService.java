package top.egon.cola.platform.rbac3.admin.runtime.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 类型 `RuntimeQueryService` 位于当前包内，是类型，用于承载 `Runtime Query Service` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `RuntimeQueryService` is a type in its package and carries the responsibility, state, or contract for `Runtime Query Service`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Runtime observability facade; recovery is addressable only by mutation id.
 */
public final class RuntimeQueryService {

    /**
     * 字段 `statusPort` 表示 `RuntimeQueryService` 中与 `status Port` 相关的状态、依赖、配置或结果（声明类型 `ControlPlaneRuntimeStatusPort`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `statusPort` stores the `status Port`-related state, dependency, configuration, or result of `RuntimeQueryService` (declared type `ControlPlaneRuntimeStatusPort`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `statusPort` 时应保持 `RuntimeQueryService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `statusPort`, preserve `RuntimeQueryService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ControlPlaneRuntimeStatusPort statusPort;
    /**
     * 字段 `mutationQueryPort` 表示 `RuntimeQueryService` 中与 `mutation Query Port` 相关的状态、依赖、配置或结果（声明类型 `MutationQueryPort`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `mutationQueryPort` stores the `mutation Query Port`-related state, dependency, configuration, or result of `RuntimeQueryService` (declared type `MutationQueryPort`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `mutationQueryPort` 时应保持 `RuntimeQueryService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `mutationQueryPort`, preserve `RuntimeQueryService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final MutationQueryPort mutationQueryPort;
    /**
     * 字段 `recoveryPort` 表示 `RuntimeQueryService` 中与 `recovery Port` 相关的状态、依赖、配置或结果（声明类型 `MutationRecoveryPort`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `recoveryPort` stores the `recovery Port`-related state, dependency, configuration, or result of `RuntimeQueryService` (declared type `MutationRecoveryPort`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `recoveryPort` 时应保持 `RuntimeQueryService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `recoveryPort`, preserve `RuntimeQueryService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final MutationRecoveryPort recoveryPort;

    /**
     * 构造器 `RuntimeQueryService` 用于创建并初始化 `RuntimeQueryService` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RuntimeQueryService` creates and initializes `RuntimeQueryService`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RuntimeQueryService` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RuntimeQueryService`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param statusPort 输入参数 `statusPort`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param mutationQueryPort 输入参数 `mutationQueryPort`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param recoveryPort 输入参数 `recoveryPort`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public RuntimeQueryService(
            ControlPlaneRuntimeStatusPort statusPort,
            MutationQueryPort mutationQueryPort,
            MutationRecoveryPort recoveryPort) {
        this.statusPort = Objects.requireNonNull(statusPort, "statusPort");
        this.mutationQueryPort = Objects.requireNonNull(
                mutationQueryPort, "mutationQueryPort");
        this.recoveryPort = Objects.requireNonNull(recoveryPort, "recoveryPort");
    }

    /**
     * 方法 `status` 按照 `RuntimeQueryService` 的职责处理输入，完成 `status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `status` processes its inputs according to `RuntimeQueryService`'s responsibility, performs the `status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `status` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `status`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public ControlPlaneRuntimeStatusPort.RuntimeStatus status() {
        return statusPort.status();
    }

    /**
     * 方法 `gatewayDdcStatus` 按照 `RuntimeQueryService` 的职责处理输入，完成 `gateway Ddc Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `gatewayDdcStatus` processes its inputs according to `RuntimeQueryService`'s responsibility, performs the `gateway Ddc Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `gatewayDdcStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `gatewayDdcStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public ControlPlaneRuntimeStatusPort.RuntimeStatus gatewayDdcStatus() {
        return statusPort.status();
    }

    /**
     * 方法 `mutations` 按照 `RuntimeQueryService` 的职责处理输入，完成 `mutations` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `mutations` processes its inputs according to `RuntimeQueryService`'s responsibility, performs the `mutations` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `mutations` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `mutations`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param cursor 输入参数 `cursor`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param pageSize 输入参数 `pageSize`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public MutationPage mutations(
            String tenantId,
            String status,
            String cursor,
            int pageSize) {
        require(tenantId, "tenantId");
        if (pageSize < 1 || pageSize > 200) {
            throw new IllegalArgumentException("pageSize must be between 1 and 200");
        }
        return mutationQueryPort.query(tenantId, status, cursor, pageSize);
    }

    /**
     * 方法 `retry` 按照 `RuntimeQueryService` 的职责处理输入，完成 `retry` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `retry` processes its inputs according to `RuntimeQueryService`'s responsibility, performs the `retry` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `retry` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `retry`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param mutationId 输入参数 `mutationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public RetryResult retry(
            String tenantId,
            String mutationId,
            String actorId) {
        return recoveryPort.retry(
                require(tenantId, "tenantId"),
                require(mutationId, "mutationId"),
                require(actorId, "actorId"));
    }

    /**
     * 方法 `require` 按照 `RuntimeQueryService` 的职责处理输入，完成 `require` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `require` processes its inputs according to `RuntimeQueryService`'s responsibility, performs the `require` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `require` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `require`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param fieldName 输入参数 `fieldName`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String require(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    /**
     * 类型 `MutationQueryPort` 位于 `RuntimeQueryService` 内，是接口，用于承载 `Mutation Query Port` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `MutationQueryPort` is an interface inside `RuntimeQueryService` and carries the responsibility, state, or contract for `Mutation Query Port`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `MutationQueryPort` 作为 `RuntimeQueryService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `MutationQueryPort` as the responsibility boundary of `RuntimeQueryService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface MutationQueryPort {
        /**
         * 方法 `query` 按照 `MutationQueryPort` 的职责处理输入，完成 `query` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `query` processes its inputs according to `MutationQueryPort`'s responsibility, performs the `query` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `query` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `query`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param cursor 输入参数 `cursor`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param pageSize 输入参数 `pageSize`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        MutationPage query(
                String tenantId,
                String status,
                String cursor,
                int pageSize);
    }

    /**
     * 类型 `MutationRecoveryPort` 位于 `RuntimeQueryService` 内，是接口，用于承载 `Mutation Recovery Port` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `MutationRecoveryPort` is an interface inside `RuntimeQueryService` and carries the responsibility, state, or contract for `Mutation Recovery Port`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `MutationRecoveryPort` 作为 `RuntimeQueryService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `MutationRecoveryPort` as the responsibility boundary of `RuntimeQueryService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface MutationRecoveryPort {
        /**
         * 方法 `retry` 按照 `MutationRecoveryPort` 的职责处理输入，完成 `retry` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `retry` processes its inputs according to `MutationRecoveryPort`'s responsibility, performs the `retry` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `retry` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `retry`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param mutationId 输入参数 `mutationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        RetryResult retry(String tenantId, String mutationId, String actorId);
    }

    /**
     * 类型 `MutationView` 位于 `RuntimeQueryService` 内，是记录类型，用于承载 `Mutation View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `MutationView` is a record inside `RuntimeQueryService` and carries the responsibility, state, or contract for `Mutation View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `MutationView` 作为 `RuntimeQueryService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `MutationView` as the responsibility boundary of `RuntimeQueryService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param mutationId 记录组件 `mutationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `mutationId` carries constructor data whose meaning is defined by the record contract.
     * @param scopeType 记录组件 `scopeType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopeType` carries constructor data whose meaning is defined by the record contract.
     * @param scopeId 记录组件 `scopeId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopeId` carries constructor data whose meaning is defined by the record contract.
     * @param commandId 记录组件 `commandId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `commandId` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param attempt 记录组件 `attempt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `attempt` carries constructor data whose meaning is defined by the record contract.
     * @param lastErrorCode 记录组件 `lastErrorCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `lastErrorCode` carries constructor data whose meaning is defined by the record contract.
     * @param updatedAt 记录组件 `updatedAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `updatedAt` carries constructor data whose meaning is defined by the record contract.
     */
    public record MutationView(
            /**
             * 字段 `mutationId` 表示 `MutationView` 中与 `mutation Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `mutationId` stores the `mutation Id`-related state, dependency, configuration, or result of `MutationView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `mutationId` 时应保持 `MutationView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `mutationId`, preserve `MutationView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String mutationId,
            /**
             * 字段 `scopeType` 表示 `MutationView` 中与 `scope Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopeType` stores the `scope Type`-related state, dependency, configuration, or result of `MutationView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopeType` 时应保持 `MutationView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopeType`, preserve `MutationView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String scopeType,
            /**
             * 字段 `scopeId` 表示 `MutationView` 中与 `scope Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopeId` stores the `scope Id`-related state, dependency, configuration, or result of `MutationView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopeId` 时应保持 `MutationView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopeId`, preserve `MutationView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String scopeId,
            /**
             * 字段 `commandId` 表示 `MutationView` 中与 `command Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `commandId` stores the `command Id`-related state, dependency, configuration, or result of `MutationView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `commandId` 时应保持 `MutationView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `commandId`, preserve `MutationView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String commandId,
            /**
             * 字段 `status` 表示 `MutationView` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `MutationView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `MutationView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `MutationView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status,
            /**
             * 字段 `attempt` 表示 `MutationView` 中与 `attempt` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `attempt` stores the `attempt`-related state, dependency, configuration, or result of `MutationView` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `attempt` 时应保持 `MutationView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `attempt`, preserve `MutationView`'s lifecycle, immutability, and thread-safety constraints.
             */
            int attempt,
            /**
             * 字段 `lastErrorCode` 表示 `MutationView` 中与 `last Error Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `lastErrorCode` stores the `last Error Code`-related state, dependency, configuration, or result of `MutationView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `lastErrorCode` 时应保持 `MutationView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `lastErrorCode`, preserve `MutationView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String lastErrorCode,
            /**
             * 字段 `updatedAt` 表示 `MutationView` 中与 `updated At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `updatedAt` stores the `updated At`-related state, dependency, configuration, or result of `MutationView` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `updatedAt` 时应保持 `MutationView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `updatedAt`, preserve `MutationView`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant updatedAt) {
    }

    /**
     * 类型 `MutationPage` 位于 `RuntimeQueryService` 内，是记录类型，用于承载 `Mutation Page` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `MutationPage` is a record inside `RuntimeQueryService` and carries the responsibility, state, or contract for `Mutation Page`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `MutationPage` 作为 `RuntimeQueryService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `MutationPage` as the responsibility boundary of `RuntimeQueryService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param items 记录组件 `items` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `items` carries constructor data whose meaning is defined by the record contract.
     * @param nextCursor 记录组件 `nextCursor` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `nextCursor` carries constructor data whose meaning is defined by the record contract.
     */
    public record MutationPage(/**
 * 字段 `items` 表示 `MutationPage` 中与 `items` 相关的状态、依赖、配置或结果（声明类型 `List&lt;MutationView&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `items` stores the `items`-related state, dependency, configuration, or result of `MutationPage` (declared type `List&lt;MutationView&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `items` 时应保持 `MutationPage` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `items`, preserve `MutationPage`'s lifecycle, immutability, and thread-safety constraints.
 */ List<MutationView> items, /**
 * 字段 `nextCursor` 表示 `MutationPage` 中与 `next Cursor` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `nextCursor` stores the `next Cursor`-related state, dependency, configuration, or result of `MutationPage` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `nextCursor` 时应保持 `MutationPage` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `nextCursor`, preserve `MutationPage`'s lifecycle, immutability, and thread-safety constraints.
 */ String nextCursor) {
        /**
         * 构造器 `MutationPage` 用于创建并初始化 `MutationPage` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `MutationPage` creates and initializes `MutationPage`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `MutationPage` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `MutationPage`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param items 输入参数 `items`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param nextCursor 输入参数 `nextCursor`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public MutationPage {
            items = List.copyOf(items);
        }
    }

    /**
     * 类型 `RetryResult` 位于 `RuntimeQueryService` 内，是记录类型，用于承载 `Retry Result` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RetryResult` is a record inside `RuntimeQueryService` and carries the responsibility, state, or contract for `Retry Result`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RetryResult` 作为 `RuntimeQueryService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RetryResult` as the responsibility boundary of `RuntimeQueryService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param mutationId 记录组件 `mutationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `mutationId` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     */
    public record RetryResult(/**
 * 字段 `mutationId` 表示 `RetryResult` 中与 `mutation Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `mutationId` stores the `mutation Id`-related state, dependency, configuration, or result of `RetryResult` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `mutationId` 时应保持 `RetryResult` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `mutationId`, preserve `RetryResult`'s lifecycle, immutability, and thread-safety constraints.
 */ String mutationId, /**
 * 字段 `status` 表示 `RetryResult` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `status` stores the `status`-related state, dependency, configuration, or result of `RetryResult` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `status` 时应保持 `RetryResult` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `status`, preserve `RetryResult`'s lifecycle, immutability, and thread-safety constraints.
 */ String status) {
    }
}
