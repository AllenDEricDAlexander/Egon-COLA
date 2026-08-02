package top.egon.cola.component.gateway.mcp.remote;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimePrompt;
import top.egon.cola.component.gateway.core.mcp.remote.RemoteAuthProvider;
import top.egon.cola.component.gateway.mcp.prompt.McpPromptDriver;
import top.egon.cola.component.gateway.mcp.rule.CompiledMcpRules;
import top.egon.cola.component.gateway.mcp.security.McpSecurityGate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Prompt Strategy for reviewed REMOTE_MCP descriptors.
 */
public final class RemoteMcpPromptDriver implements McpPromptDriver {

    private final Supplier<CompiledMcpRules> rules;

    private final McpRemoteClientPool clients;

    private final McpNamespaceRouter router;

    private final McpDialectTranslator translator;

    public RemoteMcpPromptDriver(
            Supplier<CompiledMcpRules> rules,
            McpRemoteClientPool clients,
            McpNamespaceRouter router,
            McpDialectTranslator translator) {
        this.rules = Objects.requireNonNull(rules, "rules");
        this.clients = Objects.requireNonNull(clients, "clients");
        this.router = Objects.requireNonNull(router, "router");
        this.translator = Objects.requireNonNull(translator, "translator");
    }

    @Override
    public Set<String> sourceTypes() {
        return Set.of("REMOTE_MCP");
    }

    @Override
    public Publisher<Result> render(
            McpRuntimePrompt prompt,
            Map<String, String> arguments,
            Map<String, Object> attributes) {
        McpNamespaceRouter.Binding binding = router.binding(
                active(),
                prompt.remoteMountId(),
                "PROMPT",
                prompt.name()
        );
        McpDialectTranslator.OutboundCall call = translator.outbound(
                dialect(attributes),
                binding.provider().dialect(),
                "prompts/get",
                Map.of(
                        "name", binding.remoteName(),
                        "arguments", Map.copyOf(arguments)
                ),
                Map.of(),
                trace(attributes)
        );
        McpSecurityGate.IdentityContext identity =
                McpSecurityGate.IdentityContext.from(attributes);
        return Mono.from(clients.exchange(
                        binding.provider(),
                        call,
                        new RemoteAuthProvider.AuthContext(
                                identity.subjectId(),
                                identity.tenantId(),
                                identity.clientId()
                        )
                ))
                .map(translator::result)
                .map(this::result);
    }

    private Result result(Map<String, Object> source) {
        String description = source.get("description") instanceof String text
                ? text
                : "";
        if (!(source.get("messages") instanceof List<?> messages)) {
            throw McpPromptDriver.invalid(
                    "remote MCP prompt messages are invalid"
            );
        }
        ArrayList<Message> result = new ArrayList<>();
        messages.forEach(value -> {
            if (!(value instanceof Map<?, ?> message)
                    || !(message.get("role") instanceof String role)
                    || !(message.get("content") instanceof Map<?, ?> content)
                    || !(content.get("text") instanceof String text)) {
                throw McpPromptDriver.invalid(
                        "remote MCP prompt message is invalid"
                );
            }
            result.add(new Message(role, text));
        });
        return new Result(description, result);
    }

    private CompiledMcpRules active() {
        CompiledMcpRules value = rules.get();
        return value == null ? CompiledMcpRules.empty() : value;
    }

    private McpProtocolDialect dialect(Map<String, Object> attributes) {
        Object value = attributes.get("mcp.protocol-dialect");
        return value instanceof McpProtocolDialect dialect
                ? dialect
                : McpProtocolDialect.STABLE_2025_11_25;
    }

    private Map<String, String> trace(Map<String, Object> attributes) {
        java.util.LinkedHashMap<String, String> result =
                new java.util.LinkedHashMap<>();
        List.of("traceparent", "tracestate", "x-egon-request-id")
                .forEach(name -> {
                    Object value = attributes.get(name);
                    if (value instanceof String text && !text.isBlank()) {
                        result.put(name, text.trim());
                    }
                });
        return Map.copyOf(result);
    }
}
