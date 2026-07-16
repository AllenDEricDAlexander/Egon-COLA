package top.egon.cola.component.bytecode.starter.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import top.egon.cola.component.bytecode.api.observation.ObservationEvent;
import top.egon.cola.component.bytecode.api.observation.ObservationEventSink;
import top.egon.cola.component.bytecode.api.observation.ObservationResult;
import top.egon.cola.component.bytecode.starter.observation.ObservationMetadataValidator;

import java.time.Duration;
import java.util.Map;

public final class MicrometerObservationEventSink implements ObservationEventSink {

    private static final String DURATION = "egon.bytecode.method.duration";
    private static final String ERRORS = "egon.bytecode.method.errors";
    private static final String SLOW = "egon.bytecode.method.slow";

    private final MeterRegistry registry;
    private final ObservationMetadataValidator metadataValidator;

    public MicrometerObservationEventSink(MeterRegistry registry) {
        this(registry, new ObservationMetadataValidator());
    }

    public MicrometerObservationEventSink(
            MeterRegistry registry,
            ObservationMetadataValidator metadataValidator
    ) {
        this.registry = registry;
        this.metadataValidator = metadataValidator;
    }

    @Override
    public void publish(ObservationEvent event) {
        metadataValidator.validate(event.staticTags());
        Tags tags = tags(event);
        Timer.builder(DURATION)
                .tags(tags)
                .register(registry)
                .record(Duration.ofNanos(event.durationNanos()));
        if (event.result() == ObservationResult.ERROR) {
            Counter.builder(ERRORS)
                    .tags(tags.and("exception_group", bounded(
                            event.exceptionGroup(), 64)))
                    .register(registry)
                    .increment();
        }
        if (event.slowThresholdNanos() >= 0L
                && event.durationNanos() >= event.slowThresholdNanos()) {
            Counter.builder(SLOW).tags(tags).register(registry).increment();
        }
    }

    private Tags tags(ObservationEvent event) {
        Tags tags = Tags.of(
                "class", bounded(event.className(), 160),
                "method", bounded(event.methodName(), 80),
                "layer", bounded(event.layer(), 64),
                "virtual_thread", Boolean.toString(event.virtualThread())
        );
        for (Map.Entry<String, String> entry : event.staticTags().entrySet()) {
            tags = tags.and(entry.getKey(), bounded(entry.getValue(), 128));
        }
        return tags;
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
