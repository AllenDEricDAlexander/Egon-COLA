package top.egon.cola.platform.rbac3.admin.runtime.service;

import top.egon.cola.platform.rbac3.admin.runtime.service.RuntimeQueryService;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import top.egon.cola.platform.rbac3.admin.runtime.repository.AuthorizationMutationRecoveryRepository;
import top.egon.cola.platform.rbac3.admin.runtime.service.RuntimeProjectionExecutor;
import top.egon.cola.platform.rbac3.admin.runtime.domain.dto.MutationWorkDTO;
import top.egon.cola.platform.rbac3.admin.runtime.repository.MutationRecoveryPort;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.RetryResultVO;

/**
 * 类型 `AuthorizationMutationRecoveryWorker` 位于当前包内，是类型，用于承载 `Authorization Mutation Recovery Worker` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `AuthorizationMutationRecoveryWorker` is a type in its package and carries the responsibility, state, or contract for `Authorization Mutation Recovery Worker`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Replays committed authorization mutations claimed with database row ownership.
 */
public final class AuthorizationMutationRecoveryService
        implements MutationRecoveryPort {

    /**
     * 字段 `store` 表示 `AuthorizationMutationRecoveryWorker` 中与 `store` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationMutationRecoveryRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `store` stores the `store`-related state, dependency, configuration, or result of `AuthorizationMutationRecoveryWorker` (declared type `AuthorizationMutationRecoveryRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `store` 时应保持 `AuthorizationMutationRecoveryWorker` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `store`, preserve `AuthorizationMutationRecoveryWorker`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AuthorizationMutationRecoveryRepository store;
    /**
     * 字段 `projector` 表示 `AuthorizationMutationRecoveryWorker` 中与 `projector` 相关的状态、依赖、配置或结果（声明类型 `RuntimeProjectionExecutor`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `projector` stores the `projector`-related state, dependency, configuration, or result of `AuthorizationMutationRecoveryWorker` (declared type `RuntimeProjectionExecutor`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `projector` 时应保持 `AuthorizationMutationRecoveryWorker` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `projector`, preserve `AuthorizationMutationRecoveryWorker`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RuntimeProjectionExecutor projector;
    /**
     * 字段 `clock` 表示 `AuthorizationMutationRecoveryWorker` 中与 `clock` 相关的状态、依赖、配置或结果（声明类型 `Clock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `clock` stores the `clock`-related state, dependency, configuration, or result of `AuthorizationMutationRecoveryWorker` (declared type `Clock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `clock` 时应保持 `AuthorizationMutationRecoveryWorker` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `clock`, preserve `AuthorizationMutationRecoveryWorker`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Clock clock;
    /**
     * 字段 `batchSize` 表示 `AuthorizationMutationRecoveryWorker` 中与 `batch Size` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `batchSize` stores the `batch Size`-related state, dependency, configuration, or result of `AuthorizationMutationRecoveryWorker` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `batchSize` 时应保持 `AuthorizationMutationRecoveryWorker` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `batchSize`, preserve `AuthorizationMutationRecoveryWorker`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final int batchSize;

    /**
     * 构造器 `AuthorizationMutationRecoveryWorker` 用于创建并初始化 `AuthorizationMutationRecoveryWorker` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `AuthorizationMutationRecoveryWorker` creates and initializes `AuthorizationMutationRecoveryWorker`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `AuthorizationMutationRecoveryWorker` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `AuthorizationMutationRecoveryWorker`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param store 输入参数 `store`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param projector 输入参数 `projector`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param batchSize 输入参数 `batchSize`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public AuthorizationMutationRecoveryService(
            AuthorizationMutationRecoveryRepository store,
            RuntimeProjectionExecutor projector,
            Clock clock,
            int batchSize) {
        this.store = Objects.requireNonNull(store, "store");
        this.projector = Objects.requireNonNull(projector, "projector");
        this.clock = Objects.requireNonNull(clock, "clock");
        if (batchSize < 1 || batchSize > 200) {
            throw new IllegalArgumentException("batchSize must be between 1 and 200");
        }
        this.batchSize = batchSize;
    }

    /**
     * 方法 `retry` 按照 `AuthorizationMutationRecoveryWorker` 的职责处理输入，完成 `retry` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `retry` processes its inputs according to `AuthorizationMutationRecoveryWorker`'s responsibility, performs the `retry` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `retry` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `retry`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param mutationId 输入参数 `mutationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public RetryResultVO retry(
            String tenantId,
            String mutationId,
            String actorId) {
        MutationWorkDTO work = store.claimById(
                        required(tenantId, "tenantId"),
                        required(mutationId, "mutationId"))
                .orElseThrow(() -> new IllegalArgumentException(
                        "authorization mutation was not found"));
        if ("COMPLETED".equals(work.status())) {
            return new RetryResultVO(mutationId, "COMPLETED");
        }
        boolean completed = recover(work, required(actorId, "actorId"));
        return new RetryResultVO(
                mutationId, completed ? "COMPLETED" : "RECOVERY_REQUIRED");
    }

    /**
     * 方法 `runOnce` 按照 `AuthorizationMutationRecoveryWorker` 的职责处理输入，完成 `run Once` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `runOnce` processes its inputs according to `AuthorizationMutationRecoveryWorker`'s responsibility, performs the `run Once` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `runOnce` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `runOnce`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public int runOnce() {
        int completed = 0;
        for (MutationWorkDTO work : store.claimRecoverable(batchSize)) {
            if (recover(work, "rbac3-recovery-worker")) {
                completed++;
            }
        }
        return completed;
    }

    /**
     * 方法 `recover` 按照 `AuthorizationMutationRecoveryWorker` 的职责处理输入，完成 `recover` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `recover` processes its inputs according to `AuthorizationMutationRecoveryWorker`'s responsibility, performs the `recover` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `recover` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `recover`, then continue the business flow using its result, exception, or side effect.
     *
     * @param work 输入参数 `work`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private boolean recover(MutationWorkDTO work, String actorId) {
        Instant now = clock.instant();
        try {
            projector.project(work);
            store.completed(work.mutationId(), now, actorId);
            return true;
        } catch (RuntimeException unavailable) {
            store.failed(
                    work.mutationId(), "AUTH_PROPAGATION_PENDING", now, actorId);
            return false;
        }
    }

    /**
     * 方法 `required` 按照 `AuthorizationMutationRecoveryWorker` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `AuthorizationMutationRecoveryWorker`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param field 输入参数 `field`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }



    }
