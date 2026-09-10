package top.egon.cola.component.rag.extract;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import top.egon.cola.component.rag.exception.RagExtractionException;
import top.egon.cola.component.rag.model.ExtractedDocumentBO;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Catch-all extraction backed by the optional {@code spring-ai-tika-document-reader}.
 *
 * <p>Covers Office formats such as docx, xlsx and pptx. It accepts any input and sits at the lowest
 * priority, so it is reached only when no more specific extractor claims the format.
 */
@Slf4j
public class TikaRagDocumentExtractor implements RagDocumentExtractor {

    private static final int CATCH_ALL_ORDER = 900;

    @Override
    public boolean supports(String mimeType, String fileName) {
        return true;
    }

    @Override
    public int order() {
        return CATCH_ALL_ORDER;
    }

    @Override
    public String name() {
        return "tikaRagDocumentExtractor";
    }

    @Override
    public Set<String> declaredMimeTypes() {
        return Set.of();
    }

    @Override
    public ExtractedDocumentBO extract(InputStream content, String mimeType, String fileName) {
        try {
            String text = new TikaDocumentReader(new ByteArrayResource(content.readAllBytes())).get().stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n"));
            String normalizedMime = mimeType == null ? null : mimeType.trim().toLowerCase();
            return new ExtractedDocumentBO(text, titleFrom(fileName), normalizedMime,
                    normalizedMime == null ? Map.of() : Map.of("detectedMimeType", normalizedMime));
        } catch (IOException | RuntimeException exception) {
            throw new RagExtractionException("failed to read document with tika", exception);
        }
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
