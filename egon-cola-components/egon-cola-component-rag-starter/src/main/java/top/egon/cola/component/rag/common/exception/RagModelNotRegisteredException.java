package top.egon.cola.component.rag.common.exception;

/** The requested logical embedding model is not registered. */
public class RagModelNotRegisteredException extends RagException {

    public RagModelNotRegisteredException(String safeMessage) {
        super("RAG_MODEL_NOT_REGISTERED", safeMessage);
    }
}
