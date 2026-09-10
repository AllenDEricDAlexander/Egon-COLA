package top.egon.cola.component.rag.embed;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ListableBeanFactory;
import top.egon.cola.component.rag.autoconfigure.RagProperties;
import top.egon.cola.component.rag.exception.RagConfigurationException;
import top.egon.cola.component.rag.exception.RagModelNotRegisteredException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Read-only registry mapping each configured logical model name to its resolved descriptor.
 *
 * <p>All resolution and validation happens in the constructor so the context fails closed before any
 * ingestion can run: an unresolvable bean name, two logical names sharing one bean, or a model whose
 * dimension count differs from {@code rag.dimensions} all abort start-up.
 */
@Slf4j
public class RagEmbeddingModelRegistry {

    private final Map<String, RagEmbeddingModelDescriptorBO> descriptors;

    private final String defaultLogicalName;

    public RagEmbeddingModelRegistry(RagProperties properties, ListableBeanFactory beanFactory) {
        Map<String, RagEmbeddingModelDescriptorBO> resolved = new LinkedHashMap<>();
        Map<String, String> beanNamesToLogicalNames = new LinkedHashMap<>();
        properties.embeddingModels().forEach((logicalName, modelProperties) -> {
            String beanName = modelProperties.embeddingModelBeanName();
            String previousLogicalName = beanNamesToLogicalNames.putIfAbsent(beanName, logicalName);
            if (previousLogicalName != null) {
                throw new RagConfigurationException("embedding model bean '" + beanName
                        + "' is shared by logical models '" + previousLogicalName + "' and '" + logicalName
                        + "'; each logical model needs its own bean");
            }
            EmbeddingModel model = resolveBean(beanFactory, beanName);
            int dimensions = model.dimensions();
            if (dimensions != properties.dimensions()) {
                throw new RagConfigurationException("embedding model '" + logicalName + "' reports dimensions "
                        + dimensions + " but rag.dimensions is " + properties.dimensions());
            }
            resolved.put(logicalName, new RagEmbeddingModelDescriptorBO(logicalName, model, dimensions));
        });
        this.descriptors = Map.copyOf(resolved);
        this.defaultLogicalName = properties.defaultEmbeddingModel();
        log.info("rag embedding models ready: {}, default {}, dimensions {}",
                descriptors.keySet(), defaultLogicalName, properties.dimensions());
    }

    /**
     * Resolves a logical name to its descriptor; a blank name falls back to the configured default.
     *
     * @throws RagModelNotRegisteredException when the name is unknown or no default is configured
     */
    public RagEmbeddingModelDescriptorBO resolve(String logicalName) {
        String resolvedName = (logicalName == null || logicalName.isBlank()) ? defaultLogicalName : logicalName;
        RagEmbeddingModelDescriptorBO descriptor = resolvedName == null ? null : descriptors.get(resolvedName);
        if (descriptor == null) {
            throw new RagModelNotRegisteredException("embedding model '" + resolvedName
                    + "' is not registered; available models: " + descriptors.keySet());
        }
        return descriptor;
    }

    public Set<String> logicalNames() {
        return new LinkedHashSet<>(descriptors.keySet());
    }

    public String defaultLogicalName() {
        return defaultLogicalName;
    }

    private static EmbeddingModel resolveBean(ListableBeanFactory beanFactory, String beanName) {
        if (!beanFactory.containsBean(beanName)) {
            throw new RagConfigurationException("embedding model bean '" + beanName
                    + "' is not present; known EmbeddingModel beans: "
                    + Arrays.toString(beanFactory.getBeanNamesForType(EmbeddingModel.class)));
        }
        return beanFactory.getBean(beanName, EmbeddingModel.class);
    }
}
