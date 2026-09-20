package top.egon.cola.component.bytecode.api.architecture;

import top.egon.cola.component.common.core.enums.EgonEnum;

public enum DependencyKind implements EgonEnum {
    EXTENDS(0, "EXTENDS"),
    IMPLEMENTS(1, "IMPLEMENTS"),
    FIELD(2, "FIELD"),
    PARAMETER(3, "PARAMETER"),
    RETURN(4, "RETURN"),
    THROWS(5, "THROWS"),
    SIGNATURE(6, "SIGNATURE"),
    ANNOTATION(7, "ANNOTATION"),
    NEW(8, "NEW"),
    ARRAY(9, "ARRAY"),
    CAST(10, "CAST"),
    INSTANCEOF(11, "INSTANCEOF"),
    FIELD_READ(12, "FIELD_READ"),
    FIELD_WRITE(13, "FIELD_WRITE"),
    METHOD_CALL(14, "METHOD_CALL"),
    CONSTRUCTOR_CALL(15, "CONSTRUCTOR_CALL"),
    METHOD_HANDLE(16, "METHOD_HANDLE"),
    INVOKEDYNAMIC(17, "INVOKEDYNAMIC"),
    LAMBDA_TARGET(18, "LAMBDA_TARGET"),
    CONSTANT_DYNAMIC(19, "CONSTANT_DYNAMIC"),
    CONSTANT_POOL(20, "CONSTANT_POOL");

    private final int code;
    private final String message;

    DependencyKind(int code, String message) {
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
