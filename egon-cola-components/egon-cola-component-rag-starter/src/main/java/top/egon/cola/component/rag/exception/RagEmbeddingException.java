package top.egon.cola.component.rag.exception;

/** The embedding model call failed. */
public class RagEmbeddingException extends RagException {

    public RagEmbeddingException(String safeMessage) {
        super("RAG_EMBEDDING", safeMessage);
    }

    public RagEmbeddingException(String safeMessage, Throwable cause) {
        super("RAG_EMBEDDING", safeMessage, cause);
    }
}
