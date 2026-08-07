package top.egon.cola.component.gateway.contract.mcp.rule;

import java.util.Map;
import java.util.Set;

/**
 * MCP Tool 的运行时定义。
 *
 * <p>Tool 可以引用本地 HTTP/RPC Operation，也可以来自远程挂载；自动投影的工具通过
 * {@code operationId} 复用 Gateway 接口的输入输出 Schema 和权限语义。
 */
public record McpRuntimeTool(
        String toolId,
        String serverCode,
        String name,
        String description,
        String sourceType,
        String operationId,
        String operationProtocol,
        String remoteMountId,
        String inputSchema,
        String outputSchema,
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
        operationProtocol = McpContractSupport.optional(operationProtocol);
        remoteMountId = McpContractSupport.optional(remoteMountId);
        inputSchema = McpContractSupport.optional(inputSchema);
        outputSchema = McpContractSupport.optional(outputSchema);
        annotations = McpContractSupport.sortedMap(annotations);
        requiredPermissions = McpContractSupport.sortedStrings(
                requiredPermissions
        );
        riskLevel = McpContractSupport.required(riskLevel, "riskLevel");
    }
}
