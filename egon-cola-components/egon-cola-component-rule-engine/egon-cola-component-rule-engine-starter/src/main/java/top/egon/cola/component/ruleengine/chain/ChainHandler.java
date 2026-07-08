package top.egon.cola.component.ruleengine.chain;

import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleResult;

@FunctionalInterface
public interface ChainHandler<T, R> {

    RuleResult<R> handle(T request, RuleContext context);
}
