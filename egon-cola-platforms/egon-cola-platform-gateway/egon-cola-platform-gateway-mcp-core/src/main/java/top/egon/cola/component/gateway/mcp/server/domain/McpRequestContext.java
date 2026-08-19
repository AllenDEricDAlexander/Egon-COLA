package top.egon.cola.component.gateway.mcp.server.domain;

import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeServer;

import java.util.Map;
import java.util.Objects;

/**
 * 中文说明：{@code McpRequestContext} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责MCP请求Context相关的职责与边界。
 * English summary: {@code McpRequestContext} is an immutable data carrier in the current Gateway module; it owns the mcp request context-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param server 参数 服务器；parameter server。
 * @param dialect 参数 dialect；parameter dialect。
 * @param sessionId 参数 会话Id；parameter session id。
 * @param attributes 参数 attributes；parameter attributes。
 */
public record McpRequestContext(
        /**
         * 中文说明：保存 服务器 对应的状态、依赖或配置值；字段类型为 {@code McpRuntimeServer}，由 {@code McpRequestContext} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by server; its type is {@code McpRuntimeServer}, and {@code McpRequestContext} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpRequestContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRequestContext}; do not couple callers to its representation when the owning type exposes an API.
         */
        McpRuntimeServer server,
        /**
         * 中文说明：保存 dialect 对应的状态、依赖或配置值；字段类型为 {@code McpProtocolDialect}，由 {@code McpRequestContext} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by dialect; its type is {@code McpProtocolDialect}, and {@code McpRequestContext} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpRequestContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRequestContext}; do not couple callers to its representation when the owning type exposes an API.
         */
        McpProtocolDialect dialect,
        /**
         * 中文说明：保存 会话Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpRequestContext} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by session id; its type is {@code String}, and {@code McpRequestContext} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpRequestContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRequestContext}; do not couple callers to its representation when the owning type exposes an API.
         */
        String sessionId,
        /**
         * 中文说明：保存 attributes 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code McpRequestContext} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by attributes; its type is {@code Map<String, Object>}, and {@code McpRequestContext} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpRequestContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRequestContext}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, Object> attributes
) {

    /**
     * 中文说明：创建 {@code McpRequestContext} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpRequestContext} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param server 参数 服务器；parameter server。
     * @param dialect 参数 dialect；parameter dialect。
     * @param sessionId 参数 会话Id；parameter session id。
     * @param attributes 参数 attributes；parameter attributes。
     */
    public McpRequestContext {
        server = Objects.requireNonNull(server, "server");
        dialect = Objects.requireNonNull(dialect, "dialect");
        sessionId = sessionId == null || sessionId.isBlank()
                ? null
                : sessionId.trim();
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
