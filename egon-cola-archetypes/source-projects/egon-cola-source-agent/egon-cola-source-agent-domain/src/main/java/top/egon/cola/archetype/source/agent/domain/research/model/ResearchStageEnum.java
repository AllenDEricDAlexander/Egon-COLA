package top.egon.cola.archetype.source.agent.domain.research.model;

import top.egon.cola.component.common.core.enums.EgonEnum;

/** Safe stage vocabulary for planning, parallel research and synthesis. */
public enum ResearchStageEnum implements EgonEnum {

    PLANNING(0, "planning the research outline"),

    EVIDENCE_RESEARCH(1, "researching supporting evidence"),

    COUNTERPOINT_RESEARCH(2, "researching counterpoints"),

    FRESHNESS_RESEARCH(3, "researching recency"),

    SYNTHESIS(4, "synthesizing the report"),

    COMPLETED(5, "research completed"),

    FAILED(6, "research failed");

    private final int code;

    private final String message;

    ResearchStageEnum(int code, String message) {
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
