package top.egon.cola.component.gateway.test.mcp.remote;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Deterministic Remote MCP server covering every remotely mountable primitive.
 */
@RestController
@RequestMapping("/remote")
public class RemoteMcpFixtureController {

    private static final String DASHBOARD = """
            <!doctype html><html><head><meta charset="utf-8"></head>
            <body><main id="app">Remote MCP Dashboard</main></body></html>
            """;

    private final AtomicBoolean unavailable = new AtomicBoolean();

    @PostMapping({"/stable", "/rc"})
    public ResponseEntity<Map<String, Object>> exchange(
            @RequestBody Map<String, Object> request,
            @RequestHeader HttpHeaders headers) {
        if (unavailable.get()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "REMOTE_FIXTURE_UNAVAILABLE"));
        }
        Object id = request.get("id");
        String method = Objects.toString(request.get("method"), "");
        boolean rc = "2026-07-28".equals(headers.getFirst(
                "MCP-Protocol-Version"
        ));
        if (rc && !method.equals(headers.getFirst("MCP-Method"))) {
            return jsonRpcError(id, -32600, "MCP method header mismatch");
        }
        Map<String, Object> params = object(request.get("params"));
        Map<String, Object> result;
        try {
            result = dispatch(method, params, rc);
        } catch (FixtureFailure failure) {
            return jsonRpcError(id, failure.code, failure.getMessage());
        }
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if ("initialize".equals(method) && !rc) {
            response.header("MCP-Session-Id", "remote-stable-session");
        }
        return response.body(jsonRpc(id, result));
    }

    @PostMapping("/control/fail")
    public Map<String, Object> fail() {
        unavailable.set(true);
        return Map.of("unavailable", true);
    }

    @PostMapping("/control/recover")
    public Map<String, Object> recover() {
        unavailable.set(false);
        return Map.of("unavailable", false);
    }

    private Map<String, Object> dispatch(
            String method,
            Map<String, Object> params,
            boolean rc) {
        return switch (method) {
            case "initialize", "discover" -> Map.of(
                    "protocolVersion", rc ? "2026-07-28" : "2025-11-25",
                    "capabilities", Map.of(
                            "tools", Map.of("listChanged", true),
                            "resources", Map.of("subscribe", true),
                            "prompts", Map.of("listChanged", true),
                            "completion", Map.of(),
                            "apps", Map.of()
                    ),
                    "serverInfo", Map.of(
                            "name", "egon-remote-mcp-fixture",
                            "version", "1.0.0"
                    )
            );
            case "ping" -> Map.of();
            case "tools/list" -> Map.of("tools", List.of(
                    tool("remote_echo", "Echoes arguments", false),
                    tool("remote_failure", "Returns a deterministic error", false)
            ));
            case "tools/call" -> callTool(params);
            case "resources/list" -> Map.of("resources", List.of(
                    resource("remote_text", "fixture://remote/text", "text/plain"),
                    resource("remote_blob", "fixture://remote/blob", "application/octet-stream"),
                    resource("remote_dashboard", "ui://remote/remote_dashboard", "text/html")
            ));
            case "resources/templates/list" -> Map.of("resourceTemplates", List.of(
                    Map.of(
                            "name", "remote_order",
                            "uriTemplate", "fixture://remote/orders/{id}",
                            "mimeType", "application/json"
                    )
            ));
            case "resources/read" -> readResource(params);
            case "resources/subscribe", "resources/unsubscribe" -> Map.of(
                    "subscribed", "resources/subscribe".equals(method)
            );
            case "prompts/list" -> Map.of("prompts", List.of(Map.of(
                    "name", "remote_summary",
                    "description", "Builds a deterministic summary prompt",
                    "arguments", List.of(Map.of(
                            "name", "topic",
                            "required", true
                    ))
            )));
            case "prompts/get" -> prompt(params);
            case "completion/complete" -> Map.of(
                    "completion", Map.of(
                            "values", List.of("order-1", "order-2"),
                            "total", 2,
                            "hasMore", false
                    )
            );
            case "apps/list" -> Map.of("apps", List.of(Map.of(
                    "name", "remote_dashboard",
                    "resourceUri", "ui://remote/remote_dashboard",
                    "mimeType", "text/html",
                    "sha256", "fixture-dashboard-sha256"
            )));
            default -> throw new FixtureFailure(-32601, "method not found");
        };
    }

    private Map<String, Object> callTool(Map<String, Object> params) {
        String name = Objects.toString(params.get("name"), "");
        if ("remote_failure".equals(name)) {
            throw new FixtureFailure(-32050, "deterministic remote failure");
        }
        if (!"remote_echo".equals(name)) {
            throw new FixtureFailure(-32602, "unknown remote tool");
        }
        return Map.of(
                "content", List.of(Map.of(
                        "type", "text",
                        "text", Objects.toString(
                                object(params.get("arguments")).get("value"),
                                ""
                        )
                )),
                "structuredContent", object(params.get("arguments")),
                "isError", false
        );
    }

    private Map<String, Object> readResource(Map<String, Object> params) {
        String uri = Objects.toString(params.get("uri"), "");
        if ("fixture://remote/text".equals(uri)) {
            return contents(uri, "text/plain", "remote fixture text", null);
        }
        if ("fixture://remote/blob".equals(uri)) {
            String blob = Base64.getEncoder().encodeToString(
                    new byte[]{0, 1, 2, 3, 4}
            );
            return contents(uri, "application/octet-stream", null, blob);
        }
        if ("ui://remote/remote_dashboard".equals(uri)) {
            return contents(uri, "text/html", DASHBOARD, null);
        }
        if (uri.matches("fixture://remote/orders/[A-Za-z0-9_-]+")) {
            String id = uri.substring(uri.lastIndexOf('/') + 1);
            return contents(
                    uri,
                    "application/json",
                    "{\"id\":\"" + id + "\",\"status\":\"CREATED\"}",
                    null
            );
        }
        throw new FixtureFailure(-32002, "resource not found");
    }

    private Map<String, Object> prompt(Map<String, Object> params) {
        if (!"remote_summary".equals(params.get("name"))) {
            throw new FixtureFailure(-32602, "unknown remote prompt");
        }
        String topic = Objects.toString(
                object(params.get("arguments")).get("topic"),
                "fixture"
        );
        return Map.of(
                "description", "Deterministic remote summary",
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", Map.of(
                                "type", "text",
                                "text", "Summarize " + topic
                        )
                ))
        );
    }

    private Map<String, Object> tool(
            String name,
            String description,
            boolean destructive) {
        return Map.of(
                "name", name,
                "description", description,
                "inputSchema", Map.of(
                        "type", "object",
                        "additionalProperties", true
                ),
                "annotations", Map.of(
                        "destructiveHint", destructive,
                        "idempotentHint", true
                )
        );
    }

    private Map<String, Object> resource(
            String name,
            String uri,
            String mimeType) {
        return Map.of("name", name, "uri", uri, "mimeType", mimeType);
    }

    private Map<String, Object> contents(
            String uri,
            String mimeType,
            String text,
            String blob) {
        LinkedHashMap<String, Object> content = new LinkedHashMap<>();
        content.put("uri", uri);
        content.put("mimeType", mimeType);
        if (text != null) {
            content.put("text", text);
        }
        if (blob != null) {
            content.put("blob", blob);
        }
        return Map.of("contents", List.of(Map.copyOf(content)));
    }

    private Map<String, Object> jsonRpc(Object id, Map<String, Object> result) {
        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", result);
        return Map.copyOf(response);
    }

    private ResponseEntity<Map<String, Object>> jsonRpcError(
            Object id,
            int code,
            String message) {
        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("error", Map.of("code", code, "message", message));
        return ResponseEntity.ok(Map.copyOf(response));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> object(Object value) {
        return value instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
    }

    private static final class FixtureFailure extends RuntimeException {

        private final int code;

        private FixtureFailure(int code, String message) {
            super(message);
            this.code = code;
        }
    }
}
