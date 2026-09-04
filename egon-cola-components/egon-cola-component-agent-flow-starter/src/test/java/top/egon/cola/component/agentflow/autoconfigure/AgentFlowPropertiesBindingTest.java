package top.egon.cola.component.agentflow.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.handler.NoUnboundElementsBindHandler;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentFlowPropertiesBindingTest {

    @Test
    void applies_safe_defaults_when_property_prefix_is_absent() {
        AgentFlowProperties properties = binder(Map.of())
                .bindOrCreate("egon.cola.component.agent-flow", AgentFlowProperties.class);

        assertThat(properties.enabled()).isFalse();
        assertThat(properties.executionTimeout()).isEqualTo(Duration.ofMinutes(2));
        assertThat(properties.shutdownTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(properties.flows()).isEmpty();
    }

    @Test
    void binds_nested_flow_configuration_without_mutating_order() {
        AgentFlowProperties properties = binder(Map.of(
                        "egon.cola.component.agent-flow.enabled", "true",
                        "egon.cola.component.agent-flow.execution-timeout", "30s",
                        "egon.cola.component.agent-flow.flows.research-flow.chat-model-bean-name", "researchChatModel",
                        "egon.cola.component.agent-flow.flows.research-flow.model-name", "research-model",
                        "egon.cola.component.agent-flow.flows.research-flow.root-agent-name", "planner",
                        "egon.cola.component.agent-flow.flows.research-flow.agents[0].name", "planner",
                        "egon.cola.component.agent-flow.flows.research-flow.agents[0].instruction", "Plan the work.",
                        "egon.cola.component.agent-flow.flows.research-flow.workflows[0].type", "sequential",
                        "egon.cola.component.agent-flow.flows.research-flow.workflows[0].name", "sequence",
                        "egon.cola.component.agent-flow.flows.research-flow.workflows[0].sub-agent-names[0]", "planner"))
                .bind("egon.cola.component.agent-flow", Bindable.of(AgentFlowProperties.class))
                .orElseThrow(() -> new IllegalStateException("properties were not bound"));

        assertThat(properties.executionTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.flows()).containsKey("research-flow");
        assertThat(properties.flows().get("research-flow").agents()).extracting("name")
                .containsExactly("planner");
    }

    @Test
    void rejects_unknown_property_with_strict_bind_handler() {
        assertThatThrownBy(() -> binder(Map.of(
                        "egon.cola.component.agent-flow.enabled", "true",
                        "egon.cola.component.agent-flow.unknown-setting", "value"))
                .bind("egon.cola.component.agent-flow", Bindable.of(AgentFlowProperties.class),
                        new NoUnboundElementsBindHandler(BindHandler.DEFAULT)))
                .isInstanceOf(Exception.class)
                .hasStackTraceContaining("unknown-setting");
    }

    private static Binder binder(Map<String, String> values) {
        return new Binder(new MapConfigurationPropertySource(values));
    }

}
