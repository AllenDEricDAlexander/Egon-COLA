package top.egon.cola.component.ddc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import top.egon.cola.component.ddc.client.DdcAdminClient;
import top.egon.cola.component.ddc.model.dto.DdcAckRequest;
import top.egon.cola.component.common.trace.TraceSnapshot;
import top.egon.cola.component.ddc.trace.DdcTraceSupport;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class DdcAckDelivery implements SmartLifecycle, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DdcAckDelivery.class);

    private static final String WORKER_NAME = "egon-cola-ddc-ack-delivery";

    private final DdcAdminClient adminClient;

    private final DdcAckDeliveryProperties properties;

    private final Object pendingMonitor = new Object();

    private final Map<AckKey, PendingDelivery> pending = new HashMap<>();

    private final AtomicBoolean running = new AtomicBoolean();

    private final AtomicLong submitted = new AtomicLong();

    private final AtomicLong delivered = new AtomicLong();

    private final AtomicLong retried = new AtomicLong();

    private final AtomicLong deduplicated = new AtomicLong();

    private final AtomicLong saturated = new AtomicLong();

    private final AtomicLong exhausted = new AtomicLong();

    private final AtomicLong nonRetryableFailures = new AtomicLong();

    private final AtomicLong rejectedWhileStopped = new AtomicLong();

    private final AtomicLong droppedOnShutdown = new AtomicLong();

    private volatile ScheduledThreadPoolExecutor executor;

    private volatile boolean workerTerminated = true;

    public DdcAckDelivery(DdcAdminClient adminClient,
                          DdcAckDeliveryProperties properties) {
        if (adminClient == null) {
            throw new IllegalArgumentException("adminClient must not be null");
        }
        if (properties == null) {
            throw new IllegalArgumentException("properties must not be null");
        }
        this.adminClient = adminClient;
        this.properties = properties;
        validateProperties();
    }

    public boolean submit(DdcAckRequest request) {
        AckKey key = AckKey.from(request);
        PendingDelivery delivery;
        ScheduledThreadPoolExecutor current;
        synchronized (pendingMonitor) {
            current = executor;
            if (!running.get() || current == null || current.isShutdown()) {
                rejectedWhileStopped.incrementAndGet();
                return false;
            }
            PendingDelivery existing = pending.get(key);
            if (existing != null) {
                deduplicated.incrementAndGet();
                return true;
            }
            if (pending.size() >= properties.getQueueCapacity()) {
                saturated.incrementAndGet();
                LOGGER.warn(
                        "DDC ACK delivery queue saturated changeId={} instanceId={} "
                                + "leaseId={} pending={} capacity={}",
                        key.changeId(),
                        key.instanceId(),
                        key.leaseId(),
                        pending.size(),
                        properties.getQueueCapacity()
                );
                return false;
            }
            delivery = new PendingDelivery(
                    key,
                    request,
                    DdcTraceSupport.captureOrCreate()
            );
            pending.put(key, delivery);
            submitted.incrementAndGet();
        }
        if (!schedule(current, delivery, 0L)) {
            remove(delivery);
            rejectedWhileStopped.incrementAndGet();
            return false;
        }
        return true;
    }

    @Override
    public synchronized void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        ScheduledThreadPoolExecutor created = new ScheduledThreadPoolExecutor(
                1,
                runnable -> {
                    Thread thread = new Thread(runnable, WORKER_NAME);
                    thread.setDaemon(true);
                    return thread;
                }
        );
        created.setRemoveOnCancelPolicy(true);
        created.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        created.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        executor = created;
        workerTerminated = false;
    }

    @Override
    public synchronized void stop() {
        if (!running.getAndSet(false) && executor == null) {
            return;
        }
        ScheduledThreadPoolExecutor current = executor;
        executor = null;
        if (current != null) {
            current.shutdown();
            awaitTermination(current, properties.getShutdownWait());
            if (!current.isTerminated()) {
                current.shutdownNow();
                awaitTermination(current, properties.getShutdownWait());
            }
            workerTerminated = current.isTerminated();
        } else {
            workerTerminated = true;
        }
        synchronized (pendingMonitor) {
            droppedOnShutdown.addAndGet(pending.size());
            pending.clear();
        }
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }

    @Override
    public void close() {
        stop();
    }

    public long submittedCount() {
        return submitted.get();
    }

    public long deliveredCount() {
        return delivered.get();
    }

    public long retryCount() {
        return retried.get();
    }

    public long deduplicatedCount() {
        return deduplicated.get();
    }

    public long saturationCount() {
        return saturated.get();
    }

    public long exhaustedCount() {
        return exhausted.get();
    }

    public long nonRetryableFailureCount() {
        return nonRetryableFailures.get();
    }

    public long rejectedWhileStoppedCount() {
        return rejectedWhileStopped.get();
    }

    public long droppedOnShutdownCount() {
        return droppedOnShutdown.get();
    }

    public int pendingCount() {
        synchronized (pendingMonitor) {
            return pending.size();
        }
    }

    public boolean isWorkerTerminated() {
        ScheduledThreadPoolExecutor current = executor;
        return current == null ? workerTerminated : current.isTerminated();
    }

    private void deliver(PendingDelivery delivery) {
        int attempt = delivery.attempts().incrementAndGet();
        try {
            adminClient.ack(delivery.request());
            delivered.incrementAndGet();
            remove(delivery);
        } catch (RuntimeException exception) {
            handleFailure(delivery, attempt, exception);
        }
    }

    private void handleFailure(PendingDelivery delivery,
                               int attempt,
                               RuntimeException exception) {
        boolean retryable = isRetryable(exception);
        if (retryable && attempt < properties.getMaxAttempts() && running.get()) {
            long delayMs = retryDelayMs(attempt);
            ScheduledThreadPoolExecutor current = executor;
            if (current != null && schedule(current, delivery, delayMs)) {
                retried.incrementAndGet();
                LOGGER.warn(
                        "DDC ACK delivery retry scheduled changeId={} instanceId={} "
                                + "leaseId={} attempt={} delayMs={} cause={}",
                        delivery.key().changeId(),
                        delivery.key().instanceId(),
                        delivery.key().leaseId(),
                        attempt,
                        delayMs,
                        exception.getClass().getSimpleName()
                );
                return;
            }
        }
        remove(delivery);
        if (retryable && attempt >= properties.getMaxAttempts()) {
            exhausted.incrementAndGet();
            LOGGER.warn(
                    "DDC ACK delivery exhausted changeId={} instanceId={} leaseId={} "
                            + "attempts={} cause={}",
                    delivery.key().changeId(),
                    delivery.key().instanceId(),
                    delivery.key().leaseId(),
                    attempt,
                    exception.getClass().getSimpleName()
            );
        } else if (!retryable) {
            nonRetryableFailures.incrementAndGet();
            LOGGER.warn(
                    "DDC ACK delivery rejected without retry changeId={} instanceId={} "
                            + "leaseId={} attempt={} cause={}",
                    delivery.key().changeId(),
                    delivery.key().instanceId(),
                    delivery.key().leaseId(),
                    attempt,
                    exception.getClass().getSimpleName()
            );
        }
    }

    private boolean schedule(ScheduledThreadPoolExecutor current,
                             PendingDelivery delivery,
                             long delayMs) {
        try {
            current.schedule(
                    DdcTraceSupport.wrapSnapshot(
                            delivery.traceSnapshot(),
                            "ack",
                            () -> deliver(delivery)
                    ),
                    delayMs,
                    TimeUnit.MILLISECONDS
            );
            return true;
        } catch (RejectedExecutionException exception) {
            return false;
        }
    }

    private void remove(PendingDelivery delivery) {
        synchronized (pendingMonitor) {
            pending.remove(delivery.key(), delivery);
        }
    }

    private boolean isRetryable(RuntimeException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ResourceAccessException) {
                return true;
            }
            if (current instanceof RestClientResponseException responseException) {
                return responseException.getStatusCode().is5xxServerError();
            }
            current = current.getCause();
        }
        return false;
    }

    private long retryDelayMs(int completedAttempts) {
        long initial = properties.getInitialBackoff().toMillis();
        long maximum = properties.getMaxBackoff().toMillis();
        int shift = Math.min(completedAttempts - 1, 30);
        long multiplier = 1L << shift;
        long base = initial > Long.MAX_VALUE / multiplier
                ? maximum
                : Math.min(maximum, initial * multiplier);
        double jitter = properties.getJitter();
        if (jitter == 0.0) {
            return base;
        }
        double factor = 1.0 + ThreadLocalRandom.current()
                .nextDouble(-jitter, jitter);
        return Math.max(1L, Math.min(maximum, Math.round(base * factor)));
    }

    private void awaitTermination(ScheduledThreadPoolExecutor current,
                                  Duration timeout) {
        try {
            current.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            current.shutdownNow();
        }
    }

    private void validateProperties() {
        if (properties.getQueueCapacity() <= 0) {
            throw new IllegalArgumentException("ACK queueCapacity must be positive");
        }
        if (properties.getMaxAttempts() <= 0) {
            throw new IllegalArgumentException("ACK maxAttempts must be positive");
        }
        requirePositive(properties.getInitialBackoff(), "initialBackoff");
        requirePositive(properties.getMaxBackoff(), "maxBackoff");
        requirePositive(properties.getShutdownWait(), "shutdownWait");
        if (properties.getInitialBackoff().compareTo(properties.getMaxBackoff()) > 0) {
            throw new IllegalArgumentException(
                    "ACK initialBackoff must not exceed maxBackoff"
            );
        }
        if (properties.getJitter() < 0.0 || properties.getJitter() >= 1.0) {
            throw new IllegalArgumentException("ACK jitter must be in [0, 1)");
        }
    }

    private void requirePositive(Duration value, String fieldName) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("ACK " + fieldName + " must be positive");
        }
    }

    private record AckKey(String changeId, String instanceId, String leaseId) {

        private static AckKey from(DdcAckRequest request) {
            if (request == null) {
                throw new IllegalArgumentException("ACK request must not be null");
            }
            return new AckKey(
                    required(request.getChangeId(), "changeId"),
                    required(request.getInstanceId(), "instanceId"),
                    required(request.getLeaseId(), "leaseId")
            );
        }

        private static String required(String value, String fieldName) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("ACK " + fieldName + " must not be blank");
            }
            return value;
        }
    }

    private record PendingDelivery(
            AckKey key,
            DdcAckRequest request,
            TraceSnapshot traceSnapshot,
            AtomicInteger attempts
    ) {

        private PendingDelivery(AckKey key,
                                DdcAckRequest request,
                                TraceSnapshot traceSnapshot) {
            this(key, request, traceSnapshot, new AtomicInteger());
        }
    }
}
