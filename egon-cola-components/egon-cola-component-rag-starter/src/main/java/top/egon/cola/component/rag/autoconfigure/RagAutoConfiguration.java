package top.egon.cola.component.rag.autoconfigure;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.handler.NoUnboundElementsBindHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import top.egon.cola.component.rag.api.RagExtractionService;
import top.egon.cola.component.rag.api.RagIngestionService;
import top.egon.cola.component.rag.chunk.MarkdownHeadingRagChunkingStrategy;
import top.egon.cola.component.rag.chunk.RagChunkIdFactory;
import top.egon.cola.component.rag.chunk.RagChunkingStrategy;
import top.egon.cola.component.rag.chunk.RagChunkingStrategyFactory;
import top.egon.cola.component.rag.chunk.RecursiveRagChunkingStrategy;
import top.egon.cola.component.rag.chunk.TokenRagChunkingStrategy;
import top.egon.cola.component.rag.embed.RagEmbeddingModelRegistry;
import top.egon.cola.component.rag.exception.RagConfigurationException;
import top.egon.cola.component.rag.execution.RagExtractionServiceImpl;
import top.egon.cola.component.rag.execution.RagIngestionServiceImpl;
import top.egon.cola.component.rag.extract.MarkdownRagDocumentExtractor;
import top.egon.cola.component.rag.extract.PdfRagDocumentExtractor;
import top.egon.cola.component.rag.extract.PlainTextRagDocumentExtractor;
import top.egon.cola.component.rag.extract.RagDocumentExtractor;
import top.egon.cola.component.rag.extract.RagDocumentExtractorRegistry;
import top.egon.cola.component.rag.extract.TikaRagDocumentExtractor;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Wires the flat RAG starter only when the host explicitly enables it.
 *
 * <p>Every bean is created here; the component never creates an {@code EmbeddingModel} or a
 * {@code VectorStore} of its own and never holds a provider endpoint or key.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "egon.cola.component.rag", name = "enabled",
        havingValue = "true", matchIfMissing = false)
public class RagAutoConfiguration {

    @Bean(name = "ragClock")
    @ConditionalOnMissingBean(name = "ragClock")
    public Clock ragClock() {
        return Clock.systemUTC();
    }

