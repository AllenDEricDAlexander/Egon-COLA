package top.egon.cola.component.gateway.contract.mcp.rule;

import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;

import java.util.Set;

public record McpRuntimeServer(
        String serverId,
        String serverCode,
        String name,
        String description,
        String instructions,
        Set<McpProtocolDialect> dialects,
        String oauthAudience,
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
        oauthAudience = McpContractSupport.required(
                oauthAudience,
                "oauthAudience"
        );
        listCacheTtlSeconds = McpContractSupport.nonNegative(
                listCacheTtlSeconds,
                "listCacheTtlSeconds"
        );
    }
}
