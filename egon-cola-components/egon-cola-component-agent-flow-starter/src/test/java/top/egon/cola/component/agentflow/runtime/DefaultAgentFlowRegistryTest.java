package top.egon.cola.component.agentflow.runtime;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.InvocationContext;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.agentflow.exception.AgentFlowException;
import top.egon.cola.component.agentflow.exception.AgentFlowNotFoundException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultAgentFlowRegistryTest {

    @Test
    void lists_sorted_immutable_descriptors_and_hides_runtime_objects() {
        DefaultAgentFlowRegistry registry = new DefaultAgentFlowRegistry(Map.of(
                "z-flow", runtime("z-flow", new ControlledRunner("z-runner")),
                "a-flow", runtime("a-flow", new ControlledRunner("a-runner"))), Duration.ofSeconds(1));

        try {
            assertThat(registry.listFlows()).extracting("flowId").containsExactly("a-flow", "z-flow");
            assertThatThrownBy(() -> registry.listFlows().clear()).isInstanceOf(UnsupportedOperationException.class);
            assertThat(registry.listFlows().getFirst().rootAgentName()).isEqualTo("root");
        } finally {
            registry.close().blockingAwait();
        }
    }

    @Test
    void rejects_unknown_and_closed_lookup() {
        DefaultAgentFlowRegistry registry = new DefaultAgentFlowRegistry(
                Map.of("flow", runtime("flow", new ControlledRunner("runner"))), Duration.ofSeconds(1));

        assertThatThrownBy(() -> registry.require("missing"))
                .isInstanceOf(AgentFlowNotFoundException.class)
                .hasMessageContaining("missing");
        assertThat(registry.beginClosing()).isTrue();
        assertThatThrownBy(() -> registry.require("flow"))
                .isInstanceOf(AgentFlowException.class)
                .hasMessageContaining("not open");
        registry.close().blockingAwait();
        assertThat(registry.isClosed()).isTrue();
        assertThatCode(() -> registry.close().blockingAwait()).doesNotThrowAnyException();
    }

    @Test
    void attempts_every_runner_close_and_aggregates_failure() {
        ControlledRunner failing = new ControlledRunner("failing-runner", true);
        ControlledRunner succeeding = new ControlledRunner("succeeding-runner", false);
        DefaultAgentFlowRegistry registry = new DefaultAgentFlowRegistry(Map.of(
                "failing", runtime("failing", failing),
                "succeeding", runtime("succeeding", succeeding)), Duration.ofSeconds(1));

        assertThatThrownBy(() -> registry.close().blockingAwait())
                .isInstanceOf(RuntimeException.class);
        assertThat(failing.closeCalls).isEqualTo(1);
        assertThat(succeeding.closeCalls).isEqualTo(1);
        assertThat(registry.isClosed()).isTrue();
    }

    private static AgentFlowRuntimeBO runtime(String flowId, InMemoryRunner runner) {
        return new AgentFlowRuntimeBO(flowId, "root", "chatModel", "model-name", runner.agent(), runner,
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static final class ControlledRunner extends InMemoryRunner {
        private final boolean fail;
        private int closeCalls;

        private ControlledRunner(String name) {
            this(name, false);
        }

        private ControlledRunner(String name, boolean fail) {
            super(new TestAgent("root"), name);
            this.fail = fail;
        }

        @Override
        public Completable close() {
            closeCalls++;
            return fail ? Completable.error(new IllegalStateException("close failure")) : Completable.complete();
        }
    }

    private static final class TestAgent extends BaseAgent {
        private TestAgent(String name) {
            super(name, name, List.of(), List.of(), List.of());
        }

        @Override
        protected Flowable<Event> runAsyncImpl(InvocationContext invocationContext) {
            return Flowable.empty();
        }

        @Override
        protected Flowable<Event> runLiveImpl(InvocationContext invocationContext) {
            return Flowable.empty();
        }
    }
}
