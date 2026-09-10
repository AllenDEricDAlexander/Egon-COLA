package top.egon.cola.component.rag.execution;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import top.egon.cola.component.rag.autoconfigure.RagEmbeddingModelProperties;
import top.egon.cola.component.rag.autoconfigure.NoopRagMetricsRecorder;
import top.egon.cola.component.rag.autoconfigure.RagProperties;
import top.egon.cola.component.rag.chunk.RagChunkIdFactory;
import top.egon.cola.component.rag.chunk.RagChunkingStrategy;
import top.egon.cola.component.rag.chunk.RagChunkingStrategyFactory;
import top.egon.cola.component.rag.chunk.TokenRagChunkingStrategy;
import top.egon.cola.component.rag.embed.RagEmbeddingModelRegistry;
import top.egon.cola.component.rag.extract.RagDocumentExtractor;
import top.egon.cola.component.rag.extract.RagDocumentExtractorRegistry;
import top.egon.cola.component.rag.model.ExtractedDocumentBO;
import top.egon.cola.component.rag.model.RagChunkingConfigDTO;
import top.egon.cola.component.rag.model.RagExtractionCommand;
import top.egon.cola.component.rag.model.RagIngestionCommand;
import top.egon.cola.component.rag.support.FakeEmbeddingModel;
import top.egon.cola.component.rag.support.FakeVectorStore;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static top.egon.cola.component.rag.chunk.RagChunkingStrategyEnum.TOKEN;

/**
 * Proves the reason extraction and ingestion are separate: a consumer persists the extracted text
 * and can then re-run embedding without re-reading or re-parsing the original document.
 */
class TwoPhaseIngestionTest {

    private static final int DIMENSIONS = 1536;

    @Test
    void extraction_runs_once_across_extract_and_two_ingest_calls() {
        AtomicInteger extractions = new AtomicInteger();
        RagExtractionServiceImpl extractionService = new RagExtractionServiceImpl(
                new RagDocumentExtractorRegistry(List.of(countingExtractor(extractions))), Clock.systemUTC());
        FakeVectorStore vectorStore = new FakeVectorStore();
        RagIngestionServiceImpl ingestionService = ingestionService(vectorStore);

        ExtractedDocumentBO document = extractionService.extract(
                new RagExtractionCommand("a.txt", "text/plain", stream("hello world")));
        ingestionService.ingest(command(document));
        List<String> firstChunkIds = vectorStore.addedDocuments().stream().map(Document::getId).toList();
        vectorStore.reset();
        ingestionService.ingest(command(document));
        List<String> secondChunkIds = vectorStore.addedDocuments().stream().map(Document::getId).toList();

        assertThat(extractions).hasValue(1);
        assertThat(secondChunkIds).isEqualTo(firstChunkIds).isNotEmpty();
    }

    private static RagIngestionServiceImpl ingestionService(FakeVectorStore vectorStore) {
        RagProperties properties = new RagProperties(true, DIMENSIONS, "hostVectorStore",
                Map.of("openai-small", new RagEmbeddingModelProperties("ragEmbeddingModel")), null, null, null, null);
        RagEmbeddingModelRegistry registry = new RagEmbeddingModelRegistry(properties,
                new StaticListableBeanFactory(Map.of("ragEmbeddingModel", new FakeEmbeddingModel(DIMENSIONS))));
        RagChunkingStrategyFactory factory = new RagChunkingStrategyFactory(
                List.of((RagChunkingStrategy) new TokenRagChunkingStrategy()));
        return new RagIngestionServiceImpl(registry, factory, new RagChunkIdFactory(), vectorStore,
                Clock.systemUTC(), new NoopRagMetricsRecorder());
    }

    private static RagIngestionCommand command(ExtractedDocumentBO document) {
        return new RagIngestionCommand("kb-1", "doc-1", "openai-small",
                new RagChunkingConfigDTO(TOKEN, 40, 0, 1, null), document, Map.of());
    }

    private static RagDocumentExtractor countingExtractor(AtomicInteger extractions) {
        return new RagDocumentExtractor() {
            @Override
            public boolean supports(String mimeType, String fileName) {
                return true;
            }

            @Override
            public ExtractedDocumentBO extract(InputStream content, String mimeType, String fileName) {
                extractions.incrementAndGet();
                return new ExtractedDocumentBO(read(content), fileName, "text/plain", null);
            }
        };
    }

    private static InputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    private static String read(InputStream content) {
        try {
            return new String(content.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
