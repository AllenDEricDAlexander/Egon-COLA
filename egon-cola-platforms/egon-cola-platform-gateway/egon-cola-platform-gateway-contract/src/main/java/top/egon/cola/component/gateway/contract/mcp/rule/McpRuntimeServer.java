package top.egon.cola.component.gateway.contract.mcp.rule;

import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;

import java.net.URI;
import java.util.Set;

/**
 * MCP Server 的运行时定义。
 *
 * <p>Server 是工具、资源和 Prompt 的命名及协议承载边界，同时携带平台级 OAuth 和缓存配置。
 */
public record McpRuntimeServer(
        String serverId,
        String serverCode,
        String name,
        String description,
        String instructions,
        Set<McpProtocolDialect> dialects,
        String resourceUri,
        long listCacheTtlSeconds,
        boolean enabled
) {

    public McpRuntimeServer {
        serverId = McpContractSupport.required(serverId, "serverId");
        serverCode = McpContractSupport.required(serverCode, "serverCode");
        name = McpContractSupport.required(name, "name");
        description = McpContractSupport.optional(description);
        instructions = McpContractSupport.optional(instructions);
        dialects = McpContractSupport.sortedEnums(dialects);
        if (dialects.isEmpty()) {
            throw new IllegalArgumentException("dialects must not be empty");
        }
        resourceUri = normalizedResourceUri(resourceUri);
        listCacheTtlSeconds = McpContractSupport.nonNegative(
                listCacheTtlSeconds,
                "listCacheTtlSeconds"
        );
    }

    private static String normalizedResourceUri(String value) {
        String required = McpContractSupport.required(value, "resourceUri");
        URI uri;
        try {
            uri = URI.create(required).normalize();
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException("resourceUri must be a valid URI", invalid);
        }
        if (!uri.isAbsolute() || uri.getFragment() != null) {
            throw new IllegalArgumentException(
                    "resourceUri must be absolute and must not contain a fragment"
            );
        }
        return uri.toString();
    }
}
