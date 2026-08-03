package top.egon.cola.component.gateway.test.mcp.remote;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RcConformanceControllerTest {

    private final RcConformanceController controller =
            new RcConformanceController();

    @Test
    void rejectsAMethodHeaderThatDoesNotMatchTheJsonRpcMethod() {
        HttpHeaders headers = headers("prompts/list", null);
        var response = controller.exchange(
                request(1, "tools/list", Map.of()),
                headers
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(-32020, error(response).get("code"));
    }

    @Test
    void completesAnInputRequiredElicitationOnRetry() {
        HttpHeaders headers = headers(
                "tools/call",
                "test_input_required_result_elicitation"
        );
        var initial = controller.exchange(
                request(1, "tools/call", Map.of(
                        "name", "test_input_required_result_elicitation",
                        "arguments", Map.of()
                )),
                headers
        );

        assertEquals(
                "input_required",
                result(initial).get("resultType")
        );

        var completed = controller.exchange(
                request(2, "tools/call", Map.of(
                        "name", "test_input_required_result_elicitation",
                        "arguments", Map.of(),
                        "inputResponses", Map.of(
                                "user_name", Map.of(
                                        "action", "accept",
                                        "content", Map.of("name", "Alice")
                                )
                        )
                )),
                headers
        );

        assertEquals("complete", result(completed).get("resultType"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void keepsTheFirstListedToolFreeOfCustomHeaderRequirements() {
        var listed = controller.exchange(
                request(1, "tools/list", Map.of()),
                headers("tools/list", null)
        );
        var tools = (java.util.List<Map<String, Object>>)
                result(listed).get("tools");
        String firstTool = (String) tools.getFirst().get("name");

        var called = controller.exchange(
                request(2, "tools/call", Map.of(
                        "name", firstTool,
                        "arguments", Map.of()
                )),
                headers("tools/call", firstTool)
        );

        assertEquals(HttpStatus.OK, called.getStatusCode());
    }

    private Map<String, Object> request(
            int id,
            String method,
            Map<String, Object> params) {
        return Map.of(
                "jsonrpc", "2.0",
                "id", id,
                "method", method,
                "params", mergeMeta(params)
        );
    }

    private Map<String, Object> mergeMeta(Map<String, Object> params) {
        var values = new java.util.LinkedHashMap<String, Object>(params);
        values.put("_meta", Map.of(
                "io.modelcontextprotocol/protocolVersion", "2026-07-28",
                "io.modelcontextprotocol/clientCapabilities", Map.of(),
                "io.modelcontextprotocol/clientInfo", Map.of(
                        "name", "test",
                        "version", "1.0.0"
                )
        ));
        return Map.copyOf(values);
    }

    private HttpHeaders headers(String method, String name) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("MCP-Protocol-Version", "2026-07-28");
        headers.set("Mcp-Method", method);
        if (name != null) {
            headers.set("Mcp-Name", name);
        }
        return headers;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> result(
            org.springframework.http.ResponseEntity<Map<String, Object>>
                    response) {
        return (Map<String, Object>) response.getBody().get("result");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> error(
            org.springframework.http.ResponseEntity<Map<String, Object>>
                    response) {
        return (Map<String, Object>) response.getBody().get("error");
    }
}
