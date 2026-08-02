package top.egon.cola.component.gateway.test.mcp;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class StableMcpTestClient {

    private static final String VERSION = "2025-11-25";

    private final McpTestHttpClient http = new McpTestHttpClient();

    private final URI endpoint;

    private final String token;

    private final AtomicLong ids = new AtomicLong();

    private String sessionId;

    public StableMcpTestClient(URI endpoint, String token) {
        this.endpoint = endpoint;
        this.token = token;
    }

    public Map<String, Object> initialize() throws Exception {
        McpTestHttpClient.Response response = http.post(
                endpoint,
                token,
                Map.of(),
                request("initialize", Map.of(
                        "protocolVersion", VERSION,
                        "capabilities", Map.of(),
                        "clientInfo", Map.of(
                                "name", "stable-test-client",
                                "version", "1.0.0"
                        )
                ))
        );
        requireSuccess(response);
        sessionId = response.headers().get("mcp-session-id");
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalStateException("stable MCP session was not issued");
        }
        return response.body();
    }

    public Map<String, Object> call(
            String method,
            Map<String, Object> params) throws Exception {
        if (sessionId == null) {
            initialize();
        }
        McpTestHttpClient.Response response = http.post(
                endpoint,
                token,
                Map.of(
                        "MCP-Protocol-Version", VERSION,
                        "MCP-Session-Id", sessionId
                ),
                request(method, params)
        );
        requireSuccess(response);
        return response.body();
    }

    public String sessionId() {
        return sessionId;
    }

    private Map<String, Object> request(
            String method,
            Map<String, Object> params) {
        return McpTestHttpClient.request(
                "stable-" + ids.incrementAndGet(),
                method,
                params
        );
    }

    private void requireSuccess(McpTestHttpClient.Response response) {
        if (response.status() < 200 || response.status() >= 300) {
            throw new IllegalStateException(
                    "stable MCP request failed: " + response.status()
            );
        }
    }
}
