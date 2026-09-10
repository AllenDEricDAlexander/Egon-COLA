package top.egon.cola.archetype.source.agent.common.error;

/**
 * Stable, safe error codes shared by the knowledge application, adapter and ingestion path.
 *
 * <p>Every code carries the message a client may see and whether a retry can help. The messages are
 * summaries, never vendor text: a code that reaches a response body or a document row must not leak
 * an endpoint, a key or a payload.
 */
public enum KnowledgeErrorCodeEnum {

    KNOWLEDGE_VALIDATION_ERROR("validation failed", false),
    KNOWLEDGE_NOT_ACCEPTABLE("requested representation is not acceptable", false),
    KNOWLEDGE_BASE_NOT_FOUND("knowledge base was not found", false),
    KNOWLEDGE_BASE_CODE_CONFLICT("knowledge base code already exists", false),
    KNOWLEDGE_BASE_BUSY("knowledge base still has documents in progress", false),
    KNOWLEDGE_DOCUMENT_NOT_FOUND("knowledge document was not found", false),
    KNOWLEDGE_DOCUMENT_BUSY("knowledge document is still in progress", false),
    KNOWLEDGE_CONTENT_MISSING("the document has no stored text to embed", false),
    KNOWLEDGE_IMMUTABLE_FIELD("a create-only field was supplied", false),
    KNOWLEDGE_MODEL_NOT_REGISTERED("knowledge embedding model is not registered", false),
    KNOWLEDGE_EXTRACTOR_MISSING("no document extractor is available for the format", false),
    KNOWLEDGE_FILE_TOO_LARGE("the uploaded file exceeds the size limit", false),
    KNOWLEDGE_EMBEDDING_FAILED("document ingestion failed", true),
    KNOWLEDGE_CAPACITY_EXHAUSTED("knowledge capacity is exhausted", true),
    KNOWLEDGE_DEPENDENCY_UNAVAILABLE("knowledge dependency is unavailable", true),
    KNOWLEDGE_INTERNAL_ERROR("knowledge execution failed", false);

    private final String safeMessage;
    private final boolean retryable;

    KnowledgeErrorCodeEnum(String safeMessage, boolean retryable) {
        this.safeMessage = safeMessage;
        this.retryable = retryable;
    }

    public String code() {
        return name();
    }

    public String safeMessage() {
        return safeMessage;
    }

    public boolean retryable() {
        return retryable;
    }
}
