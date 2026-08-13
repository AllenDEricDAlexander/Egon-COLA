package top.egon.cola.platform.rbac3.admin.config.runtime;

import top.egon.cola.platform.rbac3.admin.runtime.controller.scheduled.AssignmentLifecycleWorker;
import top.egon.cola.platform.rbac3.admin.runtime.controller.scheduled.AuthorizationMutationRecoveryWorker;
import top.egon.cola.platform.rbac3.admin.runtime.controller.scheduled.RuntimeSnapshotRebuildWorker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import top.egon.cola.platform.rbac3.admin.runtime.repository.AuthorizationEventPublisher;
import top.egon.cola.platform.rbac3.admin.runtime.controller.message.Rbac3RuntimeProjectionDeliveryHandler;

import java.time.Clock;
import java.util.Map;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.AuthorizationEventVO;
import top.egon.cola.platform.rbac3.admin.runtime.repository.AssignmentLifecycleRepository;
import top.egon.cola.platform.rbac3.admin.runtime.repository.AuthorizationMutationRecoveryRepository;
import top.egon.cola.platform.rbac3.admin.runtime.service.RuntimeProjectionExecutor;
import top.egon.cola.platform.rbac3.admin.runtime.repository.ProjectionCheckpointRepository;
import top.egon.cola.platform.rbac3.admin.runtime.service.AssignmentLifecycleService;
import top.egon.cola.platform.rbac3.admin.runtime.service.AuthorizationMutationRecoveryService;
import top.egon.cola.platform.rbac3.admin.runtime.service.RuntimeProjectionService;
import top.egon.cola.platform.rbac3.admin.runtime.service.RuntimeSnapshotRebuildService;
import top.egon.cola.platform.rbac3.admin.runtime.service.RuntimeSnapshotProjectionService;

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
    AssignmentLifecycleService assignmentLifecycleService(
            AssignmentLifecycleRepository store,
            AuthorizationEventPublisher events,
            Clock clock) {
        return new AssignmentLifecycleService(
                store,
                change -> events.enqueue(new AuthorizationEventVO(
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

    @Bean
    AssignmentLifecycleWorker assignmentLifecycleWorker(
            AssignmentLifecycleService service) {
        return new AssignmentLifecycleWorker(service);
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
    AuthorizationMutationRecoveryService authorizationMutationRecoveryService(
            AuthorizationMutationRecoveryRepository store,
            RuntimeProjectionExecutor projector,
            Clock clock) {
        return new AuthorizationMutationRecoveryService(
                store, projector, clock, 50);
    }

    @Bean
    AuthorizationMutationRecoveryWorker authorizationMutationRecoveryWorker(
            AuthorizationMutationRecoveryService service) {
        return new AuthorizationMutationRecoveryWorker(service);
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
    RuntimeSnapshotProjectionService runtimeSnapshotProjectionService(
            ProjectionCheckpointRepository checkpoints,
            RuntimeSnapshotRebuildService rebuildPort) {
        return new RuntimeSnapshotProjectionService(checkpoints, rebuildPort);
    }

    @Bean
    RuntimeSnapshotRebuildWorker runtimeSnapshotRebuildWorker(
            RuntimeProjectionService service) {
        return new RuntimeSnapshotRebuildWorker(service);
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
            ObjectProvider<RuntimeProjectionService> projectionService,
            ObjectMapper objectMapper) {
        return new Rbac3RuntimeProjectionDeliveryHandler(
                event -> projectionService.getObject().project(event), objectMapper);
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

    }
