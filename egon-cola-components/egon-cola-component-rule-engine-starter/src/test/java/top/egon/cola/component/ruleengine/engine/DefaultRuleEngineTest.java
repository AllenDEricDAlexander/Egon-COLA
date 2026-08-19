package top.egon.cola.component.ruleengine.engine;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ruleengine.chain.RuleChain;
import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleResult;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRuleEngineTest {

    @Test
    void shouldDelegateChainExecution() {
        RuleChainExecutor chainExecutor = new RuleChainExecutor() {
            @Override
            @SuppressWarnings("unchecked")
            public <T, R> RuleResult<R> execute(RuleChain<T, R> ruleChain, T request, RuleContext context) {
                return RuleResult.success((R) "chain-ok");
            }
        };
        RuleTreeExecutor treeExecutor = new RuleTreeExecutor() {
            @Override
            @SuppressWarnings("unchecked")
            public <T, R> RuleResult<R> execute(top.egon.cola.component.ruleengine.tree.RuleTree<T, R> ruleTree,
                                                T request, RuleContext context) {
                return RuleResult.success((R) "tree-ok");
            }
        };
        DefaultRuleEngine engine = new DefaultRuleEngine(chainExecutor, treeExecutor);

        RuleResult<String> result = engine.executeChain(RuleChain.<String, String>builder("chain").build(), "req",
                RuleContext.create());

        assertThat(result.getData()).isEqualTo("chain-ok");
    }
}
