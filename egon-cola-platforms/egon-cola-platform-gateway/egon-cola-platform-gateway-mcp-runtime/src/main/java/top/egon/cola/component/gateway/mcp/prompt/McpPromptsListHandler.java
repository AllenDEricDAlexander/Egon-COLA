package top.egon.cola.component.gateway.mcp.prompt;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class McpPromptsListHandler implements McpMethodHandler {

    private final Supplier<CompiledMcpRules> rules;

    private final McpSecurityGate security;

    public McpPromptsListHandler(
            Supplier<CompiledMcpRules> rules,
            McpSecurityGate security) {
        this.rules = Objects.requireNonNull(rules, "rules");
        this.security = Objects.requireNonNull(security, "security");
    }

    @Override
    public String method() {
        return "prompts/list";
    }

    @Override
    public Publisher<McpJsonRpcResponse> handle(
            McpJsonRpcRequest request,
            McpRequestContext context) {
        McpSecurityGate.IdentityContext identity = identity(context);
        return Flux.fromIterable(prompts(context.server().serverCode()))
                .concatMap(prompt -> Mono.from(
                                security.authorizePrompt(prompt, identity)
                        ).thenReturn(describe(prompt))
                        .onErrorResume(
                                McpProtocolException.class,
                                ignored -> Mono.empty()
                        ))
                .collectList()
                .map(prompts -> McpJsonRpcResponse.success(
                        request.id(),
                        Map.of("prompts", prompts)
                ));
    }

    private List<McpRuntimePrompt> prompts(String serverCode) {
        CompiledMcpRules active = rules.get();
        if (active == null) {
            return List.of();
        }
        return active.promptsByQualifiedName().values().stream()
                .filter(McpRuntimePrompt::enabled)
                .filter(prompt -> prompt.serverCode().equals(serverCode))
                .filter(prompt -> active.remoteAvailable(
                        prompt.remoteMountId(),
                        "PROMPT"
                ))
                .sorted(java.util.Comparator.comparing(
                        McpRuntimePrompt::name
                ))
                .toList();
    }

    private Map<String, Object> describe(McpRuntimePrompt prompt) {
        LinkedHashMap<String, Object> value = new LinkedHashMap<>();
        value.put("name", prompt.name());
        if (prompt.description() != null) {
            value.put("description", prompt.description());
        }
        value.put("arguments", prompt.arguments().stream()
                .map(name -> Map.<String, Object>of(
                        "name", name,
                        "required", true
                ))
                .toList());
        return Map.copyOf(value);
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
}
