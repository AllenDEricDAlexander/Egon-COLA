package top.egon.cola.platform.rbac3.admin.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import top.egon.cola.platform.rbac3.admin.application.port.AuthorizationEventPort;
import top.egon.cola.platform.rbac3.admin.integration.outbox.Rbac3RuntimeProjectionDeliveryHandler;

import java.time.Clock;
import java.util.Map;

/**
 * 类型 `Rbac3WorkerConfiguration` 位于当前包内，是类型，用于承载 `Rbac3 Worker Configuration` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3WorkerConfiguration` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Worker Configuration`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Schedules bounded, reentrant RBAC3 recovery work.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class Rbac3WorkerConfiguration {

    /**
     * 方法 `assignmentLifecycleWorker` 按照 `Rbac3WorkerConfiguration` 的职责处理输入，完成 `assignment Lifecycle Worker` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `assignmentLifecycleWorker` processes its inputs according to `Rbac3WorkerConfiguration`'s responsibility, performs the `assignment Lifecycle Worker` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `assignmentLifecycleWorker` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `assignmentLifecycleWorker`, then continue the business flow using its result, exception, or side effect.
     *
     * @param store 输入参数 `store`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param events 输入参数 `events`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    AssignmentLifecycleWorker assignmentLifecycleWorker(
            AssignmentLifecycleWorker.LifecycleStore store,
            AuthorizationEventPort events,
            Clock clock) {
        return new AssignmentLifecycleWorker(
                store,
                change -> events.enqueue(new AuthorizationEventPort.AuthorizationEvent(
                        change.tenantId(), "USER", change.userId(),
                        "ASSIGNMENT_CHANGED",
                        Map.of(
                                "assignmentId", change.assignmentId(),
                                "userId", change.userId(),
                                "changeType", change.changeType(),
                                "authVersion", Long.toString(change.authVersion()),
                                "aggregateVersion", Long.toString(change.authVersion())),
                        "assignment-lifecycle:" + change.assignmentId()
                )),
                clock,
                100);
    }

    /**
     * 方法 `authorizationMutationRecoveryWorker` 按照 `Rbac3WorkerConfiguration` 的职责处理输入，完成 `authorization Mutation Recovery Worker` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `authorizationMutationRecoveryWorker` processes its inputs according to `Rbac3WorkerConfiguration`'s responsibility, performs the `authorization Mutation Recovery Worker` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `authorizationMutationRecoveryWorker` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `authorizationMutationRecoveryWorker`, then continue the business flow using its result, exception, or side effect.
     *
     * @param store 输入参数 `store`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param projector 输入参数 `projector`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    AuthorizationMutationRecoveryWorker authorizationMutationRecoveryWorker(
            AuthorizationMutationRecoveryWorker.RecoveryStore store,
            AuthorizationMutationRecoveryWorker.ProjectionExecutor projector,
            Clock clock) {
        return new AuthorizationMutationRecoveryWorker(
                store, projector, clock, 50);
    }

    /**
     * 方法 `runtimeSnapshotRebuildWorker` 按照 `Rbac3WorkerConfiguration` 的职责处理输入，完成 `runtime Snapshot Rebuild Worker` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `runtimeSnapshotRebuildWorker` processes its inputs according to `Rbac3WorkerConfiguration`'s responsibility, performs the `runtime Snapshot Rebuild Worker` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `runtimeSnapshotRebuildWorker` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `runtimeSnapshotRebuildWorker`, then continue the business flow using its result, exception, or side effect.
     *
     * @param checkpoints 输入参数 `checkpoints`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param rebuildPort 输入参数 `rebuildPort`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    RuntimeSnapshotRebuildWorker runtimeSnapshotRebuildWorker(
            RuntimeSnapshotRebuildWorker.ProjectionCheckpointStore checkpoints,
            RuntimeSnapshotRebuildWorker.RebuildPort rebuildPort) {
        return new RuntimeSnapshotRebuildWorker(checkpoints, rebuildPort);
    }

    /**
     * 方法 `rbac3RuntimeProjectionDeliveryHandler` 按照 `Rbac3WorkerConfiguration` 的职责处理输入，完成 `rbac3 Runtime Projection Delivery Handler` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3RuntimeProjectionDeliveryHandler` processes its inputs according to `Rbac3WorkerConfiguration`'s responsibility, performs the `rbac3 Runtime Projection Delivery Handler` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3RuntimeProjectionDeliveryHandler` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3RuntimeProjectionDeliveryHandler`, then continue the business flow using its result, exception, or side effect.
     *
     * @param rebuildWorker 输入参数 `rebuildWorker`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param objectMapper 输入参数 `objectMapper`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    Rbac3RuntimeProjectionDeliveryHandler rbac3RuntimeProjectionDeliveryHandler(
            ObjectProvider<RuntimeSnapshotRebuildWorker> rebuildWorker,
            ObjectMapper objectMapper) {
        return new Rbac3RuntimeProjectionDeliveryHandler(
                event -> rebuildWorker.getObject().project(event), objectMapper);
    }

    /**
     * 方法 `rbac3WorkerSchedules` 按照 `Rbac3WorkerConfiguration` 的职责处理输入，完成 `rbac3 Worker Schedules` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3WorkerSchedules` processes its inputs according to `Rbac3WorkerConfiguration`'s responsibility, performs the `rbac3 Worker Schedules` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3WorkerSchedules` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3WorkerSchedules`, then continue the business flow using its result, exception, or side effect.
     *
     * @param assignmentWorker 输入参数 `assignmentWorker`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param mutationWorker 输入参数 `mutationWorker`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    Rbac3WorkerSchedules rbac3WorkerSchedules(
            AssignmentLifecycleWorker assignmentWorker,
            AuthorizationMutationRecoveryWorker mutationWorker) {
        return new Rbac3WorkerSchedules(assignmentWorker, mutationWorker);
    }

    /**
     * 类型 `Rbac3WorkerSchedules` 位于 `Rbac3WorkerConfiguration` 内，是类型，用于承载 `Rbac3 Worker Schedules` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Rbac3WorkerSchedules` is a type inside `Rbac3WorkerConfiguration` and carries the responsibility, state, or contract for `Rbac3 Worker Schedules`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Rbac3WorkerSchedules` 作为 `Rbac3WorkerConfiguration` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Rbac3WorkerSchedules` as the responsibility boundary of `Rbac3WorkerConfiguration`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    static final class Rbac3WorkerSchedules {

        /**
         * 字段 `assignmentWorker` 表示 `Rbac3WorkerSchedules` 中与 `assignment Worker` 相关的状态、依赖、配置或结果（声明类型 `AssignmentLifecycleWorker`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `assignmentWorker` stores the `assignment Worker`-related state, dependency, configuration, or result of `Rbac3WorkerSchedules` (declared type `AssignmentLifecycleWorker`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `assignmentWorker` 时应保持 `Rbac3WorkerSchedules` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `assignmentWorker`, preserve `Rbac3WorkerSchedules`'s lifecycle, immutability, and thread-safety constraints.
         */
        private final AssignmentLifecycleWorker assignmentWorker;
        /**
         * 字段 `mutationWorker` 表示 `Rbac3WorkerSchedules` 中与 `mutation Worker` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationMutationRecoveryWorker`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `mutationWorker` stores the `mutation Worker`-related state, dependency, configuration, or result of `Rbac3WorkerSchedules` (declared type `AuthorizationMutationRecoveryWorker`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `mutationWorker` 时应保持 `Rbac3WorkerSchedules` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `mutationWorker`, preserve `Rbac3WorkerSchedules`'s lifecycle, immutability, and thread-safety constraints.
         */
        private final AuthorizationMutationRecoveryWorker mutationWorker;

        /**
         * 构造器 `Rbac3WorkerSchedules` 用于创建并初始化 `Rbac3WorkerSchedules` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `Rbac3WorkerSchedules` creates and initializes `Rbac3WorkerSchedules`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `Rbac3WorkerSchedules` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `Rbac3WorkerSchedules`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param assignmentWorker 输入参数 `assignmentWorker`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param mutationWorker 输入参数 `mutationWorker`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        private Rbac3WorkerSchedules(
                AssignmentLifecycleWorker assignmentWorker,
                AuthorizationMutationRecoveryWorker mutationWorker) {
            this.assignmentWorker = assignmentWorker;
            this.mutationWorker = mutationWorker;
        }

        /**
         * 方法 `processAssignments` 按照 `Rbac3WorkerSchedules` 的职责处理输入，完成 `process Assignments` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `processAssignments` processes its inputs according to `Rbac3WorkerSchedules`'s responsibility, performs the `process Assignments` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `processAssignments` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `processAssignments`, then continue the business flow using its result, exception, or side effect.
         */
        @Scheduled(fixedDelayString =
                "${egon.rbac3.worker.assignment-fixed-delay:5s}")
        void processAssignments() {
            assignmentWorker.runOnce();
        }

        /**
         * 方法 `recoverMutations` 按照 `Rbac3WorkerSchedules` 的职责处理输入，完成 `recover Mutations` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `recoverMutations` processes its inputs according to `Rbac3WorkerSchedules`'s responsibility, performs the `recover Mutations` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `recoverMutations` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `recoverMutations`, then continue the business flow using its result, exception, or side effect.
         */
        @Scheduled(fixedDelayString =
                "${egon.rbac3.worker.mutation-fixed-delay:2s}")
        void recoverMutations() {
            mutationWorker.runOnce();
        }
    }
}
