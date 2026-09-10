package top.egon.cola.archetype.source.agent.starter;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import top.egon.cola.component.agentflow.api.AgentFlowService;
import top.egon.cola.component.rag.api.RagIngestionService;
import top.egon.cola.component.rag.api.RagRetrievalService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@SpringBootTest(classes = DeepResearchApplication.class)
@ActiveProfiles("test")
@Import(DeepResearchApplicationTest.FakeAgentDependencies.class)
class DeepResearchApplicationTest {

    @Test
    void creates_named_test_model_tools_and_agent_flow_without_network_dependencies(
            org.springframework.context.ApplicationContext context) {
        assertNotNull(context.getBean("deepResearchChatModel", ChatModel.class));
        assertNotNull(context.getBean("deepResearchSearchTools", ToolCallback[].class));
        assertNotNull(context.getBean("deepResearchManage"));
        assertNotNull(context.getBean("deepResearchAgentGateway"));
        assertNotNull(context.getBean("agentFlowService", AgentFlowService.class));
        assertFalse(context.containsBean("mcpResearchToolFactory"));
        assertTrue(context.getBean(AgentFlowService.class).listFlows().stream()
                .anyMatch(flow -> flow.flowId().equals("deep-research")));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FakeAgentDependencies {

        @Bean(name = "deepResearchChatModel")
        ChatModel deepResearchChatModel() {
            return mock(ChatModel.class);
        }

        /**
         * The {@code test} profile turns the rag component off, so the knowledge gateway's
         * retrieval dependency is faked here; the real component assembles it in every other profile.
         */
        @Bean(name = "ragRetrievalService")
        RagRetrievalService ragRetrievalService() {
            return mock(RagRetrievalService.class);
        }

        /**
         * The same profile turns off the transactional outbox, which owns the ingest handler's
         * ingestion dependency; the real component assembles it in every other profile.
         */
        @Bean(name = "ragIngestionService")
        RagIngestionService ragIngestionService() {
            return mock(RagIngestionService.class);
        }

        @Bean(name = "deepResearchSearchTools")
        ToolCallback[] deepResearchSearchTools() {
            return new ToolCallback[]{callback("search")};
        }

        private static ToolCallback callback(String name) {
            return new ToolCallback() {
                @Override
                public ToolDefinition getToolDefinition() {
                    return ToolDefinition.builder().name(name).description("deterministic test search")
                            .inputSchema("{\"type\":\"object\"}").build();
                }

                @Override
                public String call(String input) {
                    return "test-result";
                }
            };
        }
    }
}
