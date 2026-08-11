package top.egon.cola.component.gateway.starter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds gateway JSON Schema metadata to a Java property or request parameter.
 *
 * <p>Declarations discovered on a field, accessor, constructor parameter or
 * record component are merged for the logical property. Conflicting declarations
 * on those elements are rejected during schema discovery.
 *
 * <p>为 Java 属性或请求参数补充网关 JSON Schema 元数据；同一逻辑属性上的冲突声明会在发现阶段拒绝。
 */
@Target({
        ElementType.FIELD,
        ElementType.RECORD_COMPONENT,
        ElementType.METHOD,
        ElementType.PARAMETER
})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GatewaySchemaField {

    /**
     * Human-readable description published for the schema field.
     *
     * <p>发布到模式字段的人类可读描述。
     *
     * @return the description, or an empty string when unspecified
     */
    String description() default "";

    /**
     * JSON Schema type expected for the Java field.
     *
     * <p>An explicit value is validated against the inferred Java type; it does
     * not coerce an incompatible value.
     *
     * <p>Java 字段期望的 JSON Schema 类型；显式类型会校验 Java 类型兼容性，不会强制转换不兼容值。
     *
     * @return the declared schema type, or {@link GatewaySchemaType#AUTO} to infer
     *         it
     */
    GatewaySchemaType type() default GatewaySchemaType.AUTO;

    /**
     * JSON Schema format keyword published for the field.
     *
     * <p>发布到字段的 JSON Schema format 关键字。
     *
     * @return the format, or an empty string when unspecified
     */
    String format() default "";

    /**
     * Requiredness override for the schema property.
     *
     * <p>模式属性是否必需的覆盖策略。
     *
     * @return the requiredness policy
     */
    GatewaySchemaRequired required() default GatewaySchemaRequired.AUTO;

    /**
     * Example value encoded as text.
     *
     * <p>The value is parsed and validated against the effective schema type
     * during discovery.
     *
     * <p>以文本编码的示例值；发现期间会依据生效的模式类型解析并校验。
     *
     * @return the encoded example, or an empty string when unspecified
     */
    String example() default "";

    /**
     * Concrete implementation used to describe an abstract, interface or
     * {@link Object}-typed property.
     *
     * <p>For collection and map properties, this class describes the element or
     * value implementation. Incompatible or redundant implementations are
     * rejected.
     *
     * <p>用于描述抽象、接口或 {@link Object} 类型属性的具体实现；集合和映射属性中表示元素或值的实现类型。
     *
     * @return the concrete implementation, or {@link Void} when none is declared
     */
    Class<?> implementation() default Void.class;

    /**
     * Whether an {@link Object}-typed property, collection element, or map
     * value intentionally accepts arbitrary JSON.
     *
     * <p>This opt-in emits an unconstrained JSON Schema node only at the
     * annotated dynamic value boundary. Erased generic content remains
     * rejected by default.
     *
     * <p>是否明确允许 {@link Object} 类型的属性、集合元素或映射值承载任意 JSON；
     * 仅在显式标注的动态值边界输出无约束 Schema，默认仍拒绝擦除泛型。
     *
     * @return {@code true} when arbitrary JSON is intentional
     */
    boolean allowArbitraryJson() default false;
}
