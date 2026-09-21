package top.egon.cola.archetype.source.agent.domain.knowledge.model;

import top.egon.cola.component.common.core.enums.EgonEnum;

/**
 * Lifecycle of a knowledge base.
 *
 * <p>Values are persisted in {@code knowledge_base.status}; only the delete use case moves a base
 * out of {@link #ACTIVE}. Codes are declared so a reordering cannot renumber a stored status.
 */
public enum KnowledgeBaseStatusEnum implements EgonEnum {

    /** Visible and mutable. */
    ACTIVE(0, "knowledge base is active"),

    /** Soft deleted: no longer returned by queries and no longer accepting documents. */
    DELETED(1, "knowledge base is deleted");

    private final int code;

    private final String message;

    KnowledgeBaseStatusEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
