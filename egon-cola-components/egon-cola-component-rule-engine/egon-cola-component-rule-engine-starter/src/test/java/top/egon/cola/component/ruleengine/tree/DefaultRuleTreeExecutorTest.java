package top.egon.cola.component.ruleengine.tree;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.engine.DefaultRuleTreeExecutor;
import top.egon.cola.component.ruleengine.result.RuleResult;
import top.egon.cola.component.ruleengine.result.RuleStatus;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRuleTreeExecutorTest {

    private final DefaultRuleTreeExecutor executor = new DefaultRuleTreeExecutor(true, false);

    @Test
    void shouldRouteByContextAndReturnHitNode() {
        RuleNode<String, String> root = new TestNode("root", RouteDecision.toCode("level"));
        RuleNode<String, String> level = new TestNode("level", RouteDecision.end("gold"));
        RuleTree<String, String> tree = RuleTree.<String, String>builder("member-benefit", root)
                .node(level)
                .build();

        RuleResult<String> result = executor.execute(tree, "req", RuleContext.create());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo("gold");
        assertThat(result.getHitNode()).isEqualTo("level");
        assertThat(result.getTrace().nodeTraces()).hasSize(2);
    }

    @Test
    void shouldStopLoopWhenMaxStepsExceeded() {
        RuleNode<String, String> loop = new TestNode("loop", RouteDecision.toCode("loop"));
        RuleTree<String, String> tree = RuleTree.<String, String>builder("loop-tree", loop)
                .maxSteps(3)
                .build();
        RuleContext context = RuleContext.create().maxSteps(3);

        RuleResult<String> result = executor.execute(tree, "req", context);

        assertThat(result.getStatus()).isEqualTo(RuleStatus.MAX_STEPS_EXCEEDED);
        assertThat(result.getTrace().nodeTraces()).hasSize(3);
    }

    @Test
    void shouldReturnNoRouteWhenNoDefaultExists() {
        RuleNode<String, String> root = new TestNode("root", RouteDecision.noRoute("missing branch"));
        RuleTree<String, String> tree = RuleTree.<String, String>builder("no-route-tree", root).build();

        RuleResult<String> result = executor.execute(tree, "req", RuleContext.create());

        assertThat(result.getStatus()).isEqualTo(RuleStatus.NO_ROUTE);
        assertThat(result.getMessage()).contains("missing branch");
    }

    @Test
    void shouldCarryEndDataWhenTheNodeDoesNotPopulateItItself() {
        RuleNode<String, String> root = new PlainNode("root", RouteDecision.end("gold"));
        RuleTree<String, String> tree = RuleTree.<String, String>builder("end-data-tree", root).build();

        RuleResult<String> result = executor.execute(tree, "req", RuleContext.create());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo("gold");
    }

    @Test
    void shouldKeepNodeResultWhenEndCarriesNoData() {
        RuleNode<String, String> root = new PlainNode("root", RouteDecision.end(null), "from-node");
        RuleTree<String, String> tree = RuleTree.<String, String>builder("end-null-tree", root).build();

        RuleResult<String> result = executor.execute(tree, "req", RuleContext.create());

        assertThat(result.getData()).isEqualTo("from-node");
    }

    @Test
    void callerSuppliedMaxStepsWinsOverTheTreeDeclaration() {
        RuleNode<String, String> loop = new PlainNode("loop", RouteDecision.toCode("loop"));
        RuleTree<String, String> tree = RuleTree.<String, String>builder("loop-tree", loop)
                .maxSteps(10)
                .build();

        RuleResult<String> result = executor.execute(tree, "req", RuleContext.create().maxSteps(2));

        assertThat(result.getStatus()).isEqualTo(RuleStatus.MAX_STEPS_EXCEEDED);
        assertThat(result.getTrace().nodeTraces()).hasSize(2);
    }

    @Test
    void shouldReportTimeoutRaisedByTheFinalNode() {
        RuleNode<String, String> slow = new SlowNode("slow", RouteDecision.end("late"), 60L);
        RuleTree<String, String> tree = RuleTree.<String, String>builder("slow-tree", slow)
                .timeoutMillis(20L)
                .build();

        RuleResult<String> result = executor.execute(tree, "req", RuleContext.create());

        assertThat(result.getStatus()).isEqualTo(RuleStatus.TIMEOUT);
    }

    @Test
    void shouldApplyEngineDefaultsWhenTheTreeDeclaresNone() {
        DefaultRuleTreeExecutor configured = new DefaultRuleTreeExecutor(true, false, null, 2, 3000L);
        RuleNode<String, String> loop = new PlainNode("loop", RouteDecision.toCode("loop"));
        RuleTree<String, String> tree = RuleTree.<String, String>builder("loop-tree", loop).build();

        RuleResult<String> result = configured.execute(tree, "req", RuleContext.create());

        assertThat(result.getStatus()).isEqualTo(RuleStatus.MAX_STEPS_EXCEEDED);
        assertThat(result.getTrace().nodeTraces()).hasSize(2);
    }

    /**
     * Unlike {@link TestNode} this never reads the route decision, so the executor alone is
     * responsible for carrying the end payload into the result.
     */
    private static class PlainNode implements RuleNode<String, String> {

        private final String code;

        private final RouteDecision routeDecision;

        private final String data;

        private PlainNode(String code, RouteDecision routeDecision) {
            this(code, routeDecision, null);
        }

        private PlainNode(String code, RouteDecision routeDecision, String data) {
            this.code = code;
            this.routeDecision = routeDecision;
            this.data = data;
        }

        @Override
        public String code() {
            return code;
        }

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
            return RuleResult.success(data);
        }

        @Override
        public RouteDecision route(String request, RuleContext context) {
            return routeDecision;
        }
    }

    private static final class SlowNode extends PlainNode {

        private final long sleepMillis;

        private SlowNode(String code, RouteDecision routeDecision, long sleepMillis) {
            super(code, routeDecision);
            this.sleepMillis = sleepMillis;
        }

        @Override
        public RuleResult<String> execute(String request, RuleContext context) {
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            return super.execute(request, context);
        }
    }

    private static final class TestNode implements RuleNode<String, String> {

        private final String code;

        private final RouteDecision routeDecision;

        private TestNode(String code, RouteDecision routeDecision) {
            this.code = code;
            this.routeDecision = routeDecision;
        }

        @Override
        public String code() {
            return code;
        }

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
            return routeDecision.isEnd() ? RuleResult.success(routeDecision.endData(String.class)) : RuleResult.success(null);
        }

        @Override
        public RouteDecision route(String request, RuleContext context) {
            return routeDecision;
        }
    }
}
