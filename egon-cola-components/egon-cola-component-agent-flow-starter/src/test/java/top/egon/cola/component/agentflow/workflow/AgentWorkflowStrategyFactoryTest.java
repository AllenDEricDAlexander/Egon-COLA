package top.egon.cola.component.agentflow.workflow;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LoopAgent;
import com.google.adk.agents.ParallelAgent;
import com.google.adk.agents.SequentialAgent;
import io.reactivex.rxjava3.core.Flowable;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.agentflow.config.AgentWorkflowConfigDTO;
import top.egon.cola.component.agentflow.config.AgentWorkflowTypeEnum;
import top.egon.cola.component.agentflow.exception.AgentFlowConfigurationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentWorkflowStrategyFactoryTest {

    @Test
    void selects_one_strategy_for_each_supported_workflow_type() {
        AgentWorkflowStrategyFactory factory = new AgentWorkflowStrategyFactory(List.of(
                new SequentialAgentWorkflowBuilderStrategy(),
                new ParallelAgentWorkflowBuilderStrategy(),
                new LoopAgentWorkflowBuilderStrategy()));

        assertThat(factory.getStrategy(AgentWorkflowTypeEnum.SEQUENTIAL))
                .isInstanceOf(SequentialAgentWorkflowBuilderStrategy.class);
        assertThat(factory.getStrategy(AgentWorkflowTypeEnum.PARALLEL))
                .isInstanceOf(ParallelAgentWorkflowBuilderStrategy.class);
        assertThat(factory.getStrategy(AgentWorkflowTypeEnum.LOOP))
                .isInstanceOf(LoopAgentWorkflowBuilderStrategy.class);
    }

    @Test
    void rejects_duplicate_strategy_types() {
        AgentWorkflowStrategyFactory factory = new AgentWorkflowStrategyFactory(List.of(
                new SequentialAgentWorkflowBuilderStrategy(),
                new SequentialAgentWorkflowBuilderStrategy()));

        assertThatThrownBy(() -> factory.getStrategy(AgentWorkflowTypeEnum.SEQUENTIAL))
                .isInstanceOf(AgentFlowConfigurationException.class)
                .hasMessageContaining("SEQUENTIAL");
    }

    @Test
    void builds_composites_with_declared_children_and_loop_limit() {
        List<BaseAgent> children = List.of(new TestAgent("first"), new TestAgent("second"));
        AgentWorkflowStrategyFactory factory = new AgentWorkflowStrategyFactory(List.of(
                new SequentialAgentWorkflowBuilderStrategy(),
                new ParallelAgentWorkflowBuilderStrategy(),
                new LoopAgentWorkflowBuilderStrategy()));

        BaseAgent sequential = factory.getStrategy(AgentWorkflowTypeEnum.SEQUENTIAL).build(
                workflow(AgentWorkflowTypeEnum.SEQUENTIAL, "sequence", children, null), children);
        BaseAgent parallel = factory.getStrategy(AgentWorkflowTypeEnum.PARALLEL).build(
                workflow(AgentWorkflowTypeEnum.PARALLEL, "parallel", children, null), children);
        BaseAgent loop = factory.getStrategy(AgentWorkflowTypeEnum.LOOP).build(
                workflow(AgentWorkflowTypeEnum.LOOP, "loop", children, 4), children);

        assertThat(sequential).isInstanceOf(SequentialAgent.class);
        assertThat(parallel).isInstanceOf(ParallelAgent.class);
        assertThat(loop).isInstanceOf(LoopAgent.class);
        assertThat(sequential.subAgents()).extracting(BaseAgent::name)
                .containsExactly("first", "second");
        assertThat(parallel.subAgents()).extracting(BaseAgent::name)
                .containsExactly("first", "second");
        assertThat(loop.subAgents()).extracting(BaseAgent::name)
                .containsExactly("first", "second");
    }

    private static AgentWorkflowConfigDTO workflow(AgentWorkflowTypeEnum type, String name,
                                                    List<BaseAgent> children, Integer maxIterations) {
        return new AgentWorkflowConfigDTO(type, name, name + " description",
                children.stream().map(BaseAgent::name).toList(), maxIterations);
    }

    private static final class TestAgent extends BaseAgent {

        private TestAgent(String name) {
            super(name, name + " description", List.of(), List.of(), List.of());
        }

        @Override
        protected Flowable<com.google.adk.events.Event> runAsyncImpl(
                com.google.adk.agents.InvocationContext invocationContext) {
            return Flowable.empty();
        }

        @Override
        protected Flowable<com.google.adk.events.Event> runLiveImpl(
                com.google.adk.agents.InvocationContext invocationContext) {
            return Flowable.empty();
        }
    }
}
