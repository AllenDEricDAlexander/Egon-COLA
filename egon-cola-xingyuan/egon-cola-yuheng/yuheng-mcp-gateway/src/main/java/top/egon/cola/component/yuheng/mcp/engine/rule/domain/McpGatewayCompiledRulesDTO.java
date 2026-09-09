package top.egon.cola.component.yuheng.mcp.engine.rule.domain;

import top.egon.cola.component.yuheng.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.yuheng.core.provider.ProviderServiceKey;
import top.egon.cola.component.yuheng.mcp.rule.domain.CompiledMcpRules;
import top.egon.cola.component.yuheng.runtime.provider.domain.RuntimeProviderPolicy;
import top.egon.cola.component.yuheng.runtime.rule.domain.GatewayCompiledRulesDTO;
import top.egon.cola.component.yuheng.runtime.traffic.domain.RuntimeTrafficPolicy;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 中文说明：MCP 角色的不可变编译快照，不包含 API/RPC 入口索引。
 * English summary: Immutable MCP projection without HTTP route or RPC ingress tables.
 * 用法 / Usage: Share snapshot identity with the common activation pipeline, not executable state.
 */
public record McpGatewayCompiledRulesDTO(
        GatewayRuleSnapshot snapshot,
        Set<ProviderServiceKey> providerServices,
        Map<String, RuntimeProviderPolicy> providerPolicies,
        Map<String, RuntimeTrafficPolicy> trafficPolicies,
        CompiledMcpRules mcpRules
) implements GatewayCompiledRulesDTO {

    public McpGatewayCompiledRulesDTO {
        snapshot = Objects.requireNonNull(snapshot, "snapshot");
        providerServices = Set.copyOf(Objects.requireNonNull(providerServices, "providerServices"));
        providerPolicies = Map.copyOf(Objects.requireNonNull(providerPolicies, "providerPolicies"));
        trafficPolicies = Map.copyOf(Objects.requireNonNull(trafficPolicies, "trafficPolicies"));
        mcpRules = Objects.requireNonNull(mcpRules, "mcpRules");
        if (!snapshot.content().mcp().equals(mcpRules.content())) {
            throw new IllegalArgumentException("MCP_RULE_COMPILE_FAILED: detached MCP projection");
        }
    }

    @Override
    public String releaseId() {
        return snapshot.releaseId();
    }

    @Override
    public String ruleChecksum() {
        return snapshot.artifactSha256();
    }
}
