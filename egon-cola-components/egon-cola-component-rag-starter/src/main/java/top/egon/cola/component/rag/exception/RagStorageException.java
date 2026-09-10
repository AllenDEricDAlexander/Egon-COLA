package top.egon.cola.component.rag.exception;

/** The document storage call failed. */
public class RagStorageException extends RagException {

    public RagStorageException(String safeMessage) {
        super("RAG_STORAGE", safeMessage);
    }

    public RagStorageException(String safeMessage, Throwable cause) {
        super("RAG_STORAGE", safeMessage, cause);
    }
}
