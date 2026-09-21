package top.egon.cola.archetype.source.agent.common.error;

import top.egon.cola.component.common.core.enums.ErrorStatus;

/**
 * Stable, safe error codes shared by the knowledge application, adapter and ingestion path.
 *
 * <p>Every code carries the message a client may see and whether a retry can help. The messages are
 * summaries, never vendor text: a code that reaches a response body or a document row must not leak
 * an endpoint, a key or a payload. The String {@link #getStatus()} stays the constant name, so the
 * published wire vocabulary is unchanged by the common status contract.
 */
public enum KnowledgeErrorCodeEnum implements ErrorStatus {

    KNOWLEDGE_VALIDATION_ERROR(0, "validation failed", false),
    KNOWLEDGE_NOT_ACCEPTABLE(1, "requested representation is not acceptable", false),
    KNOWLEDGE_BASE_NOT_FOUND(2, "knowledge base was not found", false),
    KNOWLEDGE_BASE_CODE_CONFLICT(3, "knowledge base code already exists", false),
    KNOWLEDGE_BASE_BUSY(4, "knowledge base still has documents in progress", false),
    KNOWLEDGE_DOCUMENT_NOT_FOUND(5, "knowledge document was not found", false),
    KNOWLEDGE_DOCUMENT_BUSY(6, "knowledge document is still in progress", false),
    KNOWLEDGE_CONTENT_MISSING(7, "the document has no stored text to embed", false),
    KNOWLEDGE_IMMUTABLE_FIELD(8, "a create-only field was supplied", false),
    KNOWLEDGE_MODEL_NOT_REGISTERED(9, "knowledge embedding model is not registered", false),
    KNOWLEDGE_EXTRACTOR_MISSING(10, "no document extractor is available for the format", false),
    KNOWLEDGE_FILE_TOO_LARGE(11, "the uploaded file exceeds the size limit", false),
    KNOWLEDGE_EMBEDDING_FAILED(12, "document ingestion failed", true),
    KNOWLEDGE_CAPACITY_EXHAUSTED(13, "knowledge capacity is exhausted", true),
    KNOWLEDGE_DEPENDENCY_UNAVAILABLE(14, "knowledge dependency is unavailable", true),
    KNOWLEDGE_INTERNAL_ERROR(15, "knowledge execution failed", false);

    private final int code;

    private final String safeMessage;

    private final boolean retryable;

    KnowledgeErrorCodeEnum(int code, String safeMessage, boolean retryable) {
        this.code = code;
        this.safeMessage = safeMessage;
        this.retryable = retryable;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return safeMessage;
    }

    @Override
    public String getStatus() {
        return name();
    }

    /** @return the stable String code published on the wire, unchanged by the common contract */
    public String code() {
        return getStatus();
    }

    /** @return the client-safe summary, which is also the common enum message */
    public String safeMessage() {
        return safeMessage;
    }

    /** @return whether the caller may retry the rejected request */
    public boolean retryable() {
        return retryable;
    }
}
