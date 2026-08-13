package top.egon.cola.platform.rbac3.admin.worker;

import top.egon.cola.platform.rbac3.admin.runtime.application.RuntimeQueryService;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 类型 `AuthorizationMutationRecoveryWorker` 位于当前包内，是类型，用于承载 `Authorization Mutation Recovery Worker` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `AuthorizationMutationRecoveryWorker` is a type in its package and carries the responsibility, state, or contract for `Authorization Mutation Recovery Worker`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Replays committed authorization mutations claimed with database row ownership.
 */
public final class AuthorizationMutationRecoveryWorker
        implements RuntimeQueryService.MutationRecoveryPort {

    /**
     * 字段 `store` 表示 `AuthorizationMutationRecoveryWorker` 中与 `store` 相关的状态、依赖、配置或结果（声明类型 `RecoveryStore`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `store` stores the `store`-related state, dependency, configuration, or result of `AuthorizationMutationRecoveryWorker` (declared type `RecoveryStore`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `store` 时应保持 `AuthorizationMutationRecoveryWorker` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `store`, preserve `AuthorizationMutationRecoveryWorker`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RecoveryStore store;
    /**
     * 字段 `projector` 表示 `AuthorizationMutationRecoveryWorker` 中与 `projector` 相关的状态、依赖、配置或结果（声明类型 `ProjectionExecutor`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `projector` stores the `projector`-related state, dependency, configuration, or result of `AuthorizationMutationRecoveryWorker` (declared type `ProjectionExecutor`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `projector` 时应保持 `AuthorizationMutationRecoveryWorker` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `projector`, preserve `AuthorizationMutationRecoveryWorker`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ProjectionExecutor projector;
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
    public AuthorizationMutationRecoveryWorker(
            RecoveryStore store,
            ProjectionExecutor projector,
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
    public RuntimeQueryService.RetryResult retry(
            String tenantId,
            String mutationId,
            String actorId) {
        MutationWork work = store.claimById(
                        required(tenantId, "tenantId"),
                        required(mutationId, "mutationId"))
                .orElseThrow(() -> new IllegalArgumentException(
                        "authorization mutation was not found"));
        if ("COMPLETED".equals(work.status())) {
            return new RuntimeQueryService.RetryResult(mutationId, "COMPLETED");
        }
        boolean completed = recover(work, required(actorId, "actorId"));
        return new RuntimeQueryService.RetryResult(
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
        for (MutationWork work : store.claimRecoverable(batchSize)) {
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
    private boolean recover(MutationWork work, String actorId) {
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

    /**
     * 类型 `RecoveryStore` 位于 `AuthorizationMutationRecoveryWorker` 内，是接口，用于承载 `Recovery Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RecoveryStore` is an interface inside `AuthorizationMutationRecoveryWorker` and carries the responsibility, state, or contract for `Recovery Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RecoveryStore` 作为 `AuthorizationMutationRecoveryWorker` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RecoveryStore` as the responsibility boundary of `AuthorizationMutationRecoveryWorker`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface RecoveryStore {

        /**
         * 方法 `claimById` 按照 `RecoveryStore` 的职责处理输入，完成 `claim By Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `claimById` processes its inputs according to `RecoveryStore`'s responsibility, performs the `claim By Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `claimById` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `claimById`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param mutationId 输入参数 `mutationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        Optional<MutationWork> claimById(String tenantId, String mutationId);

        /**
         * 方法 `claimRecoverable` 按照 `RecoveryStore` 的职责处理输入，完成 `claim Recoverable` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `claimRecoverable` processes its inputs according to `RecoveryStore`'s responsibility, performs the `claim Recoverable` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `claimRecoverable` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `claimRecoverable`, then continue the business flow using its result, exception, or side effect.
         *
         * @param batchSize 输入参数 `batchSize`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        List<MutationWork> claimRecoverable(int batchSize);

        /**
         * 方法 `completed` 按照 `RecoveryStore` 的职责处理输入，完成 `completed` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `completed` processes its inputs according to `RecoveryStore`'s responsibility, performs the `completed` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `completed` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `completed`, then continue the business flow using its result, exception, or side effect.
         *
         * @param mutationId 输入参数 `mutationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void completed(String mutationId, Instant now, String actorId);

        /**
         * 方法 `failed` 按照 `RecoveryStore` 的职责处理输入，完成 `failed` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `failed` processes its inputs according to `RecoveryStore`'s responsibility, performs the `failed` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `failed` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `failed`, then continue the business flow using its result, exception, or side effect.
         *
         * @param mutationId 输入参数 `mutationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void failed(String mutationId, String reasonCode, Instant now, String actorId);
    }

    /**
     * 类型 `ProjectionExecutor` 位于 `AuthorizationMutationRecoveryWorker` 内，是接口，用于承载 `Projection Executor` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ProjectionExecutor` is an interface inside `AuthorizationMutationRecoveryWorker` and carries the responsibility, state, or contract for `Projection Executor`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ProjectionExecutor` 作为 `AuthorizationMutationRecoveryWorker` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ProjectionExecutor` as the responsibility boundary of `AuthorizationMutationRecoveryWorker`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface ProjectionExecutor {

        /**
         * 方法 `project` 按照 `ProjectionExecutor` 的职责处理输入，完成 `project` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `project` processes its inputs according to `ProjectionExecutor`'s responsibility, performs the `project` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `project` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `project`, then continue the business flow using its result, exception, or side effect.
         *
         * @param mutation 输入参数 `mutation`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void project(MutationWork mutation);
    }

    /**
     * 类型 `MutationWork` 位于 `AuthorizationMutationRecoveryWorker` 内，是记录类型，用于承载 `Mutation Work` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `MutationWork` is a record inside `AuthorizationMutationRecoveryWorker` and carries the responsibility, state, or contract for `Mutation Work`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `MutationWork` 作为 `AuthorizationMutationRecoveryWorker` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `MutationWork` as the responsibility boundary of `AuthorizationMutationRecoveryWorker`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param mutationId 记录组件 `mutationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `mutationId` carries constructor data whose meaning is defined by the record contract.
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param scopeType 记录组件 `scopeType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopeType` carries constructor data whose meaning is defined by the record contract.
     * @param scopeId 记录组件 `scopeId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopeId` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     */
    public record MutationWork(
            /**
             * 字段 `mutationId` 表示 `MutationWork` 中与 `mutation Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `mutationId` stores the `mutation Id`-related state, dependency, configuration, or result of `MutationWork` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `mutationId` 时应保持 `MutationWork` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `mutationId`, preserve `MutationWork`'s lifecycle, immutability, and thread-safety constraints.
             */
            String mutationId,
            /**
             * 字段 `tenantId` 表示 `MutationWork` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `MutationWork` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `MutationWork` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `MutationWork`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `scopeType` 表示 `MutationWork` 中与 `scope Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopeType` stores the `scope Type`-related state, dependency, configuration, or result of `MutationWork` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopeType` 时应保持 `MutationWork` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopeType`, preserve `MutationWork`'s lifecycle, immutability, and thread-safety constraints.
             */
            String scopeType,
            /**
             * 字段 `scopeId` 表示 `MutationWork` 中与 `scope Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopeId` stores the `scope Id`-related state, dependency, configuration, or result of `MutationWork` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopeId` 时应保持 `MutationWork` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopeId`, preserve `MutationWork`'s lifecycle, immutability, and thread-safety constraints.
             */
            String scopeId,
            /**
             * 字段 `status` 表示 `MutationWork` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `MutationWork` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `MutationWork` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `MutationWork`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status) {
    }
}
