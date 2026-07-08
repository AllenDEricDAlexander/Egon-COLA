package top.egon.cola.component.ruleengine.listener;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ruleengine.chain.RuleChain;
import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.engine.DefaultRuleChainExecutor;
import top.egon.cola.component.ruleengine.engine.DefaultRuleTreeExecutor;
import top.egon.cola.component.ruleengine.result.RuleResult;
import top.egon.cola.component.ruleengine.tree.NodeType;
import top.egon.cola.component.ruleengine.tree.RouteDecision;
import top.egon.cola.component.ruleengine.tree.RuleNode;
import top.egon.cola.component.ruleengine.tree.RuleTree;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleExecutionListenerIntegrationTest {

    @Test
    void shouldNotifyChainExecutorLifecycle() {
        RecordingListener listener = new RecordingListener();
        DefaultRuleChainExecutor executor = new DefaultRuleChainExecutor(true, false, listener);
        RuleChain<String, String> chain = RuleChain.<String, String>builder("chain")
                .handler((request, context) -> RuleResult.success("ok"))
                .build();

        executor.execute(chain, "req", RuleContext.create());

        assertThat(listener.events).containsExactly("before-engine:CHAIN:chain", "before-node:handler-1",
                "after-node:handler-1", "after-engine:CHAIN:chain:SUCCESS");
    }

    @Test
    void shouldNotifyTreeRouteLifecycle() {
        RecordingListener listener = new RecordingListener();
        DefaultRuleTreeExecutor executor = new DefaultRuleTreeExecutor(true, false, listener);
        RuleNode<String, String> root = new TestNode("root", RouteDecision.toCode("end"));
        RuleNode<String, String> end = new TestNode("end", RouteDecision.end("ok"));
        RuleTree<String, String> tree = RuleTree.<String, String>builder("tree", root)
                .node(end)
                .build();

        executor.execute(tree, "req", RuleContext.create());

        assertThat(listener.events).contains("before-engine:TREE:tree", "before-node:root", "after-node:root",
                "before-route:root", "after-route:root:end", "after-engine:TREE:tree:SUCCESS");
    }

    private static final class RecordingListener implements RuleExecutionListener {

        private final List<String> events = new ArrayList<>();

        @Override
        public void beforeEngineExecute(String modelType, String ruleCode, RuleContext context) {
            events.add("before-engine:" + modelType + ":" + ruleCode);
        }

        @Override
        public void afterEngineExecute(String modelType, String ruleCode, RuleContext context, RuleResult<?> result) {
            events.add("after-engine:" + modelType + ":" + ruleCode + ":" + result.getStatus());
        }

        @Override
        public void beforeNodeExecute(String nodeCode, RuleContext context) {
            events.add("before-node:" + nodeCode);
        }

        @Override
        public void afterNodeExecute(String nodeCode, RuleContext context, RuleResult<?> result) {
            events.add("after-node:" + nodeCode);
        }

        @Override
        public void beforeRoute(String nodeCode, RuleContext context) {
            events.add("before-route:" + nodeCode);
        }

        @Override
        public void afterRoute(String nodeCode, RuleContext context, RouteDecision decision) {
            events.add("after-route:" + nodeCode + ":" + decision.routeTo());
        }
    }

    private record TestNode(String code, RouteDecision decision) implements RuleNode<String, String> {

        @Override
        public String name() {
            return code;
        }

        @Override
        public NodeType type() {
            return NodeType.BIZ;
        }

        @Override
        public RuleResult<String> execute(String request, RuleContext context) {
            return decision.isEnd() ? RuleResult.success(decision.endData(String.class)) : RuleResult.success(null);
        }

        @Override
        public RouteDecision route(String request, RuleContext context) {
            return decision;
        }
    }
}
