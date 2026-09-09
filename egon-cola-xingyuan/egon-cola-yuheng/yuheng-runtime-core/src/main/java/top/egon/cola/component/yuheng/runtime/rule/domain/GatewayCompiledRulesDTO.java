package top.egon.cola.component.yuheng.runtime.rule.domain;

import top.egon.cola.component.yuheng.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.yuheng.core.provider.ProviderServiceKey;
import top.egon.cola.component.yuheng.runtime.provider.domain.RuntimeProviderPolicy;
import top.egon.cola.component.yuheng.runtime.traffic.domain.RuntimeTrafficPolicy;

import java.util.Map;
import java.util.Set;

/**
 * 中文说明：角色编译结果的最小共享视图；DDC 版本由激活状态单独维护。
 * English summary: Minimal immutable compiled-rule view shared by role-local runtimes.
 * 用法 / Usage: Implement with a validated immutable role record; checksum identifies the artifact.
 */
public interface GatewayCompiledRulesDTO {

    String releaseId();

    String ruleChecksum();

    GatewayRuleSnapshot snapshot();

    Set<ProviderServiceKey> providerServices();

    Map<String, RuntimeProviderPolicy> providerPolicies();

    Map<String, RuntimeTrafficPolicy> trafficPolicies();
}
