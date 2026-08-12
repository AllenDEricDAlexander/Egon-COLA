package top.egon.cola.component.gateway.admin.mcp.application;

/**
 * 中文说明：{@code McpValidationException} 是异常类型，位于当前 Gateway 模块的相关包中，负责MCPValidationException相关的职责与边界。
 * English summary: {@code McpValidationException} is a mcp validation exception exception in the current Gateway module; it owns the mcp validation exception-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpValidationException extends RuntimeException {

    /**
     * 中文说明：保存 code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpValidationException} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by code; its type is {@code String}, and {@code McpValidationException} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpValidationException} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpValidationException}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String code;

    /**
     * 中文说明：保存 path 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpValidationException} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by path; its type is {@code String}, and {@code McpValidationException} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpValidationException} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpValidationException}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String path;

    /**
     * 中文说明：创建 {@code McpValidationException} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpValidationException} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param code 参数 code；parameter code。
     * @param path 参数 path；parameter path。
     * @param message 参数 消息；parameter message。
     */
    public McpValidationException(String code, String path, String message) {
        super(message);
        this.code = code;
        this.path = path;
    }

    /**
     * 中文说明：执行 code 操作；该方法是 {@code McpValidationException} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the code operation; this method is the invocation entry point on {@code McpValidationException} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpValidationException.code(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 code 的处理结果；returns the result of the operation.
     */
    public String code() {
        return code;
    }

    /**
     * 中文说明：执行 path 操作；该方法是 {@code McpValidationException} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the path operation; this method is the invocation entry point on {@code McpValidationException} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpValidationException.path(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 path 的处理结果；returns the result of the operation.
     */
    public String path() {
        return path;
    }
}
