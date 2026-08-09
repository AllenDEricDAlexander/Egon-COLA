package top.egon.cola.component.gateway.mcp.resource;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeResourceTemplate;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;
import top.egon.cola.component.gateway.mcp.security.McpSecurityGate;
import top.egon.cola.component.gateway.mcp.server.McpMethodHandler;
import top.egon.cola.component.gateway.mcp.server.McpRequestContext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class McpResourceTemplatesListHandler
        implements McpMethodHandler {

    private final McpResourceCatalog catalog;

    private final McpSecurityGate security;

    public McpResourceTemplatesListHandler(
            McpResourceCatalog catalog,
            McpSecurityGate security) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.security = Objects.requireNonNull(security, "security");
    }

    @Override
    public String method() {
        return "resources/templates/list";
    }

    @Override
    public Publisher<McpJsonRpcResponse> handle(
            McpJsonRpcRequest request,
            McpRequestContext context) {
        McpSecurityGate.IdentityContext identity;
        try {
            identity = McpSecurityGate.IdentityContext.from(
                    context.attributes()
            );
        } catch (IllegalArgumentException failure) {
            throw new McpProtocolException(
                    McpErrorCode.MCP_UNAUTHENTICATED,
                    "MCP identity context is incomplete"
            );
        }
        return Flux.fromIterable(catalog.templates(
                        context.server().serverCode()
                ))
                .concatMap(template -> Mono.from(
                                security.authorizeResourceRead(
                                        template,
                                        identity
                                )
                        ).thenReturn(describe(template))
                        .onErrorResume(
                                McpProtocolException.class,
                                ignored -> Mono.empty()
                        ))
                .collectList()
                .map(templates -> McpJsonRpcResponse.success(
                        request.id(),
                        Map.of("resourceTemplates", templates)
                ));
    }

    private Map<String, Object> describe(
            McpRuntimeResourceTemplate template) {
        LinkedHashMap<String, Object> value = new LinkedHashMap<>();
        value.put("name", template.name());
        value.put("uriTemplate", template.uriTemplate());
        value.put("mimeType", template.mimeType());
        if (template.description() != null) {
            value.put("description", template.description());
        }
        return Map.copyOf(value);
    }
}
