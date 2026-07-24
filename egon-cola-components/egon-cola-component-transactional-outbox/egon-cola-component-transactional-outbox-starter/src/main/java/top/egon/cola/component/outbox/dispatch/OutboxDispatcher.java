package top.egon.cola.component.outbox.dispatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxProperties;
import top.egon.cola.component.outbox.deadletter.OutboxDeadLetterNotifier;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryFailureClassifier;
import top.egon.cola.component.outbox.delivery.DeliveryHandler;
import top.egon.cola.component.outbox.delivery.DeliveryHandlerRegistry;
import top.egon.cola.component.outbox.delivery.DeliveryResult;
import top.egon.cola.component.outbox.event.OutboxDeadLetterEvent;
import top.egon.cola.component.outbox.observability.OutboxMetrics;
import top.egon.cola.component.outbox.retry.OutboxRetryPolicy;
import top.egon.cola.component.outbox.store.OutboxRecord;
import top.egon.cola.component.outbox.store.OutboxStore;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

public class OutboxDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxDispatcher.class);
    private static final String HANDLER_MISSING = "OUTBOX_HANDLER_MISSING";
    private static final String DESTINATION_INVALID = "OUTBOX_DESTINATION_INVALID";
    private static final String RETRY_EXHAUSTED = "OUTBOX_RETRY_EXHAUSTED";

    private final OutboxStore store;
    private final DeliveryHandlerRegistry handlerRegistry;
    private final DeliveryFailureClassifier failureClassifier;
    private final OutboxRetryPolicy retryPolicy;
    private final OutboxDeadLetterNotifier deadLetterNotifier;
    private final OutboxMetrics metrics;
    private final OutboxWorkerIdentity workerIdentity;
    private final TaskExecutor taskExecutor;
    private final TransactionalOutboxProperties properties;
    private final Clock clock;
    private final Semaphore deliveryPermits;
    private final AtomicBoolean coordinatorScheduled = new AtomicBoolean();
    private final AtomicBoolean fullPollRequested = new AtomicBoolean();
    private final Object pendingMonitor = new Object();
    private final Set<String> pendingMessageIds = new LinkedHashSet<>();

    public OutboxDispatcher(
            OutboxStore store,
            DeliveryHandlerRegistry handlerRegistry,
            DeliveryFailureClassifier failureClassifier,
            OutboxRetryPolicy retryPolicy,
            OutboxDeadLetterNotifier deadLetterNotifier,
            OutboxMetrics metrics,
            OutboxWorkerIdentity workerIdentity,
            TaskExecutor taskExecutor,
            TransactionalOutboxProperties properties,
            Clock clock
    ) {
        this.store = store;
        this.handlerRegistry = handlerRegistry;
        this.failureClassifier = failureClassifier;
        this.retryPolicy = retryPolicy;
        this.deadLetterNotifier = deadLetterNotifier;
        this.metrics = metrics;
        this.workerIdentity = workerIdentity;
        this.taskExecutor = taskExecutor;
        this.properties = properties;
        this.clock = clock;
        int capacity = Math.max(
                1,
                properties.getPolling().getConcurrency()
                        + properties.getDelivery().getQueueCapacity() - 1
        );
        this.deliveryPermits = new Semaphore(capacity);
    }

    public void submitDue() {
        fullPollRequested.set(true);
        scheduleCoordinator();
    }

    public void submitMessageIds(Collection<String> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }
        synchronized (pendingMonitor) {
            for (String messageId : messageIds) {
                if (pendingMessageIds.size() >= properties.getPolling().getBatchSize()) {
                    fullPollRequested.set(true);
                    break;
                }
                pendingMessageIds.add(messageId);
            }
        }
        scheduleCoordinator();
    }

    void dispatchDue() {
        dispatch(limit -> store.claimDue(
                limit,
                workerIdentity.nextLeaseOwner(),
                properties.getDelivery().getLeaseDuration()
        ));
    }

    void dispatchMessageIds(Collection<String> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }
        List<String> selectedIds = List.copyOf(messageIds);
        dispatch(limit -> store.claimByMessageIds(
                selectedIds,
                limit,
                workerIdentity.nextLeaseOwner(),
                properties.getDelivery().getLeaseDuration()
        ));
    }

    private void scheduleCoordinator() {
        if (!coordinatorScheduled.compareAndSet(false, true)) {
            return;
        }
        AtomicBoolean started = new AtomicBoolean();
        try {
            taskExecutor.execute(() -> {
                started.set(true);
                runCoordinator();
            });
        } catch (RuntimeException exception) {
            if (started.get()) {
                throw exception;
            }
            coordinatorScheduled.set(false);
            metrics.wakeupRejected();
            LOGGER.warn("Transactional outbox coordinator submission was rejected");
        }
    }

    private void runCoordinator() {
        try {
            boolean fullPoll = fullPollRequested.getAndSet(false);
            List<String> messageIds;
            synchronized (pendingMonitor) {
                messageIds = new ArrayList<>(pendingMessageIds);
                pendingMessageIds.clear();
            }
            if (fullPoll) {
                dispatchDue();
            } else {
                dispatchMessageIds(messageIds);
            }
        } finally {
            coordinatorScheduled.set(false);
            if (hasPendingWork()) {
                scheduleCoordinator();
            }
        }
    }

    private boolean hasPendingWork() {
        if (fullPollRequested.get()) {
            return true;
        }
        synchronized (pendingMonitor) {
            return !pendingMessageIds.isEmpty();
        }
    }

    private void dispatch(ClaimOperation claimOperation) {
        int reservedPermits = reservePermits();
        if (reservedPermits == 0) {
            return;
        }
        List<OutboxRecord> records;
        try {
            records = claimOperation.claim(reservedPermits);
        } catch (RuntimeException | Error failure) {
            deliveryPermits.release(reservedPermits);
            throw failure;
        }

        int unusedPermits = reservedPermits - records.size();
        if (unusedPermits > 0) {
            deliveryPermits.release(unusedPermits);
        }
        metrics.claimed(records.size());
        try {
            for (OutboxRecord record : records) {
                submitDelivery(record);
            }
        } finally {
            updateBacklog();
        }
    }

    private int reservePermits() {
        int reserved = 0;
        int batchSize = properties.getPolling().getBatchSize();
        while (reserved < batchSize && deliveryPermits.tryAcquire()) {
            reserved++;
        }
        return reserved;
    }

    private void submitDelivery(OutboxRecord record) {
        AtomicBoolean started = new AtomicBoolean();
        try {
            taskExecutor.execute(() -> {
                started.set(true);
                try {
                    deliver(record);
                } finally {
                    deliveryPermits.release();
                }
            });
        } catch (RuntimeException exception) {
            if (started.get()) {
                throw exception;
            }
            deliveryPermits.release();
            LOGGER.warn("Transactional outbox delivery submission was rejected");
        }
    }

    private void deliver(OutboxRecord record) {
        Instant startedAt = clock.instant();
        String metricResult = "error";
        DeliveryResult result;
        try {
            DeliveryHandler handler;
            try {
                handler = handlerRegistry.required(record.channel());
            } catch (RuntimeException exception) {
                result = DeliveryResult.permanentFailure(HANDLER_MISSING, HANDLER_MISSING);
                metricResult = metricResult(result);
                applyResult(record, result);
                return;
            }
            try {
                handler.validateDestination(record.destination());
            } catch (RuntimeException exception) {
                result = DeliveryResult.permanentFailure(
                        DESTINATION_INVALID,
                        DESTINATION_INVALID
                );
                metricResult = metricResult(result);
                applyResult(record, result);
                return;
            }
            DeliveryContext context = new DeliveryContext(
                    record.messageId(),
                    record.channel(),
                    record.destination(),
                    record.payload(),
                    record.contentType(),
                    record.schemaVersion(),
                    record.headers(),
                    record.traceId(),
                    record.attemptCount(),
                    record.maxAttempts(),
                    clock.instant().plus(properties.getDelivery().getTimeout())
            );
            try {
                result = handler.deliver(context);
            } catch (Exception exception) {
                result = failureClassifier.classify(exception);
            }
            if (result == null) {
                result = DeliveryResult.permanentFailure(
                        "OUTBOX_INVALID_DELIVERY_RESULT",
                        "OUTBOX_INVALID_DELIVERY_RESULT"
                );
            }
            metricResult = metricResult(result);
            applyResult(record, result);
        } catch (Error error) {
            LOGGER.error(
                    "Fatal transactional outbox delivery error for internal record {}",
                    record.id()
            );
            throw error;
        } finally {
            Duration duration = Duration.between(startedAt, clock.instant());
            if (duration.isNegative()) {
                duration = Duration.ZERO;
            }
            metrics.delivery(record.channel(), metricResult, duration);
        }
    }

    private String metricResult(DeliveryResult result) {
        return result.kind().name().toLowerCase(java.util.Locale.ROOT);
    }

    private void applyResult(OutboxRecord record, DeliveryResult result) {
        switch (result.kind()) {
            case SUCCESS -> transition(
                    store.markSucceeded(record.id(), record.lockedBy()),
                    record
            );
            case RETRYABLE_FAILURE -> applyRetryableFailure(record, result);
            case PERMANENT_FAILURE -> markDead(
                    record,
                    sanitize(result.code(), 64),
                    sanitize(result.message(), 512)
            );
        }
    }

    private void applyRetryableFailure(OutboxRecord record, DeliveryResult result) {
        if (record.attemptCount() >= record.maxAttempts()) {
            markDead(record, RETRY_EXHAUSTED, sanitize(result.code(), 512));
            return;
        }
        boolean updated = store.markRetry(
                record.id(),
                record.lockedBy(),
                retryPolicy.nextDelay(record.attemptCount()),
                sanitize(result.code(), 64),
                sanitize(result.message(), 512)
        );
        if (transition(updated, record)) {
            metrics.retry(record.channel());
        }
    }

    private void markDead(OutboxRecord record, String errorCode, String errorMessage) {
        boolean updated = store.markDead(
                record.id(),
                record.lockedBy(),
                errorCode,
                errorMessage
        );
        if (!transition(updated, record)) {
            return;
        }
        metrics.dead(record.channel());
        deadLetterNotifier.notifyDead(new OutboxDeadLetterEvent(
                record.messageId(),
                record.channel(),
                record.destination(),
                record.attemptCount(),
                errorCode,
                errorMessage,
                record.traceId()
        ));
    }

    private boolean transition(boolean updated, OutboxRecord record) {
        if (!updated) {
            metrics.leaseLost();
            LOGGER.warn(
                    "Transactional outbox lease ownership was lost for internal record {}",
                    record.id()
            );
        }
        return updated;
    }

    private void updateBacklog() {
        try {
            metrics.updateBacklog(store.countBacklog());
        } catch (RuntimeException exception) {
            LOGGER.warn("Transactional outbox backlog measurement failed");
        }
    }

    private String sanitize(String value, int maximumLength) {
        if (value == null) {
            return null;
        }
        StringBuilder sanitized = new StringBuilder(Math.min(value.length(), maximumLength));
        value.codePoints().forEach(codePoint -> {
            int width = Character.charCount(codePoint);
            if (width <= maximumLength - sanitized.length()) {
                sanitized.appendCodePoint(Character.isISOControl(codePoint) ? ' ' : codePoint);
            }
        });
        return sanitized.toString();
    }

    @FunctionalInterface
    private interface ClaimOperation {

        List<OutboxRecord> claim(int limit);
    }
}
