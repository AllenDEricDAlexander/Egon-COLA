package top.egon.cola.component.gateway.admin.mcp.domain.enums;


/**
 * 中文说明：{@code McpCapabilityKindEnum} 是枚举类型，位于当前 Gateway 模块的相关包中，负责CapabilityKind相关的职责与边界。
 * English summary: {@code McpCapabilityKindEnum} is an enumeration in the current Gateway module; it owns the capability kind-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public enum McpCapabilityKindEnum {
    /**
     * 中文说明：表示 资源 这一固定值；它属于 {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value resource; it is a state, type, or protocol value of {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum}; do not couple callers to its representation when the owning type exposes an API.
     */
    RESOURCE("gateway_mcp_resource_draft", "resource_name"),
    /**
     * 中文说明：表示 资源模板 这一固定值；它属于 {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value resource template; it is a state, type, or protocol value of {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum}; do not couple callers to its representation when the owning type exposes an API.
     */
    RESOURCE_TEMPLATE(
            "gateway_mcp_resource_template_draft",
            "template_name"
    ),
    /**
     * 中文说明：表示 提示词 这一固定值；它属于 {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value prompt; it is a state, type, or protocol value of {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum}; do not couple callers to its representation when the owning type exposes an API.
     */
    PROMPT("gateway_mcp_prompt_draft", "prompt_name"),
    /**
     * 中文说明：表示 任务策略 这一固定值；它属于 {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value task policy; it is a state, type, or protocol value of {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum}; do not couple callers to its representation when the owning type exposes an API.
     */
    TASK_POLICY("gateway_mcp_task_policy_draft", "tool_name"),
    /**
     * 中文说明：表示 APPBINDING 这一固定值；它属于 {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value app binding; it is a state, type, or protocol value of {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum}; do not couple callers to its representation when the owning type exposes an API.
     */
    APP_BINDING("gateway_mcp_app_binding_draft", "tool_name");

    /**
     * 中文说明：保存 table 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by table; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String table;

    /**
     * 中文说明：保存 nameColumn 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by name column; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String nameColumn;

    /**
     * 中文说明：创建 {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param table 参数 table；parameter table。
     * @param nameColumn 参数 nameColumn；parameter name column。
     */
    McpCapabilityKindEnum(String table, String nameColumn) {
        this.table = table;
        this.nameColumn = nameColumn;
    }

    /**
     * 中文说明：执行 table 操作；该方法是 {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the table operation; this method is the invocation entry point on {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum.table(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 table 的处理结果；returns the result of the operation.
     */
    public String table() {
        return table;
    }

    /**
     * 中文说明：执行 nameColumn 操作；该方法是 {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the name column operation; this method is the invocation entry point on {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum.nameColumn(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 nameColumn 的处理结果；returns the result of the operation.
     */
    public String nameColumn() {
        return nameColumn;
    }
}
