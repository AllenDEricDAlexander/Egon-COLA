package top.egon.cola.archetype.source.agent.domain.research.model;

/** Supported report language values at the API boundary. */
public enum ReportLanguageEnum {
    ZH_CN("zh-CN"),
    EN_US("en-US");

    private final String wireValue;

    ReportLanguageEnum(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }
}
