package top.egon.cola.archetype.source.agent.infrastructure.knowledge.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import top.egon.cola.component.rag.autoconfigure.RagProperties;

/**
 * Publishes the vector store the RAG component resolves by name.
 *
 * <p>The store owns its own table: {@code initializeSchema(true)} makes it issue the extension and
 * {@code vector_store} DDL on start-up, so Flyway never carries a copy of the framework's schema and
 * the table's width cannot drift from the dimension the component validates the model against.
 *
 * <p>Gated on the component being enabled, like the embedding model: without RAG nothing reads the
 * table, and a host on a database without the extension is not asked to create it.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "egon.cola.component.rag", name = "enabled", havingValue = "true")
public class KnowledgeVectorConfiguration {

    @Bean("knowledgeRagVectorStore")
    @ConditionalOnMissingBean(name = "knowledgeRagVectorStore")
    public VectorStore knowledgeRagVectorStore(JdbcTemplate jdbcTemplate,
                                               @Qualifier("knowledgeEmbeddingModel") EmbeddingModel embeddingModel,
                                               @Qualifier("ragProperties") RagProperties ragProperties) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(ragProperties.dimensions())
                .initializeSchema(true)
                .build();
    }
}
