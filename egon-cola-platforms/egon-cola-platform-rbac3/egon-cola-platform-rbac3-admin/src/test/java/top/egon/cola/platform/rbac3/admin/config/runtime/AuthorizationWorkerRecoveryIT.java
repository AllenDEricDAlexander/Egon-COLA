package top.egon.cola.platform.rbac3.admin.config.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryResult;
import top.egon.cola.platform.rbac3.admin.runtime.controller.message.Rbac3RuntimeProjectionDeliveryHandler;
import top.egon.cola.platform.rbac3.admin.runtime.service.RuntimeQueryService;
import top.egon.cola.platform.rbac3.admin.runtime.controller.scheduled.AssignmentLifecycleWorker;
import top.egon.cola.platform.rbac3.admin.runtime.controller.scheduled.AuthorizationMutationRecoveryWorker;
import top.egon.cola.platform.rbac3.admin.runtime.controller.scheduled.RuntimeSnapshotRebuildWorker;
import top.egon.cola.platform.rbac3.admin.runtime.service.AssignmentLifecycleService;
import top.egon.cola.platform.rbac3.admin.runtime.service.AuthorizationMutationRecoveryService;
import top.egon.cola.platform.rbac3.admin.runtime.service.RuntimeProjectionService;
import top.egon.cola.platform.rbac3.admin.runtime.service.RuntimeSnapshotProjectionService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import top.egon.cola.platform.rbac3.admin.runtime.domain.enums.Rbac3RuntimeProjectionDeliveryHandlerProjectionOutcomeEnum;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.EventEnvelopeVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.RetryResultVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.LifecycleChangeVO;
import top.egon.cola.platform.rbac3.admin.runtime.repository.AuthorizationMutationRecoveryRepository;
import top.egon.cola.platform.rbac3.admin.runtime.domain.dto.MutationWorkDTO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.enums.RuntimeSnapshotRebuildClaimEnum;
import top.egon.cola.platform.rbac3.admin.runtime.repository.ProjectionCheckpointRepository;

class AuthorizationWorkerRecoveryIT {

    private static final Instant NOW = Instant.parse("2026-07-30T12:00:00Z");

    @Test
    void deliveryHandlerDefersRebuildWorkerResolutionUntilDelivery()
            throws NoSuchMethodException {
        var factory = Rbac3WorkerConfiguration.class.getDeclaredMethod(
                "rbac3RuntimeProjectionDeliveryHandler",
                ObjectProvider.class,
                ObjectMapper.class);

        assertThat(factory.getParameterTypes())
                .containsExactly(ObjectProvider.class,
                        ObjectMapper.class);

        AtomicInteger resolutions = new AtomicInteger();
        RuntimeProjectionService projectionService = new RuntimeSnapshotProjectionService(
                new InMemoryCheckpointStore(), event -> {
                });
        @SuppressWarnings("unchecked")
        ObjectProvider<RuntimeProjectionService> provider =
                mock(ObjectProvider.class);
        when(provider.getObject()).thenAnswer(invocation -> {
            resolutions.incrementAndGet();
            return projectionService;
        });
        var handler = new Rbac3WorkerConfiguration()
                .rbac3RuntimeProjectionDeliveryHandler(
                        provider, new ObjectMapper().findAndRegisterModules());

        assertThat(resolutions).hasValue(0);
        DeliveryResult result = handler.deliver(new DeliveryContext(
                "message-1", "rbac3-runtime",
                "rbac3.role-activation.changed.v1",
                """
                        {"eventId":"event-1",
                         "eventType":"rbac3.role-activation.changed.v1",
                         "schemaVersion":1,
                         "occurredAt":"2026-07-30T12:00:00Z",
                         "tenantId":"7","aggregateType":"SESSION",
                         "aggregateId":"99","aggregateVersion":4,
                         "traceId":"trace-1","payload":{}}
                        """,
                "application/json", "1", Map.of(), "trace-1",
                1, 10, NOW.plusSeconds(30)));

        assertThat(result.kind()).isEqualTo(DeliveryResult.Kind.SUCCESS);
        assertThat(resolutions).hasValue(1);
    }

    @Test
    void mutationRecoveryIsAddressedByIdAndIsIdempotent() {
        InMemoryRecoveryStore store = new InMemoryRecoveryStore();
        AtomicInteger projections = new AtomicInteger();
        var service = new AuthorizationMutationRecoveryService(
                store, mutation -> projections.incrementAndGet(),
                Clock.fixed(NOW, ZoneOffset.UTC), 20);

        RetryResultVO first = service.retry("7", "900", "operator");
        RetryResultVO duplicate = service.retry("7", "900", "operator");

        assertThat(first.status()).isEqualTo("COMPLETED");
        assertThat(duplicate.status()).isEqualTo("COMPLETED");
        assertThat(projections).hasValue(1);
        assertThat(store.failureCodes).isEmpty();
    }

