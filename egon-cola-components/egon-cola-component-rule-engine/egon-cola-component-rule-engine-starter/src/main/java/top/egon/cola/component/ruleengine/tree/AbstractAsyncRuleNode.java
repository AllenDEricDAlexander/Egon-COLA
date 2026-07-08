package top.egon.cola.component.ruleengine.tree;

import top.egon.cola.component.ruleengine.async.RuleAsyncExecutor;

public abstract class AbstractAsyncRuleNode<T, R> extends AbstractRuleNode<T, R> {

    private final RuleAsyncExecutor asyncExecutor;

    protected AbstractAsyncRuleNode(String code, String name, NodeType type, RuleAsyncExecutor asyncExecutor) {
        super(code, name, type);
        this.asyncExecutor = asyncExecutor;
    }

    protected RuleAsyncExecutor asyncExecutor() {
        return asyncExecutor;
    }
}
