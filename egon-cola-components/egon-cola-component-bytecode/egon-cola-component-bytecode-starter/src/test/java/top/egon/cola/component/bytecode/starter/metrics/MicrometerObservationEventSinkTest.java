package top.egon.cola.component.bytecode.starter.metrics;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.bytecode.api.observation.ObservationEvent;
import top.egon.cola.component.bytecode.api.observation.ObservationResult;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MicrometerObservationEventSinkTest {

    @Test
    void publishesDurationErrorsAndSlowCountsWithBoundedTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerObservationEventSink sink = new MicrometerObservationEventSink(registry);

        sink.publish(event(ObservationResult.SUCCESS, "NONE", 5_000L, 10_000L));
        sink.publish(event(
                ObservationResult.ERROR, "IllegalStateException", 20_000L, 10_000L));

        assertEquals(2L, registry.get("egon.bytecode.method.duration").timer().count());
        assertEquals(1.0, registry.get("egon.bytecode.method.errors").counter().count());
        assertEquals(1.0, registry.get("egon.bytecode.method.slow").counter().count());
        Set<String> tagKeys = registry.getMeters().stream()
                .flatMap(meter -> meter.getId().getTags().stream())
                .map(io.micrometer.core.instrument.Tag::getKey)
                .collect(Collectors.toSet());
        assertFalse(tagKeys.contains("trace_id"));
        assertFalse(tagKeys.contains("request_id"));
        assertFalse(tagKeys.contains("thread"));
        assertFalse(tagKeys.contains("method_descriptor"));
    }

    @Test
    void rejectsDynamicOrUnboundedStaticTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerObservationEventSink sink = new MicrometerObservationEventSink(registry);
        ObservationEvent invalid = new ObservationEvent(
                1L, "sample.Service", "work", "()V", "APPLICATION",
                10L, ObservationResult.SUCCESS, "NONE", "secret-trace", false,
                Map.of("dynamic", "${secret}"), 100L);

        assertThrows(IllegalArgumentException.class, () -> sink.publish(invalid));
        assertEquals(0, registry.getMeters().size());
    }

    private ObservationEvent event(
            ObservationResult result,
            String exceptionGroup,
            long duration,
            long slowThreshold
    ) {
        return new ObservationEvent(
                1L, "sample.application.Service", "work", "(Ljava/lang/String;)V",
                "APPLICATION", duration, result, exceptionGroup, "secret-trace", false,
                Map.of("channel", "test"), slowThreshold);
    }
}
