package top.egon.cola.component.gateway.test.mcp;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class RcMcpTestClient {

    private static final String VERSION = "2026-07-28";

    private final McpTestHttpClient http = new McpTestHttpClient();

    private final URI endpoint;

    private final String token;

    private final AtomicLong ids = new AtomicLong();

    public RcMcpTestClient(URI endpoint, String token) {
        this.endpoint = endpoint;
        this.token = token;
    }

    public Map<String, Object> call(
            String method,
            Map<String, Object> sourceParams) throws Exception {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>(
                sourceParams
        );
        params.put("_meta", Map.of(
                "client", "rc-test-client",
                "protocolVersion", VERSION
        ));
        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        headers.put("MCP-Protocol-Version", VERSION);
        headers.put("MCP-Method", method);
        Object name = sourceParams.get("name");
        if (name != null) {
            headers.put("MCP-Name", name.toString());
        }
        McpTestHttpClient.Response response = http.post(
                endpoint,
                token,
                Map.copyOf(headers),
                McpTestHttpClient.request(
                        "rc-" + ids.incrementAndGet(),
                        method,
                        Map.copyOf(params)
                )
        );
        if (response.status() < 200 || response.status() >= 300) {
            throw new IllegalStateException(
                    "RC MCP request failed: " + response.status()
            );
        }
        return response.body();
    }
}
