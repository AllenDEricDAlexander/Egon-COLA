package top.egon.cola.component.ruleengine.engine;

import top.egon.cola.component.ruleengine.chain.RuleChain;
import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleResult;
import top.egon.cola.component.ruleengine.tree.RuleTree;

public class DefaultRuleEngine implements RuleEngine {

    private final RuleChainExecutor chainExecutor;

    private final RuleTreeExecutor treeExecutor;

    public DefaultRuleEngine(RuleChainExecutor chainExecutor, RuleTreeExecutor treeExecutor) {
        this.chainExecutor = chainExecutor;
        this.treeExecutor = treeExecutor;
    }

    @Override
    public <T, R> RuleResult<R> executeChain(RuleChain<T, R> ruleChain, T request) {
        return executeChain(ruleChain, request, RuleContext.create());
    }

    @Override
    public <T, R> RuleResult<R> executeChain(RuleChain<T, R> ruleChain, T request, RuleContext context) {
        return chainExecutor.execute(ruleChain, request, context);
    }

    @Override
    public <T, R> RuleResult<R> executeTree(RuleTree<T, R> ruleTree, T request) {
        return executeTree(ruleTree, request, RuleContext.create());
    }

    @Override
    public <T, R> RuleResult<R> executeTree(RuleTree<T, R> ruleTree, T request, RuleContext context) {
        return treeExecutor.execute(ruleTree, request, context);
    }
}
