package top.egon.cola.component.gateway.contract.mcp.rule;

public record McpRuntimeTaskPolicy(
        String taskPolicyId,
        String serverCode,
        String toolName,
        boolean durable,
        boolean inputAllowed,
        long executionTimeoutSeconds,
        long resultTtlSeconds,
        int maxAttempts,
        boolean enabled
) {

    public McpRuntimeTaskPolicy {
        taskPolicyId = McpContractSupport.required(
                taskPolicyId,
                "taskPolicyId"
        );
        serverCode = McpContractSupport.required(serverCode, "serverCode");
        toolName = McpContractSupport.required(toolName, "toolName");
        executionTimeoutSeconds = McpContractSupport.nonNegative(
                executionTimeoutSeconds,
                "executionTimeoutSeconds"
        );
        resultTtlSeconds = McpContractSupport.nonNegative(
                resultTtlSeconds,
                "resultTtlSeconds"
        );
        maxAttempts = McpContractSupport.positive(maxAttempts, "maxAttempts");
    }
}
