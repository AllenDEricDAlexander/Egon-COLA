package top.egon.cola.component.rag.execution;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import top.egon.cola.component.rag.api.RagIngestionService;
import top.egon.cola.component.rag.autoconfigure.RagMetricsRecorder;
import top.egon.cola.component.rag.chunk.RagChunkIdFactory;
import top.egon.cola.component.rag.chunk.RagChunkingStrategy;
import top.egon.cola.component.rag.chunk.RagChunkingStrategyFactory;
import top.egon.cola.component.rag.converter.RagChunkConverter;
import top.egon.cola.component.rag.exception.RagVectorStoreException;
import top.egon.cola.component.rag.embed.RagEmbeddingModelDescriptorBO;
import top.egon.cola.component.rag.embed.RagEmbeddingModelRegistry;
import top.egon.cola.component.rag.metadata.RagMetadataKeys;
import top.egon.cola.component.rag.model.RagChunkBO;
import top.egon.cola.component.rag.model.RagIngestionCommand;
import top.egon.cola.component.rag.model.RagIngestionResult;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Chunks, embeds and writes one document, after removing whatever that document had before.
 *
 * <p>The delete-then-write order combined with deterministic chunk ids is what makes a re-run
 * idempotent: the vector store cannot be enumerated, so a rebuild replaces the document's chunks
 * rather than appending to them.
 *
 * <p>Embedding happens inside the host's vector store because the vector store SPI accepts
 * documents, not pre-computed vectors, so the component never holds an embedding call of its own.
 */
@Slf4j
@RequiredArgsConstructor
public class RagIngestionServiceImpl implements RagIngestionService {

    private final RagEmbeddingModelRegistry modelRegistry;

    private final RagChunkingStrategyFactory chunkingStrategyFactory;

    private final RagChunkIdFactory chunkIdFactory;

    private final VectorStore vectorStore;

    private final Clock clock;

    private final RagMetricsRecorder metricsRecorder;

    @Override
    public RagIngestionResult ingest(RagIngestionCommand command) {
        Instant startedAt = clock.instant();
        RagEmbeddingModelDescriptorBO descriptor = modelRegistry.resolve(command.logicalModelName());
        RagChunkingStrategy strategy = chunkingStrategyFactory.resolve(command.chunkingConfig().strategy());
        List<RagChunkBO> chunks = strategy.split(command.document(), command.chunkingConfig());
        Map<String, String> attributes = command.mergedAttributes();

        List<Document> documents = new ArrayList<>(chunks.size());
        for (RagChunkBO chunk : chunks) {
            String chunkId = chunkIdFactory.create(command.documentId(), chunk.chunkIndex());
            Map<String, Object> metadata = RagMetadataKeys.toDocumentMetadata(command.collectionId(),
                    command.documentId(), descriptor.logicalName(), chunk.chunkIndex(), attributes);
            documents.add(RagChunkConverter.INSTANCE.toDocument(chunk, chunkId, metadata));
        }

        log.info("rag ingestion started: collection={}, document={}, model={}, strategy={}, chunks={}",
                command.collectionId(), command.documentId(), descriptor.logicalName(),
                command.chunkingConfig().strategy(), chunks.size());

        deleteExistingChunks(command.documentId());
        writeChunks(documents, command.documentId());

        Duration elapsed = Duration.between(startedAt, clock.instant());
        log.info("rag ingestion finished: collection={}, document={}, chunks={}, durationMs={}, result=SUCCESS",
                command.collectionId(), command.documentId(), chunks.size(), elapsed.toMillis());
        metricsRecorder.recordIngestion(descriptor.logicalName(), command.chunkingConfig().strategy().name(),
                "SUCCESS", elapsed, chunks.size());
        return new RagIngestionResult(command.collectionId(), command.documentId(), descriptor.logicalName(),
                descriptor.dimensions(), chunks.size(), elapsed);
    }

    /**
     * Deleting before writing is what makes a re-run replace rather than append. A failure here
     * aborts before anything is written, so the document keeps its previous chunks.
     */
    private void deleteExistingChunks(String documentId) {
        try {
            vectorStore.delete(new FilterExpressionBuilder()
                    .eq(RagMetadataKeys.DOCUMENT_ID, documentId)
                    .build());
        } catch (RuntimeException exception) {
            throw new RagVectorStoreException(
                    "failed to delete previous chunks for document " + documentId, exception);
        }
    }

    private void writeChunks(List<Document> documents, String documentId) {
        try {
            vectorStore.add(documents);
        } catch (RuntimeException exception) {
            throw new RagVectorStoreException("failed to write chunks for document " + documentId, exception);
        }
    }
}
