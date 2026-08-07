package top.egon.cola.component.gateway.contract.rule;

import top.egon.cola.component.gateway.contract.mcp.rule.McpRuleContent;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Gateway Engine 一次发布所需的完整规则集合。
 *
 * <p>内容包含操作、路由、各类治理策略、RPC Descriptor 和 MCP 规则；所有列表在构造时按稳定
 * 标识排序，保证快照摘要和发布结果可重复。
 */
public record GatewayRuleContent(
        String gatewayGroupId,
        String gatewayGroupCode,
        String env,
        String namespace,
        List<GatewayRuntimeOperation> operations,
        List<GatewayRuntimeRoute> routes,
        List<GatewayRuntimePolicy> providerPolicies,
        List<GatewayRuntimePolicy> trafficPolicies,
        List<GatewayRuntimePolicy> securityPolicies,
        List<GatewayRuntimePolicy> corsPolicies,
        List<GatewayRpcDescriptor> rpcDescriptors,
        McpRuleContent mcp
) {

    public GatewayRuleContent {
        gatewayGroupId = required(gatewayGroupId, "gatewayGroupId");
        gatewayGroupCode = required(gatewayGroupCode, "gatewayGroupCode");
        env = required(env, "env");
        namespace = required(namespace, "namespace");
        operations = sorted(
                operations,
                Comparator.comparing(GatewayRuntimeOperation::operationId)
        );
        routes = sorted(
                routes,
                Comparator.comparing(GatewayRuntimeRoute::routeId)
        );
        providerPolicies = sorted(
                providerPolicies,
                Comparator.comparing(GatewayRuntimePolicy::policyId)
        );
        trafficPolicies = sorted(
                trafficPolicies,
                Comparator.comparing(GatewayRuntimePolicy::policyId)
        );
        securityPolicies = sorted(
                securityPolicies,
                Comparator.comparing(GatewayRuntimePolicy::policyId)
        );
        corsPolicies = sorted(
                corsPolicies,
                Comparator.comparing(GatewayRuntimePolicy::policyId)
        );
        rpcDescriptors = sorted(
                rpcDescriptors,
                Comparator.comparing(GatewayRpcDescriptor::descriptorId)
        );
        mcp = mcp == null ? McpRuleContent.empty() : mcp;
    }

    public GatewayRuleContent(
            String gatewayGroupId,
            String gatewayGroupCode,
            String env,
            String namespace,
            List<GatewayRuntimeOperation> operations,
            List<GatewayRuntimeRoute> routes,
            List<GatewayRuntimePolicy> providerPolicies,
            List<GatewayRuntimePolicy> trafficPolicies,
            List<GatewayRuntimePolicy> securityPolicies,
            List<GatewayRuntimePolicy> corsPolicies,
            List<GatewayRpcDescriptor> rpcDescriptors) {
        this(
                gatewayGroupId,
                gatewayGroupCode,
                env,
                namespace,
                operations,
                routes,
                providerPolicies,
                trafficPolicies,
                securityPolicies,
                corsPolicies,
                rpcDescriptors,
                McpRuleContent.empty()
        );
    }

    private static <T> List<T> sorted(
            List<T> source,
            Comparator<T> comparator) {
        return Objects.requireNonNull(source, "rule list")
                .stream()
                .sorted(comparator)
                .toList();
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
