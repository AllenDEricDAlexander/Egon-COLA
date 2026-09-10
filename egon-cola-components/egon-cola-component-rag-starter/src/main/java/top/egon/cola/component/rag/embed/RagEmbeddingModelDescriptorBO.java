package top.egon.cola.component.rag.embed;

import org.springframework.ai.embedding.EmbeddingModel;
import top.egon.cola.component.rag.exception.RagConfigurationException;

import java.util.Objects;

/**
 * One resolved embedding model: its stable logical name, the host-supplied bean and its dimension
 * count.
 *
 * <p>The logical name is what gets written into every vector's metadata and what retrieval filters
 * on, so it must stay stable for the lifetime of the data it produced.
 */
public record RagEmbeddingModelDescriptorBO(String logicalName, EmbeddingModel embeddingModel, int dimensions) {

    public RagEmbeddingModelDescriptorBO {
        if (logicalName == null || logicalName.isBlank()) {
            throw new RagConfigurationException("logical embedding model name must not be blank");
        }
        Objects.requireNonNull(embeddingModel, "embeddingModel must not be null");
        if (dimensions <= 0) {
            throw new RagConfigurationException(
                    "embedding model '" + logicalName + "' reports non-positive dimensions");
        }
    }
}
