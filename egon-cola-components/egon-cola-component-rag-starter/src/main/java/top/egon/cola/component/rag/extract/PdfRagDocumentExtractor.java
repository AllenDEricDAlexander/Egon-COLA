package top.egon.cola.component.rag.extract;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import top.egon.cola.component.rag.exception.RagExtractionException;
import top.egon.cola.component.rag.model.ExtractedDocumentBO;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PDF extraction backed by the optional {@code spring-ai-pdf-document-reader}.
 *
 * <p>Only instantiated when that artifact is on the class path; its bean is declared with a
 * matching {@code @ConditionalOnClass} so a missing reader degrades to "no extractor for PDF"
 * instead of a start-up failure.
 */
@Slf4j
public class PdfRagDocumentExtractor implements RagDocumentExtractor {

    private static final Set<String> MIME_TYPES = Set.of("application/pdf");

    @Override
    public boolean supports(String mimeType, String fileName) {
        String normalizedMime = mimeType == null ? null : mimeType.trim().toLowerCase();
        String extension = extensionOf(fileName);
        return MIME_TYPES.contains(normalizedMime) || ".pdf".equals(extension);
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public String name() {
        return "pdfRagDocumentExtractor";
    }

    @Override
    public Set<String> declaredMimeTypes() {
        return MIME_TYPES;
    }

    @Override
    public ExtractedDocumentBO extract(InputStream content, String mimeType, String fileName) {
        try {
            List<Document> pages = new PagePdfDocumentReader(new ByteArrayResource(content.readAllBytes())).get();
            String text = pages.stream().map(Document::getText).collect(Collectors.joining("\n"));
            return new ExtractedDocumentBO(text, titleFrom(fileName), "application/pdf",
                    Map.of("pageCount", String.valueOf(pages.size())));
        } catch (IOException | RuntimeException exception) {
            throw new RagExtractionException("failed to read pdf document", exception);
        }
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
