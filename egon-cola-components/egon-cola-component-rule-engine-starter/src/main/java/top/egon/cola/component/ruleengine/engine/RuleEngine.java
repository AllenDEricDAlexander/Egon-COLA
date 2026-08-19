package top.egon.cola.component.ruleengine.engine;

import top.egon.cola.component.ruleengine.chain.RuleChain;
import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleResult;
import top.egon.cola.component.ruleengine.tree.RuleTree;

public interface RuleEngine {

    <T, R> RuleResult<R> executeChain(RuleChain<T, R> ruleChain, T request);

    <T, R> RuleResult<R> executeChain(RuleChain<T, R> ruleChain, T request, RuleContext context);

    <T, R> RuleResult<R> executeTree(RuleTree<T, R> ruleTree, T request);

    <T, R> RuleResult<R> executeTree(RuleTree<T, R> ruleTree, T request, RuleContext context);
}
