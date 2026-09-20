package top.egon.cola.component.rag.common.exception;

/** A caller-supplied value violates the component contract. */
public class RagValidationException extends RagException {

    public RagValidationException(String safeMessage) {
        super("RAG_VALIDATION", safeMessage);
    }
}
