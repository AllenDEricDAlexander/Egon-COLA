package top.egon.cola.component.gateway.mcp.remote;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTool;
import top.egon.cola.component.gateway.core.mcp.remote.RemoteAuthProvider;
import top.egon.cola.component.gateway.mcp.rule.CompiledMcpRules;
import top.egon.cola.component.gateway.mcp.security.McpSecurityGate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Strategy for invoking reviewed REMOTE_MCP Tool descriptors.
 */
public final class RemoteMcpToolDriver {

    private final Supplier<CompiledMcpRules> rules;

    private final McpRemoteClientPool clients;

    private final McpNamespaceRouter router;

    private final McpDialectTranslator translator;

    public RemoteMcpToolDriver(
            Supplier<CompiledMcpRules> rules,
            McpRemoteClientPool clients,
            McpNamespaceRouter router,
            McpDialectTranslator translator) {
        this.rules = Objects.requireNonNull(rules, "rules");
        this.clients = Objects.requireNonNull(clients, "clients");
        this.router = Objects.requireNonNull(router, "router");
        this.translator = Objects.requireNonNull(translator, "translator");
    }

    public Publisher<Map<String, Object>> invoke(
            McpRuntimeTool tool,
            Map<String, Object> arguments,
            McpSecurityGate.IdentityContext identity,
            McpProtocolDialect inboundDialect,
            Map<String, Object> meta,
            Map<String, String> traceHeaders) {
        if (tool.remoteMountId() == null) {
            return Mono.error(new IllegalArgumentException(
                    "remote MCP Tool mount is required"
            ));
        }
        McpNamespaceRouter.Binding binding = router.binding(
                active(),
                tool.remoteMountId(),
                "TOOL",
                tool.name()
        );
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("name", binding.remoteName());
        params.put("arguments", Map.copyOf(arguments));
        McpDialectTranslator.OutboundCall call = translator.outbound(
                inboundDialect,
                binding.provider().dialect(),
                "tools/call",
                params,
                meta,
                traceHeaders
        );
        return Mono.from(clients.exchange(
                        binding.provider(),
                        call,
                        new RemoteAuthProvider.AuthContext(
                                identity.subjectId(),
                                identity.tenantId(),
                                identity.clientId()
                        )
                ))
                .map(translator::result);
    }

    private CompiledMcpRules active() {
        CompiledMcpRules current = rules.get();
        return current == null ? CompiledMcpRules.empty() : current;
    }
}
