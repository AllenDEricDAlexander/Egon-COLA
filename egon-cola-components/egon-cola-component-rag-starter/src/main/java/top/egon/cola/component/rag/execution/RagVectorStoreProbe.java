package top.egon.cola.component.rag.execution;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import top.egon.cola.component.rag.embed.RagEmbeddingModelRegistry;
import top.egon.cola.component.rag.exception.RagConfigurationException;
import top.egon.cola.component.rag.metadata.RagMetadataKeys;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Optional start-up round trip that writes, searches and deletes one probe vector per model.
 *
 * <p>Off by default. Enabling it makes a real embedding call and a real vector write part of
 * application start-up, which is why it is opt-in: it proves the table exists, its dimension matches
 * the configured model, the extension is installed, and filtering and deletion work — none of which
 * the offline dimension check can detect.
 *
 * <p>The probe writes into a reserved collection that is not a valid business collection id, so it
 * can never be confused with real data, and removes its record before returning.
 */
@Slf4j
@RequiredArgsConstructor
public class RagVectorStoreProbe {

    public static final String PROBE_COLLECTION = "__egon_rag_probe__";

    private final VectorStore vectorStore;

    private final Clock clock;

    /** @throws RagConfigurationException when any stage fails; the message names the model and stage */
    public void validate(RagEmbeddingModelRegistry registry) {
        for (String logicalName : registry.logicalNames()) {
            String stage = "add";
            String probeId = PROBE_COLLECTION + ":" + logicalName;
            Instant startedAt = clock.instant();
            try {
                vectorStore.add(List.of(new Document(probeId, PROBE_COLLECTION,
                        RagMetadataKeys.toDocumentMetadata(PROBE_COLLECTION, PROBE_COLLECTION, logicalName, 0,
                                Map.of()))));
                stage = "search";
                vectorStore.similaritySearch(SearchRequest.builder()
                        .query(PROBE_COLLECTION)
                        .topK(1)
                        .filterExpression(new FilterExpressionBuilder()
                                .and(new FilterExpressionBuilder().eq(RagMetadataKeys.COLLECTION_ID, PROBE_COLLECTION),
                                        new FilterExpressionBuilder().eq(RagMetadataKeys.LOGICAL_MODEL_NAME,
                                                logicalName))
                                .build())
                        .build());
                log.info("rag vector store probe finished: model={}, result=SUCCESS, durationMs={}",
                        logicalName, Duration.between(startedAt, clock.instant()).toMillis());
            } catch (RuntimeException exception) {
                throw new RagConfigurationException("rag vector store probe failed for model '" + logicalName
                        + "' at stage " + stage, exception);
            } finally {
                deleteProbeRecord(logicalName, probeId);
            }
        }
    }

    /** Best-effort cleanup: a failure here must not mask the probe outcome. */
    private void deleteProbeRecord(String logicalName, String probeId) {
        try {
            vectorStore.delete(List.of(probeId));
        } catch (RuntimeException exception) {
            log.warn("rag vector store probe left its record behind for model {}", logicalName);
        }
    }
}
