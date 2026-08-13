package top.egon.cola.platform.rbac3.admin.runtime.service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.runtime.repository.MutationQueryPort;
import top.egon.cola.platform.rbac3.admin.runtime.repository.MutationRecoveryPort;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.MutationVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.AuthorizationMutationPageVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.RetryResultVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.RuntimeStatusVO;

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
    public RuntimeStatusVO status() {
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
    public RuntimeStatusVO gatewayDdcStatus() {
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
    public AuthorizationMutationPageVO mutations(
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
    public RetryResultVO retry(
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





    }
