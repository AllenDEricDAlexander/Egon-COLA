package top.egon.cola.component.gateway.mcp.completion;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimePrompt;
import top.egon.cola.component.gateway.mcp.prompt.McpPromptDriver;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;
import top.egon.cola.component.gateway.mcp.resource.McpResourceCatalog;
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

public final class McpCompletionHandler implements McpMethodHandler {

    private final Supplier<CompiledMcpRules> rules;

    private final McpResourceCatalog resources;

    private final Map<String, McpCompletionProvider> providers;

    private final McpSecurityGate security;

    public McpCompletionHandler(
            Supplier<CompiledMcpRules> rules,
            McpResourceCatalog resources,
            List<McpCompletionProvider> providers,
            McpSecurityGate security) {
        this.rules = Objects.requireNonNull(rules, "rules");
        this.resources = Objects.requireNonNull(resources, "resources");
        this.providers = index(providers);
        this.security = Objects.requireNonNull(security, "security");
    }

    @Override
    public String method() {
        return "completion/complete";
    }

    @Override
    public Publisher<McpJsonRpcResponse> handle(
            McpJsonRpcRequest request,
            McpRequestContext context) {
        Map<?, ?> reference = object(request.params().get("ref"), "ref");
        Map<?, ?> argument = object(
                request.params().get("argument"),
                "argument"
        );
        String referenceType = string(reference.get("type"), "ref.type");
        String argumentName = string(
                argument.get("name"),
                "argument.name"
        );
        String prefix = optional(argument.get("value"));
        McpSecurityGate.IdentityContext identity = identity(context);
        Resolved resolved = switch (referenceType) {
            case "ref/prompt" -> prompt(
                    context.server().serverCode(),
                    string(reference.get("name"), "ref.name"),
                    argumentName,
                    identity
            );
            case "ref/resource" -> resource(
                    context.server().serverCode(),
                    string(reference.get("uri"), "ref.uri"),
                    identity
            );
            default -> throw McpPromptDriver.invalid(
                    "MCP completion reference type is invalid"
            );
        };
        McpCompletionProvider provider = providers.get(resolved.sourceType());
        if (provider == null) {
            throw McpPromptDriver.invalid(
                    "MCP completion provider is unavailable"
            );
        }
        return Mono.from(resolved.authorization())
                .then(Mono.from(provider.complete(
                        new McpCompletionProvider.Request(
                                context.server().serverCode(),
                                referenceType,
                                resolved.referenceName(),
                                argumentName,
                                prefix,
                                resolved.operationId(),
                                attributes(context)
                        )
                )))
                .map(result -> McpJsonRpcResponse.success(
                        request.id(),
                        Map.of("completion", Map.of(
                                "values", result.values(),
                                "total", result.total(),
                                "hasMore", result.hasMore()
                        ))
                ));
    }

    private Resolved prompt(
            String serverCode,
            String name,
            String argumentName,
            McpSecurityGate.IdentityContext identity) {
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
        if (!prompt.arguments().contains(argumentName)) {
            throw McpPromptDriver.invalid(
                    "MCP prompt argument is not declared"
            );
        }
        String sourceType = prompt.remoteMountId() != null
                ? "REMOTE_MCP"
                : "LOCAL_OPERATION".equals(prompt.sourceType())
                        ? "LOCAL_OPERATION"
                        : "LOCAL_DICTIONARY";
        return new Resolved(
                name,
                sourceType,
                prompt.operationId(),
                security.authorizePrompt(prompt, identity)
        );
    }

    private Resolved resource(
            String serverCode,
            String uri,
            McpSecurityGate.IdentityContext identity) {
        McpResourceCatalog.ResolvedResource resolved = resources.resolve(
                serverCode,
                uri
        );
        Publisher<Void> authorization = resolved.resource() != null
                ? security.authorizeResourceRead(
                        resolved.resource(),
                        identity
                )
                : security.authorizeResourceRead(
                        resolved.template(),
                        identity
                );
        String sourceType = resolved.remoteMountId() != null
                ? "REMOTE_MCP"
                : "LOCAL_OPERATION".equals(resolved.driverType())
                        ? "LOCAL_OPERATION"
                        : "LOCAL_DICTIONARY";
        return new Resolved(
                resolved.uri(),
                sourceType,
                resolved.operationId(),
                authorization
        );
    }

    private Map<String, McpCompletionProvider> index(
            List<McpCompletionProvider> source) {
        LinkedHashMap<String, McpCompletionProvider> result =
                new LinkedHashMap<>();
        Objects.requireNonNull(source, "providers").forEach(provider -> {
            if (result.putIfAbsent(
                    provider.sourceType(),
                    provider
            ) != null) {
                throw new IllegalArgumentException(
                        "MCP completion source types must be unique"
                );
            }
        });
        return Collections.unmodifiableMap(result);
    }

    private Map<?, ?> object(Object value, String field) {
        if (!(value instanceof Map<?, ?> map)) {
            throw McpPromptDriver.invalid(
                    "MCP completion " + field + " must be an object"
            );
        }
        return map;
    }

    private String string(Object value, String field) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw McpPromptDriver.invalid(
                    "MCP completion " + field + " is required"
            );
        }
        return text.trim();
    }

    private String optional(Object value) {
        if (value == null) {
            return "";
        }
        if (!(value instanceof String text)) {
            throw McpPromptDriver.invalid(
                    "MCP completion argument value must be a string"
            );
        }
        return text;
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

    private record Resolved(
            String referenceName,
            String sourceType,
            String operationId,
            Publisher<Void> authorization
    ) {
    }
}
