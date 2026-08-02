package top.egon.cola.component.gateway.mcp.remote;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimePrompt;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeResource;
import top.egon.cola.component.gateway.core.mcp.remote.RemoteAuthProvider;
import top.egon.cola.component.gateway.mcp.completion.McpCompletionProvider;
import top.egon.cola.component.gateway.mcp.prompt.McpPromptDriver;
import top.egon.cola.component.gateway.mcp.rule.CompiledMcpRules;
import top.egon.cola.component.gateway.mcp.security.McpSecurityGate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Completion Strategy for remote Prompt and Resource references.
 */
public final class RemoteMcpCompletionProvider
        implements McpCompletionProvider {

    private final Supplier<CompiledMcpRules> rules;

    private final McpRemoteClientPool clients;

    private final McpNamespaceRouter router;

    private final McpDialectTranslator translator;

    public RemoteMcpCompletionProvider(
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
    public String sourceType() {
        return "REMOTE_MCP";
    }

    @Override
    public Publisher<Result> complete(Request request) {
        if (McpCompletionProvider.sensitiveArgumentName(
                request.argumentName()
        ) || McpCompletionProvider.sensitiveValue(request.valuePrefix())) {
            return Mono.error(McpPromptDriver.invalid(
                    "MCP completion cannot enumerate sensitive values"
            ));
        }
        Reference reference = reference(request);
        McpNamespaceRouter.Binding binding = router.binding(
                active(),
                reference.mountId(),
                reference.primitiveType(),
                reference.exposedName()
        );
        Map<String, Object> ref = "ref/prompt".equals(request.referenceType())
                ? Map.of(
                        "type", "ref/prompt",
                        "name", binding.remoteName()
                )
                : Map.of(
                        "type", "ref/resource",
                        "uri", reference.remoteReference()
                );
        McpDialectTranslator.OutboundCall call = translator.outbound(
                dialect(request.attributes()),
                binding.provider().dialect(),
                "completion/complete",
                Map.of(
                        "ref", ref,
                        "argument", Map.of(
                                "name", request.argumentName(),
                                "value", request.valuePrefix()
                        )
                ),
                Map.of(),
                trace(request.attributes())
        );
        McpSecurityGate.IdentityContext identity =
                McpSecurityGate.IdentityContext.from(request.attributes());
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
        if (!(source.get("completion") instanceof Map<?, ?> completion)
                || !(completion.get("values") instanceof List<?> values)) {
            throw McpPromptDriver.invalid(
                    "remote MCP completion result is invalid"
            );
        }
        ArrayList<String> result = new ArrayList<>();
        values.forEach(value -> {
            if (!(value instanceof String text)
                    || McpCompletionProvider.sensitiveValue(text)
                    || result.size() >= 100) {
                return;
            }
            result.add(text);
        });
        int total = completion.get("total") instanceof Number number
                ? Math.max(result.size(), number.intValue())
                : result.size();
        boolean hasMore = Boolean.TRUE.equals(completion.get("hasMore"));
        return new Result(result, total, hasMore);
    }

    private Reference reference(Request request) {
        CompiledMcpRules current = active();
        if ("ref/prompt".equals(request.referenceType())) {
            McpRuntimePrompt prompt = current.promptsByQualifiedName().get(
                    CompiledMcpRules.qualified(
                            request.serverCode(),
                            request.referenceName()
                    )
            );
            if (prompt != null && prompt.remoteMountId() != null) {
                return new Reference(
                        prompt.remoteMountId(),
                        "PROMPT",
                        prompt.name(),
                        prompt.name()
                );
            }
        }
        if ("ref/resource".equals(request.referenceType())) {
            for (McpRuntimeResource resource
                    : current.resourcesByQualifiedName().values()) {
                if (resource.serverCode().equals(request.serverCode())
                        && resource.uri().equals(request.referenceName())
                        && resource.remoteMountId() != null) {
                    return new Reference(
                            resource.remoteMountId(),
                            "RESOURCE",
                            resource.name(),
                            resource.configuration().getOrDefault(
                                    "remoteUri",
                                    resource.uri()
                            )
                    );
                }
            }
        }
        throw McpPromptDriver.invalid(
                "remote MCP completion reference was not found"
        );
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

    private record Reference(
            String mountId,
            String primitiveType,
            String exposedName,
            String remoteReference
    ) {
    }
}
