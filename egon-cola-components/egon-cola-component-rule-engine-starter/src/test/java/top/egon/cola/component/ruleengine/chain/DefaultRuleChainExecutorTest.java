package top.egon.cola.component.ruleengine.chain;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.engine.DefaultRuleChainExecutor;
import top.egon.cola.component.ruleengine.result.RuleResult;
import top.egon.cola.component.ruleengine.result.RuleStatus;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRuleChainExecutorTest {

    private final DefaultRuleChainExecutor executor = new DefaultRuleChainExecutor(true, false);

    @Test
    void shouldExecuteHandlersInOrder() {
        RuleChain<String, String> chain = RuleChain.<String, String>builder("order-check")
                .name("Order Check")
                .handler((request, context) -> {
                    context.set("first", request);
                    return RuleResult.success(null);
                })
                .handler((request, context) -> RuleResult.success(context.get("first", String.class) + "-ok"))
                .build();

        RuleResult<String> result = executor.execute(chain, "req", RuleContext.create("r1", "t1"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo("req-ok");
        assertThat(result.getTrace().nodeTraces()).hasSize(2);
    }

    @Test
    void shouldStopWhenHandlerReturnsStop() {
        AtomicInteger count = new AtomicInteger();
        RuleChain<String, String> chain = RuleChain.<String, String>builder("risk-check")
                .handler((request, context) -> {
                    count.incrementAndGet();
                    return RuleResult.stop(600101, "risk blocked", "blocked");
                })
                .handler((request, context) -> {
                    count.incrementAndGet();
                    return RuleResult.success("unreachable");
                })
                .build();

        RuleResult<String> result = executor.execute(chain, "req", RuleContext.create());

        assertThat(result.getStatus()).isEqualTo(RuleStatus.STOPPED);
        assertThat(result.getCode()).isEqualTo(600101);
        assertThat(result.getData()).isEqualTo("blocked");
        assertThat(count).hasValue(1);
    }

    @Test
    void shouldWrapHandlerException() {
        RuleChain<String, String> chain = RuleChain.<String, String>builder("exception-chain")
                .handler((request, context) -> {
                    throw new IllegalStateException("boom");
                })
                .build();

        RuleResult<String> result = executor.execute(chain, "req", RuleContext.create());

        assertThat(result.getStatus()).isEqualTo(RuleStatus.NODE_ERROR);
        assertThat(result.getException()).isInstanceOf(IllegalStateException.class);
    }
}
