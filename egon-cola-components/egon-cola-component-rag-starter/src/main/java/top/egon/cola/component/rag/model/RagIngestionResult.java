package top.egon.cola.component.rag.model;

import top.egon.cola.component.rag.exception.RagValidationException;

import java.time.Duration;

/**
 * Outcome of one ingestion call.
 *
 * <p>The identity fields are echoed so a caller can log or persist them without re-deriving them
 * from its own request.
 */
public record RagIngestionResult(String collectionId,
                                 String documentId,
                                 String logicalModelName,
                                 int dimensions,
                                 int chunkCount,
                                 Duration elapsed) {

    public RagIngestionResult {
        if (chunkCount < 0) {
            throw new RagValidationException("chunkCount must not be negative");
        }
        if (elapsed == null || elapsed.isNegative()) {
            throw new RagValidationException("elapsed must not be negative");
        }
        if (dimensions <= 0) {
            throw new RagValidationException("dimensions must be positive");
        }
    }
}
