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

/**
 * Deterministic server for the pinned 2026-07-28 draft conformance scenarios.
 *
 * <p>This controller intentionally remains separate from the Remote MCP
 * federation fixture. It models the draft stateless wire, caching hints,
 * standardized headers and ephemeral input-required results expected by the
 * official CLI.</p>
 */
@RestController
@RequestMapping("/conformance")
class RcConformanceController {

    private static final String PROTOCOL_VERSION = "2026-07-28";

    private static final String META_PROTOCOL_VERSION =
            "io.modelcontextprotocol/protocolVersion";

    private static final String META_CLIENT_CAPABILITIES =
            "io.modelcontextprotocol/clientCapabilities";

    private static final String META_SERVER_INFO =
            "io.modelcontextprotocol/serverInfo";

    private static final String HEADER_TOOL = "test_header_roundtrip";

    private static final String HEADER_ARGUMENT = "headerValue";

    private static final String HEADER_SUFFIX = "Test-Value";

    private static final String BASE64_PREFIX = "=?base64?";

    @PostMapping("/rc")
    ResponseEntity<Map<String, Object>> exchange(
            @RequestBody Map<String, Object> request,
            @RequestHeader HttpHeaders headers) {
        if (!request.containsKey("id")) {
            return ResponseEntity.accepted().build();
        }
        Object id = request.get("id");
        String method = Objects.toString(request.get("method"), "");
        Map<String, Object> params = object(request.get("params"));
        try {
            validateRequest(method, params, headers);
            Map<String, Object> result = dispatch(method, params);
            return ResponseEntity.ok(jsonRpc(id, normalize(method, result)));
        } catch (FixtureFailure failure) {
            return jsonRpcError(id, failure);
        }
    }

    private void validateRequest(
            String method,
            Map<String, Object> params,
            HttpHeaders headers) {
        Map<String, Object> meta = object(params.get("_meta"));
        if (!meta.containsKey(META_PROTOCOL_VERSION)
                || !meta.containsKey(META_CLIENT_CAPABILITIES)
                || !(meta.get(META_CLIENT_CAPABILITIES) instanceof Map)) {
            throw failure(
                    HttpStatus.BAD_REQUEST,
                    -32602,
                    "Invalid params: required MCP _meta fields are missing"
            );
        }

        String headerVersion = header(headers, "MCP-Protocol-Version");
        if (headerVersion == null) {
            throw failure(
                    HttpStatus.BAD_REQUEST,
                    -32020,
                    "Missing MCP-Protocol-Version header"
            );
        }
        String metaVersion = Objects.toString(
                meta.get(META_PROTOCOL_VERSION),
                ""
        );
        if (!headerVersion.equals(metaVersion)) {
            throw failure(
                    HttpStatus.BAD_REQUEST,
                    -32020,
                    "MCP-Protocol-Version header mismatch"
            );
        }
        if (!PROTOCOL_VERSION.equals(headerVersion)) {
            throw new FixtureFailure(
                    HttpStatus.BAD_REQUEST,
                    -32022,
                    "Unsupported protocol version",
                    Map.of(
                            "supported", List.of(PROTOCOL_VERSION),
                            "requested", headerVersion
                    )
            );
        }

        String methodHeader = header(headers, "Mcp-Method");
        if (!method.equals(methodHeader)) {
            throw failure(
                    HttpStatus.BAD_REQUEST,
                    -32020,
                    "Mcp-Method header mismatch"
            );
        }

        String expectedName = expectedName(method, params);
        if (expectedName != null
                && !expectedName.equals(header(headers, "Mcp-Name"))) {
            throw failure(
                    HttpStatus.BAD_REQUEST,
                    -32020,
                    "Mcp-Name header mismatch"
            );
        }
        if ("tools/call".equals(method)
                && HEADER_TOOL.equals(expectedName)) {
            validateCustomHeader(params, headers);
        }
    }

