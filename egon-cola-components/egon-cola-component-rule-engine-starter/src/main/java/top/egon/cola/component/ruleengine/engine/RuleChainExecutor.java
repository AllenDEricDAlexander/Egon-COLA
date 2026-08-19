package top.egon.cola.component.ruleengine.engine;

import top.egon.cola.component.ruleengine.chain.RuleChain;
import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleResult;

public interface RuleChainExecutor {

    <T, R> RuleResult<R> execute(RuleChain<T, R> ruleChain, T request, RuleContext context);
}
