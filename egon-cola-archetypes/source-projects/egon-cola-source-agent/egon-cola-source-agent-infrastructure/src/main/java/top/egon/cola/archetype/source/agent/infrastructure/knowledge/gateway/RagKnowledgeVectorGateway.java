package top.egon.cola.archetype.source.agent.infrastructure.knowledge.gateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import top.egon.cola.archetype.source.agent.domain.knowledge.gateway.KnowledgeVectorGateway;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeChunkBO;
import top.egon.cola.archetype.source.agent.infrastructure.knowledge.metadata.KnowledgeVectorMetadata;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.rag.api.RagRetrievalService;
import top.egon.cola.component.rag.exception.RagVectorStoreException;
import top.egon.cola.component.rag.metadata.RagMetadataKeys;
import top.egon.cola.component.rag.model.RagRetrievalQuery;
import top.egon.cola.component.rag.model.RagRetrievedChunkBO;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Adapter from the knowledge vector port to the RAG component's retrieval service.
 *
 * <p>The tenant is taken from the running thread, never from the caller: whatever attributes arrive
 * are merged and then the tenant key is overwritten, so a call can neither omit the tenant scope nor
 * ask for another one. The component folds this condition together with the collection and logical
 * model it always applies, and it is that request the vector store filters on.
 *
 * <p>Failures are not translated here: the component's exceptions are the vocabulary of the
 * dependency, and the layer that owns the use case decides what a failed retrieval means for it.
 */
@Slf4j
@Service("knowledgeVectorGateway")
@RequiredArgsConstructor
public class RagKnowledgeVectorGateway implements KnowledgeVectorGateway {

    private final @Qualifier("ragRetrievalService") RagRetrievalService ragRetrievalService;

    /** The store the component resolves by name, shared with the ingestion path it writes through. */
    private final @Qualifier("knowledgeRagVectorStore") VectorStore vectorStore;

    private final EgonColaTenantIdProvider tenantIdProvider;

    @Override
    public List<KnowledgeChunkBO> retrieve(String collectionId, String logicalModelName, String query,
                                           int topK, Map<String, String> attributes) {
        Objects.requireNonNull(collectionId, "collectionId must not be null");
        Objects.requireNonNull(logicalModelName, "logicalModelName must not be null");
        Objects.requireNonNull(query, "query must not be null");

        Map<String, String> scopedAttributes = new LinkedHashMap<>();
        if (attributes != null) {
            scopedAttributes.putAll(attributes);
        }
        scopedAttributes.put(KnowledgeVectorMetadata.TENANT_ID, String.valueOf(tenantIdProvider.currentTenantId()));

        List<RagRetrievedChunkBO> retrieved = ragRetrievalService.retrieve(new RagRetrievalQuery(
                collectionId, logicalModelName, query, topK, 0.0, scopedAttributes));
        log.debug("retrieved {} chunk(s) of collection {} for the current tenant", retrieved.size(), collectionId);
        return retrieved.stream().map(RagKnowledgeVectorGateway::toChunkBO).toList();
    }

    @Override
    public void deleteDocument(String collectionId, String documentId) {
        Objects.requireNonNull(collectionId, "collectionId must not be null");
        Objects.requireNonNull(documentId, "documentId must not be null");
        try {
            // The component exposes no delete API, so this deletes the way its own re-ingest does:
            // by the reserved document key every chunk carries. The collection is matched as well, so
            // one base's identifier can never reach another base's chunks.
            FilterExpressionBuilder filters = new FilterExpressionBuilder();
            vectorStore.delete(filters.and(
                    filters.eq(RagMetadataKeys.DOCUMENT_ID, documentId),
                    filters.eq(RagMetadataKeys.COLLECTION_ID, collectionId)).build());
        } catch (RuntimeException failure) {
            throw new RagVectorStoreException("failed to delete the chunks of document " + documentId, failure);
        }
        log.info("knowledge chunks deleted knowledgeBaseId={} documentId={} outcome=DELETED",
                collectionId, documentId);
    }

    /** Maps one retrieved chunk into the domain vocabulary, refusing a chunk without a document id. */
    private static KnowledgeChunkBO toChunkBO(RagRetrievedChunkBO chunk) {
        return new KnowledgeChunkBO(documentId(chunk), chunk.chunkIndex(), chunk.content(), chunk.score());
    }

    private static Long documentId(RagRetrievedChunkBO chunk) {
        try {
            return Long.valueOf(chunk.documentId());
        } catch (NumberFormatException | NullPointerException failure) {
            throw new IllegalStateException("a retrieved chunk carries no numeric document id", failure);
        }
    }
}
