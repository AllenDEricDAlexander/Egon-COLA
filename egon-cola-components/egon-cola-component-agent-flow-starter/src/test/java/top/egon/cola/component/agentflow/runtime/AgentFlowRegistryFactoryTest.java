package top.egon.cola.component.agentflow.runtime;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.InvocationContext;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import io.reactivex.rxjava3.core.Flowable;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.agentflow.config.AgentConfigDTO;
import top.egon.cola.component.agentflow.config.AgentFlowConfigDTO;
import top.egon.cola.component.agentflow.config.AgentFlowConfigValidator;
import top.egon.cola.component.agentflow.config.AgentWorkflowConfigDTO;
import top.egon.cola.component.agentflow.config.AgentWorkflowTypeEnum;
import top.egon.cola.component.agentflow.autoconfigure.AgentFlowProperties;
import top.egon.cola.component.agentflow.exception.AgentFlowConfigurationException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentFlowRegistryFactoryTest {

    @Test
    void does_not_publish_partial_registry_and_closes_created_runners_on_failure() {
        ControlledRunner firstRunner = new ControlledRunner("first-runner");
        ScriptedAgentFlowFactory flowFactory = new ScriptedAgentFlowFactory(firstRunner);
        AgentFlowRegistryFactory registryFactory = new AgentFlowRegistryFactory(flowFactory,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));

        AgentFlowProperties properties = properties(Map.of(
                "first", validFlow("first"),
                "second", validFlow("second")));

        assertThatThrownBy(() -> registryFactory.create(properties, Map.of()))
                .isInstanceOf(AgentFlowConfigurationException.class)
                .hasMessageContaining("second");
        assertThat(firstRunner.closeCalls).isEqualTo(1);
    }

    @Test
    void publishes_all_compiled_flows_with_stable_creation_time() {
        ScriptedAgentFlowFactory flowFactory = new ScriptedAgentFlowFactory(
                new ControlledRunner("first-runner"), new ControlledRunner("second-runner"));
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        AgentFlowRegistry registry = new AgentFlowRegistryFactory(flowFactory, clock)
                .create(properties(Map.of("second", validFlow("second"), "first", validFlow("first"))), Map.of());

        try {
            assertThat(registry.listFlows()).extracting("flowId")
                    .containsExactly("first", "second");
            assertThat(registry.require("first").createdAt()).isEqualTo(clock.instant());
        } finally {
            registry.close().blockingAwait();
        }
    }

    private static AgentFlowProperties properties(Map<String, AgentFlowConfigDTO> flows) {
        return new AgentFlowProperties(true, Duration.ofMinutes(2), Duration.ofSeconds(10), flows);
    }

    private static AgentFlowConfigDTO validFlow(String name) {
        return new AgentFlowConfigDTO("chatModel", "model-name", "planner",
                List.of(new AgentConfigDTO("planner", name, "Plan work", name + "-output")), List.of());
    }

    private static final class ScriptedAgentFlowFactory extends AgentFlowFactory {
        private final Map<String, ControlledRunner> runners;

        private ScriptedAgentFlowFactory(ControlledRunner... runners) {
            super(null, null);
            this.runners = java.util.Arrays.stream(runners)
                    .collect(java.util.stream.Collectors.toUnmodifiableMap(
                            runner -> runner.agent().name().replace("-runner", ""), runner -> runner));
        }

        @Override
        public AgentFlowCompilationBO create(String flowId, AgentFlowConfigDTO config,
                                             Map<String, org.springframework.ai.chat.model.ChatModel> chatModels) {
            ControlledRunner runner = runners.get(flowId);
            if (runner == null) {
                throw new AgentFlowConfigurationException(flowId, "flow", "scripted compilation failure");
            }
            return new AgentFlowCompilationBO(flowId, runner.agent(), runner);
        }
    }

    private static final class ControlledRunner extends InMemoryRunner {
        private int closeCalls;

        private ControlledRunner(String name) {
            super(new TestAgent(name), name);
        }

        @Override
        public io.reactivex.rxjava3.core.Completable close() {
            closeCalls++;
            return io.reactivex.rxjava3.core.Completable.complete();
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
