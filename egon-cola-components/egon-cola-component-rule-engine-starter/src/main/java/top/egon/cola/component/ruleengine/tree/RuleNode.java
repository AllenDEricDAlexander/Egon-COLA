package top.egon.cola.component.ruleengine.tree;

import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleResult;

public interface RuleNode<T, R> {

    String code();

    String name();

    NodeType type();

    RuleResult<R> execute(T request, RuleContext context);

    default RouteDecision route(T request, RuleContext context) {
        return RouteDecision.noRoute("node has no route");
    }
}
