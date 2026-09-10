package top.egon.cola.component.rag.exception;

/** Document extraction failed inside the selected extractor. */
public class RagExtractionException extends RagException {

    public RagExtractionException(String safeMessage) {
        super("RAG_EXTRACTION", safeMessage);
    }

    public RagExtractionException(String safeMessage, Throwable cause) {
        super("RAG_EXTRACTION", safeMessage, cause);
    }
}
