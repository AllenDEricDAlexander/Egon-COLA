package top.egon.cola.component.ruleengine.async;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ruleengine.context.RuleContext;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRuleAsyncExecutorTest {

    @Test
    void shouldLoadValueIntoContext() {
        RuleContext context = RuleContext.create();
        DefaultRuleAsyncExecutor executor = new DefaultRuleAsyncExecutor(2, 4);

        executor.loadToContext("user", () -> "alice", context, Duration.ofSeconds(1));

        assertThat(context.get("user", String.class)).isEqualTo("alice");
        executor.shutdown();
    }
}
