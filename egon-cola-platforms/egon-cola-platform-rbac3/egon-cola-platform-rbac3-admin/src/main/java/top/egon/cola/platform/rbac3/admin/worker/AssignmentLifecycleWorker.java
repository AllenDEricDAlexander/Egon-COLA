package top.egon.cola.platform.rbac3.admin.worker;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * 类型 `AssignmentLifecycleWorker` 位于当前包内，是类型，用于承载 `Assignment Lifecycle Worker` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `AssignmentLifecycleWorker` is a type in its package and carries the responsibility, state, or contract for `Assignment Lifecycle Worker`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Applies due assignment state changes claimed by PostgreSQL SKIP LOCKED queries.
 */
public final class AssignmentLifecycleWorker {

    /**
     * 字段 `store` 表示 `AssignmentLifecycleWorker` 中与 `store` 相关的状态、依赖、配置或结果（声明类型 `LifecycleStore`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `store` stores the `store`-related state, dependency, configuration, or result of `AssignmentLifecycleWorker` (declared type `LifecycleStore`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `store` 时应保持 `AssignmentLifecycleWorker` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `store`, preserve `AssignmentLifecycleWorker`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final LifecycleStore store;
    /**
     * 字段 `publisher` 表示 `AssignmentLifecycleWorker` 中与 `publisher` 相关的状态、依赖、配置或结果（声明类型 `ChangePublisher`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `publisher` stores the `publisher`-related state, dependency, configuration, or result of `AssignmentLifecycleWorker` (declared type `ChangePublisher`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `publisher` 时应保持 `AssignmentLifecycleWorker` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `publisher`, preserve `AssignmentLifecycleWorker`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ChangePublisher publisher;
    /**
     * 字段 `clock` 表示 `AssignmentLifecycleWorker` 中与 `clock` 相关的状态、依赖、配置或结果（声明类型 `Clock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `clock` stores the `clock`-related state, dependency, configuration, or result of `AssignmentLifecycleWorker` (declared type `Clock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `clock` 时应保持 `AssignmentLifecycleWorker` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `clock`, preserve `AssignmentLifecycleWorker`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Clock clock;
    /**
     * 字段 `batchSize` 表示 `AssignmentLifecycleWorker` 中与 `batch Size` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `batchSize` stores the `batch Size`-related state, dependency, configuration, or result of `AssignmentLifecycleWorker` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `batchSize` 时应保持 `AssignmentLifecycleWorker` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `batchSize`, preserve `AssignmentLifecycleWorker`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final int batchSize;

    /**
     * 构造器 `AssignmentLifecycleWorker` 用于创建并初始化 `AssignmentLifecycleWorker` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `AssignmentLifecycleWorker` creates and initializes `AssignmentLifecycleWorker`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `AssignmentLifecycleWorker` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `AssignmentLifecycleWorker`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param store 输入参数 `store`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param publisher 输入参数 `publisher`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param batchSize 输入参数 `batchSize`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public AssignmentLifecycleWorker(
            LifecycleStore store,
            ChangePublisher publisher,
            Clock clock,
            int batchSize) {
        this.store = Objects.requireNonNull(store, "store");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.clock = Objects.requireNonNull(clock, "clock");
        if (batchSize < 1 || batchSize > 500) {
            throw new IllegalArgumentException("batchSize must be between 1 and 500");
        }
        this.batchSize = batchSize;
    }

    /**
     * 方法 `runOnce` 按照 `AssignmentLifecycleWorker` 的职责处理输入，完成 `run Once` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `runOnce` processes its inputs according to `AssignmentLifecycleWorker`'s responsibility, performs the `run Once` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `runOnce` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `runOnce`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public int runOnce() {
        return store.processDue(clock.instant(), batchSize, publisher);
    }

    /**
     * 类型 `LifecycleStore` 位于 `AssignmentLifecycleWorker` 内，是接口，用于承载 `Lifecycle Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `LifecycleStore` is an interface inside `AssignmentLifecycleWorker` and carries the responsibility, state, or contract for `Lifecycle Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `LifecycleStore` 作为 `AssignmentLifecycleWorker` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `LifecycleStore` as the responsibility boundary of `AssignmentLifecycleWorker`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface LifecycleStore {

        /**
         * 方法 `processDue` 按照 `LifecycleStore` 的职责处理输入，完成 `process Due` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `processDue` processes its inputs according to `LifecycleStore`'s responsibility, performs the `process Due` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `processDue` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `processDue`, then continue the business flow using its result, exception, or side effect.
         *
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param batchSize 输入参数 `batchSize`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param publisher 输入参数 `publisher`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        int processDue(Instant now, int batchSize, ChangePublisher publisher);
    }

    /**
     * 类型 `ChangePublisher` 位于 `AssignmentLifecycleWorker` 内，是接口，用于承载 `Change Publisher` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ChangePublisher` is an interface inside `AssignmentLifecycleWorker` and carries the responsibility, state, or contract for `Change Publisher`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ChangePublisher` 作为 `AssignmentLifecycleWorker` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ChangePublisher` as the responsibility boundary of `AssignmentLifecycleWorker`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface ChangePublisher {

        /**
         * 方法 `publish` 按照 `ChangePublisher` 的职责处理输入，完成 `publish` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `publish` processes its inputs according to `ChangePublisher`'s responsibility, performs the `publish` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `publish` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `publish`, then continue the business flow using its result, exception, or side effect.
         *
         * @param change 输入参数 `change`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void publish(LifecycleChange change);
    }

    /**
     * 类型 `LifecycleChange` 位于 `AssignmentLifecycleWorker` 内，是记录类型，用于承载 `Lifecycle Change` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `LifecycleChange` is a record inside `AssignmentLifecycleWorker` and carries the responsibility, state, or contract for `Lifecycle Change`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `LifecycleChange` 作为 `AssignmentLifecycleWorker` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `LifecycleChange` as the responsibility boundary of `AssignmentLifecycleWorker`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param assignmentId 记录组件 `assignmentId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `assignmentId` carries constructor data whose meaning is defined by the record contract.
     * @param userId 记录组件 `userId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `userId` carries constructor data whose meaning is defined by the record contract.
     * @param changeType 记录组件 `changeType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `changeType` carries constructor data whose meaning is defined by the record contract.
     * @param authVersion 记录组件 `authVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authVersion` carries constructor data whose meaning is defined by the record contract.
     */
    public record LifecycleChange(
            /**
             * 字段 `tenantId` 表示 `LifecycleChange` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `LifecycleChange` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `LifecycleChange` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `LifecycleChange`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `assignmentId` 表示 `LifecycleChange` 中与 `assignment Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `assignmentId` stores the `assignment Id`-related state, dependency, configuration, or result of `LifecycleChange` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `assignmentId` 时应保持 `LifecycleChange` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `assignmentId`, preserve `LifecycleChange`'s lifecycle, immutability, and thread-safety constraints.
             */
            String assignmentId,
            /**
             * 字段 `userId` 表示 `LifecycleChange` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `LifecycleChange` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userId` 时应保持 `LifecycleChange` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userId`, preserve `LifecycleChange`'s lifecycle, immutability, and thread-safety constraints.
             */
            String userId,
            /**
             * 字段 `changeType` 表示 `LifecycleChange` 中与 `change Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `changeType` stores the `change Type`-related state, dependency, configuration, or result of `LifecycleChange` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `changeType` 时应保持 `LifecycleChange` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `changeType`, preserve `LifecycleChange`'s lifecycle, immutability, and thread-safety constraints.
             */
            String changeType,
            /**
             * 字段 `authVersion` 表示 `LifecycleChange` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `LifecycleChange` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `LifecycleChange` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `LifecycleChange`'s lifecycle, immutability, and thread-safety constraints.
             */
            long authVersion) {
    }
}
