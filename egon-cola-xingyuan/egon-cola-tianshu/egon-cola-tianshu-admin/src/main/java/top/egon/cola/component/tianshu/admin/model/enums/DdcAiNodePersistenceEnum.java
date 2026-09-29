package top.egon.cola.component.tianshu.admin.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import top.egon.cola.component.common.core.enums.EgonEnum;

public enum DdcAiNodePersistenceEnum implements EgonEnum {
    DIRECTORY("DIRECTORY", 301, "Directory"),
    FILE("FILE", 302, "File");

    @EnumValue
    private final String value;
    private final int code;
    private final String message;

    DdcAiNodePersistenceEnum(String value, int code, String message) {
        this.value = value;
        this.code = code;
        this.message = message;
    }

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

    public static DdcAiNodePersistenceEnum fromValue(String value) {
        for (DdcAiNodePersistenceEnum node : values()) {
            if (node.value.equals(value)) return node;
        }
        throw new IllegalArgumentException("Unknown AI node persistence value");
    }
}
