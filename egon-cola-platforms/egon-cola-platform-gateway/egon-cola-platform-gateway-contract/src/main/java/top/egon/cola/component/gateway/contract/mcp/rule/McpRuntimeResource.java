package top.egon.cola.component.gateway.contract.mcp.rule;

import java.util.Map;
import java.util.Set;

public record McpRuntimeResource(
        String resourceId,
        String serverCode,
        String name,
        String uri,
        String description,
        String mimeType,
        String driverType,
        String operationId,
        String remoteMountId,
        Map<String, String> configuration,
        Set<String> requiredPermissions,
        long maxBytes,
        boolean enabled
) {

    public McpRuntimeResource {
        resourceId = McpContractSupport.required(resourceId, "resourceId");
        serverCode = McpContractSupport.required(serverCode, "serverCode");
        name = McpContractSupport.required(name, "name");
        uri = McpContractSupport.required(uri, "uri");
        description = McpContractSupport.optional(description);
        mimeType = McpContractSupport.required(mimeType, "mimeType");
        driverType = McpContractSupport.required(driverType, "driverType");
        operationId = McpContractSupport.optional(operationId);
        remoteMountId = McpContractSupport.optional(remoteMountId);
        configuration = McpContractSupport.sortedMap(configuration);
        requiredPermissions = McpContractSupport.sortedStrings(
                requiredPermissions
        );
        maxBytes = McpContractSupport.nonNegative(maxBytes, "maxBytes");
    }
}