    @Test
    void scheduledRecoveryClaimsOnlyItsSkipLockedBatchAndRetriesFailures() {
        InMemoryRecoveryStore store = new InMemoryRecoveryStore();
        store.pending.add(new MutationWorkDTO(
                "901", "7", "SESSION", "99", "RECOVERY_REQUIRED"));
        AtomicInteger attempts = new AtomicInteger();
        var service = new AuthorizationMutationRecoveryService(
                store, mutation -> {
            if (attempts.getAndIncrement() == 0) {
                throw new IllegalStateException("redis unavailable");
            }
        }, Clock.fixed(NOW, ZoneOffset.UTC), 1);
        var worker = new AuthorizationMutationRecoveryWorker(service);

        assertThat(worker.runOnce()).isZero();
        assertThat(store.failureCodes).containsExactly("AUTH_PROPAGATION_PENDING");
        assertThat(worker.runOnce()).isEqualTo(1);
    }

    @Test
    void outboxProjectionUsesStableEventAndAggregateVersionCheckpoint() {
        InMemoryCheckpointStore checkpoints = new InMemoryCheckpointStore();
        AtomicInteger rebuilds = new AtomicInteger();
        var worker = new RuntimeSnapshotRebuildWorker(
                new RuntimeSnapshotProjectionService(
                        checkpoints, event -> rebuilds.incrementAndGet()));
        var event = event("event-1", 4L);

        assertThat(worker.project(event)).isEqualTo(
                Rbac3RuntimeProjectionDeliveryHandlerProjectionOutcomeEnum.APPLIED);
        assertThat(worker.project(event)).isEqualTo(
                Rbac3RuntimeProjectionDeliveryHandlerProjectionOutcomeEnum.ALREADY_APPLIED);
        assertThat(rebuilds).hasValue(1);
    }

    @Test
    void assignmentWorkerPublishesOnlyCommittedLifecycleChanges() {
        List<LifecycleChangeVO> published = new ArrayList<>();
        AssignmentLifecycleService service = new AssignmentLifecycleService(
                (now, batchSize, publisher) -> {
                    publisher.publish(new LifecycleChangeVO(
                            "7", "assignment-1", "user-1", "ACTIVATED", 5));
                    return 1;
                }, published::add, Clock.fixed(NOW, ZoneOffset.UTC), 10);
        AssignmentLifecycleWorker worker = new AssignmentLifecycleWorker(service);

        assertThat(worker.runOnce()).isEqualTo(1);
        assertThat(published).singleElement()
                .extracting(LifecycleChangeVO::changeType)
                .isEqualTo("ACTIVATED");
    }

    private EventEnvelopeVO event(
            String eventId,
            long version) {
        return new EventEnvelopeVO(
                eventId, "rbac3.role-activation.changed.v1", 1, NOW,
                "7", "SESSION", "99", version, "trace-1",
                Map.of("mutationId", "900"));
    }

    private static final class InMemoryRecoveryStore
            implements AuthorizationMutationRecoveryRepository {

        private final List<MutationWorkDTO> pending =
                new ArrayList<>(List.of(new MutationWorkDTO(
                        "900", "7", "SESSION", "99", "RECOVERY_REQUIRED")));
        private final List<String> completed = new ArrayList<>();
        private final List<String> failureCodes = new ArrayList<>();

        @Override
        public Optional<MutationWorkDTO> claimById(
                String tenantId, String mutationId) {
            if (completed.contains(mutationId)) {
                return Optional.of(new MutationWorkDTO(
                        mutationId, tenantId, "SESSION", "99", "COMPLETED"));
            }
            return pending.stream()
                    .filter(value -> value.tenantId().equals(tenantId)
                            && value.mutationId().equals(mutationId))
                    .findFirst();
        }

        @Override
        public List<MutationWorkDTO> claimRecoverable(
                int batchSize) {
            return pending.stream()
                    .filter(value -> !completed.contains(value.mutationId()))
                    .limit(batchSize)
                    .toList();
        }

        @Override
        public void completed(String mutationId, Instant now, String actorId) {
            if (!completed.contains(mutationId)) {
                completed.add(mutationId);
            }
        }

        @Override
        public void failed(String mutationId, String reasonCode, Instant now, String actorId) {
            failureCodes.add(reasonCode);
        }
    }

    private static final class InMemoryCheckpointStore
            implements ProjectionCheckpointRepository {

        private final Map<String, Long> versions = new java.util.HashMap<>();

        @Override
        public RuntimeSnapshotRebuildClaimEnum claim(
                String tenantId,
                String eventId,
                String aggregateType,
                String aggregateId,
                long aggregateVersion) {
            Long previous = versions.putIfAbsent(eventId, aggregateVersion);
            return previous == null
                    ? RuntimeSnapshotRebuildClaimEnum.ACQUIRED
                    : RuntimeSnapshotRebuildClaimEnum.ALREADY_APPLIED;
        }

        @Override
        public void complete(
                String tenantId,
                String eventId,
                String aggregateType,
                String aggregateId,
                long aggregateVersion) {
        }

        @Override
        public void release(
                String tenantId,
                String eventId,
                String aggregateType,
                String aggregateId,
                long aggregateVersion) {
            versions.remove(eventId, aggregateVersion);
        }
    }
}
