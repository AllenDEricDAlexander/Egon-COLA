package top.egon.cola.component.gateway.engine.mcp;

import java.net.URI;

/**
 * 为持久化 MCP 任务提供绑定到精确租户与目标 Resource 的 SERVICE Token。
 * Supplies SERVICE tokens bound to the exact tenant and target Resource for durable MCP tasks.
 * 补充说明 / Supplementary summary: {@code McpTaskServiceTokenSupplier} 是接口契约，位于当前 Gateway 模块的相关包中，负责MCP任务服务TokenSupplier相关的职责与边界。
 * English supplement: {@code McpTaskServiceTokenSupplier} is an interface contract in the current Gateway module; it owns the mcp task service token supplier-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@FunctionalInterface
public interface McpTaskServiceTokenSupplier {

    /**
     * 获取不带 Bearer 前缀的短期 SERVICE Access Token。
     * Obtains a short-lived SERVICE access token without the Bearer prefix.
     *
     * @param tenantId 任务的精确租户；exact task tenant
     * @param resourceUri 目标 MCP Provider Resource URI；target MCP Provider Resource URI
     * @return SERVICE Access Token；SERVICE access token
     * 补充说明 / Supplementary summary: 执行 issue 操作；该方法是 {@code McpTaskServiceTokenSupplier} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English supplement: Executes the issue operation; this method is the invocation entry point on {@code McpTaskServiceTokenSupplier} and performs the corresponding runtime, management, or protocol work.
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskServiceTokenSupplier.issue(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    String issue(String tenantId, URI resourceUri);
}
