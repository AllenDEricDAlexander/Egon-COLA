package top.egon.cola.component.rag.execution;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import top.egon.cola.component.rag.api.RagRetrievalService;
import top.egon.cola.component.rag.autoconfigure.RagProperties;
import top.egon.cola.component.rag.converter.RagChunkConverter;
import top.egon.cola.component.rag.embed.RagEmbeddingModelDescriptorBO;
import top.egon.cola.component.rag.embed.RagEmbeddingModelRegistry;
import top.egon.cola.component.rag.exception.RagValidationException;
import top.egon.cola.component.rag.exception.RagVectorStoreException;
import top.egon.cola.component.rag.metadata.RagMetadataKeys;
import top.egon.cola.component.rag.model.RagRetrievalQuery;
import top.egon.cola.component.rag.model.RagRetrievedChunkBO;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Builds the search request and maps what comes back.
 *
 * <p>A Collection and model filter is always present because the component builds it, not the
 * caller; business attributes are folded on top. The component logs the collection, model, topK,
 * hit count and duration, and never the query text or the retrieved content.
 */
@Slf4j
@RequiredArgsConstructor
public class RagRetrievalServiceImpl implements RagRetrievalService {

    private final RagEmbeddingModelRegistry modelRegistry;

    private final VectorStore vectorStore;

    private final RagProperties ragProperties;

    private final Clock clock;

    @Override
    public List<RagRetrievedChunkBO> retrieve(RagRetrievalQuery query) {
        Instant startedAt = clock.instant();
        RagEmbeddingModelDescriptorBO descriptor = modelRegistry.resolve(query.logicalModelName());
        int topK = query.topK() == 0 ? ragProperties.retrieval().defaultTopK() : query.topK();
        int maxTopK = ragProperties.retrieval().maxTopK();
        if (topK > maxTopK) {
            throw new RagValidationException("topK must not exceed " + maxTopK);
        }

        SearchRequest request = SearchRequest.builder()
                .query(query.query())
                .topK(topK)
                .similarityThreshold(query.similarityThreshold())
                .filterExpression(forcedFilter(query, descriptor.logicalName()))
                .build();

        List<Document> documents;
        try {
            documents = vectorStore.similaritySearch(request);
        } catch (RuntimeException exception) {
            throw new RagVectorStoreException("failed to search the vector store", exception);
        }

        List<RagRetrievedChunkBO> results = documents.stream()
                .map(RagChunkConverter.INSTANCE::toSource)
                .sorted(Comparator.comparing(RagRetrievedChunkBO::score,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        log.info("rag retrieval finished: collection={}, model={}, topK={}, hits={}, durationMs={}, result=SUCCESS",
                query.collectionId(), descriptor.logicalName(), topK, results.size(),
                Duration.between(startedAt, clock.instant()).toMillis());
        return results;
    }

    private static Filter.Expression forcedFilter(RagRetrievalQuery query, String logicalModelName) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op filter = builder.and(
                builder.eq(RagMetadataKeys.COLLECTION_ID, query.collectionId()),
                builder.eq(RagMetadataKeys.LOGICAL_MODEL_NAME, logicalModelName));
        for (var entry : query.attributes().entrySet()) {
            filter = builder.and(filter, builder.eq(entry.getKey(), entry.getValue()));
        }
        return filter.build();
    }
}
