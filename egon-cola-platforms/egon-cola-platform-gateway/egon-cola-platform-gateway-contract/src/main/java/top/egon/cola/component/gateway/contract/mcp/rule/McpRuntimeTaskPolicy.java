package top.egon.cola.component.gateway.contract.mcp.rule;

/**
 * MCP 长任务执行策略，规定工具是否持久化、超时、结果保留和最大重试次数。
 */
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
