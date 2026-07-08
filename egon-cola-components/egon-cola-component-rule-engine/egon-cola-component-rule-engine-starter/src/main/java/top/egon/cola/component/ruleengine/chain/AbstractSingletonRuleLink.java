package top.egon.cola.component.ruleengine.chain;

import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleResult;

public abstract class AbstractSingletonRuleLink<T, R> implements SingletonRuleLink<T, R> {

    private volatile SingletonRuleLink<T, R> next;

    @Override
    public SingletonRuleLink<T, R> appendNext(SingletonRuleLink<T, R> next) {
        this.next = next;
        return next;
    }

    @Override
    public final RuleResult<R> handle(T request, RuleContext context) {
        RuleResult<R> result = apply(request, context);
        if (!result.isSuccess() || context.isStopped() || next == null) {
            return result;
        }
        return next.handle(request, context);
    }

    protected abstract RuleResult<R> apply(T request, RuleContext context);
}
