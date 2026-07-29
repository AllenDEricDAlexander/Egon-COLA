package top.egon.cola.component.accessguard.adapter.programmatic;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.api.GuardRequest;
import top.egon.cola.component.accessguard.core.GuardDecision;
import top.egon.cola.component.accessguard.core.GuardEngine;
import top.egon.cola.component.accessguard.core.GuardEntryType;
import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.GuardInvocationKind;
import top.egon.cola.component.accessguard.core.GuardOutcome;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultAccessGuardClientTest {

    @Test
    void programmaticRequestsUseTheUnifiedInvocationModel() throws Throwable {
        AtomicReference<GuardInvocation> captured = new AtomicReference<>();
        GuardEngine engine = new GuardEngine() {
            @Override
            public GuardOutcome evaluate(GuardInvocation invocation) {
                captured.set(invocation);
                return GuardOutcome.allowed(invocation.ruleId(), 1L);
            }

            @Override
            public Object execute(GuardInvocation invocation) throws Throwable {
                captured.set(invocation);
                return invocation.continuation().execute();
            }
        };
        DefaultAccessGuardClient client = new DefaultAccessGuardClient(engine);
        AtomicInteger calls = new AtomicInteger();
        GuardRequest request = new GuardRequest(
                "draw", new Object[]{"user-1"}, Map.of("tenant", "t1"), String.class, null);

        GuardOutcome outcome = client.evaluate(request);
        String value = client.execute(request, () -> {
            calls.incrementAndGet();
            return "ok";
        });

        assertThat(outcome.decision()).isEqualTo(GuardDecision.PASS);
        assertThat(value).isEqualTo("ok");
        assertThat(calls).hasValue(1);
        assertThat(captured.get().entryType()).isEqualTo(GuardEntryType.PROGRAMMATIC);
        assertThat(captured.get().kind()).isEqualTo(GuardInvocationKind.OPERATION);
        assertThat(captured.get().arguments()).containsExactly("user-1");
    }
}
