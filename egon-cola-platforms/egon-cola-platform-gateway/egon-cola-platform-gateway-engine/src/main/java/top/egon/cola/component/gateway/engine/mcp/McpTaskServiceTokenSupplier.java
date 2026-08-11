package top.egon.cola.component.gateway.engine.mcp;

import java.net.URI;

/**
 * 为持久化 MCP 任务提供绑定到精确租户与目标 Resource 的 SERVICE Token。
 * Supplies SERVICE tokens bound to the exact tenant and target Resource for durable MCP tasks.
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
     */
    String issue(String tenantId, URI resourceUri);
}