    private void validateCustomHeader(
            Map<String, Object> params,
            HttpHeaders headers) {
        String rawHeader = header(
                headers,
                "Mcp-Param-" + HEADER_SUFFIX
        );
        if (rawHeader == null) {
            throw failure(
                    HttpStatus.BAD_REQUEST,
                    -32020,
                    "Required Mcp-Param header is missing"
            );
        }
        String decoded = decodeHeaderValue(rawHeader);
        String bodyValue = Objects.toString(
                object(params.get("arguments")).get(HEADER_ARGUMENT),
                ""
        );
        if (!bodyValue.equals(decoded)) {
            throw failure(
                    HttpStatus.BAD_REQUEST,
                    -32020,
                    "Mcp-Param header mismatch"
            );
        }
    }

    private String decodeHeaderValue(String value) {
        if (!value.startsWith(BASE64_PREFIX) || !value.endsWith("?=")) {
            return value;
        }
        String encoded = value.substring(
                BASE64_PREFIX.length(),
                value.length() - 2
        );
        if (encoded.length() % 4 != 0
                || !encoded.matches("[A-Za-z0-9+/]*={0,2}")) {
            throw invalidBase64();
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(encoded);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw invalidBase64();
        }
    }

    private FixtureFailure invalidBase64() {
        return failure(
                HttpStatus.BAD_REQUEST,
                -32020,
                "Invalid Base64 Mcp-Param header"
        );
    }

    private Map<String, Object> dispatch(
            String method,
            Map<String, Object> params) {
        return switch (method) {
            case "server/discover" -> discover();
            case "tools/list" -> Map.of("tools", tools());
            case "tools/call" -> callTool(params);
            case "prompts/list" -> Map.of("prompts", prompts());
            case "prompts/get" -> getPrompt(params);
            case "resources/list" -> Map.of(
                    "resources", List.of(Map.of(
                            "uri", "test://rc-cache",
                            "name", "RC Cache Resource",
                            "description", "Draft caching fixture",
                            "mimeType", "text/plain"
                    ))
            );
            case "resources/templates/list" -> Map.of(
                    "resourceTemplates", List.of(Map.of(
                            "uriTemplate", "test://rc-template/{id}",
                            "name", "RC Cache Template",
                            "description", "Draft template fixture",
                            "mimeType", "text/plain"
                    ))
            );
            case "resources/read" -> readResource(params);
            case "completion/complete" -> Map.of(
                    "completion", Map.of(
                            "values", List.of(),
                            "total", 0,
                            "hasMore", false
                    )
            );
            default -> throw failure(
                    HttpStatus.NOT_FOUND,
                    -32601,
                    "Method not found: " + method
            );
        };
    }

    private Map<String, Object> discover() {
        return Map.of(
                "supportedVersions", List.of(PROTOCOL_VERSION),
                "capabilities", Map.of(
                        "tools", Map.of(),
                        "prompts", Map.of(),
                        "resources", Map.of()
                ),
                "_meta", Map.of(
                        META_SERVER_INFO,
                        Map.of(
                                "name", "egon-rc-conformance-fixture",
                                "version", "1.0.0"
                        )
                )
        );
    }

