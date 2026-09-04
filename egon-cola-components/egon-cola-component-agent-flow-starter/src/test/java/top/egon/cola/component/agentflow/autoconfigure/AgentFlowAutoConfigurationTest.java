package top.egon.cola.component.agentflow.autoconfigure;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import top.egon.cola.component.agentflow.api.AgentFlowService;
import top.egon.cola.component.agentflow.config.AgentFlowConfigValidator;
import top.egon.cola.component.agentflow.exception.AgentFlowException;
import top.egon.cola.component.agentflow.execution.AgentFlowSessionExecutionGuard;
import top.egon.cola.component.agentflow.runtime.AgentFlowRegistry;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentFlowAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AgentFlowAutoConfiguration.class))
            .withUserConfiguration(ValidationConfiguration.class, ChatModelConfiguration.class);

    @Test
    void remains_default_off_when_property_is_absent_or_false() {
        contextRunner.run(context -> assertNoAgentFlowBeans(context));
        contextRunner.withPropertyValues("egon.cola.component.agent-flow.enabled=false")
                .run(context -> assertNoAgentFlowBeans(context));
    }

    @Test
    void creates_named_runtime_beans_for_valid_configuration() {
        contextRunner.withPropertyValues(validProperties()).run(context -> {
            assertThat(context).hasNotFailed();
            for (String beanName : AGENT_FLOW_BEAN_NAMES) {
                assertThat(context).hasBean(beanName);
            }
            assertThat(context).getBean("agentFlowProperties").isInstanceOf(AgentFlowProperties.class);
            assertThat(context).getBean("agentFlowConfigValidator").isInstanceOf(AgentFlowConfigValidator.class);
            assertThat(context).getBean("agentFlowRegistry").isInstanceOf(AgentFlowRegistry.class);
            assertThat(context).getBean("agentFlowSessionExecutionGuard")
                    .isInstanceOf(AgentFlowSessionExecutionGuard.class);
            assertThat(context).getBean("agentFlowService").isInstanceOf(AgentFlowService.class);
        });
    }

    @Test
    void rejects_unknown_properties_before_compilation() {
        contextRunner.withPropertyValues(
                        concat(validProperties(), "egon.cola.component.agent-flow.unknown-setting=value"))
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejects_missing_chat_model_and_invalid_graph() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(AgentFlowAutoConfiguration.class))
                .withUserConfiguration(ValidationConfiguration.class)
                .withPropertyValues(validProperties())
                .run(context -> assertThat(context).hasFailed());

        contextRunner.withPropertyValues(concat(validProperties(),
                        "egon.cola.component.agent-flow.flows.research-flow.root-agent-name=missing"))
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void honors_same_name_clock_override() {
        contextRunner.withUserConfiguration(CustomClockConfiguration.class)
                .withPropertyValues(validProperties())
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean("agentFlowClock"))
                            .isSameAs(CustomClockConfiguration.CLOCK);
                });
    }

    @Test
    void closes_service_and_rejects_new_operations_after_context_close() {
        contextRunner.withPropertyValues(validProperties()).run(context -> {
            assertThat(context).hasNotFailed();
            AgentFlowService service = context.getBean("agentFlowService", AgentFlowService.class);
            assertThat(service).isInstanceOf(AgentFlowService.class);
            context.close();
            assertThatThrownBy(service::listFlows).isInstanceOf(AgentFlowException.class);
        });
    }

    private static void assertNoAgentFlowBeans(AssertableApplicationContext context) {
        for (String beanName : AGENT_FLOW_BEAN_NAMES) {
            assertThat(context).doesNotHaveBean(beanName);
        }
    }

    private static String[] validProperties() {
        return new String[]{
                "egon.cola.component.agent-flow.enabled=true",
                "egon.cola.component.agent-flow.execution-timeout=30s",
                "egon.cola.component.agent-flow.shutdown-timeout=10s",
                "egon.cola.component.agent-flow.flows.research-flow.chat-model-bean-name=researchChatModel",
                "egon.cola.component.agent-flow.flows.research-flow.model-name=research-model",
                "egon.cola.component.agent-flow.flows.research-flow.root-agent-name=planner",
                "egon.cola.component.agent-flow.flows.research-flow.agents[0].name=planner",
                "egon.cola.component.agent-flow.flows.research-flow.agents[0].instruction=Plan the work."
        };
    }

    private static String[] concat(String[] values, String extra) {
        String[] result = java.util.Arrays.copyOf(values, values.length + 1);
        result[values.length] = extra;
        return result;
    }

    private static final String[] AGENT_FLOW_BEAN_NAMES = {
            "agentFlowClock",
            "agentFlowProperties",
            "agentFlowValidationUtils",
            "agentFlowConfigValidator",
            "sequentialAgentWorkflowBuilderStrategy",
            "parallelAgentWorkflowBuilderStrategy",
            "loopAgentWorkflowBuilderStrategy",
            "agentWorkflowBuilderStrategies",
            "agentWorkflowStrategyFactory",
            "agentFlowChatModels",
            "springAiModelAdapterFactory",
            "agentFlowFactory",
            "agentFlowRegistryFactory",
            "agentFlowRegistry",
            "agentFlowSessionExecutionGuard",
            "agentFlowService"
    };

    @Configuration(proxyBeanMethods = false)
    static class ValidationConfiguration {

        @Bean
        LocalValidatorFactoryBean validator() {
            return new LocalValidatorFactoryBean();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ChatModelConfiguration {

        @Bean(name = "researchChatModel")
        ChatModel researchChatModel() {
            return Mockito.mock(ChatModel.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomClockConfiguration {

        private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

        @Bean(name = "agentFlowClock")
        Clock agentFlowClock() {
            return CLOCK;
        }
    }
}
