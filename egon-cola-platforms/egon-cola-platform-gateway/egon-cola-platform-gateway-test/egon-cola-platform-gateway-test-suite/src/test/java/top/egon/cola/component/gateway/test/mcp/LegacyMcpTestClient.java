package top.egon.cola.component.gateway.test.mcp;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class LegacyMcpTestClient {

    private final McpTestHttpClient http = new McpTestHttpClient();

    private final URI endpoint;

    private final String token;

    private final AtomicLong ids = new AtomicLong();

    private String sessionId;

    public LegacyMcpTestClient(URI endpoint, String token) {
        this.endpoint = endpoint;
        this.token = token;
    }

    public void open() throws Exception {
        McpTestHttpClient.Session session = http.openSse(endpoint, token);
        if (session.status() != 200 || session.endpoint().isBlank()) {
            throw new IllegalStateException("legacy MCP SSE endpoint missing");
        }
        sessionId = session.sessionId();
    }

    public Map<String, Object> call(
            String method,
            Map<String, Object> params) throws Exception {
        if (sessionId == null) {
            open();
        }
        McpTestHttpClient.Response accepted = http.post(
                endpoint,
                token,
                Map.of("MCP-Session-Id", sessionId),
                McpTestHttpClient.request(
                        "legacy-" + ids.incrementAndGet(),
                        method,
                        params
                )
        );
        if (accepted.status() != 202) {
            throw new IllegalStateException(
                    "legacy MCP request was not accepted: "
                            + accepted.status()
            );
        }
        return http.readSseEvent(endpoint, token, sessionId);
    }

    public String sessionId() {
        return sessionId;
    }
}
