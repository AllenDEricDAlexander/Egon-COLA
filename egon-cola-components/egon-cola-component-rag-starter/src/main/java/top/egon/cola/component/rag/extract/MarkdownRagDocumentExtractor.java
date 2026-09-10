package top.egon.cola.component.rag.extract;

import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.rag.exception.RagExtractionException;
import top.egon.cola.component.rag.model.ExtractedDocumentBO;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

/**
 * Built-in extractor for Markdown.
 *
 * <p>Kept separate from the plain text extractor so its heading structure is preserved for the
 * {@code MARKDOWN_HEADING} chunking strategy. It does not rewrite the content: the original line
 * structure is what makes that strategy deterministic.
 */
@Slf4j
public class MarkdownRagDocumentExtractor implements RagDocumentExtractor {

    private static final Set<String> MIME_TYPES = Set.of("text/markdown");

    private static final Set<String> EXTENSIONS = Set.of(".md", ".markdown");

    @Override
    public boolean supports(String mimeType, String fileName) {
        String normalizedMime = mimeType == null ? null : mimeType.trim().toLowerCase();
        String extension = extensionOf(fileName);
        return MIME_TYPES.contains(normalizedMime) || (extension != null && EXTENSIONS.contains(extension));
    }

    @Override
    public int order() {
        return 50;
    }

    @Override
    public String name() {
        return "markdownRagDocumentExtractor";
    }

    @Override
    public Set<String> declaredMimeTypes() {
        return MIME_TYPES;
    }

    @Override
    public ExtractedDocumentBO extract(InputStream content, String mimeType, String fileName) {
        String text;
        try {
            text = new String(content.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new RagExtractionException("failed to read markdown document", exception);
        }
        long headingCount = text.lines()
                .filter(line -> line.stripLeading().startsWith("#"))
                .count();
        return new ExtractedDocumentBO(text, titleFrom(fileName), "text/markdown",
                Map.of("headingCount", String.valueOf(headingCount)));
    }

    private static String extensionOf(String fileName) {
        if (fileName == null) {
            return null;
        }
        String trimmed = fileName.trim().toLowerCase();
        int dot = trimmed.lastIndexOf('.');
        return dot < 0 ? null : trimmed.substring(dot);
    }

    private static String titleFrom(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        String trimmed = fileName.trim();
        int dot = trimmed.lastIndexOf('.');
        return dot <= 0 ? trimmed : trimmed.substring(0, dot);
    }
}
