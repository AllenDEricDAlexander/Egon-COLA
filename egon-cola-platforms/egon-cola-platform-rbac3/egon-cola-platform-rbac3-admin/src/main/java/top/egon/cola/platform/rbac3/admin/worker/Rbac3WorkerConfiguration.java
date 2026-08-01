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
 * Schedules bounded, reentrant RBAC3 recovery work.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class Rbac3WorkerConfiguration {

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

    @Bean
    AuthorizationMutationRecoveryWorker authorizationMutationRecoveryWorker(
            AuthorizationMutationRecoveryWorker.RecoveryStore store,
            AuthorizationMutationRecoveryWorker.ProjectionExecutor projector,
            Clock clock) {
        return new AuthorizationMutationRecoveryWorker(
                store, projector, clock, 50);
    }

    @Bean
    RuntimeSnapshotRebuildWorker runtimeSnapshotRebuildWorker(
            RuntimeSnapshotRebuildWorker.ProjectionCheckpointStore checkpoints,
            RuntimeSnapshotRebuildWorker.RebuildPort rebuildPort) {
        return new RuntimeSnapshotRebuildWorker(checkpoints, rebuildPort);
    }

    @Bean
    Rbac3RuntimeProjectionDeliveryHandler rbac3RuntimeProjectionDeliveryHandler(
            ObjectProvider<RuntimeSnapshotRebuildWorker> rebuildWorker,
            ObjectMapper objectMapper) {
        return new Rbac3RuntimeProjectionDeliveryHandler(
                event -> rebuildWorker.getObject().project(event), objectMapper);
    }

    @Bean
    Rbac3WorkerSchedules rbac3WorkerSchedules(
            AssignmentLifecycleWorker assignmentWorker,
            AuthorizationMutationRecoveryWorker mutationWorker) {
        return new Rbac3WorkerSchedules(assignmentWorker, mutationWorker);
    }

    static final class Rbac3WorkerSchedules {

        private final AssignmentLifecycleWorker assignmentWorker;
        private final AuthorizationMutationRecoveryWorker mutationWorker;

        private Rbac3WorkerSchedules(
                AssignmentLifecycleWorker assignmentWorker,
                AuthorizationMutationRecoveryWorker mutationWorker) {
            this.assignmentWorker = assignmentWorker;
            this.mutationWorker = mutationWorker;
        }

        @Scheduled(fixedDelayString =
                "${egon.rbac3.worker.assignment-fixed-delay:5s}")
        void processAssignments() {
            assignmentWorker.runOnce();
        }

        @Scheduled(fixedDelayString =
                "${egon.rbac3.worker.mutation-fixed-delay:2s}")
        void recoverMutations() {
            mutationWorker.runOnce();
        }
    }
}
