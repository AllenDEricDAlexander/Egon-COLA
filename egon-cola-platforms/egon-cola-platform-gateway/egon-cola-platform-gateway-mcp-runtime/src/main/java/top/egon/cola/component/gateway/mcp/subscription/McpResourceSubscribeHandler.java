package top.egon.cola.component.gateway.mcp.subscription;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;
import top.egon.cola.component.gateway.mcp.resource.McpResourceCatalog;
import top.egon.cola.component.gateway.mcp.resource.McpResourceDriver;
import top.egon.cola.component.gateway.mcp.security.McpSecurityGate;
import top.egon.cola.component.gateway.mcp.server.McpMethodHandler;
import top.egon.cola.component.gateway.mcp.server.McpRequestContext;

import java.util.Map;
import java.util.Objects;

public final class McpResourceSubscribeHandler implements McpMethodHandler {

    private final McpResourceCatalog catalog;

    private final McpSubscriptionService subscriptions;

    private final McpSecurityGate security;

    public McpResourceSubscribeHandler(
            McpResourceCatalog catalog,
            McpSubscriptionService subscriptions,
            McpSecurityGate security) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.subscriptions = Objects.requireNonNull(
                subscriptions,
                "subscriptions"
        );
        this.security = Objects.requireNonNull(security, "security");
    }

    @Override
    public String method() {
        return "resources/subscribe";
    }

    @Override
    public Publisher<McpJsonRpcResponse> handle(
            McpJsonRpcRequest request,
            McpRequestContext context) {
        if (context.sessionId() == null) {
            throw McpResourceDriver.rejected(
                    "MCP resource subscription requires a session"
            );
        }
        String uri = string(request.params().get("uri"));
        McpResourceCatalog.ResolvedResource resolved = catalog.resolve(
                context.server().serverCode(),
                uri
        );
        McpSecurityGate.IdentityContext identity = identity(context);
        Publisher<Void> authorization = resolved.resource() != null
                ? security.authorizeResourceRead(
                resolved.resource(),
                identity
        )
                : security.authorizeResourceRead(
                resolved.template(),
                identity
        );
        return Mono.from(authorization)
                .then(Mono.from(subscriptions.subscribe(
                        context.sessionId(),
                        resolved.uri()
                )))
                .map(subscription -> McpJsonRpcResponse.success(
                        request.id(),
                        Map.of(
                                "subscriptionId",
                                subscription.subscriptionId(),
                                "uri",
                                subscription.uri()
                        )
                ));
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

    private String string(Object value) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw McpResourceDriver.rejected("MCP resource URI is required");
        }
        return text.trim();
    }
}
