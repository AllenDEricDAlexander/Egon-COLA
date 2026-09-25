package top.egon.cola.component.outbox.dispatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.outbox.api.OutboxReceipt;
import top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxProperties;
import top.egon.cola.component.outbox.deadletter.OutboxDeadLetterNotifier;
import top.egon.cola.component.outbox.delivery.DefaultDeliveryFailureClassifier;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryHandler;
import top.egon.cola.component.outbox.delivery.DeliveryHandlerRegistry;
import top.egon.cola.component.outbox.delivery.DeliveryResult;
import top.egon.cola.component.outbox.event.OutboxDeadLetterEvent;
import top.egon.cola.component.outbox.observability.OutboxMetrics;
import top.egon.cola.component.outbox.common.exception.OutboxStateMachineException;
import top.egon.cola.component.outbox.store.NewOutboxRecord;
import top.egon.cola.component.outbox.store.OutboxRecord;
import top.egon.cola.component.outbox.store.OutboxStatus;
import top.egon.cola.component.outbox.store.OutboxStore;
import top.egon.cola.component.outbox.statemachine.OutboxLifecycleService;
import top.egon.cola.component.outbox.statemachine.OutboxLifecycleSignalEnum;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxDispatcherTest {

    private final RecordingOutboxStore store = new RecordingOutboxStore();
    private final RecordingDeliveryHandler handler = new RecordingDeliveryHandler();
    private final RecordingMetrics metrics = new RecordingMetrics();
    private final OutboxLifecycleService lifecycleService = mock(OutboxLifecycleService.class);
    private final List<OutboxDeadLetterEvent> deadEvents = new ArrayList<>();
    private OutboxDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        TransactionalOutboxProperties properties = new TransactionalOutboxProperties();
        properties.getPolling().setBatchSize(10);
        properties.getPolling().setConcurrency(2);
        properties.getDelivery().setQueueCapacity(8);
        when(lifecycleService.evaluate(anyString(), eq(OutboxLifecycleSignalEnum.DELIVERY_SUCCEEDED), anyInt(), anyInt()))
                .thenReturn(OutboxStatus.SUCCEEDED);
        when(lifecycleService.evaluate(anyString(), eq(OutboxLifecycleSignalEnum.DELIVERY_RETRYABLE), anyInt(), anyInt()))
                .thenAnswer(invocation -> invocation.<Integer>getArgument(2)
                        >= invocation.<Integer>getArgument(3) ? OutboxStatus.DEAD : OutboxStatus.RETRY_WAIT);
        when(lifecycleService.evaluate(anyString(), eq(OutboxLifecycleSignalEnum.DELIVERY_PERMANENT), anyInt(), anyInt()))
                .thenReturn(OutboxStatus.DEAD);
        dispatcher = dispatcher(
                new DeliveryHandlerRegistry(List.of(handler)),
                properties
        );
    }

    @Test
    void shouldMarkSuccessfulDeliveryWithCurrentOwner() {
        store.add(record(1, "message-1", "http", 11, 10));
        handler.results.put("message-1", DeliveryResult.success());

        dispatcher.dispatchDue();

        assertThat(store.succeeded).containsExactly("message-1");
        assertThat(store.retried).isEmpty();
        assertThat(store.dead).isEmpty();
        verify(lifecycleService).evaluate("PROCESSING", OutboxLifecycleSignalEnum.DELIVERY_SUCCEEDED, 11, 10);
    }

    @Test
    void shouldRetryRetryableFailureWhenAttemptsRemain() {
        store.add(record(1, "message-1", "http", 2, 10));
        handler.results.put(
                "message-1",
                DeliveryResult.retryableFailure("HTTP_503", "unavailable")
        );

        dispatcher.dispatchDue();

        assertThat(store.retried.get("message-1")).isEqualTo(Duration.ofSeconds(2));
        assertThat(metrics.retryCount).isEqualTo(1);
        verify(lifecycleService).evaluate("PROCESSING", OutboxLifecycleSignalEnum.DELIVERY_RETRYABLE, 2, 10);
    }

    @Test
    void shouldDeadLetterExhaustedFailureWithoutStoppingBatch() {
        store.add(
                record(1, "poison", "http", 10, 10),
                record(2, "healthy", "http", 1, 10)
        );
        handler.results.put(
                "poison",
                DeliveryResult.retryableFailure("HTTP_503", "unavailable")
        );
        handler.results.put("healthy", DeliveryResult.success());

        dispatcher.dispatchDue();

        assertThat(store.dead).containsExactly("poison");
        assertThat(store.deadCodes.get("poison")).isEqualTo("OUTBOX_RETRY_EXHAUSTED");
        assertThat(store.succeeded).containsExactly("healthy");
        assertThat(metrics.deadCount).isEqualTo(1);
        assertThat(deadEvents).extracting(OutboxDeadLetterEvent::messageId)
                .containsExactly("poison");
        verify(lifecycleService).evaluate("PROCESSING", OutboxLifecycleSignalEnum.DELIVERY_RETRYABLE, 10, 10);
        verify(lifecycleService).evaluate("PROCESSING", OutboxLifecycleSignalEnum.DELIVERY_SUCCEEDED, 1, 10);
    }

    @Test
    void shouldObserveLeaseLossWithoutOverwritingNewOwner() {
        store.add(record(1, "message-1", "http", 1, 10));
        store.allowOwnerUpdate = false;
        handler.results.put("message-1", DeliveryResult.success());

        dispatcher.dispatchDue();

        assertThat(metrics.leaseLostCount).isEqualTo(1);
        verify(lifecycleService).evaluate("PROCESSING", OutboxLifecycleSignalEnum.DELIVERY_SUCCEEDED, 1, 10);
    }

    @Test
    void shouldNotCountRetryWhenTheStoreRejectsTheOldOwner() {
        store.add(record(1, "message-1", "http", 2, 10));
        store.allowOwnerUpdate = false;
        handler.results.put("message-1", DeliveryResult.retryableFailure("HTTP_503", "unavailable"));

        dispatcher.dispatchDue();

        assertThat(store.retried).isEmpty();
        assertThat(metrics.retryCount).isZero();
        assertThat(metrics.leaseLostCount).isEqualTo(1);
    }

    @Test
    void shouldNotPublishDeadLetterWhenTheStoreRejectsTheOldOwner() {
        store.add(record(1, "message-1", "http", 1, 10));
        store.allowOwnerUpdate = false;
        handler.results.put("message-1", DeliveryResult.permanentFailure("INVALID", "invalid"));

        dispatcher.dispatchDue();

        assertThat(store.dead).isEmpty();
        assertThat(metrics.deadCount).isZero();
        assertThat(metrics.leaseLostCount).isEqualTo(1);
        assertThat(deadEvents).isEmpty();
    }

    @Test
    void shouldLeaveRecordProcessingWhenErrorEscapes() {
        store.add(record(1, "message-1", "http", 1, 10));
        handler.errors.put("message-1", new AssertionError("fatal"));

        assertThatThrownBy(dispatcher::dispatchDue).isInstanceOf(AssertionError.class);

        assertThat(store.succeeded).isEmpty();
        assertThat(store.retried).isEmpty();
        assertThat(store.dead).isEmpty();
        org.mockito.Mockito.verifyNoInteractions(lifecycleService);
    }

    @Test
    void shouldDeadLetterMissingHandlerWithoutStoppingBatch() {
        store.add(
                record(1, "missing", "removed", 1, 10),
                record(2, "healthy", "http", 1, 10)
        );
        handler.results.put("healthy", DeliveryResult.success());

        dispatcher.dispatchDue();

        assertThat(store.deadCodes.get("missing")).isEqualTo("OUTBOX_HANDLER_MISSING");
        assertThat(store.succeeded).containsExactly("healthy");
        verify(lifecycleService).evaluate("PROCESSING", OutboxLifecycleSignalEnum.DELIVERY_PERMANENT, 1, 10);
        verify(lifecycleService).evaluate("PROCESSING", OutboxLifecycleSignalEnum.DELIVERY_SUCCEEDED, 1, 10);
    }

    @Test
    void shouldRejectUnexpectedLifecycleTargetBeforeWriting() {
        store.add(record(1, "message-1", "http", 1, 10));
        handler.results.put("message-1", DeliveryResult.success());
        when(lifecycleService.evaluate(anyString(), eq(OutboxLifecycleSignalEnum.DELIVERY_SUCCEEDED), anyInt(), anyInt()))
                .thenReturn(OutboxStatus.PROCESSING);

        assertThatThrownBy(dispatcher::dispatchDue)
                .isInstanceOf(OutboxStateMachineException.class);

        assertThat(store.succeeded).isEmpty();
        assertThat(store.retried).isEmpty();
        assertThat(store.dead).isEmpty();
        verify(lifecycleService).evaluate("PROCESSING", OutboxLifecycleSignalEnum.DELIVERY_SUCCEEDED, 1, 10);
    }

    private OutboxDispatcher dispatcher(
            DeliveryHandlerRegistry registry,
            TransactionalOutboxProperties properties
    ) {
        return new OutboxDispatcher(
                store,
                registry,
                new DefaultDeliveryFailureClassifier(),
                attempt -> Duration.ofSeconds(attempt),
                new OutboxDeadLetterNotifier(List.of(deadEvents::add)),
                metrics,
                new OutboxWorkerIdentity("node-a"),
                Runnable::run,
                properties,
                Clock.fixed(Instant.parse("2026-07-24T12:00:00Z"), ZoneOffset.UTC),
                lifecycleService
        );
    }

    private OutboxRecord record(long id, String messageId, String channel, int attempt, int max) {
        Instant now = Instant.parse("2026-07-24T12:00:00Z");
        return new OutboxRecord(
                id,
                messageId,
                "key-" + messageId,
                "fingerprint",
                channel,
                "orders",
                "{}",
                "application/json",
                null,
                Map.of(),
                "trace-1",
                OutboxStatus.PROCESSING,
                attempt,
                max,
                now,
                null,
                null,
                null,
                null,
                now,
                now,
                null
        );
    }

    private static final class RecordingDeliveryHandler implements DeliveryHandler {

        private final Map<String, DeliveryResult> results = new LinkedHashMap<>();
        private final Map<String, Error> errors = new LinkedHashMap<>();

        @Override
        public String channel() {
            return "http";
        }

        @Override
        public void validateDestination(String destination) {
        }

        @Override
        public DeliveryResult deliver(DeliveryContext context) {
            Error error = errors.get(context.messageId());
            if (error != null) {
                throw error;
            }
            return results.get(context.messageId());
        }
    }

    private static final class RecordingMetrics implements OutboxMetrics {

        private int leaseLostCount;
        private int retryCount;
        private int deadCount;

        @Override
        public void enqueue(boolean created) {
        }

        @Override
        public void claimed(int count) {
        }

        @Override
        public void delivery(String channel, String result, Duration duration) {
        }

        @Override
        public void retry(String channel) {
            retryCount++;
        }

        @Override
        public void dead(String channel) {
            deadCount++;
        }

        @Override
        public void leaseLost() {
            leaseLostCount++;
        }

        @Override
        public void wakeupRejected() {
        }

        @Override
        public void updateBacklog(long value) {
        }
    }

    private static final class RecordingOutboxStore implements OutboxStore {

        private final List<OutboxRecord> records = new ArrayList<>();
        private final Map<Long, String> messageIds = new LinkedHashMap<>();
        private final List<String> succeeded = new ArrayList<>();
        private final Map<String, Duration> retried = new LinkedHashMap<>();
        private final List<String> dead = new ArrayList<>();
        private final Map<String, String> deadCodes = new LinkedHashMap<>();
        private boolean allowOwnerUpdate = true;

        void add(OutboxRecord... added) {
            records.addAll(List.of(added));
            for (OutboxRecord record : added) {
                messageIds.put(record.id(), record.messageId());
            }
        }

        @Override
        public OutboxReceipt enqueue(NewOutboxRecord record) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<OutboxRecord> claimDue(int limit, String leaseOwner, Duration leaseDuration) {
            return records.stream().limit(limit).map(record -> withOwner(record, leaseOwner)).toList();
        }

        @Override
        public List<OutboxRecord> claimByMessageIds(
                Collection<String> requestedIds,
                int limit,
                String leaseOwner,
                Duration leaseDuration
        ) {
            return records.stream()
                    .filter(record -> requestedIds.contains(record.messageId()))
                    .limit(limit)
                    .map(record -> withOwner(record, leaseOwner))
                    .toList();
        }

        @Override
        public boolean markSucceeded(long id, String leaseOwner) {
            if (allowOwnerUpdate) {
                succeeded.add(messageIds.get(id));
            }
            return allowOwnerUpdate;
        }

        @Override
        public boolean markRetry(
                long id,
                String leaseOwner,
                Duration delay,
                String errorCode,
                String errorMessage
        ) {
            if (allowOwnerUpdate) {
                retried.put(messageIds.get(id), delay);
            }
            return allowOwnerUpdate;
        }

        @Override
        public boolean markDead(
                long id,
                String leaseOwner,
                String errorCode,
                String errorMessage
        ) {
            if (allowOwnerUpdate) {
                String messageId = messageIds.get(id);
                dead.add(messageId);
                deadCodes.put(messageId, errorCode);
            }
            return allowOwnerUpdate;
        }

        @Override
        public int deleteSucceeded(Duration retention, int limit) {
            return 0;
        }

        @Override
        public long countBacklog() {
            return records.size();
        }

        @Override
        public void validateSchema() {
        }

        private OutboxRecord withOwner(OutboxRecord record, String leaseOwner) {
            return new OutboxRecord(
                    record.id(),
                    record.messageId(),
                    record.idempotencyKey(),
                    record.messageFingerprint(),
                    record.channel(),
                    record.destination(),
                    record.payload(),
                    record.contentType(),
                    record.schemaVersion(),
                    record.headers(),
                    record.traceId(),
                    record.status(),
                    record.attemptCount(),
                    record.maxAttempts(),
                    record.nextAttemptAt(),
                    leaseOwner,
                    record.lockedUntil(),
                    record.lastErrorCode(),
                    record.lastErrorMessage(),
                    record.createdAt(),
                    record.updatedAt(),
                    record.completedAt()
            );
        }
    }
}
