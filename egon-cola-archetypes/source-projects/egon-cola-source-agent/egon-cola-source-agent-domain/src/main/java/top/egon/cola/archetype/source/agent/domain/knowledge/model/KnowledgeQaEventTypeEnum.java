package top.egon.cola.archetype.source.agent.domain.knowledge.model;

/**
 * Public lifecycle event variants of one knowledge base question and answer.
 *
 * <p>Frozen by the SSE contract: the wire names are the values of this enum, and a new variant
 * needs an explicit version decision rather than an added constant.
 */
public enum KnowledgeQaEventTypeEnum {

    /** Retrieval finished and generation is about to start; carries the references. */
    STARTED,

    /** One text increment of the answer. */
    PROGRESS,

    /** Terminal success: the complete answer and the same references as {@code STARTED}. */
    COMPLETED,

    /** Terminal failure after the stream was established. */
    FAILED
}
