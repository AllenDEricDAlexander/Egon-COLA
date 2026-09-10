package top.egon.cola.component.rag.autoconfigure;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import top.egon.cola.component.rag.exception.RagConfigurationException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Strictly bound configuration under {@code egon.cola.component.rag}.
 *
 * <p>Unknown keys are rejected by the binding handler; missing or out-of-range values are rejected
 * here so the context fails closed instead of starting with a partially usable engine.
 *
 * <p>The Jakarta constraints below declare the same contract declaratively and are executed by the
 * explicit {@code Validator} call in {@link RagAutoConfiguration}. This record deliberately carries
 * no {@code @Validated}: records are final, and {@code @Validated} would make Spring attempt a CGLIB
 * proxy that cannot subclass a final class.
 *
 * <p>The {@code @ConfigurationProperties} annotation exists so the configuration processor emits
 * {@code spring-configuration-metadata.json} for IDE completion and key-set checks. Binding itself
 * is performed by {@link RagAutoConfiguration} through an explicit {@code Binder} with
 * {@code NoUnboundElementsBindHandler}, because standard {@code @ConfigurationProperties} binding
 * would silently ignore unknown keys while this component must reject them.
 */
@ConfigurationProperties(prefix = "egon.cola.component.rag")
public record RagProperties(boolean enabled,
                            int dimensions,
                            @NotBlank String vectorStoreBeanName,
                            @NotEmpty @Valid Map<String, @Valid RagEmbeddingModelProperties> embeddingModels,
                            String defaultEmbeddingModel,
                            @NotNull @Valid RagStorageProperties storage,
                            @NotNull @Valid RagRetrievalProperties retrieval,
                            @NotNull @Valid RagValidationProperties validation) {

    private static final int MAX_DIMENSIONS = 8192;

    public RagProperties {
        if (dimensions <= 0 || dimensions > MAX_DIMENSIONS) {
            throw new RagConfigurationException("dimensions must be between 1 and " + MAX_DIMENSIONS);
        }
        vectorStoreBeanName = vectorStoreBeanName == null ? null : vectorStoreBeanName.trim();
        if (vectorStoreBeanName == null || vectorStoreBeanName.isEmpty()) {
            throw new RagConfigurationException("vector-store-bean-name must not be blank");
        }
        embeddingModels = normalizeModels(embeddingModels);
        defaultEmbeddingModel = resolveDefault(embeddingModels, defaultEmbeddingModel);
        storage = storage == null ? new RagStorageProperties(null, null) : storage;
        retrieval = retrieval == null ? new RagRetrievalProperties(0, 0) : retrieval;
        validation = validation == null ? new RagValidationProperties(false) : validation;
    }

    private static Map<String, RagEmbeddingModelProperties> normalizeModels(
            Map<String, RagEmbeddingModelProperties> models) {
        if (models == null || models.isEmpty()) {
            throw new RagConfigurationException("at least one entry under embedding-models is required");
        }
        Map<String, RagEmbeddingModelProperties> normalized = new LinkedHashMap<>();
        models.forEach((key, value) -> {
            String normalizedKey = key == null ? null : key.trim();
            if (normalizedKey == null || normalizedKey.isEmpty()) {
                throw new RagConfigurationException("embedding-models keys must not be blank");
            }
            if (normalized.put(normalizedKey, value) != null) {
                throw new RagConfigurationException(
                        "embedding-models contains duplicate logical name '" + normalizedKey + "'");
            }
        });
        return Map.copyOf(normalized);
    }

    private static String resolveDefault(Map<String, RagEmbeddingModelProperties> models, String declaredDefault) {
        if (declaredDefault != null && !declaredDefault.isBlank()) {
            String trimmed = declaredDefault.trim();
            if (!models.containsKey(trimmed)) {
                throw new RagConfigurationException("default-embedding-model '" + trimmed
                        + "' is not registered; available models: " + models.keySet());
            }
            return trimmed;
        }
        return models.size() == 1 ? models.keySet().iterator().next() : null;
    }
}
