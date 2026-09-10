package top.egon.cola.component.rag.model;

import top.egon.cola.component.rag.exception.RagValidationException;

import java.util.Map;

/**
 * Result of turning a byte stream into text.
 *
 * <p>This is the contract between {@code RagDocumentExtractor} and everything downstream: the text
 * carries no identity, and {@code attributes} carries structural metadata such as page counts that
 * later merges with the caller's business attributes.
 */
public record ExtractedDocumentBO(String text, String title, String mimeType, Map<String, String> attributes) {

    public ExtractedDocumentBO {
        if (text == null) {
            throw new RagValidationException("extracted text must not be null");
        }
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
