package top.egon.cola.archetype.source.agent.domain.knowledge.model;

/**
 * Lifecycle of a knowledge base.
 *
 * <p>Values are persisted in {@code knowledge_base.status}; only the delete use case moves a base
 * out of {@link #ACTIVE}.
 */
public enum KnowledgeBaseStatusEnum {

    /** Visible and mutable. */
    ACTIVE,

    /** Soft deleted: no longer returned by queries and no longer accepting documents. */
    DELETED
}
