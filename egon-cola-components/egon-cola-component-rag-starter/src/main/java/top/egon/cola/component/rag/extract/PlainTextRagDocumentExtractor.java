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
 * Built-in extractor for the plain text family: text, CSV, JSON and XML.
 *
 * <p>Reads UTF-8 and does not rewrite the content, so chunking stays deterministic.
 */
@Slf4j
public class PlainTextRagDocumentExtractor implements RagDocumentExtractor {

    private static final Set<String> MIME_TYPES =
            Set.of("text/plain", "text/csv", "application/json", "application/xml", "text/xml");

    private static final Set<String> EXTENSIONS = Set.of(".txt", ".csv", ".json", ".xml", ".log");

    @Override
    public boolean supports(String mimeType, String fileName) {
        String normalizedMime = mimeType == null ? null : mimeType.trim().toLowerCase();
        String extension = extensionOf(fileName);
        return MIME_TYPES.contains(normalizedMime) || (extension != null && EXTENSIONS.contains(extension));
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public String name() {
        return "plainTextRagDocumentExtractor";
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
            throw new RagExtractionException("failed to read plain text document", exception);
        }
        String extension = extensionOf(fileName);
        String normalizedMime = mimeType == null ? null : mimeType.trim().toLowerCase();
        return new ExtractedDocumentBO(text, titleFrom(fileName), normalizedMime,
                extension == null ? Map.of() : Map.of("sourceExtension", extension));
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
