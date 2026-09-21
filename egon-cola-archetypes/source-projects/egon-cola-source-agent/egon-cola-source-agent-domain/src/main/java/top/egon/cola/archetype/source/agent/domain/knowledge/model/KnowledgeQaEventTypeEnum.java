package top.egon.cola.archetype.source.agent.domain.knowledge.model;

import top.egon.cola.component.common.core.enums.EgonEnum;

/**
 * Public lifecycle event variants of one knowledge base question and answer.
 *
 * <p>Frozen by the SSE contract: the wire names are the values of this enum, and a new variant
 * needs an explicit version decision rather than an added constant.
 */
public enum KnowledgeQaEventTypeEnum implements EgonEnum {

    /** Retrieval finished and generation is about to start; carries the references. */
    STARTED(0, "answer generation started"),

    /** One text increment of the answer. */
    PROGRESS(1, "answer increment"),

    /** Terminal success: the complete answer and the same references as {@code STARTED}. */
    COMPLETED(2, "answer completed"),

    /** Terminal failure after the stream was established. */
    FAILED(3, "answer failed");

    private final int code;

    private final String message;

    KnowledgeQaEventTypeEnum(int code, String message) {
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
