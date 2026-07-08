package top.egon.cola.component.ruleengine.chain;

import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleResult;

public interface SingletonRuleLink<T, R> {

    SingletonRuleLink<T, R> appendNext(SingletonRuleLink<T, R> next);

    RuleResult<R> handle(T request, RuleContext context);
}
