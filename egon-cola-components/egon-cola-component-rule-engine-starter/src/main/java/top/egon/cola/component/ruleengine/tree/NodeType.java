package top.egon.cola.component.ruleengine.tree;

import top.egon.cola.component.common.core.enums.EgonEnum;

public enum NodeType implements EgonEnum {
    ROOT(0, "ROOT"),
    SWITCH(1, "SWITCH"),
    BIZ(2, "BIZ"),
    END(3, "END"),
    OTHER(4, "OTHER");

    private final int code;

    private final String message;

    NodeType(int code, String message) {
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
