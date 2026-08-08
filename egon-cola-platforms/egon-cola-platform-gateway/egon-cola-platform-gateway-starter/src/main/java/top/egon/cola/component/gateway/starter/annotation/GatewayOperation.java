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
 *
 * <p>为网关操作方法补充目录、治理和 MCP 元数据；协议路由等事实仍由宿主框架发现。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GatewayOperation {

    /**
     * Catalogue-facing operation name.
     *
     * <p>目录中展示的操作名称。
     *
     * @return the declared name, or an empty string to use the Java method name
     */
    String name() default "";

    /**
     * Short summary suitable for operation listings.
     *
     * <p>适合显示在操作列表中的简短摘要。
     *
     * @return the operation summary, or an empty string when unspecified
     */
    String summary() default "";

    /**
     * Detailed description of the operation's behavior.
     *
     * <p>操作行为的详细说明。
     *
     * @return the operation description, or an empty string when unspecified
     */
    String description() default "";

    /**
     * Team or person responsible for the operation.
     *
     * <p>负责该操作的团队或人员标识。
     *
     * @return the owner identifier, or an empty string when unspecified
     */
    String owner() default "";

    /**
     * Whether the operation may be exposed outside the internal service boundary.
     *
     * <p>操作是否允许暴露到内部服务边界之外。
     *
     * @return {@code true} when external access is allowed
     */
    boolean externalAccessible() default false;

    /**
     * Whether repeating the operation with the same input is declared safe.
     *
     * <p>使用相同输入重复执行该操作是否被声明为安全。
     *
     * @return {@code true} when the operation is idempotent
     */
    boolean idempotent() default false;

    /**
     * Whether the operation is included in the group's MCP exposure metadata.
     *
     * <p>该操作是否纳入接口分组的 MCP 暴露元数据。
     *
     * @return {@code true} to register the operation as an MCP tool
     */
    boolean registerMcp() default false;

    /**
     * Name published for the MCP tool.
     *
     * <p>This value must be non-blank when {@link #registerMcp()} is enabled.
     *
     * <p>发布给 MCP 工具的名称；启用 MCP 注册时不能为空。
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
     * <p>发现阶段会对权限项进行去空格、校验、去重并排序后再发布。
     *
     * @return the required permission identifiers
     */
    String[] mcpRequiredPermissions() default {};

    /**
     * Governance risk level assigned to the MCP tool.
     *
     * <p>分配给 MCP 工具的治理风险等级。
     *
     * @return the MCP risk level
     */
    McpRiskLevel mcpRiskLevel() default McpRiskLevel.LOW;

    /**
     * Search and classification tags published with the operation.
     *
     * <p>随操作发布、用于搜索和分类的标签。
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
     * <p>HTTP 操作请求参数的完整模式声明；启用 MCP 时必须覆盖每个 HTTP 参数。
     * RPC 请求模式由 Protobuf 描述符生成，不应在此重复声明。
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
     * <p>HTTP 响应及业务载荷包装的模式声明；注册 MCP 的非 void HTTP 操作必须显式提供。
     * RPC 响应模式由 Protobuf 描述符生成，不应在此重复声明。
     *
     * @return the HTTP response schema declaration
     */
    GatewayResponseSchema responseSchema() default @GatewayResponseSchema;
}
