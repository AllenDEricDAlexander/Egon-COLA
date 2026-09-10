package top.egon.cola.component.rag.exception;

/** The vector store call failed. */
public class RagVectorStoreException extends RagException {

    public RagVectorStoreException(String safeMessage) {
        super("RAG_VECTOR_STORE", safeMessage);
    }

    public RagVectorStoreException(String safeMessage, Throwable cause) {
        super("RAG_VECTOR_STORE", safeMessage, cause);
    }
}
