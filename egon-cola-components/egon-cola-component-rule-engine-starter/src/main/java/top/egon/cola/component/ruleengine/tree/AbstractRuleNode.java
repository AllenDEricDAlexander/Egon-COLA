package top.egon.cola.component.ruleengine.tree;

import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleResult;

public abstract class AbstractRuleNode<T, R> implements RuleNode<T, R> {

    private final String code;

    private final String name;

    private final NodeType type;

    protected AbstractRuleNode(String code, String name, NodeType type) {
        this.code = code;
        this.name = name;
        this.type = type;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public NodeType type() {
        return type;
    }

    @Override
    public abstract RuleResult<R> execute(T request, RuleContext context);
}
