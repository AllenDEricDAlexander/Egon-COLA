package top.egon.cola.component.rag.exception;

/** Chunking failed inside the selected strategy. */
public class RagChunkingException extends RagException {

    public RagChunkingException(String safeMessage) {
        super("RAG_CHUNKING", safeMessage);
    }

    public RagChunkingException(String safeMessage, Throwable cause) {
        super("RAG_CHUNKING", safeMessage, cause);
    }
}
