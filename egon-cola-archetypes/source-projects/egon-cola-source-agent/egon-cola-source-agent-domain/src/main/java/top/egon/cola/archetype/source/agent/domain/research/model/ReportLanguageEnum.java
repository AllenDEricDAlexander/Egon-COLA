package top.egon.cola.archetype.source.agent.domain.research.model;

import top.egon.cola.component.common.core.enums.EgonEnum;

/** Supported report language values at the API boundary. */
public enum ReportLanguageEnum implements EgonEnum {

    ZH_CN(0, "simplified chinese report", "zh-CN"),

    EN_US(1, "english report", "en-US");

    private final int code;

    private final String message;

    private final String wireValue;

    ReportLanguageEnum(int code, String message, String wireValue) {
        this.code = code;
        this.message = message;
        this.wireValue = wireValue;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    /** @return the language tag the published API accepts and returns */
    public String wireValue() {
        return wireValue;
    }
}
