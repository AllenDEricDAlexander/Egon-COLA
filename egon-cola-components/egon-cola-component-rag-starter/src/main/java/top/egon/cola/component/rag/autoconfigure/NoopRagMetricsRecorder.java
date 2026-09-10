package top.egon.cola.component.rag.autoconfigure;

import java.time.Duration;

/** Used when Micrometer is not on the class path; collecting metrics is simply skipped. */
public class NoopRagMetricsRecorder implements RagMetricsRecorder {

    @Override
    public boolean enabled() {
        return false;
    }

    @Override
    public void recordIngestion(String logicalModelName, String strategyName, String result, Duration elapsed,
                                int chunkCount) {
        // deliberately empty
    }

    @Override
    public void recordRetrieval(String logicalModelName, String result, Duration elapsed, int hitCount) {
        // deliberately empty
    }
}
