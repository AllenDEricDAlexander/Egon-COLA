package top.egon.cola.archetype.source.agent.domain.research.model;

import top.egon.cola.component.common.core.enums.EgonEnum;

/** Public lifecycle event variants emitted by a Deep Research run. */
public enum ResearchEventTypeEnum implements EgonEnum {

    STARTED(0, "research run started"),

    PROGRESS(1, "research stage progress"),

    COMPLETED(2, "research run completed"),

    FAILED(3, "research run failed");

    private final int code;

    private final String message;

    ResearchEventTypeEnum(int code, String message) {
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
