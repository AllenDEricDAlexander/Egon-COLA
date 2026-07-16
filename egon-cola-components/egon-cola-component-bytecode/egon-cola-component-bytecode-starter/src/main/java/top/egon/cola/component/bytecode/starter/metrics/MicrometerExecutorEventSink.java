package top.egon.cola.component.bytecode.starter.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import top.egon.cola.component.bytecode.api.executor.ExecutorEvent;
import top.egon.cola.component.bytecode.api.executor.ExecutorEventSink;

import java.time.Duration;
import java.util.Set;

public final class MicrometerExecutorEventSink implements ExecutorEventSink {

    private static final String SUBMITTED = "egon.cola.bytecode.executor.tasks.submitted";
    private static final String STARTED = "egon.cola.bytecode.executor.tasks.started";
    private static final String FINISHED = "egon.cola.bytecode.executor.tasks.finished";
    private static final String QUEUE_WAIT = "egon.cola.bytecode.executor.queue.wait";
    private static final String EXECUTION = "egon.cola.bytecode.executor.execution";

    public static final Set<String> METRIC_NAMES = Set.of(
            SUBMITTED, STARTED, FINISHED, QUEUE_WAIT, EXECUTION);

    private final MeterRegistry registry;
    private final double samplingRate;

    public MicrometerExecutorEventSink(MeterRegistry registry, double samplingRate) {
        this.registry = registry;
        this.samplingRate = samplingRate;
    }

    @Override
    public void publish(ExecutorEvent event) {
        if (!sampled(event)) {
            return;
        }
        Tags tags = tags(event);
        switch (event.phase()) {
            case "SUBMITTED" -> counter(SUBMITTED, tags);
            case "STARTED" -> {
                counter(STARTED, tags);
                record(QUEUE_WAIT, event.startedNanos() - event.submittedNanos(), tags);
            }
            case "COMPLETED", "FAILED" -> {
                counter(FINISHED, tags);
                record(EXECUTION, event.completedNanos() - event.startedNanos(), tags);
            }
            case "REJECTED" -> counter(FINISHED, tags);
            default -> {
                // Unknown phases are ignored to keep metrics bounded across protocol minors.
            }
        }
    }

    private void counter(String name, Tags tags) {
        Counter.builder(name).tags(tags).register(registry).increment();
    }

    private void record(String name, long nanos, Tags tags) {
        if (nanos >= 0) {
            Timer.builder(name).tags(tags).register(registry).record(Duration.ofNanos(nanos));
        }
    }

    private Tags tags(ExecutorEvent event) {
        String executor = event.executorName();
        if (executor == null || executor.isBlank() || executor.contains("@")) {
            executor = "unmanaged";
        }
        return Tags.of(
                "executor", bounded(executor, 64),
                "executor_type", bounded(event.executorType(), 128),
                "result", bounded(event.result(), 32),
                "exception_group", bounded(event.exceptionGroup(), 64),
                "virtual_thread", Boolean.toString(event.virtualThread())
        );
    }

    private boolean sampled(ExecutorEvent event) {
        if (samplingRate >= 1.0) {
            return true;
        }
        if (samplingRate <= 0.0) {
            return false;
        }
        long hash = event.callSiteId() * 31 + event.submittedNanos();
        double bucket = Long.remainderUnsigned(hash, 10_000) / 10_000.0;
        return bucket < samplingRate;
    }

    private String bounded(String value, int maximumLength) {
        if (value == null || value.isBlank()) {
            return "none";
        }
        String sanitized = value.replaceAll("[^a-zA-Z0-9_.:$-]", "_");
        return sanitized.length() <= maximumLength
                ? sanitized : sanitized.substring(0, maximumLength);
    }
}
