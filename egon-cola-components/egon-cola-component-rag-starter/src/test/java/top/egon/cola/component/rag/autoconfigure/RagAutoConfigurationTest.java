package top.egon.cola.component.rag.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.rag.support.FakeEmbeddingModel;
import top.egon.cola.component.rag.support.FakeVectorStore;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Locks the default-off switch and the strict binding contract of the RAG starter.
 *
 * <p>No provider bean is supplied here: this class proves the switch and the binding rules, not the
 * host-bean resolution covered by later steps.
 */
class RagAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RagAutoConfiguration.class, ValidationAutoConfiguration.class));

    @Test
    void creates_no_rag_bean_when_disabled() {
        runner.run(context -> assertThat(context).doesNotHaveBean("ragProperties"));
    }

    @Test
    void creates_no_rag_bean_when_enabled_is_false() {
        runner.withPropertyValues("egon.cola.component.rag.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean("ragProperties"));
    }

    @Test
    void creates_rag_beans_when_enabled_with_required_keys() {
        runner.withUserConfiguration(MatchingHostBeans.class).withPropertyValues(enabledKeySet())
                .run(context -> assertThat(context).hasNotFailed()
                        .hasBean("ragProperties")
                        .hasBean("ragClock"));
    }

    @Test
    void fails_when_unknown_key_present() {
        List<String> keys = new ArrayList<>(List.of(enabledKeySet()));
        keys.add("egon.cola.component.rag.unexpected-key=1");
        runner.withPropertyValues(keys.toArray(String[]::new))
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasStackTraceContaining("unexpected-key"));
    }

    @Test
    void fails_when_dimensions_missing() {
        runner.withPropertyValues(
                        "egon.cola.component.rag.enabled=true",
                        "egon.cola.component.rag.vector-store-bean-name=hostVectorStore",
                        "egon.cola.component.rag.embedding-models.openai-small.embedding-model-bean-name=ragEmbeddingModel")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void fails_when_embedding_models_empty() {
        runner.withPropertyValues(
                        "egon.cola.component.rag.enabled=true",
                        "egon.cola.component.rag.dimensions=1536",
                        "egon.cola.component.rag.vector-store-bean-name=hostVectorStore")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void fails_when_max_top_k_smaller_than_default() {
        List<String> keys = new ArrayList<>(List.of(enabledKeySet()));
        keys.add("egon.cola.component.rag.retrieval.default-top-k=50");
        keys.add("egon.cola.component.rag.retrieval.max-top-k=10");
        runner.withPropertyValues(keys.toArray(String[]::new))
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void fails_when_default_embedding_model_not_registered() {
        List<String> keys = new ArrayList<>(List.of(enabledKeySet()));
        keys.add("egon.cola.component.rag.default-embedding-model=missing-model");
        runner.withPropertyValues(keys.toArray(String[]::new))
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasStackTraceContaining("missing-model"));
    }

    @Test
    void resolves_registry_and_vector_store_when_host_beans_present() {
        runner.withUserConfiguration(MatchingHostBeans.class)
                .withPropertyValues(enabledKeySet())
                .run(context -> assertThat(context).hasNotFailed()
                        .hasBean("ragVectorStore")
                        .hasBean("ragEmbeddingModelRegistry"));
    }

    @Test
    void fails_when_vector_store_bean_name_unresolved() {
        List<String> keys = new ArrayList<>(List.of(enabledKeySet()));
        keys.set(2, "egon.cola.component.rag.vector-store-bean-name=missingVectorStore");
        runner.withUserConfiguration(MatchingHostBeans.class)
                .withPropertyValues(keys.toArray(String[]::new))
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasStackTraceContaining("missingVectorStore"));
    }

    @Test
    void fails_when_embedding_model_bean_name_unresolved() {
        List<String> keys = new ArrayList<>(List.of(enabledKeySet()));
        keys.set(3, "egon.cola.component.rag.embedding-models.openai-small.embedding-model-bean-name=missingModel");
        runner.withUserConfiguration(MatchingHostBeans.class)
                .withPropertyValues(keys.toArray(String[]::new))
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasStackTraceContaining("missingModel"));
    }

    @Test
    void fails_when_two_logical_models_share_one_embedding_bean() {
        List<String> keys = new ArrayList<>(List.of(enabledKeySet()));
        keys.add("egon.cola.component.rag.embedding-models.bge-large.embedding-model-bean-name=ragEmbeddingModel");
        runner.withUserConfiguration(MatchingHostBeans.class)
                .withPropertyValues(keys.toArray(String[]::new))
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasStackTraceContaining("ragEmbeddingModel"));
    }

    @Test
    void fails_when_embedding_dimensions_mismatch() {
        runner.withUserConfiguration(MismatchedHostBeans.class)
                .withPropertyValues(enabledKeySet())
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasStackTraceContaining("768"));
    }

    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    static class MatchingHostBeans {

        @org.springframework.context.annotation.Bean(name = "hostVectorStore")
        FakeVectorStore ragVectorStore() {
            return new FakeVectorStore();
        }

        @org.springframework.context.annotation.Bean(name = "ragEmbeddingModel")
        FakeEmbeddingModel ragEmbeddingModel() {
            return new FakeEmbeddingModel(1536);
        }
    }

    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    static class MismatchedHostBeans {

        @org.springframework.context.annotation.Bean(name = "hostVectorStore")
        FakeVectorStore ragVectorStore() {
            return new FakeVectorStore();
        }

        @org.springframework.context.annotation.Bean(name = "ragEmbeddingModel")
        FakeEmbeddingModel ragEmbeddingModel() {
            return new FakeEmbeddingModel(768);
        }
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