    @Bean(name = "ragProperties")
    @ConditionalOnMissingBean(name = "ragProperties")
    public RagProperties ragProperties(Environment environment, Validator validator) {
        RagProperties properties = Binder.get(environment).bindOrCreate(
                "egon.cola.component.rag",
                Bindable.of(RagProperties.class),
                new NoUnboundElementsBindHandler(BindHandler.DEFAULT));
        Set<ConstraintViolation<RagProperties>> violations = validator.validate(properties);
        if (!violations.isEmpty()) {
            throw new RagConfigurationException("invalid rag configuration: " + violations.stream()
                    .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; ")));
        }
        return properties;
    }

    /**
     * Publishes the host's configured vector store under a stable internal name.
     *
     * <p>The host bean name is a configuration value, so it cannot be referenced by a compile-time
     * {@code @Qualifier}; resolving it once here keeps every consumer free of bean-name lookups and
     * keeps the component working when the host registers several {@code VectorStore} beans.
     */
    @Bean(name = "ragVectorStore")
    @ConditionalOnMissingBean(name = "ragVectorStore")
    public VectorStore ragVectorStore(ListableBeanFactory beanFactory,
                                      @Qualifier("ragProperties") RagProperties properties) {
        String beanName = properties.vectorStoreBeanName();
        if (!beanFactory.containsBean(beanName)) {
            throw new RagConfigurationException("vector store bean '" + beanName
                    + "' is not present; known VectorStore beans: "
                    + Arrays.toString(beanFactory.getBeanNamesForType(VectorStore.class)));
        }
        return beanFactory.getBean(beanName, VectorStore.class);
    }

    @Bean(name = "ragEmbeddingModelRegistry")
    @ConditionalOnMissingBean(name = "ragEmbeddingModelRegistry")
    public RagEmbeddingModelRegistry ragEmbeddingModelRegistry(@Qualifier("ragProperties") RagProperties properties,
                                                               ListableBeanFactory beanFactory) {
        return new RagEmbeddingModelRegistry(properties, beanFactory);
    }

    @Bean(name = "plainTextRagDocumentExtractor")
    @ConditionalOnMissingBean(PlainTextRagDocumentExtractor.class)
    public PlainTextRagDocumentExtractor plainTextRagDocumentExtractor() {
        return new PlainTextRagDocumentExtractor();
    }

    @Bean(name = "markdownRagDocumentExtractor")
    @ConditionalOnMissingBean(MarkdownRagDocumentExtractor.class)
    public MarkdownRagDocumentExtractor markdownRagDocumentExtractor() {
        return new MarkdownRagDocumentExtractor();
    }

    /**
     * PDF support arrives only when the optional reader artifact is present; the return type is the
     * SPI so Spring never has to load an extractor whose backing library might be absent.
     */
    @Bean(name = "pdfRagDocumentExtractor")
    @ConditionalOnClass(name = "org.springframework.ai.reader.pdf.PagePdfDocumentReader")
    @ConditionalOnMissingBean(name = "pdfRagDocumentExtractor")
    public RagDocumentExtractor pdfRagDocumentExtractor() {
        return new PdfRagDocumentExtractor();
    }

    @Bean(name = "tikaRagDocumentExtractor")
    @ConditionalOnClass(name = "org.springframework.ai.reader.tika.TikaDocumentReader")
    @ConditionalOnMissingBean(name = "tikaRagDocumentExtractor")
    public RagDocumentExtractor tikaRagDocumentExtractor() {
        return new TikaRagDocumentExtractor();
    }

    /**
     * Collects the built-in extractors together with any extractor the host registers. A host bean
     * can therefore replace a built-in implementation outright by declaring one of the same type.
     */
    @Bean(name = "ragDocumentExtractorRegistry")
    @ConditionalOnMissingBean(name = "ragDocumentExtractorRegistry")
    public RagDocumentExtractorRegistry ragDocumentExtractorRegistry(List<RagDocumentExtractor> extractors) {
        return new RagDocumentExtractorRegistry(extractors);
    }

    @Bean(name = "ragExtractionService")
    @ConditionalOnMissingBean(name = "ragExtractionService")
    public RagExtractionService ragExtractionService(
            @Qualifier("ragDocumentExtractorRegistry") RagDocumentExtractorRegistry extractorRegistry,
            @Qualifier("ragClock") Clock clock) {
        return new RagExtractionServiceImpl(extractorRegistry, clock);
    }

    @Bean(name = "tokenRagChunkingStrategy")
    @ConditionalOnMissingBean(TokenRagChunkingStrategy.class)
    public RagChunkingStrategy tokenRagChunkingStrategy() {
        return new TokenRagChunkingStrategy();
    }

    @Bean(name = "markdownHeadingRagChunkingStrategy")
    @ConditionalOnMissingBean(MarkdownHeadingRagChunkingStrategy.class)
    public RagChunkingStrategy markdownHeadingRagChunkingStrategy() {
        return new MarkdownHeadingRagChunkingStrategy();
    }

    @Bean(name = "recursiveRagChunkingStrategy")
    @ConditionalOnMissingBean(RecursiveRagChunkingStrategy.class)
    public RagChunkingStrategy recursiveRagChunkingStrategy() {
        return new RecursiveRagChunkingStrategy();
    }

    @Bean(name = "ragChunkingStrategyFactory")
    @ConditionalOnMissingBean(name = "ragChunkingStrategyFactory")
    public RagChunkingStrategyFactory ragChunkingStrategyFactory(List<RagChunkingStrategy> strategies) {
        return new RagChunkingStrategyFactory(strategies);
    }

    @Bean(name = "ragChunkIdFactory")
    @ConditionalOnMissingBean(name = "ragChunkIdFactory")
    public RagChunkIdFactory ragChunkIdFactory() {
        return new RagChunkIdFactory();
    }

    @Bean(name = "ragIngestionService")
    @ConditionalOnMissingBean(name = "ragIngestionService")
    public RagIngestionService ragIngestionService(
            @Qualifier("ragEmbeddingModelRegistry") RagEmbeddingModelRegistry modelRegistry,
            @Qualifier("ragChunkingStrategyFactory") RagChunkingStrategyFactory chunkingStrategyFactory,
            @Qualifier("ragChunkIdFactory") RagChunkIdFactory chunkIdFactory,
            @Qualifier("ragVectorStore") VectorStore vectorStore,
            @Qualifier("ragClock") Clock clock) {
        return new RagIngestionServiceImpl(modelRegistry, chunkingStrategyFactory, chunkIdFactory, vectorStore, clock);
    }
}
