package top.egon.cola.component.outbox.dispatch;

import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.TaskScheduler;
import top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxProperties;
import top.egon.cola.component.outbox.cleanup.OutboxCleanupJob;

import java.util.concurrent.ScheduledFuture;

public class OutboxPoller implements SmartLifecycle {

    private final OutboxDispatcher dispatcher;
    private final OutboxCleanupJob cleanupJob;
    private final TaskScheduler taskScheduler;
    private final TransactionalOutboxProperties properties;
    private volatile boolean running;
    private ScheduledFuture<?> pollingFuture;
    private ScheduledFuture<?> cleanupFuture;

    public OutboxPoller(
            OutboxDispatcher dispatcher,
            OutboxCleanupJob cleanupJob,
            TaskScheduler taskScheduler,
            TransactionalOutboxProperties properties
    ) {
        this.dispatcher = dispatcher;
        this.cleanupJob = cleanupJob;
        this.taskScheduler = taskScheduler;
        this.properties = properties;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        if (properties.getPolling().isEnabled()) {
            pollingFuture = taskScheduler.scheduleWithFixedDelay(
                    this::submitDueWhenRunning,
                    properties.getPolling().getFixedDelay()
            );
        }
        if (properties.getCleanup().isEnabled()) {
            cleanupFuture = taskScheduler.scheduleWithFixedDelay(
                    this::cleanupWhenRunning,
                    properties.getCleanup().getFixedDelay()
            );
        }
    }

    @Override
    public synchronized void stop() {
        running = false;
        cancel(pollingFuture);
        cancel(cleanupFuture);
        pollingFuture = null;
        cleanupFuture = null;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void submitDueWhenRunning() {
        if (running) {
            dispatcher.submitDue();
        }
    }

    private void cleanupWhenRunning() {
        if (running) {
            cleanupJob.runOnce();
        }
    }

    private void cancel(ScheduledFuture<?> future) {
        if (future != null) {
            future.cancel(false);
        }
    }
}
