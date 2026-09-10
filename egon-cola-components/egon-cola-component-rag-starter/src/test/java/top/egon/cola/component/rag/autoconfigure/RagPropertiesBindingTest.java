package top.egon.cola.component.rag.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.rag.exception.RagConfigurationException;
import top.egon.cola.component.rag.storage.RagDocumentStorageTypeEnum;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Locks the documented defaults, the key normalization rules and the fail-closed checks. */
class RagPropertiesBindingTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RagAutoConfiguration.class, ValidationAutoConfiguration.class));

    @Test
    void applies_documented_defaults() {
        runner.withUserConfiguration(HostBeans.class).withPropertyValues(enabledKeySet()).run(context -> {
            RagProperties properties = context.getBean("ragProperties", RagProperties.class);
            assertThat(properties.storage().type()).isEqualTo(RagDocumentStorageTypeEnum.LOCAL);
            assertThat(properties.storage().local().root()).isEqualTo(RagStorageProperties.DEFAULT_LOCAL_ROOT);
            assertThat(properties.retrieval().defaultTopK()).isEqualTo(RagRetrievalProperties.DEFAULT_TOP_K);
            assertThat(properties.retrieval().maxTopK()).isEqualTo(RagRetrievalProperties.DEFAULT_MAX_TOP_K);
            assertThat(properties.validation().probeOnStartup()).isFalse();
            assertThat(properties.defaultEmbeddingModel()).isEqualTo("openai-small");
            assertThat(properties.vectorStoreBeanName()).isEqualTo("hostVectorStore");
        });
    }

    @Test
    void normalizes_embedding_model_keys() {
        RagProperties properties = propertiesWithModels(modelsOf(" openai-small ", "ragEmbeddingModel"));

        assertThat(properties.embeddingModels()).containsOnlyKeys("openai-small");
    }

    @Test
    void rejects_duplicate_normalized_model_keys() {
        Map<String, RagEmbeddingModelProperties> models = new LinkedHashMap<>();
        models.put("openai-small", new RagEmbeddingModelProperties("ragEmbeddingModelA"));
        models.put(" openai-small", new RagEmbeddingModelProperties("ragEmbeddingModelB"));

        assertThatThrownBy(() -> propertiesWithModels(models))
                .isInstanceOf(RagConfigurationException.class)
                .hasMessageContaining("openai-small");
    }

    @Test
    void rejects_blank_embedding_model_bean_name() {
        assertThatThrownBy(() -> new RagEmbeddingModelProperties(" "))
                .isInstanceOf(RagConfigurationException.class);
    }

    @Test
    void rejects_default_model_absent_from_registry() {
        assertThatThrownBy(() -> propertiesWithModels(modelsOf("openai-small", "ragEmbeddingModel"), "large-model"))
                .isInstanceOf(RagConfigurationException.class)
                .hasMessageContaining("large-model");
    }

    @Test
    void leaves_default_model_unset_when_several_models_and_none_declared() {
        Map<String, RagEmbeddingModelProperties> models = new LinkedHashMap<>();
        models.put("openai-small", new RagEmbeddingModelProperties("ragEmbeddingModelSmall"));
        models.put("bge-large", new RagEmbeddingModelProperties("ragEmbeddingModelLarge"));

        RagProperties properties = propertiesWithModels(models);

        assertThat(properties.defaultEmbeddingModel()).isNull();
    }

    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    static class HostBeans {

        @org.springframework.context.annotation.Bean(name = "hostVectorStore")
        top.egon.cola.component.rag.support.FakeVectorStore hostVectorStore() {
            return new top.egon.cola.component.rag.support.FakeVectorStore();
        }

        @org.springframework.context.annotation.Bean(name = "ragEmbeddingModel")
        top.egon.cola.component.rag.support.FakeEmbeddingModel ragEmbeddingModel() {
            return new top.egon.cola.component.rag.support.FakeEmbeddingModel(1536);
        }
    }

    private static Map<String, RagEmbeddingModelProperties> modelsOf(String key, String beanName) {
        Map<String, RagEmbeddingModelProperties> models = new LinkedHashMap<>();
        models.put(key, new RagEmbeddingModelProperties(beanName));
        return models;
    }

    private static RagProperties propertiesWithModels(Map<String, RagEmbeddingModelProperties> models) {
        return propertiesWithModels(models, null);
    }

    private static RagProperties propertiesWithModels(Map<String, RagEmbeddingModelProperties> models,
                                                      String defaultModel) {
        return new RagProperties(true, 1536, "ragVectorStore", models, defaultModel, null, null, null);
    }

    private static String[] enabledKeySet() {
        return new String[]{
                "egon.cola.component.rag.enabled=true",
                "egon.cola.component.rag.dimensions=1536",
                "egon.cola.component.rag.vector-store-bean-name=hostVectorStore",
                "egon.cola.component.rag.embedding-models.openai-small.embedding-model-bean-name=ragEmbeddingModel"
        };
    }
}
