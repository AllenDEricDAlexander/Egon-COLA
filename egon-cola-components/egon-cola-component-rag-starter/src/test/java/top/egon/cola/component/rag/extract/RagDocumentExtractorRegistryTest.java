package top.egon.cola.component.rag.extract;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.rag.exception.RagExtractorConflictException;
import top.egon.cola.component.rag.exception.RagExtractorMissingException;
import top.egon.cola.component.rag.model.ExtractedDocumentBO;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Locks priority routing, ambiguity rejection and the fail-closed missing-extractor behaviour. */
class RagDocumentExtractorRegistryTest {

    @Test
    void selects_lowest_order_when_multiple_support() {
        RagDocumentExtractorRegistry registry = new RagDocumentExtractorRegistry(List.of(
                stub("slow", 20, Set.of("text/plain")),
                stub("fast", 10, Set.of("text/plain"))));

        assertThat(registry.route("text/plain", "a.txt").name()).isEqualTo("fast");
    }

    @Test
    void routes_when_mime_and_file_name_are_null() {
        RagDocumentExtractorRegistry registry = new RagDocumentExtractorRegistry(List.of(
                stub("plain", 100, Set.of("text/plain")),
                catchAllStub("catch-all", 900)));

        assertThat(registry.route(null, null).name()).isEqualTo("catch-all");
    }

    @Test
    void fails_when_no_extractor_supports_the_format() {
        RagDocumentExtractorRegistry registry = new RagDocumentExtractorRegistry(List.of(
                stub("plain", 100, Set.of("text/plain"))));

        assertThatThrownBy(() -> registry.route("application/xml", "a.xml"))
                .isInstanceOf(RagExtractorMissingException.class)
                .hasMessageContaining("text/plain");
    }

    @Test
    void fails_when_two_extractors_share_order_and_capability() {
        List<RagDocumentExtractor> ambiguous = List.of(
                stub("a", 100, Set.of("text/plain")),
                stub("b", 100, Set.of("text/plain")));

        assertThatThrownBy(() -> new RagDocumentExtractorRegistry(ambiguous))
                .isInstanceOf(RagExtractorConflictException.class)
                .hasMessageContaining("a")
                .hasMessageContaining("b");
    }

    @Test
    void reports_registered_mime_capabilities() {
        RagDocumentExtractorRegistry registry = new RagDocumentExtractorRegistry(List.of(
                stub("plain", 100, Set.of("text/plain", "application/json"))));

        assertThat(registry.registeredMimeCapabilities()).contains("text/plain", "application/json");
    }

    private static RagDocumentExtractor stub(String name, int order, Set<String> mimeTypes) {
        return new RagDocumentExtractor() {
            @Override
            public boolean supports(String mimeType, String fileName) {
                return mimeType != null && mimeTypes.contains(mimeType.toLowerCase());
            }

            @Override
            public ExtractedDocumentBO extract(InputStream content, String mimeType, String fileName) {
                return new ExtractedDocumentBO("stub", null, mimeType, null);
            }

            @Override
            public int order() {
                return order;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public Set<String> declaredMimeTypes() {
                return mimeTypes;
            }
        };
    }

    /** Lowest-priority implementation that accepts anything, including null hints. */
    private static RagDocumentExtractor catchAllStub(String name, int order) {
        return new RagDocumentExtractor() {
            @Override
            public boolean supports(String mimeType, String fileName) {
                return true;
            }

            @Override
            public ExtractedDocumentBO extract(InputStream content, String mimeType, String fileName) {
                return new ExtractedDocumentBO("stub", null, null, null);
            }

            @Override
            public int order() {
                return order;
            }

            @Override
            public String name() {
                return name;
            }
        };
    }
}
