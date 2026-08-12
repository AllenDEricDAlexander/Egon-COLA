package top.egon.cola.component.gateway.mcp.server.handler;

import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeServer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 中文说明：{@code McpServerDescription} 是类型，位于当前 Gateway 模块的相关包中，负责MCP服务器Description相关的职责与边界。
 * English summary: {@code McpServerDescription} is a type in the current Gateway module; it owns the mcp server description-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
final class McpServerDescription {

    /**
     * 中文说明：创建 {@code McpServerDescription} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpServerDescription} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     */
    private McpServerDescription() {
    }

    /**
     * 中文说明：执行 describe 操作；该方法是 {@code McpServerDescription} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the describe operation; this method is the invocation entry point on {@code McpServerDescription} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerDescription.describe(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param server 参数 服务器；parameter server。
     * @return 返回 describe 的处理结果；returns the result of the operation.
     */
    static Map<String, Object> describe(McpRuntimeServer server) {
        LinkedHashMap<String, Object> description = new LinkedHashMap<>();
        description.put("code", server.serverCode());
        description.put("name", server.name());
        if (server.description() != null) {
            description.put("description", server.description());
        }
        if (server.instructions() != null) {
            description.put("instructions", server.instructions());
        }
        description.put("resourceUri", server.resourceUri());
        return Collections.unmodifiableMap(description);
    }

    /**
     * 中文说明：执行 result 操作；该方法是 {@code McpServerDescription} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the result operation; this method is the invocation entry point on {@code McpServerDescription} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerDescription.result(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param context 参数 context；parameter context。
     * @return 返回 result 的处理结果；returns the result of the operation.
     */
    static Map<String, Object> result(McpRequestContextView context) {
        return Map.of(
                "protocolVersion", context.protocolVersion(),
                "server", describe(context.server()),
                "capabilities", Map.of(
                        "tools", Map.of("listChanged", true),
                        "resources", Map.of("subscribe", true),
                        "prompts", Map.of("listChanged", true),
                        "tasks", Map.of("durable", true),
                        "apps", Map.of("uiResources", true)
                )
        );
    }

    /**
     * 中文说明：{@code McpRequestContextView} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责MCP请求ContextView相关的职责与边界。
     * English summary: {@code McpRequestContextView} is an immutable data carrier in the current Gateway module; it owns the mcp request context view-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param server 参数 服务器；parameter server。
     * @param protocolVersion 参数 protocolVersion；parameter protocol version。
     */
    record McpRequestContextView(
            /**
             * 中文说明：保存 服务器 对应的状态、依赖或配置值；字段类型为 {@code McpRuntimeServer}，由 {@code McpServerDescription.McpRequestContextView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server; its type is {@code McpRuntimeServer}, and {@code McpServerDescription.McpRequestContextView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpServerDescription.McpRequestContextView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerDescription.McpRequestContextView}; do not couple callers to its representation when the owning type exposes an API.
             */
            McpRuntimeServer server,
            /**
             * 中文说明：保存 protocolVersion 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpServerDescription.McpRequestContextView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by protocol version; its type is {@code String}, and {@code McpServerDescription.McpRequestContextView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpServerDescription.McpRequestContextView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerDescription.McpRequestContextView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String protocolVersion) {
    }
}
