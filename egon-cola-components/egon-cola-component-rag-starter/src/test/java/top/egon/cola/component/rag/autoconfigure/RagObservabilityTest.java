package top.egon.cola.component.rag.autoconfigure;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.rag.chunk.RagChunkIdFactory;
import top.egon.cola.component.rag.chunk.RagChunkingStrategy;
import top.egon.cola.component.rag.chunk.RagChunkingStrategyFactory;
import top.egon.cola.component.rag.chunk.TokenRagChunkingStrategy;
import top.egon.cola.component.rag.embed.RagEmbeddingModelRegistry;
import top.egon.cola.component.rag.execution.RagIngestionServiceImpl;
import top.egon.cola.component.rag.model.ExtractedDocumentBO;
import top.egon.cola.component.rag.model.RagChunkingConfigDTO;
import top.egon.cola.component.rag.model.RagIngestionCommand;
import top.egon.cola.component.rag.support.FakeEmbeddingModel;
import top.egon.cola.component.rag.support.FakeVectorStore;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static top.egon.cola.component.rag.chunk.RagChunkingStrategyEnum.TOKEN;

/** Locks the metrics seam and the promise that document content never reaches the log. */
class RagObservabilityTest {

    private static final int DIMENSIONS = 1536;

    private static final String SECRET_CONTENT = "SECRET-CONTENT-MARKER ";

    @Test
    void registers_metrics_when_a_meter_registry_is_present() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RagAutoConfiguration.class, ValidationAutoConfiguration.class))
                .withUserConfiguration(HostBeans.class)
                .withBean(SimpleMeterRegistry.class, SimpleMeterRegistry::new)
                .withPropertyValues(enabledKeySet())
                .run(context -> {
                    RagMetricsRecorder recorder = context.getBean("ragMetricsRecorder", RagMetricsRecorder.class);
                    assertThat(recorder.enabled()).isTrue();

                    ingestOnce(context.getBean(SimpleMeterRegistry.class), context);

                    assertThat(context.getBean(SimpleMeterRegistry.class).find("rag.ingest").counters())
                            .isNotEmpty();
                    assertThat(context.getBean(SimpleMeterRegistry.class).find("rag.ingest.duration").timers())
                            .isNotEmpty();
                });
    }

    @Test
    void starts_and_skips_metrics_without_micrometer() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader("io.micrometer.core"))
                .withConfiguration(AutoConfigurations.of(RagAutoConfiguration.class, ValidationAutoConfiguration.class))
                .withUserConfiguration(HostBeans.class)
                .withPropertyValues(enabledKeySet())
                .run(context -> assertThat(context).hasNotFailed()
                        .getBean("ragMetricsRecorder", RagMetricsRecorder.class)
                        .extracting(RagMetricsRecorder::enabled)
                        .isEqualTo(false));
    }

    @Test
    void never_logs_the_document_content() {
        ListAppender<ILoggingEvent> appender = attachTo(RagIngestionServiceImpl.class);
        try {
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(RagAutoConfiguration.class, ValidationAutoConfiguration.class))
                    .withUserConfiguration(HostBeans.class)
                    .withPropertyValues(enabledKeySet())
                    .run(context -> ingestOnce(new SimpleMeterRegistry(), context));

            assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
                    .noneMatch(message -> message.contains("SECRET-CONTENT-MARKER"));
        } finally {
            detachFrom(RagIngestionServiceImpl.class, appender);
        }
    }

    @Test
    void tags_metrics_with_low_cardinality_values_only() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        new MicrometerRagMetricsRecorder(registry).recordIngestion("openai-small", "TOKEN", "SUCCESS",
                Duration.ofMillis(5), 3);

        assertThat(registry.find("rag.ingest").counters()).allSatisfy(counter ->
                assertThat(counter.getId().getTags()).allSatisfy(tag ->
                        assertThat(tag.getKey()).isIn("model", "strategy", "result")));
    }

    private static void ingestOnce(SimpleMeterRegistry registry, org.springframework.context.ApplicationContext context) {
        RagProperties properties = new RagProperties(true, DIMENSIONS, "hostVectorStore",
                Map.of("openai-small", new RagEmbeddingModelProperties("ragEmbeddingModel")), null, null, null, null);
        RagEmbeddingModelRegistry modelRegistry = new RagEmbeddingModelRegistry(properties,
                new StaticListableBeanFactory(Map.of("ragEmbeddingModel", new FakeEmbeddingModel(DIMENSIONS))));
        RagChunkingStrategyFactory factory = new RagChunkingStrategyFactory(
                List.of((RagChunkingStrategy) new TokenRagChunkingStrategy()));
        RagIngestionServiceImpl service = new RagIngestionServiceImpl(modelRegistry, factory,
                new RagChunkIdFactory(), context.getBean("hostVectorStore", FakeVectorStore.class),
                Clock.systemUTC(), new MicrometerRagMetricsRecorder(registry));

        service.ingest(new RagIngestionCommand("kb-1", "doc-1", "openai-small",
                new RagChunkingConfigDTO(TOKEN, 40, 0, 1, null),
                new ExtractedDocumentBO(SECRET_CONTENT.repeat(20), "title", "text/plain", null), Map.of()));
    }

    private static ListAppender<ILoggingEvent> attachTo(Class<?> type) {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        Logger logger = (Logger) LoggerFactory.getLogger(type);
        logger.setLevel(Level.INFO);
        logger.addAppender(appender);
        return appender;
    }

    private static void detachFrom(Class<?> type, ListAppender<ILoggingEvent> appender) {
        ((Logger) LoggerFactory.getLogger(type)).detachAppender(appender);
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
