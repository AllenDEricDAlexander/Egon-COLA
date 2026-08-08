package top.egon.cola.component.ddc.model.enums;

/**
 * DDC 支持的配置值类型。
 * / Configuration value types supported by DDC.
 */
public enum DdcValueType {
    /** 字符串值。 / String value. */
    STRING,

    /** 整数值。 / Integer value. */
    INTEGER,

    /** 长整数值。 / Long integer value. */
    LONG,

    /** 布尔值。 / Boolean value. */
    BOOLEAN,

    /** 双精度浮点值。 / Double-precision floating-point value. */
    DOUBLE,

    /** 任意精度十进制值。 / Arbitrary-precision decimal value. */
    DECIMAL,

    /** 枚举值。 / Enum value. */
    ENUM,

    /** 列表值。 / List value. */
    LIST,

    /** JSON 值。 / JSON value. */
    JSON
}
