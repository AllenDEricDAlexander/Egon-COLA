package top.egon.cola.component.gateway.starter.annotation;

/**
 * Requiredness policy for a property in a generated gateway JSON Schema.
 *
 * <p>定义生成网关 JSON Schema 时属性的必需性策略。
 */
public enum GatewaySchemaRequired {
    /** Infer requiredness from Java Bean metadata and validation constraints. 根据 Java Bean 元数据和校验约束推断必需性。 */
    AUTO,

    /** Include the property in the containing schema's required set. 将属性加入所属模式的必需属性集合。 */
    REQUIRED,

    /**
     * Keep the property optional.
     *
     * <p>This value is rejected when the property also carries a required
     * validation constraint.
     *
     * <p>保持属性可选；若同时存在必需校验约束，则该值会被拒绝。
     */
    OPTIONAL
}
