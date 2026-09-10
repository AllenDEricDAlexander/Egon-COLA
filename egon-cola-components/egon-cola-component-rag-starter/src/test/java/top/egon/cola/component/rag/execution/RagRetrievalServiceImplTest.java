package top.egon.cola.component.rag.execution;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import top.egon.cola.component.rag.autoconfigure.RagEmbeddingModelProperties;
import top.egon.cola.component.rag.autoconfigure.NoopRagMetricsRecorder;
import top.egon.cola.component.rag.autoconfigure.RagProperties;
import top.egon.cola.component.rag.autoconfigure.RagRetrievalProperties;
import top.egon.cola.component.rag.embed.RagEmbeddingModelRegistry;
import top.egon.cola.component.rag.exception.RagModelNotRegisteredException;
import top.egon.cola.component.rag.exception.RagValidationException;
import top.egon.cola.component.rag.exception.RagVectorStoreException;
import top.egon.cola.component.rag.metadata.RagMetadataKeys;
import top.egon.cola.component.rag.model.RagRetrievalQuery;
import top.egon.cola.component.rag.model.RagRetrievedChunkBO;
import top.egon.cola.component.rag.support.FakeEmbeddingModel;
import top.egon.cola.component.rag.support.FakeVectorStore;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Locks the forced filters, the ordering and the failure semantics of retrieval. */
class RagRetrievalServiceImplTest {

    private static final int DIMENSIONS = 1536;

    private FakeVectorStore vectorStore;

    private RagRetrievalServiceImpl service;

    @BeforeEach
    void setUp() {
        vectorStore = new FakeVectorStore();
        service = serviceWith(new RagRetrievalProperties(8, 50));
    }

    @Test
    void forces_the_collection_and_the_logical_model_filters() {
        service.retrieve(query("kb-1", "openai-small", "how to configure timeout", 8, Map.of()));

        SearchRequest request = vectorStore.searchRequests().get(0);
        assertThat(request.getFilterExpression()).isNotNull();
        assertThat(request.getFilterExpression().toString())
                .contains(RagMetadataKeys.COLLECTION_ID)
                .contains("kb-1")
                .contains(RagMetadataKeys.LOGICAL_MODEL_NAME)
                .contains("openai-small");
    }

    @Test
    void returns_only_chunks_of_the_requested_collection_and_model() {
        seed("kb-1", "openai-small", "doc-1", 0);
        seed("kb-2", "openai-small", "doc-2", 0);
        seed("kb-1", "other-model", "doc-3", 0);

        List<RagRetrievedChunkBO> results = service.retrieve(
                query("kb-1", "openai-small", "anything", 8, Map.of()));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).documentId()).isEqualTo("doc-1");
    }

    @Test
    void applies_business_attribute_filters_on_top_of_the_forced_ones() {
        seed("kb-1", "openai-small", "doc-1", 0, Map.of("source", "manual"));
        seed("kb-1", "openai-small", "doc-2", 0, Map.of("source", "import"));

        List<RagRetrievedChunkBO> results = service.retrieve(
                query("kb-1", "openai-small", "anything", 8, Map.of("source", "manual")));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).documentId()).isEqualTo("doc-1");
    }

    @Test
    void returns_an_empty_list_when_nothing_matches() {
        assertThat(service.retrieve(query("kb-1", "openai-small", "anything", 8, Map.of()))).isEmpty();
    }

    @Test
    void rejects_a_top_k_above_the_configured_maximum() {
        assertThatThrownBy(() -> service.retrieve(query("kb-1", "openai-small", "q", 999, Map.of())))
                .isInstanceOf(RagValidationException.class)
                .hasMessageContaining("50");
    }

    @Test
    void falls_back_to_the_configured_default_top_k() {
        service.retrieve(query("kb-1", "openai-small", "q", 0, Map.of()));

        assertThat(vectorStore.searchRequests().get(0).getTopK()).isEqualTo(8);
    }

    @Test
    void fails_when_the_logical_model_is_not_registered() {
        assertThatThrownBy(() -> service.retrieve(query("kb-1", "missing", "q", 8, Map.of())))
                .isInstanceOf(RagModelNotRegisteredException.class)
                .hasMessageContaining("openai-small");
    }

    @Test
    void propagates_a_dependency_failure_instead_of_degrading_to_an_empty_result() {
        vectorStore.failOn(FakeVectorStore.FailurePoint.SEARCH);

        assertThatThrownBy(() -> service.retrieve(query("kb-1", "openai-small", "q", 8, Map.of())))
                .isInstanceOf(RagVectorStoreException.class);
    }

    private RagRetrievalServiceImpl serviceWith(RagRetrievalProperties retrieval) {
        RagProperties properties = new RagProperties(true, DIMENSIONS, "hostVectorStore",
                Map.of("openai-small", new RagEmbeddingModelProperties("ragEmbeddingModel")), "openai-small",
                null, retrieval, null);
        RagEmbeddingModelRegistry registry = new RagEmbeddingModelRegistry(properties,
                new StaticListableBeanFactory(Map.of("ragEmbeddingModel", new FakeEmbeddingModel(DIMENSIONS))));
        return new RagRetrievalServiceImpl(registry, vectorStore, properties, Clock.systemUTC(),
                new NoopRagMetricsRecorder());
    }

    private void seed(String collectionId, String model, String documentId, int chunkIndex) {
        seed(collectionId, model, documentId, chunkIndex, Map.of());
    }

    private void seed(String collectionId, String model, String documentId, int chunkIndex,
                      Map<String, String> attributes) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(RagMetadataKeys.COLLECTION_ID, collectionId);
        metadata.put(RagMetadataKeys.DOCUMENT_ID, documentId);
        metadata.put(RagMetadataKeys.CHUNK_INDEX, chunkIndex);
        metadata.put(RagMetadataKeys.LOGICAL_MODEL_NAME, model);
        metadata.putAll(attributes);
        vectorStore.add(List.of(new Document(documentId + ":" + chunkIndex, "chunk body", metadata)));
    }

    private static RagRetrievalQuery query(String collectionId, String model, String text, int topK,
                                           Map<String, String> attributes) {
        return new RagRetrievalQuery(collectionId, model, text, topK, 0.0, attributes);
    }
}
