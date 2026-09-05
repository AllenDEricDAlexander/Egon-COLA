package top.egon.cola.component.gateway.runtime.operation.fixture;

import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.runtime.provider.domain.RuntimeProviderPolicy;
import top.egon.cola.component.gateway.runtime.rule.domain.GatewayCompiledRulesDTO;
import top.egon.cola.component.gateway.runtime.traffic.domain.RuntimeTrafficPolicy;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 中文说明：Operation 测试只提供快照，由测试直接注入 ProviderSelector，不装配角色编译器。
 * English summary: Minimal immutable operation-test projection; tests supply provider selection directly.
 */
public record OperationCompiledRulesTestDTO(GatewayRuleSnapshot snapshot) implements GatewayCompiledRulesDTO {

    public OperationCompiledRulesTestDTO {
        snapshot = Objects.requireNonNull(snapshot, "snapshot");
    }

    @Override
    public String releaseId() {
        return snapshot.releaseId();
    }

    @Override
    public String ruleChecksum() {
        return snapshot.artifactSha256();
    }

    @Override
    public Set<ProviderServiceKey> providerServices() {
        return Set.of();
    }

    @Override
    public Map<String, RuntimeProviderPolicy> providerPolicies() {
        return Map.of();
    }

    @Override
    public Map<String, RuntimeTrafficPolicy> trafficPolicies() {
        return Map.of();
    }
}
