package top.egon.cola.component.ruleengine.test;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ruleengine.chain.RuleChain;
import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.engine.DefaultRuleChainExecutor;
import top.egon.cola.component.ruleengine.result.RuleResult;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineOrderChainSampleTest {

    @Test
    void shouldRunOrderPreCheckChain() {
        RuleChain<OrderRequest, String> chain = RuleChain.<OrderRequest, String>builder("order-pre-check")
                .handler((request, context) -> {
                    context.set("paramChecked", request.orderId() != null);
                    return RuleResult.success(null);
                })
                .handler((request, context) -> request.stock() > 0
                        ? RuleResult.success("allowed")
                        : RuleResult.stop(600201, "stock unavailable", "blocked"))
                .build();

        RuleResult<String> result = new DefaultRuleChainExecutor(true, false)
                .execute(chain, new OrderRequest("O-1", 3), RuleContext.create());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo("allowed");
        assertThat(result.getTrace().nodeTraces()).hasSize(2);
    }

    private record OrderRequest(String orderId, int stock) {
    }
}
