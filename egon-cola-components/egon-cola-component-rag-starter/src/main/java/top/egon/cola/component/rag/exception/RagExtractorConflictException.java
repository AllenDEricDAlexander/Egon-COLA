package top.egon.cola.component.rag.exception;

/** Two extractors share the same order and overlapping support. */
public class RagExtractorConflictException extends RagException {

    public RagExtractorConflictException(String safeMessage) {
        super("RAG_EXTRACTOR_CONFLICT", safeMessage);
    }
}
