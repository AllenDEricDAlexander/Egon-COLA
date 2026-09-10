package top.egon.cola.component.rag.exception;

/** No registered extractor supports the requested document format. */
public class RagExtractorMissingException extends RagException {

    public RagExtractorMissingException(String safeMessage) {
        super("RAG_EXTRACTOR_MISSING", safeMessage);
    }
}
