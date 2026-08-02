package top.egon.cola.component.gateway.contract.mcp.rule;

import java.util.Map;
import java.util.Set;

public record McpRuntimeTool(
        String toolId,
        String serverCode,
        String name,
        String description,
        String sourceType,
        String operationId,
        String remoteMountId,
        String inputSchema,
        String outputSchema,
        Map<String, String> argumentBindings,
        Map<String, String> resultBindings,
        Map<String, String> annotations,
        Set<String> requiredPermissions,
        String riskLevel,
        boolean idempotent,
        boolean enabled
) {

    public McpRuntimeTool {
        toolId = McpContractSupport.required(toolId, "toolId");
        serverCode = McpContractSupport.required(serverCode, "serverCode");
        name = McpContractSupport.required(name, "name");
        description = McpContractSupport.optional(description);
        sourceType = McpContractSupport.required(sourceType, "sourceType");
        operationId = McpContractSupport.optional(operationId);
        remoteMountId = McpContractSupport.optional(remoteMountId);
        inputSchema = McpContractSupport.optional(inputSchema);
        outputSchema = McpContractSupport.optional(outputSchema);
        argumentBindings = McpContractSupport.sortedMap(argumentBindings);
        resultBindings = McpContractSupport.sortedMap(resultBindings);
        annotations = McpContractSupport.sortedMap(annotations);
        requiredPermissions = McpContractSupport.sortedStrings(
                requiredPermissions
        );
        riskLevel = McpContractSupport.required(riskLevel, "riskLevel");
    }
}
