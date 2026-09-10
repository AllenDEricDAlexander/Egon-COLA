package top.egon.cola.component.rag.execution;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import top.egon.cola.component.rag.autoconfigure.RagEmbeddingModelProperties;
import top.egon.cola.component.rag.autoconfigure.RagProperties;
import top.egon.cola.component.rag.chunk.RagChunkIdFactory;
import top.egon.cola.component.rag.chunk.RagChunkingStrategy;
import top.egon.cola.component.rag.chunk.RagChunkingStrategyFactory;
import top.egon.cola.component.rag.chunk.TokenRagChunkingStrategy;
import top.egon.cola.component.rag.embed.RagEmbeddingModelRegistry;
import top.egon.cola.component.rag.exception.RagModelNotRegisteredException;
import top.egon.cola.component.rag.exception.RagValidationException;
import top.egon.cola.component.rag.exception.RagVectorStoreException;
import top.egon.cola.component.rag.metadata.RagMetadataKeys;
import top.egon.cola.component.rag.model.ExtractedDocumentBO;
import top.egon.cola.component.rag.model.RagChunkingConfigDTO;
import top.egon.cola.component.rag.model.RagIngestionCommand;
import top.egon.cola.component.rag.model.RagIngestionResult;
import top.egon.cola.component.rag.support.FakeEmbeddingModel;
import top.egon.cola.component.rag.support.FakeVectorStore;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static top.egon.cola.component.rag.chunk.RagChunkingStrategyEnum.TOKEN;

/** Locks the ingestion order, the metadata merge and the failure semantics. */
class RagIngestionServiceImplTest {

    private static final int DIMENSIONS = 1536;

    private static final String TEXT = "alpha beta gamma delta epsilon zeta eta theta iota kappa lambda mu "
            + "nu xi omicron pi rho sigma tau upsilon phi chi psi omega ".repeat(20);

    private FakeVectorStore vectorStore;

    private RagIngestionServiceImpl service;

    @BeforeEach
    void setUp() {
        vectorStore = new FakeVectorStore();
        service = serviceWith(Clock.systemUTC());
    }

    @Test
    void deletes_the_previous_chunks_before_writing() {
        RagIngestionResult result = service.ingest(command(Map.of()));

        assertThat(vectorStore.deletedExpressions()).hasSize(1);
        assertThat(vectorStore.deletedExpressions().get(0).toString()).contains("doc-1");
        assertThat(vectorStore.addedDocuments()).hasSize(result.chunkCount());
        assertThat(result.chunkCount()).isPositive();
    }

    @Test
    void echoes_the_identity_and_the_model_dimensions() {
        RagIngestionResult result = service.ingest(command(Map.of()));

        assertThat(result.collectionId()).isEqualTo("kb-1");
        assertThat(result.documentId()).isEqualTo("doc-1");
        assertThat(result.logicalModelName()).isEqualTo("openai-small");
        assertThat(result.dimensions()).isEqualTo(DIMENSIONS);
    }

    @Test
    void writes_reserved_identity_metadata_on_every_chunk() {
        service.ingest(command(Map.of()));

        assertThat(vectorStore.addedDocuments()).allSatisfy(document -> assertThat(document.getMetadata())
                .containsEntry(RagMetadataKeys.COLLECTION_ID, "kb-1")
                .containsEntry(RagMetadataKeys.DOCUMENT_ID, "doc-1")
                .containsEntry(RagMetadataKeys.LOGICAL_MODEL_NAME, "openai-small"));
    }

    @Test
    void lets_business_attributes_win_over_structural_ones() {
        RagIngestionCommand command = command(Map.of("source", "structural"), Map.of("source", "business"));

        service.ingest(command);

        assertThat(vectorStore.addedDocuments()).allSatisfy(document ->
                assertThat(document.getMetadata()).containsEntry("source", "business"));
    }

    @Test
    void rejects_reserved_keys_supplied_as_business_attributes() {
        assertThatThrownBy(() -> command(Map.of(RagMetadataKeys.DOCUMENT_ID, "forged")))
                .isInstanceOf(RagValidationException.class)
                .hasMessageContaining(RagMetadataKeys.DOCUMENT_ID);
        assertThat(vectorStore.addedDocuments()).isEmpty();
    }

    @Test
    void does_not_write_when_the_delete_fails() {
        vectorStore.failOn(FakeVectorStore.FailurePoint.DELETE);

        assertThatThrownBy(() -> service.ingest(command(Map.of())))
                .isInstanceOf(RagVectorStoreException.class);
        assertThat(vectorStore.addedDocuments()).isEmpty();
    }

    @Test
    void propagates_a_write_failure() {
        vectorStore.failOn(FakeVectorStore.FailurePoint.ADD);

        assertThatThrownBy(() -> service.ingest(command(Map.of())))
                .isInstanceOf(RagVectorStoreException.class)
                .hasMessageContaining("write");
    }

    @Test
    void fails_when_the_logical_model_is_not_registered() {
        RagIngestionCommand command = new RagIngestionCommand("kb-1", "doc-1", "missing-model",
                chunkingConfig(), document(), Map.of());

        assertThatThrownBy(() -> service.ingest(command))
                .isInstanceOf(RagModelNotRegisteredException.class)
                .hasMessageContaining("openai-small");
    }

    @Test
    void derives_elapsed_from_the_injected_clock() {
        RagIngestionServiceImpl fixed = serviceWith(
                Clock.fixed(Instant.parse("2026-09-10T00:00:02Z"), ZoneOffset.UTC));

        // A frozen clock proves the duration comes from the injected source, not from the wall clock.
        assertThat(fixed.ingest(command(Map.of())).elapsed()).isEqualTo(Duration.ZERO);
    }

    private RagIngestionServiceImpl serviceWith(Clock clock) {
        RagProperties properties = new RagProperties(true, DIMENSIONS, "hostVectorStore",
                Map.of("openai-small", new RagEmbeddingModelProperties("ragEmbeddingModel")), null, null, null, null);
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory(
                Map.of("ragEmbeddingModel", new FakeEmbeddingModel(DIMENSIONS)));
        RagEmbeddingModelRegistry registry = new RagEmbeddingModelRegistry(properties, beanFactory);
        RagChunkingStrategyFactory factory = new RagChunkingStrategyFactory(
                List.of((RagChunkingStrategy) new TokenRagChunkingStrategy()));
        return new RagIngestionServiceImpl(registry, factory, new RagChunkIdFactory(), vectorStore, clock);
    }

    private static RagIngestionCommand command(Map<String, String> attributes) {
        return command(Map.of(), attributes);
    }

    private static RagIngestionCommand command(Map<String, String> structural, Map<String, String> business) {
        return new RagIngestionCommand("kb-1", "doc-1", "openai-small", chunkingConfig(),
                new ExtractedDocumentBO(TEXT, "title", "text/plain", structural), business);
    }

    private static ExtractedDocumentBO document() {
        return new ExtractedDocumentBO(TEXT, "title", "text/plain", null);
    }

    private static RagChunkingConfigDTO chunkingConfig() {
        return new RagChunkingConfigDTO(TOKEN, 40, 0, 1, null);
    }
}
