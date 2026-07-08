package top.egon.cola.component.ruleengine.tree;

import top.egon.cola.component.ruleengine.context.RuleContext;

@FunctionalInterface
public interface RuleRouter<T, R> {

    RouteDecision route(T request, RuleContext context);
}
