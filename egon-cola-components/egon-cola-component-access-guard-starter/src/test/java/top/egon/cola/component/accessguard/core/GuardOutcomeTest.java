package top.egon.cola.component.accessguard.core;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.api.GuardedOperation;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuardOutcomeTest {

    @Test
    void fallbackPreservesRootDecision() {
        GuardOutcome outcome = GuardOutcome.of(
                GuardOutcomeType.DEGRADED,
                GuardDecision.RATE_LIMITED,
                GuardResolution.FALLBACK,
                "draw",
                "rate-limit",
                7L,
                Duration.ofMillis(3));

        assertThat(outcome.decision()).isEqualTo(GuardDecision.RATE_LIMITED);
        assertThat(outcome.resolution()).isEqualTo(GuardResolution.FALLBACK);
        assertThat(outcome.elapsed()).isEqualTo(Duration.ofMillis(3));
    }

    @Test
    void outcomeStringNeverContainsThrowableMessage() {
        GuardOutcome outcome = new GuardOutcome(
                GuardOutcomeType.FAILED,
                GuardDecision.STORE_FAILED,
                GuardResolution.THROWN,
                "draw",
                "deny-list",
                3L,
                "REDISSON",
                "AOP",
                Duration.ofMillis(2),
                Duration.ZERO,
                new GuardFailure("STORE", "TIMEOUT"));

        assertThat(outcome.toString())
                .contains("STORE", "TIMEOUT")
                .doesNotContain("redis://", "user-1");
    }

    @Test
    void invocationDefensivelyCopiesArgumentsAndAttributes() throws Exception {
        Method method = Sample.class.getDeclaredMethod("draw", String.class);
        Object[] arguments = {"user-1"};
        Map<String, Object> attributes = new HashMap<>(Map.of("tenant", "t1"));
        GuardedOperation<String> continuation = () -> "ok";
        GuardInvocation invocation = new GuardInvocation(
                "draw",
                new Sample(),
                Sample.class,
                method,
                arguments,
                attributes,
                GuardEntryType.AOP,
                GuardInvocationKind.METHOD,
                continuation);

        arguments[0] = "changed";
        attributes.put("tenant", "changed");
        Object[] returnedArguments = invocation.arguments();
        returnedArguments[0] = "returned-change";

        assertThat(invocation.arguments()).containsExactly("user-1");
        assertThat(invocation.attributes()).containsEntry("tenant", "t1");
        assertThat(invocation.continuation()).isSameAs(continuation);
    }

    @Test
    void blankRuleIdsAreRejectedAtTheBoundary() {
        assertThatThrownBy(() -> GuardOutcome.allowed(" ", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ruleId");
    }

    static class Sample {

        String draw(String userId) {
            return userId;
        }
    }
}
