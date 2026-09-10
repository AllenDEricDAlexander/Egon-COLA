package top.egon.cola.component.rag.extract;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.rag.exception.RagExtractionException;
import top.egon.cola.component.rag.exception.RagExtractorMissingException;
import top.egon.cola.component.rag.exception.RagValidationException;
import top.egon.cola.component.rag.execution.RagExtractionServiceImpl;
import top.egon.cola.component.rag.model.ExtractedDocumentBO;
import top.egon.cola.component.rag.model.RagExtractionCommand;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Locks the extraction orchestration: routing, propagation and result validation. */
class RagExtractionServiceImplTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-09-10T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void returns_extracted_document() {
        RagExtractionServiceImpl service = serviceWith(plainTextStub());

        ExtractedDocumentBO document = service.extract(command("a.txt", "text/plain", "hello"));

        assertThat(document.text()).isEqualTo("hello");
        assertThat(document.mimeType()).isEqualTo("text/plain");
    }

    @Test
    void propagates_extraction_failure_unchanged_and_without_adding_content() {
        RagExtractionException raised = new RagExtractionException("failed to parse document");
        RagExtractionServiceImpl service = serviceWith(failingStub(raised));

        assertThatThrownBy(() -> service.extract(command("a.txt", "text/plain", "SECRET-CONTENT")))
                .isSameAs(raised)
                .hasMessageNotContaining("SECRET-CONTENT");
    }

    @Test
    void fails_when_no_extractor_supports_the_format() {
        RagExtractionServiceImpl service = serviceWith(plainTextStub());

        assertThatThrownBy(() -> service.extract(command("a.xml", "application/xml", "<a/>")))
                .isInstanceOf(RagExtractorMissingException.class)
                .hasMessageContaining("text/plain");
    }

    @Test
    void rejects_null_content() {
        RagExtractionServiceImpl service = serviceWith(plainTextStub());

        assertThatThrownBy(() -> service.extract(new RagExtractionCommand("a.txt", "text/plain", null)))
                .isInstanceOf(RagValidationException.class);
    }

    @Test
    void rejects_reserved_keys_returned_by_an_extractor() {
        RagExtractionServiceImpl service = serviceWith(reservedKeyStub());

        assertThatThrownBy(() -> service.extract(command("a.txt", "text/plain", "hello")))
                .isInstanceOf(RagValidationException.class)
                .hasMessageContaining("documentId");
    }

    private static RagExtractionServiceImpl serviceWith(RagDocumentExtractor extractor) {
        return new RagExtractionServiceImpl(new RagDocumentExtractorRegistry(List.of(extractor)), FIXED_CLOCK);
    }

    private static RagExtractionCommand command(String fileName, String mimeType, String content) {
        return new RagExtractionCommand(fileName, mimeType, stream(content));
    }

    private static InputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    private static RagDocumentExtractor plainTextStub() {
        return new RagDocumentExtractor() {
            @Override
            public boolean supports(String mimeType, String fileName) {
                return "text/plain".equalsIgnoreCase(mimeType);
            }

            @Override
            public ExtractedDocumentBO extract(InputStream content, String mimeType, String fileName) {
                return new ExtractedDocumentBO(read(content), fileName, "text/plain", Map.of("source", "stub"));
            }

            @Override
            public Set<String> declaredMimeTypes() {
                return Set.of("text/plain");
            }
        };
    }

    private static RagDocumentExtractor failingStub(RagExtractionException raised) {
        return new RagDocumentExtractor() {
            @Override
            public boolean supports(String mimeType, String fileName) {
                return true;
            }

            @Override
            public ExtractedDocumentBO extract(InputStream content, String mimeType, String fileName) {
                throw raised;
            }

            @Override
            public int order() {
                return 10;
            }
        };
    }

    private static RagDocumentExtractor reservedKeyStub() {
        return new RagDocumentExtractor() {
            @Override
            public boolean supports(String mimeType, String fileName) {
                return true;
            }

            @Override
            public ExtractedDocumentBO extract(InputStream content, String mimeType, String fileName) {
                return new ExtractedDocumentBO("text", null, "text/plain", Map.of("documentId", "forged"));
            }

            @Override
            public int order() {
                return 10;
            }
        };
    }

    private static String read(InputStream content) {
        try {
            return new String(content.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new RagExtractionException("failed to read test stream", exception);
        }
    }
}
