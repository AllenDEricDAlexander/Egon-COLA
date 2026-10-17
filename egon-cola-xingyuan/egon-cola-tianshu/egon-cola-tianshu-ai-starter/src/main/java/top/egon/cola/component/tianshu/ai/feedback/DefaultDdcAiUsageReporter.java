package top.egon.cola.component.tianshu.ai.feedback;

import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.tianshu.ai.observability.DdcAiStarterMetrics;
import top.egon.cola.component.tianshu.ai.properties.DdcAiFeedbackProperties;
import top.egon.cola.component.tianshu.api.client.DdcAiFeedbackClient;
import top.egon.cola.component.tianshu.error.DdcClientTransportException;
import top.egon.cola.component.tianshu.model.ai.DdcAiUsageReportBatchDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiUsageReportDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiUsageReportReceiptDTO;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 有界队列 + 定时批量刷写的上报器；传输失败按 1s、2s、4s 退避重试，达上限后丢弃并计数。
 * / Bounded-queue reporter with scheduled batch flushes; transport failures back off 1s, 2s, 4s and the batch is
 * dropped and counted once attempts are exhausted.
 */
@Slf4j
public class DefaultDdcAiUsageReporter implements DdcAiUsageReporter, AutoCloseable {

    static final String REPORTED = "tianshu.ai.feedback.reported";
    static final String DROPPED = "tianshu.ai.feedback.dropped";
    static final Duration BASE_BACKOFF = Duration.ofSeconds(1);

    private final DdcAiFeedbackClient client;
    private final DdcAiFeedbackProperties properties;
    private final DdcAiStarterMetrics metrics;
    private final Validator validator;
    private final ScheduledExecutorService scheduler;
    private final Sleeper sleeper;
    private final BlockingQueue<DdcAiUsageReportDTO> queue;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final ReentrantLock flushLock = new ReentrantLock();
    private volatile ScheduledFuture<?> flushTask;

    public DefaultDdcAiUsageReporter(DdcAiFeedbackClient client, DdcAiFeedbackProperties properties,
            DdcAiStarterMetrics metrics, Validator validator, ScheduledExecutorService scheduler, Sleeper sleeper) {
        this.client = Objects.requireNonNull(client, "client");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        this.validator = Objects.requireNonNull(validator, "validator");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.sleeper = Objects.requireNonNull(sleeper, "sleeper");
        this.queue = new ArrayBlockingQueue<>(properties.getQueueCapacity());
    }

    /** 启动定时刷写。 / Starts the scheduled flush. */
    public void start() {
        long interval = Math.max(1L, properties.getFlushInterval().toMillis());
        flushTask = scheduler.scheduleWithFixedDelay(this::flushSafely, interval, interval, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean report(DdcAiUsageReportDTO report) {
        if (closed.get()) {
            drop("CLOSED", 1);
            return false;
        }
        if (report == null || !validator.validate(report).isEmpty()) {
            drop("INVALID", 1);
            return false;
        }
        if (!queue.offer(report)) {
            drop("QUEUE_FULL", 1);
            return false;
        }
        return true;
    }

    /** 按批刷写当前队列，返回发送批次数。 / Flushes the queue in batches and returns the number of batches sent. */
    public int flush() {
        flushLock.lock();
        try {
            int batches = 0;
            while (!queue.isEmpty()) {
                List<DdcAiUsageReportDTO> batch = new ArrayList<>(properties.getBatchSize());
                queue.drainTo(batch, properties.getBatchSize());
                if (batch.isEmpty()) break;
                send(batch);
                batches++;
            }
            return batches;
        } finally {
            flushLock.unlock();
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        ScheduledFuture<?> task = flushTask;
        if (task != null) task.cancel(false);
        flushSafely();
    }

    private void flushSafely() {
        try {
            flush();
        } catch (RuntimeException unexpected) {
            log.warn("AI usage report flush failed: {}", unexpected.getClass().getSimpleName());
        }
    }

    private void send(List<DdcAiUsageReportDTO> batch) {
        for (int attempt = 1; ; attempt++) {
            try {
                DdcAiUsageReportReceiptDTO receipt = client.report(
                        DdcAiUsageReportBatchDTO.builder().reports(List.copyOf(batch)).build());
                for (DdcAiUsageReportReceiptDTO.ItemDTO item : receipt.getItems()) {
                    metrics.increment(REPORTED, 1, "status", item.getStatus().name());
                }
                return;
            } catch (DdcClientTransportException transport) {
                if (!transport.retryable() || attempt >= properties.getMaxAttempts()) {
                    log.warn("AI usage report batch dropped after {} attempt(s): size={}, retryable={}", attempt,
                            batch.size(), transport.retryable());
                    drop("TRANSPORT", batch.size());
                    return;
                }
                if (!backoff(attempt)) {
                    drop("TRANSPORT", batch.size());
                    return;
                }
            } catch (RuntimeException other) {
                log.warn("AI usage report batch dropped: size={}, failure={}", batch.size(),
                        other.getClass().getSimpleName());
                drop("ERROR", batch.size());
                return;
            }
        }
    }

    private boolean backoff(int attempt) {
        try {
            sleeper.sleep(BASE_BACKOFF.multipliedBy(1L << (attempt - 1)));
            return true;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void drop(String reason, long count) {
        metrics.increment(DROPPED, count, "reason", reason);
    }

    /** 退避等待端口，测试可替换。 / Backoff wait port, replaceable in tests. */
    @FunctionalInterface
    public interface Sleeper {

        Sleeper THREAD = duration -> Thread.sleep(duration.toMillis());

        void sleep(Duration duration) throws InterruptedException;
    }
}
