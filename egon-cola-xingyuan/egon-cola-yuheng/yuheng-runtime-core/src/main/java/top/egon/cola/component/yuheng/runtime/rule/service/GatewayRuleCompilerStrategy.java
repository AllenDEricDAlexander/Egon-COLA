package top.egon.cola.component.yuheng.runtime.rule.service;

import top.egon.cola.component.yuheng.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.yuheng.runtime.rule.domain.GatewayCompiledRulesDTO;

/**
 * 中文说明：将同一发布快照编译为固定角色的本地不可变状态。
 * English summary: Compiles a unified release into one role-specific immutable projection.
 * 用法 / Usage: Bind exactly one qualified implementation per executable; never select by runtime mode.
 */
@FunctionalInterface
public interface GatewayRuleCompilerStrategy<T extends GatewayCompiledRulesDTO> {

    T compile(GatewayRuleSnapshot snapshot);
}
