package top.egon.cola.component.gateway.engine.mcp;

import org.reactivestreams.Publisher;
import org.springframework.context.SmartLifecycle;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.mcp.task.McpTaskExecutor;
import top.egon.cola.component.gateway.mcp.task.McpTaskService;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Polls the shared task store; lease ownership keeps nodes mutually exclusive.
 */
public final class McpTaskWorker implements SmartLifecycle {

    private final McpTaskService tasks;

    private final McpTaskExecutor executor;

    private final String workerOwner;

    private final Duration leaseDuration;

    private final Duration pollInterval;

    private final AtomicBoolean running = new AtomicBoolean();

    private ScheduledExecutorService scheduler;

    public McpTaskWorker(
            McpTaskService tasks,
            McpTaskExecutor executor,
            String workerOwner,
            Duration leaseDuration,
            Duration pollInterval) {
        this.tasks = Objects.requireNonNull(tasks, "tasks");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.workerOwner = required(workerOwner, "workerOwner");
        this.leaseDuration = positive(leaseDuration, "leaseDuration");
        this.pollInterval = positive(pollInterval, "pollInterval");
    }

    public Publisher<Void> runOnce() {
        return tasks.executeNext(workerOwner, executor);
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "gateway-mcp-task-worker");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(
                this::pollSafely,
                0L,
                pollInterval.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void stop() {
        running.set(false);
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    private void pollSafely() {
        if (!running.get()) {
            return;
        }
        Mono.from(runOnce())
                .then(Mono.from(tasks.cleanup()))
                .onErrorComplete()
                .block();
    }

    private Duration positive(Duration value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
