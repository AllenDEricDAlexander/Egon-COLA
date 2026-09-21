package top.egon.cola.archetype.source.serviceopen.common.enums;

import top.egon.cola.component.common.core.enums.EgonEnum;

/** Boolean flag representation shared by the generated persistence model. */
public enum YesNoEnum implements EgonEnum {

    YES(0, "yes"),
    NO(1, "no");

    private final int code;

    private final String message;

    YesNoEnum(int code, String message) {
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

    public boolean isYes() {
        return this == YES;
    }
}
