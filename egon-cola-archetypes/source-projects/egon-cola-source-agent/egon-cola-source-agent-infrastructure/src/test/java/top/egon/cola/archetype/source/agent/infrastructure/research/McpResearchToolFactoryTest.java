package top.egon.cola.archetype.source.agent.infrastructure.research;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import top.egon.cola.archetype.source.agent.infrastructure.research.tool.McpResearchToolFactory;
import top.egon.cola.archetype.source.agent.infrastructure.research.tool.McpResearchToolProperties;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpResearchToolFactoryTest {

    @Test
    void creates_sorted_unique_immutable_callback_snapshot_and_closes_client() {
        RecordingClient client = new RecordingClient(List.of(callback("search.z"), callback("search.a")));
        McpResearchToolFactory factory = new McpResearchToolFactory(properties(), ignored -> client);

        ToolCallback[] callbacks = factory.create();

        assertEquals(List.of("search.a", "search.z"),
                List.of(callbacks).stream().map(tool -> tool.getToolDefinition().name()).toList());
        callbacks[0] = callback("mutated");
        assertEquals("search.a", factory.callbacks().getFirst().getToolDefinition().name());
        factory.close();
        factory.close();
        assertEquals(1, client.closeCount);
    }

    @Test
    void rejects_empty_or_duplicate_tools_and_closes_partial_client() {
        RecordingClient empty = new RecordingClient(List.of());
        McpResearchToolFactory emptyFactory = new McpResearchToolFactory(properties(), ignored -> empty);
        assertThrows(IllegalStateException.class, emptyFactory::create);
        assertEquals(1, empty.closeCount);

        RecordingClient duplicate = new RecordingClient(List.of(callback("search"), callback("search")));
        McpResearchToolFactory duplicateFactory = new McpResearchToolFactory(properties(), ignored -> duplicate);
        assertThrows(IllegalStateException.class, duplicateFactory::create);
        assertEquals(1, duplicate.closeCount);
    }

    @Test
    void rejects_missing_server_configuration_before_connecting() {
        RecordingClient client = new RecordingClient(List.of(callback("search")));
        McpResearchToolFactory factory = new McpResearchToolFactory(
                new McpResearchToolProperties("", "/sse", "synthetic-key", Duration.ofSeconds(5)),
                ignored -> client);

        assertThrows(IllegalArgumentException.class, factory::create);
        assertTrue(factory.callbacks().isEmpty());
        assertEquals(0, client.closeCount);
    }

    private static McpResearchToolProperties properties() {
        return new McpResearchToolProperties("http://mcp.test", "/sse", "synthetic-key", Duration.ofSeconds(5));
    }

    private static ToolCallback callback(String name) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description("test tool")
                        .inputSchema("{\"type\":\"object\"}").build();
            }

            @Override
            public String call(String input) {
                return input;
            }
        };
    }

    private static final class RecordingClient implements McpResearchToolFactory.ClientAdapter {
        private final List<ToolCallback> callbacks;
        private int closeCount;

        private RecordingClient(List<ToolCallback> callbacks) {
            this.callbacks = callbacks;
        }

        @Override
        public List<ToolCallback> callbacks() {
            return callbacks;
        }

        @Override
        public void close() {
            closeCount++;
        }
    }
}
