package top.egon.cola.component.agentflow.autoconfigure;

import com.google.adk.runner.InMemoryRunner;
import jakarta.validation.Validator;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.handler.NoUnboundElementsBindHandler;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.agentflow.api.AgentFlowService;
import top.egon.cola.component.agentflow.config.AgentFlowConfigValidator;
import top.egon.cola.component.agentflow.execution.AgentFlowSessionExecutionGuard;
import top.egon.cola.component.agentflow.execution.DefaultAgentFlowService;
import top.egon.cola.component.agentflow.runtime.AgentFlowFactory;
import top.egon.cola.component.agentflow.runtime.AgentFlowRegistry;
import top.egon.cola.component.agentflow.runtime.AgentFlowRegistryFactory;
import top.egon.cola.component.agentflow.runtime.SpringAiModelAdapterFactory;
import top.egon.cola.component.agentflow.workflow.AgentWorkflowBuilderStrategy;
import top.egon.cola.component.agentflow.workflow.AgentWorkflowStrategyFactory;
import top.egon.cola.component.agentflow.workflow.LoopAgentWorkflowBuilderStrategy;
import top.egon.cola.component.agentflow.workflow.ParallelAgentWorkflowBuilderStrategy;
import top.egon.cola.component.agentflow.workflow.SequentialAgentWorkflowBuilderStrategy;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Wires the flat Agent Flow starter only when the host explicitly enables it. */
@AutoConfiguration
@ConditionalOnProperty(
        prefix = "egon.cola.component.agent-flow",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false)
public class AgentFlowAutoConfiguration {

    @Bean(name = "agentFlowClock")
    @ConditionalOnMissingBean(name = "agentFlowClock")
    public Clock agentFlowClock() {
        return Clock.systemUTC();
    }

    @Bean(name = "agentFlowProperties")
    @ConditionalOnMissingBean(name = "agentFlowProperties")
    public AgentFlowProperties agentFlowProperties(org.springframework.core.env.Environment environment) {
        return Binder.get(environment).bindOrCreate(
                "egon.cola.component.agent-flow",
                Bindable.of(AgentFlowProperties.class),
                new NoUnboundElementsBindHandler(BindHandler.DEFAULT));
    }

    @Bean(name = "agentFlowValidationUtils")
    @ConditionalOnMissingBean(name = "agentFlowValidationUtils")
    public ValidationUtils agentFlowValidationUtils(Validator validator) {
        return new ValidationUtils(validator);
    }

    @Bean(name = "agentFlowConfigValidator")
    @ConditionalOnMissingBean(name = "agentFlowConfigValidator")
    public AgentFlowConfigValidator agentFlowConfigValidator(
            @Qualifier("agentFlowValidationUtils") ValidationUtils validationUtils) {
        return new AgentFlowConfigValidator(validationUtils);
    }

    @Bean(name = "sequentialAgentWorkflowBuilderStrategy")
    @ConditionalOnMissingBean(name = "sequentialAgentWorkflowBuilderStrategy")
    public SequentialAgentWorkflowBuilderStrategy sequentialAgentWorkflowBuilderStrategy() {
        return new SequentialAgentWorkflowBuilderStrategy();
    }

    @Bean(name = "parallelAgentWorkflowBuilderStrategy")
    @ConditionalOnMissingBean(name = "parallelAgentWorkflowBuilderStrategy")
    public ParallelAgentWorkflowBuilderStrategy parallelAgentWorkflowBuilderStrategy() {
        return new ParallelAgentWorkflowBuilderStrategy();
    }

    @Bean(name = "loopAgentWorkflowBuilderStrategy")
    @ConditionalOnMissingBean(name = "loopAgentWorkflowBuilderStrategy")
    public LoopAgentWorkflowBuilderStrategy loopAgentWorkflowBuilderStrategy() {
        return new LoopAgentWorkflowBuilderStrategy();
    }

