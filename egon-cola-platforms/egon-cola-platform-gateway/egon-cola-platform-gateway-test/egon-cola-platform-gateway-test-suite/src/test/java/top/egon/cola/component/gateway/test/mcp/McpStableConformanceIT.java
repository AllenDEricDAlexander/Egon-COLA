package top.egon.cola.component.gateway.test.mcp;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stable 2025-11-25 server-side conformance against the process fixture.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class McpStableConformanceIT {

    private RemoteMcpFixtureServer server;

    private StableMcpTestClient client;

    @BeforeAll
    void startFixture() {
        server = RemoteMcpFixtureServer.start();
        client = new StableMcpTestClient(server.stableEndpoint(), null);
    }

    @AfterAll
    void stopFixture() {
        if (server != null) {
            server.close();
        }
    }

    @Test
    void stableDialectCoversAllAdvertisedPrimitives() throws Exception {
        Map<String, Object> initialized = result(client.initialize());
        assertEquals("2025-11-25", initialized.get("protocolVersion"));
        assertNotNull(client.sessionId());

        assertTrue(result(client.call("ping", Map.of())).isEmpty());
        assertNames(
                result(client.call("tools/list", Map.of())),
                "tools",
                "remote_echo",
                "remote_failure"
        );
        Map<String, Object> echo = result(client.call(
                "tools/call",
                Map.of(
                        "name", "remote_echo",
                        "arguments", Map.of("value", "stable")
                )
        ));
        assertEquals(
                "stable",
                object(echo.get("structuredContent")).get("value")
        );
        assertEquals(
                -32050,
                object(client.call("tools/call", Map.of(
                        "name", "remote_failure",
                        "arguments", Map.of()
                )).get("error")).get("code")
        );

        assertNames(
                result(client.call("resources/list", Map.of())),
                "resources",
                "remote_text",
                "remote_blob",
                "remote_dashboard"
        );
        Map<String, Object> text = firstContent(client.call(
                "resources/read",
                Map.of("uri", "fixture://remote/text")
        ));
        assertEquals("remote fixture text", text.get("text"));
        Map<String, Object> blob = firstContent(client.call(
                "resources/read",
                Map.of("uri", "fixture://remote/blob")
        ));
        assertFalse(blob.get("blob").toString().isBlank());
        Map<String, Object> templated = firstContent(client.call(
                "resources/read",
                Map.of("uri", "fixture://remote/orders/order-7")
        ));
        assertTrue(templated.get("text").toString().contains("order-7"));
        assertEquals(
                true,
                result(client.call(
                        "resources/subscribe",
                        Map.of("uri", "fixture://remote/text")
                )).get("subscribed")
        );

        assertNames(
                result(client.call("prompts/list", Map.of())),
                "prompts",
                "remote_summary"
        );
        Map<String, Object> prompt = result(client.call(
                "prompts/get",
                Map.of(
                        "name", "remote_summary",
                        "arguments", Map.of("topic", "orders")
                )
        ));
        assertTrue(prompt.toString().contains("Summarize orders"));
        Map<String, Object> completion = object(result(client.call(
                "completion/complete",
                Map.of("argument", Map.of("name", "id", "value", "order"))
        )).get("completion"));
        assertEquals(2, completion.get("total"));
        assertNames(
                result(client.call("apps/list", Map.of())),
                "apps",
                "remote_dashboard"
        );
    }

    static Map<String, Object> result(Map<String, Object> response) {
        return object(response.get("result"));
    }

    private Map<String, Object> firstContent(
            Map<String, Object> response) {
        List<?> contents = (List<?>) result(response).get("contents");
        return object(contents.getFirst());
    }

    private void assertNames(
            Map<String, Object> result,
            String field,
            String... expected) {
        List<?> entries = (List<?>) result.get(field);
        List<String> names = entries.stream()
                .map(McpStableConformanceIT::object)
                .map(entry -> entry.get("name").toString())
                .toList();
        assertEquals(List.of(expected), names);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> object(Object value) {
        return value instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
    }
}
