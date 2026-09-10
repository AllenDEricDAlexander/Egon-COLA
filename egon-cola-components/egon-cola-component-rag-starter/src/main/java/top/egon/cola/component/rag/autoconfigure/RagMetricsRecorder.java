package top.egon.cola.component.rag.autoconfigure;

import java.time.Duration;

/**
 * Metrics seam for ingestion and retrieval.
 *
 * <p>An interface rather than a single class because Micrometer is an optional dependency: the
 * Micrometer implementation cannot be loaded when it is absent, so a host without it gets the no-op
 * implementation instead of a start-up failure.
 *
 * <p>Tags are limited to low cardinality values — outcome, model and strategy — because collection
 * and document identifiers would create an unbounded number of time series.
 */
public interface RagMetricsRecorder {

    /** Whether metrics are actually being collected. */
    boolean enabled();

    void recordIngestion(String logicalModelName, String strategyName, String result, Duration elapsed,
                         int chunkCount);

    void recordRetrieval(String logicalModelName, String result, Duration elapsed, int hitCount);
}
