package top.egon.cola.component.accessguard.core;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.api.AccessGuardRejectedException;
import top.egon.cola.component.accessguard.core.plan.ExecutionConfig;
import top.egon.cola.component.accessguard.core.plan.ObservabilityConfig;
import top.egon.cola.component.accessguard.execution.RejectionMode;
import top.egon.cola.component.accessguard.execution.TimeLimitExceededException;
import top.egon.cola.component.accessguard.execution.TimeLimitMode;
import top.egon.cola.component.accessguard.execution.TimeLimiter;
import top.egon.cola.component.accessguard.execution.TimeLimiterType;
import top.egon.cola.component.accessguard.key.GuardKeyScope;
import top.egon.cola.component.accessguard.observability.CompositeGuardEventPublisher;
import top.egon.cola.component.accessguard.observability.GuardEvent;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuardExecutionCoordinatorTest {

    @Test
    void executesAdmittedInvocationAndPublishesOneFinalEvent() throws Throwable {
        AtomicInteger businessCalls = new AtomicInteger();
        List<GuardEvent> events = new ArrayList<>();
        GuardExecutionCoordinator coordinator = new GuardExecutionCoordinator(
                (invocation, config) -> invocation.continuation().execute(),
                (invocation, outcome, config) -> {
                    throw new AccessGuardRejectedException(outcome);
                },
                () -> 110L,
                new CompositeGuardEventPublisher(List.of(events::add)));
        GuardAdmission admission = new GuardAdmission(
                allowedOutcome(),
                disabledExecution(),
                new ObservabilityConfig(true, false, true, true, true),
                100L);

        PreparedGuardExecution prepared = coordinator.prepare(invocation(businessCalls), admission);
        GuardExecutionResult<Object> result = coordinator.execute(prepared);

        assertThat(result.value()).isEqualTo("business");
        assertThat(result.outcome().type()).isEqualTo(GuardOutcomeType.ALLOWED);
        assertThat(result.outcome().elapsed()).isEqualTo(Duration.ofNanos(10));
        assertThat(businessCalls).hasValue(1);
        assertThat(events).singleElement().extracting(GuardEvent::outcome).isEqualTo(result.outcome());
    }

    @Test
    void resolvesAdmissionRejectionWithoutInvokingBusinessCode() throws Throwable {
        AtomicInteger businessCalls = new AtomicInteger();
        List<GuardEvent> events = new ArrayList<>();
        GuardExecutionCoordinator coordinator = new GuardExecutionCoordinator(
                (invocation, config) -> invocation.continuation().execute(),
                (invocation, outcome, config) -> "fallback",
                () -> 200L,
                new CompositeGuardEventPublisher(List.of(events::add)));
        GuardAdmission admission = new GuardAdmission(
                GuardOutcome.rejected("draw", GuardDecision.DENY_LIST_HIT, "deny-list", 7L),
                new ExecutionConfig(
                        new ExecutionConfig.TimeLimitConfig(false, TimeLimitMode.DISABLED,
                                TimeLimiterType.CALLER_THREAD, Duration.ofSeconds(1), true),
                        new ExecutionConfig.RejectionConfig(RejectionMode.FALLBACK, "", "")),
                ObservabilityConfig.defaults(),
                100L);

        GuardExecutionResult<Object> result = coordinator.execute(
                coordinator.prepare(invocation(businessCalls), admission));

        assertThat(result.value()).isEqualTo("fallback");
        assertThat(result.outcome().type()).isEqualTo(GuardOutcomeType.DEGRADED);
        assertThat(result.outcome().resolution()).isEqualTo(GuardResolution.FALLBACK);
        assertThat(businessCalls).hasValue(0);
        assertThat(events).hasSize(1);
    }

    @Test
    void timeoutUsesExecutionFailureAndDoesNotRunBusinessCode() throws Throwable {
        AtomicInteger businessCalls = new AtomicInteger();
        TimeLimiter timeout = (invocation, config) -> {
            throw new TimeLimitExceededException(config.timeout());
        };
        GuardExecutionCoordinator coordinator = new GuardExecutionCoordinator(
                timeout,
                (invocation, outcome, config) -> "timeout-fallback",
                () -> 300L,
                top.egon.cola.component.accessguard.observability.GuardEventPublisher.noop());
        ExecutionConfig execution = new ExecutionConfig(
                new ExecutionConfig.TimeLimitConfig(true, TimeLimitMode.ENFORCE,
                        TimeLimiterType.VIRTUAL_THREAD, Duration.ofMillis(50), true),
                new ExecutionConfig.RejectionConfig(RejectionMode.FALLBACK, "", ""));

        GuardExecutionResult<Object> result = coordinator.execute(
                coordinator.prepare(invocation(businessCalls), new GuardAdmission(
                        allowedOutcome(), execution, ObservabilityConfig.defaults(), 250L)));

        assertThat(result.value()).isEqualTo("timeout-fallback");
        assertThat(result.outcome().decision()).isEqualTo(GuardDecision.TIME_LIMIT_EXCEEDED);
        assertThat(result.outcome().resolution()).isEqualTo(GuardResolution.FALLBACK);
        assertThat(businessCalls).hasValue(0);
    }

    private static GuardOutcome allowedOutcome() {
        return new GuardOutcome(
                GuardOutcomeType.ALLOWED,
                GuardDecision.PASS,
                GuardResolution.NONE,
                "draw",
                "",
                7L,
                "LOCAL",
                "AOP",
                Duration.ZERO,
                Duration.ZERO,
                null);
    }

    private static ExecutionConfig disabledExecution() {
        return new ExecutionConfig(
                new ExecutionConfig.TimeLimitConfig(false, TimeLimitMode.DISABLED,
                        TimeLimiterType.CALLER_THREAD, Duration.ofSeconds(1), true),
                new ExecutionConfig.RejectionConfig(RejectionMode.THROW, "", ""));
    }

    private static GuardInvocation invocation(AtomicInteger businessCalls) throws Exception {
        Method method = Sample.class.getDeclaredMethod("draw");
        return new GuardInvocation(
                "draw",
                new Sample(),
                Sample.class,
                method,
                new Object[0],
                Map.of("scope", GuardKeyScope.KEY),
                GuardEntryType.PROGRAMMATIC,
                GuardInvocationKind.METHOD,
                () -> {
                    businessCalls.incrementAndGet();
                    return "business";
                });
    }

    static class Sample {

        String draw() {
            return "business";
        }
    }
}
