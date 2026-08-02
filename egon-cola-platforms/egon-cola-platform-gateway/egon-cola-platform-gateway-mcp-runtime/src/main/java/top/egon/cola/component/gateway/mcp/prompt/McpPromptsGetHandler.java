package top.egon.cola.component.gateway.mcp.prompt;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimePrompt;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;
import top.egon.cola.component.gateway.mcp.rule.CompiledMcpRules;
import top.egon.cola.component.gateway.mcp.security.McpSecurityGate;
import top.egon.cola.component.gateway.mcp.server.McpMethodHandler;
import top.egon.cola.component.gateway.mcp.server.McpRequestContext;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class McpPromptsGetHandler implements McpMethodHandler {

    private final Supplier<CompiledMcpRules> rules;

    private final Map<String, McpPromptDriver> drivers;

    private final McpSecurityGate security;

    public McpPromptsGetHandler(
            Supplier<CompiledMcpRules> rules,
            List<McpPromptDriver> drivers,
            McpSecurityGate security) {
        this.rules = Objects.requireNonNull(rules, "rules");
        this.drivers = index(drivers);
        this.security = Objects.requireNonNull(security, "security");
    }

    @Override
    public String method() {
        return "prompts/get";
    }

    @Override
    public Publisher<McpJsonRpcResponse> handle(
            McpJsonRpcRequest request,
            McpRequestContext context) {
        McpRuntimePrompt prompt = prompt(
                context.server().serverCode(),
                string(request.params().get("name"), "name")
        );
        McpPromptDriver driver = drivers.get(prompt.sourceType());
        if (driver == null) {
            throw McpPromptDriver.invalid(
                    "MCP prompt driver is unavailable"
            );
        }
        Map<String, String> arguments = arguments(
                request.params().get("arguments")
        );
        McpSecurityGate.IdentityContext identity = identity(context);
        return Mono.from(security.authorizePrompt(prompt, identity))
                .then(Mono.from(driver.render(
                        prompt,
                        arguments,
                        attributes(context)
                )))
                .map(result -> McpJsonRpcResponse.success(
                        request.id(),
                        describe(result)
                ));
    }

    private McpRuntimePrompt prompt(String serverCode, String name) {
        CompiledMcpRules active = rules.get();
        McpRuntimePrompt prompt = active == null
                ? null
                : active.promptsByQualifiedName().get(
                        CompiledMcpRules.qualified(serverCode, name)
                );
        if (prompt == null || !prompt.enabled()
                || !active.remoteAvailable(
                prompt.remoteMountId(),
                "PROMPT"
        )) {
            throw McpPromptDriver.invalid("MCP prompt was not found");
        }
        return prompt;
    }

    private Map<String, Object> describe(McpPromptDriver.Result result) {
        return Map.of(
                "description", result.description(),
                "messages", result.messages().stream()
                        .map(message -> Map.<String, Object>of(
                                "role", message.role(),
                                "content", Map.of(
                                        "type", "text",
                                        "text", message.text()
                                )
                        ))
                        .toList()
        );
    }

    private Map<String, McpPromptDriver> index(
            List<McpPromptDriver> source) {
        LinkedHashMap<String, McpPromptDriver> result = new LinkedHashMap<>();
        Objects.requireNonNull(source, "drivers").forEach(driver ->
                driver.sourceTypes().forEach(type -> {
                    if (result.putIfAbsent(type, driver) != null) {
                        throw new IllegalArgumentException(
                                "MCP prompt source types must be unique"
                        );
                    }
                }));
        return Collections.unmodifiableMap(result);
    }

    private Map<String, String> arguments(Object value) {
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> source)) {
            throw McpPromptDriver.invalid(
                    "MCP prompt arguments must be an object"
            );
        }
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        source.forEach((key, item) -> {
            if (!(key instanceof String name)
                    || !(item instanceof String text)) {
                throw McpPromptDriver.invalid(
                        "MCP prompt arguments must contain strings"
                );
            }
            result.put(name, text);
        });
        return Map.copyOf(result);
    }

    private String string(Object value, String field) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw McpPromptDriver.invalid(
                    "MCP prompt " + field + " is required"
            );
        }
        return text.trim();
    }

    private McpSecurityGate.IdentityContext identity(
            McpRequestContext context) {
        try {
            return McpSecurityGate.IdentityContext.from(context.attributes());
        } catch (IllegalArgumentException failure) {
            throw new McpProtocolException(
                    McpErrorCode.MCP_UNAUTHENTICATED,
                    "MCP identity context is incomplete"
            );
        }
    }

    private Map<String, Object> attributes(McpRequestContext context) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>(
                context.attributes()
        );
        result.put("mcp.protocol-dialect", context.dialect());
        return Map.copyOf(result);
    }
}
