package top.egon.cola.component.ruleengine.context;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RuleContextTest {

    @Test
    void shouldReadWriteAttributesAndControlFlow() {
        RuleContext context = RuleContext.create("req-1", "trace-1")
                .maxSteps(2)
                .timeout(Duration.ofMillis(500));

        context.set("amount", 100);

        assertThat(context.get("amount", Integer.class)).isEqualTo(100);
        assertThat(context.contains("amount")).isTrue();
        assertThat(context.isProceed()).isTrue();

        context.incrementStep();
        context.incrementStep();
        context.incrementStep();

        assertThat(context.isExceededMaxSteps()).isTrue();

        context.stop();

        assertThat(context.isStopped()).isTrue();
        assertThat(context.isProceed()).isFalse();
    }
}
