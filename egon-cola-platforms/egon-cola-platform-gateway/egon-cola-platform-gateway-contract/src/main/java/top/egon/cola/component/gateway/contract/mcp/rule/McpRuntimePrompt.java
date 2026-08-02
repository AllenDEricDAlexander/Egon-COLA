package top.egon.cola.component.gateway.contract.mcp.rule;

import java.util.List;
import java.util.Set;

public record McpRuntimePrompt(
        String promptId,
        String serverCode,
        String name,
        String description,
        String sourceType,
        String template,
        String operationId,
        String remoteMountId,
        List<String> arguments,
        Set<String> requiredPermissions,
        boolean enabled
) {

    public McpRuntimePrompt {
        promptId = McpContractSupport.required(promptId, "promptId");
        serverCode = McpContractSupport.required(serverCode, "serverCode");
        name = McpContractSupport.required(name, "name");
        description = McpContractSupport.optional(description);
        sourceType = McpContractSupport.required(sourceType, "sourceType");
        template = McpContractSupport.optional(template);
        operationId = McpContractSupport.optional(operationId);
        remoteMountId = McpContractSupport.optional(remoteMountId);
        arguments = McpContractSupport.sortedStrings(arguments);
        requiredPermissions = McpContractSupport.sortedStrings(
                requiredPermissions
        );
    }
}
