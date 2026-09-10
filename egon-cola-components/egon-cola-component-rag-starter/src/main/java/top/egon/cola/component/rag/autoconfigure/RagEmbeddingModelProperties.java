package top.egon.cola.component.rag.autoconfigure;

import jakarta.validation.constraints.NotBlank;
import top.egon.cola.component.rag.exception.RagConfigurationException;

/**
 * One logical embedding model: the name of the host-supplied {@code EmbeddingModel} bean.
 *
 * <p>The logical name is the configuration key itself and is written into every vector's metadata,
 * so it must stay stable for the lifetime of the data it produced.
 */
public record RagEmbeddingModelProperties(@NotBlank String embeddingModelBeanName) {

    public RagEmbeddingModelProperties {
        embeddingModelBeanName = embeddingModelBeanName == null ? null : embeddingModelBeanName.trim();
        if (embeddingModelBeanName == null || embeddingModelBeanName.isEmpty()) {
            throw new RagConfigurationException("embedding-model-bean-name must not be blank");
        }
    }
}
