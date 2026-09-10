package top.egon.cola.component.rag.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.rag.support.FakeEmbeddingModel;
import top.egon.cola.component.rag.support.FakeVectorStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Locks the component's published surface: its bean names and its auto-configuration entry. */
class RagDeliveryBoundaryTest {

    private static final int DIMENSIONS = 1536;

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RagAutoConfiguration.class, ValidationAutoConfiguration.class))
            .withUserConfiguration(HostBeans.class);

    @Test
    void exposes_the_documented_bean_names() {
        runner.withPropertyValues(enabledKeySet()).run(context -> assertThat(context).hasNotFailed()
                .hasBean("ragProperties")
                .hasBean("ragClock")
                .hasBean("ragVectorStore")
                .hasBean("ragEmbeddingModelRegistry")
                .hasBean("plainTextRagDocumentExtractor")
                .hasBean("markdownRagDocumentExtractor")
                .hasBean("ragDocumentExtractorRegistry")
                .hasBean("tokenRagChunkingStrategy")
                .hasBean("markdownHeadingRagChunkingStrategy")
                .hasBean("recursiveRagChunkingStrategy")
                .hasBean("ragChunkingStrategyFactory")
                .hasBean("ragChunkIdFactory")
                .hasBean("ragDocumentStorage")
                .hasBean("ragExtractionService")
                .hasBean("ragIngestionService")
                .hasBean("ragRetrievalService")
                .hasBean("ragMetricsRecorder")
                .hasBean("ragVectorStoreProbe"));
    }

    @Test
    void exposes_nothing_when_disabled() {
        runner.run(context -> assertThat(context).doesNotHaveBean("ragProperties")
                .doesNotHaveBean("ragIngestionService")
                .doesNotHaveBean("ragDocumentStorage"));
    }

    @Test
    void registers_the_auto_configuration_entry_point() throws IOException {
        Path imports = Path.of("src/main/resources/META-INF/spring/"
                + "org.springframework.boot.autoconfigure.AutoConfiguration.imports");

        assertThat(Files.readString(imports, StandardCharsets.UTF_8).strip())
                .isEqualTo("top.egon.cola.component.rag.autoconfigure.RagAutoConfiguration");
    }

    @Configuration(proxyBeanMethods = false)
    static class HostBeans {

        @Bean(name = "hostVectorStore")
        FakeVectorStore hostVectorStore() {
            return new FakeVectorStore();
        }

        @Bean(name = "ragEmbeddingModel")
        FakeEmbeddingModel ragEmbeddingModel() {
            return new FakeEmbeddingModel(DIMENSIONS);
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
