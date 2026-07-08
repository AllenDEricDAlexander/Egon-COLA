package top.egon.cola.component.ruleengine.engine;

import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleResult;
import top.egon.cola.component.ruleengine.tree.RuleTree;

public interface RuleTreeExecutor {

    <T, R> RuleResult<R> execute(RuleTree<T, R> ruleTree, T request, RuleContext context);
}
