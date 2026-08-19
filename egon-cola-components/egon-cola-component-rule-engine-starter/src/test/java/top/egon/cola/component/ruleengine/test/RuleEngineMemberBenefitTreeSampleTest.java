package top.egon.cola.component.ruleengine.test;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.engine.DefaultRuleTreeExecutor;
import top.egon.cola.component.ruleengine.result.RuleResult;
import top.egon.cola.component.ruleengine.result.RuleStatus;
import top.egon.cola.component.ruleengine.tree.NodeType;
import top.egon.cola.component.ruleengine.tree.RouteDecision;
import top.egon.cola.component.ruleengine.tree.RuleNode;
import top.egon.cola.component.ruleengine.tree.RuleTree;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineMemberBenefitTreeSampleTest {

    @Test
    void shouldStopMemberBenefitLoopByMaxSteps() {
        RuleNode<MemberRequest, String> root = new StaticNode("root", NodeType.ROOT, RouteDecision.toCode("account"));
        RuleNode<MemberRequest, String> account = new StaticNode("account", NodeType.BIZ, RouteDecision.toCode("level"));
        RuleNode<MemberRequest, String> level = new StaticNode("level", NodeType.SWITCH,
                RouteDecision.toCode("coupon"));
        RuleNode<MemberRequest, String> coupon = new StaticNode("coupon", NodeType.BIZ,
                RouteDecision.toCode("level", "recheck level after coupon"));

        RuleTree<MemberRequest, String> tree = RuleTree.<MemberRequest, String>builder("member-benefit", root)
                .node(account)
                .node(level)
                .node(coupon)
                .maxSteps(5)
                .build();

        RuleResult<String> result = new DefaultRuleTreeExecutor(true, false)
                .execute(tree, new MemberRequest("U-1"), RuleContext.create().maxSteps(5));

        assertThat(result.getStatus()).isEqualTo(RuleStatus.MAX_STEPS_EXCEEDED);
        assertThat(result.getTrace().nodeTraces()).hasSize(5);
    }

    private record StaticNode(String code, NodeType type, RouteDecision decision)
            implements RuleNode<MemberRequest, String> {

        @Override
        public String name() {
            return code;
        }

        @Override
        public RuleResult<String> execute(MemberRequest request, RuleContext context) {
            return RuleResult.success(null);
        }

        @Override
        public RouteDecision route(MemberRequest request, RuleContext context) {
            return decision;
        }
    }

    private record MemberRequest(String userId) {
    }
}
