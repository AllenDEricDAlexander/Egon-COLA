package top.egon.cola.component.gateway.mcp.resource;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;
import top.egon.cola.component.gateway.mcp.security.McpSecurityGate;
import top.egon.cola.component.gateway.mcp.server.McpMethodHandler;
import top.egon.cola.component.gateway.mcp.server.McpRequestContext;

import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class McpResourcesReadHandler implements McpMethodHandler {

    private final McpResourceCatalog catalog;

    private final Map<String, McpResourceDriver> drivers;

    private final McpSecurityGate security;

    public McpResourcesReadHandler(
            McpResourceCatalog catalog,
            List<McpResourceDriver> drivers,
            McpSecurityGate security) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.drivers = index(drivers);
        this.security = Objects.requireNonNull(security, "security");
    }

    @Override
    public String method() {
        return "resources/read";
    }

    @Override
    public Publisher<McpJsonRpcResponse> handle(
            McpJsonRpcRequest request,
            McpRequestContext context) {
        String uri = string(request.params().get("uri"));
        McpResourceCatalog.ResolvedResource resolved = catalog.resolve(
                context.server().serverCode(),
                uri
        );
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
        McpResourceDriver driver = drivers.get(resolved.driverType());
        if (driver == null) {
            throw McpResourceDriver.rejected(
                    "MCP resource driver is unavailable"
            );
        }
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
                .then(Mono.from(driver.read(resolved.request(
                        context.attributes()
                ))))
                .map(content -> McpJsonRpcResponse.success(
                        request.id(),
                        Map.of("contents", List.of(describe(content)))
                ));
    }

    private Map<String, Object> describe(McpResourceDriver.Content content) {
        LinkedHashMap<String, Object> value = new LinkedHashMap<>();
        value.put("uri", content.uri());
        value.put("mimeType", content.mimeType());
        if (content.textual()) {
            value.put("text", content.text());
        } else {
            value.put("blob", Base64.getEncoder().encodeToString(
                    content.data()
            ));
        }
        return Map.copyOf(value);
    }

    private Map<String, McpResourceDriver> index(
            List<McpResourceDriver> source) {
        LinkedHashMap<String, McpResourceDriver> result = new LinkedHashMap<>();
        Objects.requireNonNull(source, "drivers").forEach(driver -> {
            String type = driver.driverType();
            if (type == null || type.isBlank()
                    || result.putIfAbsent(type.trim(), driver) != null) {
                throw new IllegalArgumentException(
                        "MCP resource driver types must be unique"
                );
            }
        });
        return Collections.unmodifiableMap(result);
    }

    private String string(Object value) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw McpResourceDriver.rejected("MCP resource URI is required");
        }
        return text.trim();
    }
}
