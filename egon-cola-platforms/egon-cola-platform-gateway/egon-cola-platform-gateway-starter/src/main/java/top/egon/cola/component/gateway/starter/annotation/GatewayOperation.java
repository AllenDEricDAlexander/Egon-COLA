package top.egon.cola.component.gateway.starter.annotation;

import top.egon.cola.component.gateway.contract.mcp.rule.McpRiskLevel;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Supplies catalogue and MCP metadata for a gateway operation method.
 *
 * <p>Protocol-specific facts such as the HTTP route or RPC method identity are
 * discovered from the hosting framework. This annotation adds descriptive,
 * governance and schema metadata that those framework declarations do not own.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GatewayOperation {

    /**
     * Catalogue-facing operation name.
     *
     * @return the declared name, or an empty string to use the Java method name
     */
    String name() default "";

    /**
     * Short summary suitable for operation listings.
     *
     * @return the operation summary, or an empty string when unspecified
     */
    String summary() default "";

    /**
     * Detailed description of the operation's behavior.
     *
     * @return the operation description, or an empty string when unspecified
     */
    String description() default "";

    /**
     * Team or person responsible for the operation.
     *
     * @return the owner identifier, or an empty string when unspecified
     */
    String owner() default "";

    /**
     * Whether the operation may be exposed outside the internal service boundary.
     *
     * @return {@code true} when external access is allowed
     */
    boolean externalAccessible() default false;

    /**
     * Whether repeating the operation with the same input is declared safe.
     *
     * @return {@code true} when the operation is idempotent
     */
    boolean idempotent() default false;

    /**
     * Whether the operation is included in the group's MCP exposure metadata.
     *
     * @return {@code true} to register the operation as an MCP tool
     */
    boolean registerMcp() default false;

    /**
     * Name published for the MCP tool.
     *
     * <p>This value must be non-blank when {@link #registerMcp()} is enabled.
     *
     * @return the MCP tool name, or an empty string when MCP is not used
     */
    String mcpName() default "";

    /**
     * Permissions required to invoke the MCP tool.
     *
     * <p>During discovery, entries are trimmed, validated, deduplicated and sorted
     * before publication.
     *
     * @return the required permission identifiers
     */
    String[] mcpRequiredPermissions() default {};

    /**
     * Governance risk level assigned to the MCP tool.
     *
     * @return the MCP risk level
     */
    McpRiskLevel mcpRiskLevel() default McpRiskLevel.LOW;

    /**
     * Search and classification tags published with the operation.
     *
     * @return the operation tags
     */
    String[] tags() default {};

    /**
     * Complete schema declarations for the HTTP operation's request parameters.
     *
     * <p>Every HTTP parameter must be covered when MCP registration is enabled.
     * RPC request schemas are generated from Protobuf descriptors and must not be
     * declared here.
     *
     * @return the HTTP request schema declarations
     */
    GatewayRequestSchemaField[] requestSchemaFields() default {};

    /**
     * Schema declaration for the HTTP response and any business payload wrapper.
     *
     * <p>A non-void HTTP operation registered with MCP requires an explicit
     * declaration. RPC response schemas are generated from Protobuf descriptors
     * and must not be declared here.
     *
     * @return the HTTP response schema declaration
     */
    GatewayResponseSchema responseSchema() default @GatewayResponseSchema;
}
