package top.egon.cola.component.ruleengine.listener;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ruleengine.context.RuleContext;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleExecutionListenerCompositeTest {

    @Test
    void shouldInvokeListenersInOrderAndIgnoreFailureByDefault() {
        List<String> calls = new ArrayList<>();
        RuleExecutionListener first = new RecordingListener("first", calls);
        RuleExecutionListener broken = new BrokenListener();
        RuleExecutionListener last = new RecordingListener("last", calls);
        RuleExecutionListenerComposite composite = new RuleExecutionListenerComposite(List.of(first, broken, last),
                true);

        composite.beforeEngineExecute("chain", "rule", RuleContext.create());

        assertThat(calls).containsExactly("first", "last");
    }

    private record RecordingListener(String name, List<String> calls) implements RuleExecutionListener {

        @Override
        public void beforeEngineExecute(String modelType, String ruleCode, RuleContext context) {
            calls.add(name);
        }
    }

    private static final class BrokenListener implements RuleExecutionListener {

        @Override
        public void beforeEngineExecute(String modelType, String ruleCode, RuleContext context) {
            throw new IllegalStateException("listener failed");
        }
    }
}
