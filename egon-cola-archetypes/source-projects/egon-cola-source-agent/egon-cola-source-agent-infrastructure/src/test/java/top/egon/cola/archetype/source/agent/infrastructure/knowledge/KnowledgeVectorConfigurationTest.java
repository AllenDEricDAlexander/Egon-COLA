package top.egon.cola.archetype.source.agent.infrastructure.knowledge;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import top.egon.cola.archetype.source.agent.domain.knowledge.gateway.KnowledgeVectorGateway;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeChunkBO;
import top.egon.cola.archetype.source.agent.infrastructure.knowledge.config.KnowledgeEmbeddingConfiguration;
import top.egon.cola.archetype.source.agent.infrastructure.knowledge.config.KnowledgeVectorConfiguration;
import top.egon.cola.archetype.source.agent.infrastructure.knowledge.gateway.RagKnowledgeVectorGateway;
import top.egon.cola.archetype.source.agent.infrastructure.knowledge.metadata.KnowledgeVectorMetadata;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.rag.autoconfigure.RagAutoConfiguration;
import top.egon.cola.component.rag.embed.RagEmbeddingModelRegistry;
import top.egon.cola.component.rag.metadata.RagMetadataKeys;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Locks the assembly contract the RAG component resolves by name, and the tenant filter the
 * knowledge domain forces on top of it.
 *
 * <p>Everything runs offline: the embedding model is built from typed properties against a name the
 * framework knows the dimension of, the vector store is built on a mocked {@code JdbcTemplate} whose
 * statements are inspected instead of executed, and the retrieval path uses an in-memory store. The
 * real table and the real provider are exercised by the runtime acceptance, not here.
 */
class KnowledgeVectorConfigurationTest {

    private static final int DIMENSIONS = 1536;

    /** The OpenAI model whose dimension the framework resolves without calling the provider. */
    private static final String EMBEDDING_MODEL_NAME = "text-embedding-ada-002";

    private static final String LOGICAL_MODEL_NAME = "openai-compatible";

    private static final String COLLECTION_ID = "1001";

    private static final long TENANT = 42L;

