package top.egon.cola.component.rag.exception;

/**
 * Base type for every failure raised by the RAG starter.
 *
 * <p>Every instance carries a stable machine-readable {@code code} and a {@code safeMessage} that
 * never contains document content, chunk text, vectors, provider payloads, endpoints or keys.
 */
public class RagException extends RuntimeException {

    private final String code;

    private final String safeMessage;

    public RagException(String code, String safeMessage) {
        super(safeMessage);
        this.code = code;
        this.safeMessage = safeMessage;
    }

    public RagException(String code, String safeMessage, Throwable cause) {
        super(safeMessage, cause);
        this.code = code;
        this.safeMessage = safeMessage;
    }

    public String code() {
        return code;
    }

    public String safeMessage() {
        return safeMessage;
    }
}
