package top.egon.cola.archetype.source.agent.starter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import top.egon.cola.component.agentflow.autoconfigure.AgentFlowProperties;
import top.egon.cola.component.agentflow.config.AgentFlowConfigDTO;
import top.egon.cola.component.agentflow.config.AgentWorkflowConfigDTO;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = DeepResearchApplication.class)
@ActiveProfiles("test")
@Import(DeepResearchApplicationTest.FakeAgentDependencies.class)
class DeepResearchFlowTest {

    @Test
    void keeps_planner_parallel_research_and_writer_graph_in_declared_order(
            @Autowired AgentFlowProperties properties) {
        AgentFlowConfigDTO flow = properties.flows().get("deep-research");

        assertEquals("deepResearchChatModel", flow.chatModelBeanName());
        assertEquals("deep-research", flow.rootAgentName());
        assertEquals(List.of("Planner", "EvidenceResearcher", "CounterpointResearcher",
                "FreshnessResearcher", "Writer"), flow.agents().stream().map(agent -> agent.name()).toList());
        assertEquals(List.of("research_plan", "evidence_findings", "counterpoint_findings",
                "freshness_findings", "final_report"),
                flow.agents().stream().map(agent -> agent.outputKey()).toList());

        AgentWorkflowConfigDTO root = flow.workflows().getFirst();
        AgentWorkflowConfigDTO parallel = flow.workflows().getLast();
        assertEquals("deep-research", root.name());
        assertEquals(List.of("Planner", "ParallelResearch", "Writer"), root.subAgentNames());
        assertEquals("ParallelResearch", parallel.name());
        assertEquals(List.of("EvidenceResearcher", "CounterpointResearcher", "FreshnessResearcher"),
                parallel.subAgentNames());
        assertTrue(flow.agents().stream().map(agent -> agent.outputKey()).distinct().count() == 5);
    }
}
