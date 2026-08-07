package top.egon.cola.component.gateway.contract.mcp.rule;

import java.util.Map;
import java.util.Set;

/**
 * MCP Resource 模板的运行时定义，用 URI 模板描述一组可参数化的资源。
 */
public record McpRuntimeResourceTemplate(
        String templateId,
        String serverCode,
        String name,
        String uriTemplate,
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

    public McpRuntimeResourceTemplate {
        templateId = McpContractSupport.required(templateId, "templateId");
        serverCode = McpContractSupport.required(serverCode, "serverCode");
        name = McpContractSupport.required(name, "name");
        uriTemplate = McpContractSupport.required(
                uriTemplate,
                "uriTemplate"
        );
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
