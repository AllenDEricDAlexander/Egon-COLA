package top.egon.cola.component.bytecode.starter.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.bytecode.api.executor.ExecutorEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MicrometerExecutorEventSinkTest {

    @Test
    void recordsFiveMetricNamesWithBoundedTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerExecutorEventSink sink = new MicrometerExecutorEventSink(registry, 1.0);
        String unboundedExecutor = "pool@" + "a".repeat(300);

        sink.publish(event("SUBMITTED", "PENDING", unboundedExecutor, 10, 0, 0));
        sink.publish(event("STARTED", "RUNNING", unboundedExecutor, 10, 20, 0));
        sink.publish(event("FAILED", "ERROR", unboundedExecutor, 10, 20, 30));

        assertEquals(MicrometerExecutorEventSink.METRIC_NAMES,
                registry.getMeters().stream()
                        .map(meter -> meter.getId().getName())
                        .collect(java.util.stream.Collectors.toSet()));
        registry.getMeters().forEach(meter -> {
            assertEquals(5, meter.getId().getTags().size());
            meter.getId().getTags().forEach(tag -> assertFalse(tag.getValue().length() > 128));
        });
    }

    private ExecutorEvent event(
            String phase,
            String result,
            String executor,
            long submitted,
            long started,
            long completed
    ) {
        return new ExecutorEvent(
                1,
                executor,
                "java.util.concurrent.ThreadPoolExecutor",
                phase,
                result,
                "IllegalStateException",
                false,
                submitted,
                started,
                completed
        );
    }
}
