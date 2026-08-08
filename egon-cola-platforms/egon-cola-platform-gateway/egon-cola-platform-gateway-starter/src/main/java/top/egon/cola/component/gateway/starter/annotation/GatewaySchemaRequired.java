package top.egon.cola.component.gateway.starter.annotation;

/**
 * Requiredness policy for a property in a generated gateway JSON Schema.
 */
public enum GatewaySchemaRequired {
    /** Infer requiredness from Java Bean metadata and validation constraints. */
    AUTO,

    /** Include the property in the containing schema's required set. */
    REQUIRED,

    /**
     * Keep the property optional.
     *
     * <p>This value is rejected when the property also carries a required
     * validation constraint.
     */
    OPTIONAL
}
