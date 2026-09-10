package top.egon.cola.component.rag.extract;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.rag.autoconfigure.RagAutoConfiguration;
import top.egon.cola.component.rag.exception.RagExtractorMissingException;
import top.egon.cola.component.rag.support.FakeEmbeddingModel;
import top.egon.cola.component.rag.support.FakeVectorStore;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Locks the optional-parser contract: a missing reader must not break start-up, but requesting that
 * format must fail with a diagnostic that lists what is registered.
 */
class RagOptionalExtractorTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RagAutoConfiguration.class, ValidationAutoConfiguration.class))
            .withUserConfiguration(HostBeans.class);

    @Test
    void starts_without_the_pdf_reader() {
        runner.withClassLoader(new FilteredClassLoader("org.springframework.ai.reader.pdf"))
                .withPropertyValues(enabledKeySet())
                .run(context -> assertThat(context).hasNotFailed()
                        .doesNotHaveBean("pdfRagDocumentExtractor")
                        .hasBean("tikaRagDocumentExtractor"));
    }

    @Test
    void reports_missing_extractor_when_no_reader_covers_the_format() {
        // Tika is a catch-all, so a format is only truly unsupported when both optional readers are
        // absent; the built-in text extractors claim text, CSV, JSON and XML only.
        runner.withClassLoader(new FilteredClassLoader("org.springframework.ai.reader.pdf",
                        "org.springframework.ai.reader.tika"))
                .withPropertyValues(enabledKeySet())
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    RagExtractionServiceHolder holder = new RagExtractionServiceHolder(
                            context.getBean(RagDocumentExtractorRegistry.class));

                    assertThatThrownBy(() -> holder.extract("application/pdf", "report.pdf"))
                            .isInstanceOf(RagExtractorMissingException.class)
                            .hasMessageContaining("text/plain");
                });
    }

    @Test
    void registers_the_pdf_extractor_when_the_reader_is_present() {
        runner.withPropertyValues(enabledKeySet())
                .run(context -> assertThat(context).hasNotFailed()
                        .hasBean("pdfRagDocumentExtractor"));
    }

    @Test
    void tika_is_the_lowest_priority_fallback() {
        runner.withPropertyValues(enabledKeySet()).run(context -> {
            RagDocumentExtractorRegistry registry = context.getBean(RagDocumentExtractorRegistry.class);

            assertThat(registry.route("text/plain", "a.txt").name()).isEqualTo("plainTextRagDocumentExtractor");
            assertThat(registry.route("application/x-unknown", "a.bin").name()).isEqualTo("tikaRagDocumentExtractor");
        });
    }

    private record RagExtractionServiceHolder(RagDocumentExtractorRegistry registry) {

        void extract(String mimeType, String fileName) {
            registry.route(mimeType, fileName);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class HostBeans {

        @Bean(name = "hostVectorStore")
        FakeVectorStore hostVectorStore() {
            return new FakeVectorStore();
        }

        @Bean(name = "ragEmbeddingModel")
        FakeEmbeddingModel ragEmbeddingModel() {
            return new FakeEmbeddingModel(1536);
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