    @Bean(name = "agentWorkflowBuilderStrategies")
    @ConditionalOnMissingBean(name = "agentWorkflowBuilderStrategies")
    public List<AgentWorkflowBuilderStrategy> agentWorkflowBuilderStrategies(
            @Qualifier("sequentialAgentWorkflowBuilderStrategy") SequentialAgentWorkflowBuilderStrategy sequential,
            @Qualifier("parallelAgentWorkflowBuilderStrategy") ParallelAgentWorkflowBuilderStrategy parallel,
            @Qualifier("loopAgentWorkflowBuilderStrategy") LoopAgentWorkflowBuilderStrategy loop) {
        return List.of(sequential, parallel, loop);
    }

    @Bean(name = "agentWorkflowStrategyFactory")
    @ConditionalOnMissingBean(name = "agentWorkflowStrategyFactory")
    public AgentWorkflowStrategyFactory agentWorkflowStrategyFactory(
            @Qualifier("agentWorkflowBuilderStrategies") List<AgentWorkflowBuilderStrategy> strategies) {
        return new AgentWorkflowStrategyFactory(strategies);
    }

    @Bean(name = "agentFlowChatModels")
    @ConditionalOnMissingBean(name = "agentFlowChatModels")
    public Map<String, ChatModel> agentFlowChatModels(ListableBeanFactory beanFactory) {
        return Map.copyOf(beanFactory.getBeansOfType(ChatModel.class));
    }

    @Bean(name = "springAiModelAdapterFactory")
    @ConditionalOnMissingBean(name = "springAiModelAdapterFactory")
    public SpringAiModelAdapterFactory springAiModelAdapterFactory(
            @Qualifier("agentFlowChatModels") Map<String, ChatModel> chatModels) {
        Objects.requireNonNull(chatModels, "agentFlowChatModels must not be null");
        return new SpringAiModelAdapterFactory();
    }

    @Bean(name = "agentFlowFactory")
    @ConditionalOnMissingBean(name = "agentFlowFactory")
    public AgentFlowFactory agentFlowFactory(
            @Qualifier("springAiModelAdapterFactory") SpringAiModelAdapterFactory modelAdapterFactory,
            @Qualifier("agentWorkflowStrategyFactory") AgentWorkflowStrategyFactory strategyFactory) {
        return new AgentFlowFactory(modelAdapterFactory, strategyFactory);
    }

    @Bean(name = "agentFlowRegistryFactory")
    @ConditionalOnMissingBean(name = "agentFlowRegistryFactory")
    public AgentFlowRegistryFactory agentFlowRegistryFactory(
            @Qualifier("agentFlowFactory") AgentFlowFactory flowFactory,
            @Qualifier("agentFlowClock") Clock clock) {
        return new AgentFlowRegistryFactory(flowFactory, clock);
    }

    @Bean(name = "agentFlowRegistry")
    @ConditionalOnMissingBean(name = "agentFlowRegistry")
    public AgentFlowRegistry agentFlowRegistry(
            @Qualifier("agentFlowProperties") AgentFlowProperties properties,
            @Qualifier("agentFlowConfigValidator") AgentFlowConfigValidator configValidator,
            @Qualifier("agentFlowRegistryFactory") AgentFlowRegistryFactory registryFactory,
            @Qualifier("agentFlowChatModels") Map<String, ChatModel> chatModels) {
        AgentFlowProperties validated = configValidator.validate(properties);
        return registryFactory.create(validated, chatModels);
    }

    @Bean(name = "agentFlowSessionExecutionGuard")
    @ConditionalOnMissingBean(name = "agentFlowSessionExecutionGuard")
    public AgentFlowSessionExecutionGuard agentFlowSessionExecutionGuard() {
        return new AgentFlowSessionExecutionGuard();
    }

    @Bean(name = "agentFlowService", destroyMethod = "close")
    @ConditionalOnMissingBean(name = "agentFlowService")
    public DefaultAgentFlowService agentFlowService(
            @Qualifier("agentFlowRegistry") AgentFlowRegistry registry,
            @Qualifier("agentFlowSessionExecutionGuard") AgentFlowSessionExecutionGuard guard,
            @Qualifier("agentFlowProperties") AgentFlowProperties properties,
            @Qualifier("agentFlowValidationUtils") ValidationUtils validationUtils,
            @Qualifier("agentFlowClock") Clock clock) {
        return new DefaultAgentFlowService(registry, guard, properties, validationUtils, clock);
    }
}
