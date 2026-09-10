package top.egon.cola.component.rag.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;

/** Records the component's counters, timers and summaries into the host's Micrometer registry. */
public class MicrometerRagMetricsRecorder implements RagMetricsRecorder {

    private static final String INGEST = "rag.ingest";

    private static final String RETRIEVE = "rag.retrieve";

    private final MeterRegistry registry;

    public MicrometerRagMetricsRecorder(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public void recordIngestion(String logicalModelName, String strategyName, String result, Duration elapsed,
                                int chunkCount) {
        registry.counter(INGEST, "model", logicalModelName, "strategy", strategyName, "result", result).increment();
        Timer.builder(INGEST + ".duration")
                .tag("model", logicalModelName)
                .tag("result", result)
                .register(registry)
                .record(elapsed);
        registry.summary(INGEST + ".chunks", "model", logicalModelName).record(chunkCount);
    }

    @Override
    public void recordRetrieval(String logicalModelName, String result, Duration elapsed, int hitCount) {
        registry.counter(RETRIEVE, "model", logicalModelName, "result", result).increment();
        Timer.builder(RETRIEVE + ".duration")
                .tag("model", logicalModelName)
                .tag("result", result)
                .register(registry)
                .record(elapsed);
        registry.summary(RETRIEVE + ".hits", "model", logicalModelName).record(hitCount);
    }
}
