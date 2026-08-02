package top.egon.cola.component.gateway.mcp.resource;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeResource;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;
import top.egon.cola.component.gateway.mcp.security.McpSecurityGate;
import top.egon.cola.component.gateway.mcp.server.McpMethodHandler;
import top.egon.cola.component.gateway.mcp.server.McpRequestContext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class McpResourcesListHandler implements McpMethodHandler {

    private final McpResourceCatalog catalog;

    private final McpSecurityGate security;

    public McpResourcesListHandler(
            McpResourceCatalog catalog,
            McpSecurityGate security) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.security = Objects.requireNonNull(security, "security");
    }

    @Override
    public String method() {
        return "resources/list";
    }

    @Override
    public Publisher<McpJsonRpcResponse> handle(
            McpJsonRpcRequest request,
            McpRequestContext context) {
        McpSecurityGate.IdentityContext identity = identity(context);
        return Flux.fromIterable(catalog.resources(
                        context.server().serverCode()
                ))
                .concatMap(resource -> Mono.from(
                                security.authorizeResourceRead(
                                        resource,
                                        identity
                                )
                        ).thenReturn(describe(resource))
                        .onErrorResume(
                                McpProtocolException.class,
                                ignored -> Mono.empty()
                        ))
                .collectList()
                .map(resources -> McpJsonRpcResponse.success(
                        request.id(),
                        Map.of("resources", resources)
                ));
    }

    private Map<String, Object> describe(McpRuntimeResource resource) {
        LinkedHashMap<String, Object> value = new LinkedHashMap<>();
        value.put("name", resource.name());
        value.put("uri", resource.uri());
        value.put("mimeType", resource.mimeType());
        if (resource.description() != null) {
            value.put("description", resource.description());
        }
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