    private static final long OTHER_TENANT = 43L;

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    /** The rag component plus the host wiring every case needs; each case adds what it varies. */
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RagAutoConfiguration.class, ValidationAutoConfiguration.class))
            .withBean(JdbcTemplate.class, () -> jdbcTemplate)
            .withBean(Validator.class, VALIDATOR_FACTORY::getValidator)
            .withPropertyValues(enabledRagKeys());

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void exposes_named_embedding_model_and_vector_store() {
        runner.withUserConfiguration(KnowledgeEmbeddingConfiguration.class, KnowledgeVectorConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("knowledgeEmbeddingModel").hasBean("knowledgeRagVectorStore");
                    assertThat(context.getBean("knowledgeEmbeddingModel")).isInstanceOf(OpenAiEmbeddingModel.class);
                    assertThat(context.getBean("knowledgeRagVectorStore")).isInstanceOf(PgVectorStore.class);

                    // The component resolves exactly the names the host publishes: the configured
                    // vector-store-bean-name is our bean, not a copy of it.
                    assertThat(context.getBean("ragVectorStore")).isSameAs(context.getBean("knowledgeRagVectorStore"));

                    RagEmbeddingModelRegistry registry =
                            context.getBean("ragEmbeddingModelRegistry", RagEmbeddingModelRegistry.class);
                    assertThat(registry.logicalNames()).containsExactly(LOGICAL_MODEL_NAME);
                    assertThat(registry.resolve(LOGICAL_MODEL_NAME).logicalName()).isEqualTo(LOGICAL_MODEL_NAME);
                    assertThat(registry.resolve(LOGICAL_MODEL_NAME).dimensions()).isEqualTo(DIMENSIONS);
                });
    }

    @Test
    void creates_the_vector_table_with_the_configured_dimensions() {
        runner.withUserConfiguration(KnowledgeEmbeddingConfiguration.class, KnowledgeVectorConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    ArgumentCaptor<String> statements = ArgumentCaptor.forClass(String.class);
                    verify(jdbcTemplate, atLeastOnce()).execute(statements.capture());
                    assertThat(statements.getAllValues())
                            .anySatisfy(sql -> assertThat(sql).contains("CREATE EXTENSION IF NOT EXISTS vector"))
                            .anySatisfy(sql -> assertThat(sql)
                                    .contains("CREATE TABLE IF NOT EXISTS public.vector_store")
                                    .contains("embedding vector(" + DIMENSIONS + ")"));
                });
    }

    @Test
    void forces_the_tenant_filter() {
        RecordingVectorStore vectorStore = new RecordingVectorStore();
        vectorStore.add(List.of(chunkOf(TENANT, 2001L), chunkOf(OTHER_TENANT, 2002L)));
        runner.withBean("knowledgeRagVectorStore", VectorStore.class, () -> vectorStore)
                .withBean(EgonColaTenantIdProvider.class, () -> () -> TENANT)
                .withUserConfiguration(KnowledgeEmbeddingConfiguration.class, RagKnowledgeVectorGateway.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    KnowledgeVectorGateway gateway = context.getBean(KnowledgeVectorGateway.class);

                    List<KnowledgeChunkBO> chunks =
                            gateway.retrieve(COLLECTION_ID, LOGICAL_MODEL_NAME, "how does it work", 8, Map.of());

                    assertThat(chunks).extracting(KnowledgeChunkBO::documentId).containsExactly(2001L);
                    Filter.Expression filter = vectorStore.searchRequests().getFirst().getFilterExpression();
                    assertThat(filter.toString())
                            .contains(KnowledgeVectorMetadata.TENANT_ID)
                            .contains(String.valueOf(TENANT))
                            .contains(COLLECTION_ID)
                            .contains(LOGICAL_MODEL_NAME);
                });
    }

    /**
     * The two wiring classes are complete, so the only way this context can fail is the dimension
     * disagreement: the model the host publishes reports 1536 while the component was told 768.
     */
    @Test
    void fails_when_dimensions_mismatch() {
        runner.withUserConfiguration(KnowledgeEmbeddingConfiguration.class, KnowledgeVectorConfiguration.class)
                .withPropertyValues("egon.cola.component.rag.dimensions=768")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasStackTraceContaining(String.valueOf(DIMENSIONS)));
    }

    private static String[] enabledRagKeys() {
        return new String[] {
                "egon.cola.component.rag.enabled=true",
                "egon.cola.component.rag.dimensions=" + DIMENSIONS,
                "egon.cola.component.rag.vector-store-bean-name=knowledgeRagVectorStore",
                "egon.cola.component.rag.embedding-models." + LOGICAL_MODEL_NAME
                        + ".embedding-model-bean-name=knowledgeEmbeddingModel",
                "egon.cola.component.rag.default-embedding-model=" + LOGICAL_MODEL_NAME,
                "agent.knowledge.embedding.base-url=http://test.invalid",
                "agent.knowledge.embedding.api-key=test-embedding-key",
                "agent.knowledge.embedding.model-name=" + EMBEDDING_MODEL_NAME
        };
    }

    /** One stored chunk carrying the reserved identity plus the tenant the host writes. */
    private static Document chunkOf(long tenantId, long documentId) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(RagMetadataKeys.COLLECTION_ID, COLLECTION_ID);
        metadata.put(RagMetadataKeys.DOCUMENT_ID, String.valueOf(documentId));
        metadata.put(RagMetadataKeys.LOGICAL_MODEL_NAME, LOGICAL_MODEL_NAME);
        metadata.put(RagMetadataKeys.CHUNK_INDEX, 0);
        metadata.put(KnowledgeVectorMetadata.TENANT_ID, String.valueOf(tenantId));
        return new Document(documentId + ":0", "chunk of document " + documentId, metadata);
    }

    /** In-memory store that records every request and evaluates the equality filters it is given. */
    private static final class RecordingVectorStore implements VectorStore {

        private final Map<String, Document> documents = new LinkedHashMap<>();

        private final List<SearchRequest> searchRequests = new ArrayList<>();

        private List<SearchRequest> searchRequests() {
            return List.copyOf(searchRequests);
        }

        @Override
        public void add(List<Document> toAdd) {
            toAdd.forEach(document -> documents.put(document.getId(), document));
        }

        @Override
        public void delete(List<String> ids) {
            ids.forEach(documents::remove);
        }

        @Override
        public void delete(Filter.Expression expression) {
            documents.values().removeIf(document -> matches(expression, document));
        }

        @Override
        public List<Document> similaritySearch(SearchRequest request) {
            searchRequests.add(request);
            return documents.values().stream()
                    .filter(document -> matches(request.getFilterExpression(), document))
                    .limit(request.getTopK())
                    .toList();
        }

        private static boolean matches(Filter.Expression expression, Document document) {
            if (expression == null) {
                return true;
            }
            return switch (expression.type()) {
                case AND -> matches(asExpression(expression.left()), document)
                        && matches(asExpression(expression.right()), document);
                case EQ -> {
                    if (!(expression.left() instanceof Filter.Key key)
                            || !(expression.right() instanceof Filter.Value value)) {
                        yield true;
                    }
                    Object actual = document.getMetadata().get(key.key());
                    yield actual != null && String.valueOf(actual).equals(String.valueOf(value.value()));
                }
                default -> true;
            };
        }

        private static Filter.Expression asExpression(Filter.Operand operand) {
            return operand instanceof Filter.Expression nested ? nested : null;
        }
    }
}
