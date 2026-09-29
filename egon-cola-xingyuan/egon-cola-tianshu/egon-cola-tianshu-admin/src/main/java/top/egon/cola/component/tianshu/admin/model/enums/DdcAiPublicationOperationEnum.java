package top.egon.cola.component.tianshu.admin.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import top.egon.cola.component.common.core.enums.EgonEnum;

public enum DdcAiPublicationOperationEnum implements EgonEnum {
    PUBLISH("PUBLISH", 401, "Publish"),
    ROLLBACK("ROLLBACK", 402, "Rollback"),
    PROMOTE("PROMOTE", 403, "Promote");

    @EnumValue
    private final String value;
    private final int code;
    private final String message;

    DdcAiPublicationOperationEnum(String value, int code, String message) {
        this.value = value;
        this.code = code;
        this.message = message;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @JsonCreator
    public static DdcAiPublicationOperationEnum fromValue(String value) {
        for (DdcAiPublicationOperationEnum operation : values()) {
            if (operation.value.equals(value)) return operation;
        }
        throw new IllegalArgumentException("Unknown AI publication operation");
    }
}
