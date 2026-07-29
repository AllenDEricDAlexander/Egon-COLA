package top.egon.cola.component.accessguard.store.local;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LocalStateCleaner implements AutoCloseable {

    private final List<Runnable> cleanupActions;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean closed = new AtomicBoolean();

    public LocalStateCleaner(String threadName, Duration interval, List<Runnable> cleanupActions) {
        if (threadName == null || threadName.isBlank()) {
            throw new IllegalArgumentException("threadName must not be blank");
        }
        if (interval == null || interval.isZero() || interval.isNegative()) {
            throw new IllegalArgumentException("interval must be positive");
        }
        this.cleanupActions = List.copyOf(cleanupActions);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, threadName);
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(
                this::cleanSafely,
                interval.toNanos(),
                interval.toNanos(),
                TimeUnit.NANOSECONDS);
    }

    public void cleanNow() {
        if (closed.get()) {
            throw new IllegalStateException("LocalStateCleaner is closed");
        }
        cleanupActions.forEach(Runnable::run);
    }

    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        scheduler.shutdownNow();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("LocalStateCleaner did not terminate");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while closing LocalStateCleaner", exception);
        }
    }

    private void cleanSafely() {
        try {
            cleanNow();
        } catch (RuntimeException ignored) {
            // Cleanup failure is surfaced later through observability without terminating the scheduler.
        }
    }
}
