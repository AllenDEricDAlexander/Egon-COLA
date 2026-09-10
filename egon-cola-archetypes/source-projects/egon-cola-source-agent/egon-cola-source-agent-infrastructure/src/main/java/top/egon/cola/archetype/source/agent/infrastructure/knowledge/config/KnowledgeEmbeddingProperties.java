package top.egon.cola.archetype.source.agent.infrastructure.knowledge.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Typed host configuration of the knowledge embedding endpoint.
 *
 * <p>The knowledge domain reaches its provider through its own endpoint, the way the research domain
 * reaches its chat model through its own: the two do not share a vendor, a key or a model. The
 * dimension count is deliberately absent — it belongs to the RAG component
 * ({@code egon.cola.component.rag.dimensions}), which also checks the configured model against it,
 * so the vector table's width has exactly one source.
 */
@Validated
@ConfigurationProperties(prefix = "agent.knowledge.embedding", ignoreUnknownFields = false)
public record KnowledgeEmbeddingProperties(@NotBlank String baseUrl,
                                           @NotBlank String apiKey,
                                           @NotBlank String modelName) {

    public KnowledgeEmbeddingProperties {
        baseUrl = normalize(baseUrl);
        apiKey = normalize(apiKey);
        modelName = normalize(modelName);
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