    private List<Map<String, Object>> tools() {
        return List.of(
                tool("test_streaming_elicitation"),
                tool(
                        HEADER_TOOL,
                        "Validates standardized custom headers",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        HEADER_ARGUMENT, Map.of(
                                                "type", "string",
                                                "x-mcp-header", HEADER_SUFFIX
                                        )
                                ),
                                "required", List.of(HEADER_ARGUMENT)
                        )
                ),
                tool("test_missing_capability"),
                tool("test_logging_tool"),
                tool("test_input_required_result_elicitation"),
                tool("test_input_required_result_sampling"),
                tool("test_input_required_result_list_roots"),
                tool("test_input_required_result_request_state"),
                tool("test_input_required_result_multiple_inputs"),
                tool("test_input_required_result_multi_round"),
                tool("test_input_required_result_tampered_state"),
                tool("test_input_required_result_capabilities")
        );
    }

    private List<Map<String, Object>> prompts() {
        return List.of(Map.of(
                "name", "test_input_required_result_prompt",
                "description", "Prompt exercising input-required results",
                "arguments", List.of()
        ));
    }

    private Map<String, Object> callTool(Map<String, Object> params) {
        String name = Objects.toString(params.get("name"), "");
        return switch (name) {
            case HEADER_TOOL,
                    "test_streaming_elicitation",
                    "test_logging_tool" -> complete("completed: " + name);
            case "test_missing_capability" -> missingCapability(params);
            case "test_input_required_result_elicitation" ->
                    elicitationFlow(params);
            case "test_input_required_result_sampling" ->
                    simpleInputFlow(
                            params,
                            "capital_question",
                            samplingRequest("What is the capital of France?"),
                            "Sampling response accepted"
                    );
            case "test_input_required_result_list_roots" ->
                    simpleInputFlow(
                            params,
                            "client_roots",
                            Map.of("method", "roots/list", "params", Map.of()),
                            "Roots response accepted"
                    );
            case "test_input_required_result_request_state" ->
                    requestStateFlow(params);
            case "test_input_required_result_multiple_inputs" ->
                    multipleInputFlow(params);
            case "test_input_required_result_multi_round" ->
                    multiRoundFlow(params);
            case "test_input_required_result_tampered_state" ->
                    tamperedStateFlow(params);
            case "test_input_required_result_capabilities" ->
                    capabilityInputFlow(params);
            default -> throw failure(
                    HttpStatus.BAD_REQUEST,
                    -32602,
                    "Unknown conformance tool: " + name
            );
        };
    }

    private Map<String, Object> missingCapability(
            Map<String, Object> params) {
        Map<String, Object> capabilities = capabilities(params);
        if (!(capabilities.get("sampling") instanceof Map)) {
            throw new FixtureFailure(
                    HttpStatus.BAD_REQUEST,
                    -32021,
                    "Required client capability is missing",
                    Map.of("requiredCapabilities", Map.of(
                            "sampling", Map.of()
                    ))
            );
        }
        return complete("Sampling capability is present");
    }

    private Map<String, Object> elicitationFlow(
            Map<String, Object> params) {
        if (hasInputResponse(params, "user_name")) {
            return complete("Hello, Alice!");
        }
        return inputRequired(Map.of(
                "user_name", elicitationRequest(
                        "What is your name?",
                        "name",
                        Map.of("type", "string")
                )
        ), null);
    }

    private Map<String, Object> simpleInputFlow(
            Map<String, Object> params,
            String responseKey,
            Map<String, Object> inputRequest,
            String completionText) {
        if (hasInputResponse(params, responseKey)) {
            return complete(completionText);
        }
        return inputRequired(Map.of(responseKey, inputRequest), null);
    }

    private Map<String, Object> requestStateFlow(
            Map<String, Object> params) {
        String state = "request-state-v1.signature";
        if (hasInputResponse(params, "confirm")) {
            requireState(params, state);
            return complete("state-ok");
        }
        return inputRequired(Map.of(
                "confirm", elicitationRequest(
                        "Please confirm",
                        "ok",
                        Map.of("type", "boolean")
                )
        ), state);
    }

    private Map<String, Object> multipleInputFlow(
            Map<String, Object> params) {
        String state = "multiple-inputs-v1.signature";
        if (hasInputResponse(params, "user_name")
                && hasInputResponse(params, "greeting")
                && hasInputResponse(params, "client_roots")) {
            requireState(params, state);
            return complete("All input responses accepted");
        }
        return inputRequired(Map.of(
                "user_name", elicitationRequest(
                        "What is your name?",
                        "name",
                        Map.of("type", "string")
                ),
                "greeting", samplingRequest("Generate a greeting"),
                "client_roots", Map.of(
                        "method", "roots/list",
                        "params", Map.of()
                )
        ), state);
    }

    private Map<String, Object> multiRoundFlow(
            Map<String, Object> params) {
        String state = Objects.toString(params.get("requestState"), "");
        if ("multi-round-1.signature".equals(state)
                && hasInputResponse(params, "step1")) {
            return inputRequired(Map.of(
                    "step2", elicitationRequest(
                            "What is your favorite color?",
                            "color",
                            Map.of("type", "string")
                    )
            ), "multi-round-2.signature");
        }
        if ("multi-round-2.signature".equals(state)
                && hasInputResponse(params, "step2")) {
            return complete("Multi-round input completed");
        }
        if (!state.isEmpty()) {
            throw failure(
                    HttpStatus.BAD_REQUEST,
                    -32602,
                    "Invalid multi-round requestState"
            );
        }
        return inputRequired(Map.of(
                "step1", elicitationRequest(
                        "What is your name?",
                        "name",
                        Map.of("type", "string")
                )
        ), "multi-round-1.signature");
    }

    private Map<String, Object> tamperedStateFlow(
            Map<String, Object> params) {
        String state = "tamper-proof-v1.signature";
        if (params.containsKey("inputResponses")) {
            requireState(params, state);
            return complete("Integrity-protected state accepted");
        }
        return inputRequired(Map.of(
                "confirm", elicitationRequest(
                        "Confirm the request",
                        "ok",
                        Map.of("type", "boolean")
                )
        ), state);
    }

    private Map<String, Object> capabilityInputFlow(
            Map<String, Object> params) {
        Map<String, Object> capabilities = capabilities(params);
        LinkedHashMap<String, Object> requests = new LinkedHashMap<>();
        if (capabilities.get("sampling") instanceof Map) {
            requests.put(
                    "sampling_only",
                    samplingRequest("Provide a sampling response")
            );
        }
        if (capabilities.get("elicitation") instanceof Map) {
            requests.put(
                    "elicitation_only",
                    elicitationRequest(
                            "Provide input",
                            "value",
                            Map.of("type", "string")
                    )
            );
        }
        if (requests.isEmpty()) {
            throw new FixtureFailure(
                    HttpStatus.BAD_REQUEST,
                    -32021,
                    "No supported input capability was declared",
                    Map.of("requiredCapabilities", Map.of(
                            "sampling", Map.of()
                    ))
            );
        }
        return inputRequired(Map.copyOf(requests), null);
    }

    private Map<String, Object> getPrompt(Map<String, Object> params) {
        String name = Objects.toString(params.get("name"), "");
        if (!"test_input_required_result_prompt".equals(name)) {
            throw failure(
                    HttpStatus.BAD_REQUEST,
                    -32602,
                    "Unknown conformance prompt: " + name
            );
        }
        if (hasInputResponse(params, "user_context")) {
            return Map.of(
                    "resultType", "complete",
                    "messages", List.of(Map.of(
                            "role", "user",
                            "content", Map.of(
                                    "type", "text",
                                    "text", "Prompt using test context"
                            )
                    ))
            );
        }
        return inputRequired(Map.of(
                "user_context", elicitationRequest(
                        "What context should the prompt use?",
                        "context",
                        Map.of("type", "string")
                )
        ), null);
    }

    private Map<String, Object> readResource(
            Map<String, Object> params) {
        String uri = Objects.toString(params.get("uri"), "");
        if (!"test://rc-cache".equals(uri)) {
            throw new FixtureFailure(
                    HttpStatus.BAD_REQUEST,
                    -32602,
                    "Resource not found",
                    Map.of("uri", uri)
            );
        }
        return Map.of("contents", List.of(Map.of(
                "uri", uri,
                "mimeType", "text/plain",
                "text", "RC caching fixture"
        )));
    }

    private Map<String, Object> elicitationRequest(
            String message,
            String propertyName,
            Map<String, Object> propertySchema) {
        return Map.of(
                "method", "elicitation/create",
                "params", Map.of(
                        "message", message,
                        "requestedSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        propertyName,
                                        propertySchema
                                ),
                                "required", List.of(propertyName)
                        )
                )
        );
    }

    private Map<String, Object> samplingRequest(String prompt) {
        return Map.of(
                "method", "sampling/createMessage",
                "params", Map.of(
                        "messages", List.of(Map.of(
                                "role", "user",
                                "content", Map.of(
                                        "type", "text",
                                        "text", prompt
                                )
                        )),
                        "maxTokens", 100
                )
        );
    }

    private Map<String, Object> inputRequired(
            Map<String, Object> inputRequests,
            String requestState) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("resultType", "input_required");
        result.put("inputRequests", inputRequests);
        if (requestState != null) {
            result.put("requestState", requestState);
        }
        return Map.copyOf(result);
    }

    private Map<String, Object> complete(String text) {
        return Map.of(
                "resultType", "complete",
                "content", List.of(Map.of(
                        "type", "text",
                        "text", text
                )),
                "isError", false
        );
    }

    private void requireState(
            Map<String, Object> params,
            String expectedState) {
        if (!expectedState.equals(params.get("requestState"))) {
            throw failure(
                    HttpStatus.BAD_REQUEST,
                    -32602,
                    "requestState integrity validation failed"
            );
        }
    }

    private boolean hasInputResponse(
            Map<String, Object> params,
            String key) {
        Object responses = params.get("inputResponses");
        if (!(responses instanceof Map<?, ?> responseMap)) {
            return false;
        }
        return responseMap.get(key) instanceof Map<?, ?>;
    }

    private Map<String, Object> capabilities(
            Map<String, Object> params) {
        Map<String, Object> meta = object(params.get("_meta"));
        return object(meta.get(META_CLIENT_CAPABILITIES));
    }

    private Map<String, Object> normalize(
            String method,
            Map<String, Object> result) {
        LinkedHashMap<String, Object> normalized = new LinkedHashMap<>(result);
        normalized.putIfAbsent("resultType", "complete");
        if (isCacheable(method)) {
            normalized.putIfAbsent("ttlMs", 0);
            normalized.putIfAbsent("cacheScope", "private");
        }
        return Map.copyOf(normalized);
    }

    private boolean isCacheable(String method) {
        return switch (method) {
            case "server/discover",
                    "tools/list",
                    "prompts/list",
                    "resources/list",
                    "resources/templates/list",
                    "resources/read" -> true;
            default -> false;
        };
    }

    private Map<String, Object> tool(String name) {
        return tool(
                name,
                "RC conformance tool " + name,
                Map.of(
                        "type", "object",
                        "properties", Map.of()
                )
        );
    }

    private Map<String, Object> tool(
            String name,
            String description,
            Map<String, Object> inputSchema) {
        return Map.of(
                "name", name,
                "description", description,
                "inputSchema", inputSchema
        );
    }

    private String expectedName(
            String method,
            Map<String, Object> params) {
        return switch (method) {
            case "tools/call", "prompts/get" ->
                    stringOrNull(params.get("name"));
            case "resources/read" -> stringOrNull(params.get("uri"));
            case "tasks/get", "tasks/update", "tasks/cancel" ->
                    stringOrNull(params.get("taskId"));
            default -> null;
        };
    }

    private String stringOrNull(Object value) {
        return value instanceof String string ? string : null;
    }

    private String header(HttpHeaders headers, String name) {
        String value = headers.getFirst(name);
        return value == null ? null : value.trim();
    }

    private Map<String, Object> jsonRpc(
            Object id,
            Map<String, Object> result) {
        return Map.of(
                "jsonrpc", "2.0",
                "id", id,
                "result", result
        );
    }

    private ResponseEntity<Map<String, Object>> jsonRpcError(
            Object id,
            FixtureFailure failure) {
        LinkedHashMap<String, Object> error = new LinkedHashMap<>();
        error.put("code", failure.code);
        error.put("message", failure.getMessage());
        if (!failure.data.isEmpty()) {
            error.put("data", failure.data);
        }
        return ResponseEntity.status(failure.status).body(Map.of(
                "jsonrpc", "2.0",
                "id", id,
                "error", Map.copyOf(error)
        ));
    }

    private FixtureFailure failure(
            HttpStatus status,
            int code,
            String message) {
        return new FixtureFailure(status, code, message, Map.of());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> object(Object value) {
        return value instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
    }

    private static final class FixtureFailure extends RuntimeException {

        private final HttpStatus status;
        private final int code;
        private final Map<String, Object> data;

        private FixtureFailure(
                HttpStatus status,
                int code,
                String message,
                Map<String, Object> data) {
            super(message);
            this.status = status;
            this.code = code;
            this.data = data;
        }
    }
}
