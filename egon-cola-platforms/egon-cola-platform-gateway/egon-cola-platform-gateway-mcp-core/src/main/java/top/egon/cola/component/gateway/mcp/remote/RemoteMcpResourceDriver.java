package top.egon.cola.component.gateway.mcp.remote;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeResource;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeResourceTemplate;
import top.egon.cola.component.gateway.core.mcp.remote.RemoteAuthProvider;
import top.egon.cola.component.gateway.mcp.resource.McpResourceDriver;
import top.egon.cola.component.gateway.mcp.rule.CompiledMcpRules;
import top.egon.cola.component.gateway.mcp.security.McpSecurityGate;
import top.egon.cola.component.gateway.mcp.telemetry.McpTelemetry;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Resource Strategy for reviewed REMOTE_MCP descriptors and Apps.
 */
public final class RemoteMcpResourceDriver implements McpResourceDriver {

    public static final String DRIVER_TYPE = "REMOTE_MCP";

    private final Supplier<CompiledMcpRules> rules;

    private final McpRemoteClientPool clients;

    private final McpNamespaceRouter router;

    private final McpDialectTranslator translator;

    public RemoteMcpResourceDriver(
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
    public String driverType() {
        return DRIVER_TYPE;
    }

    @Override
    public Publisher<Content> read(ReadRequest request) {
        Descriptor descriptor = descriptor(request.serverCode(), request.name());
        McpNamespaceRouter.Binding binding = router.binding(
                active(),
                descriptor.remoteMountId(),
                descriptor.primitiveType(),
                descriptor.name()
        );
        String remoteUri = descriptor.configuration().getOrDefault(
                "remoteUri",
                request.uri()
        );
        McpDialectTranslator.OutboundCall call = translator.outbound(
                dialect(request.attributes()),
                binding.provider().dialect(),
                "resources/read",
                Map.of("uri", remoteUri),
                Map.of(),
                trace(request.attributes())
        );
        McpSecurityGate.IdentityContext identity = identity(
                request.attributes()
        );
        McpTelemetry.Scope telemetry = McpTelemetry.current(
                request.attributes()
        );
        telemetry.remoteProvider(binding.provider().providerCode());
        var exchange = clients.exchange(
                binding.provider(),
                call,
                auth(identity)
        );
        return Mono.from(McpTelemetry.observeChild(
                        telemetry,
                        McpTelemetry.ChildKind.REMOTE,
                        exchange
                ))
                .map(translator::result)
                .map(result -> content(request, result));
    }

    private Content content(
            ReadRequest request,
            Map<String, Object> result) {
        Object raw = result.get("contents");
        if (!(raw instanceof List<?> contents) || contents.isEmpty()
                || !(contents.getFirst() instanceof Map<?, ?> source)) {
            throw McpResourceDriver.rejected(
                    "remote MCP resource content is invalid"
            );
        }
        String mimeType = source.get("mimeType") instanceof String mime
                && !mime.isBlank()
                ? mime.trim()
                : request.mimeType();
        byte[] bytes;
        boolean textual;
        if (source.get("text") instanceof String text) {
            bytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            textual = true;
        } else if (source.get("blob") instanceof String blob) {
            try {
                bytes = Base64.getDecoder().decode(blob);
            } catch (IllegalArgumentException failure) {
                throw McpResourceDriver.rejected(
                        "remote MCP resource blob is invalid"
                );
            }
            textual = false;
        } else {
            throw McpResourceDriver.rejected(
                    "remote MCP resource content is missing"
            );
        }
        if (bytes.length > request.maximumBytes()) {
            throw McpResourceDriver.rejected(
                    "remote MCP resource exceeds its maximum size"
            );
        }
        Map<String, Object> metadata = source.get("_meta") instanceof Map<?, ?>
                ? stringObjectMap((Map<?, ?>) source.get("_meta"))
                : Map.of();
        return new Content(
                request.uri(),
                mimeType,
                bytes,
                textual,
                metadata
        );
    }

    private Descriptor descriptor(String serverCode, String name) {
        CompiledMcpRules current = active();
        String key = CompiledMcpRules.qualified(serverCode, name);
        McpRuntimeResource resource = current.resourcesByQualifiedName().get(
                key
        );
        if (resource != null && resource.remoteMountId() != null) {
            return new Descriptor(
                    resource.name(),
                    resource.remoteMountId(),
                    "RESOURCE",
                    resource.configuration()
            );
        }
        McpRuntimeResourceTemplate template =
                current.templatesByQualifiedName().get(key);
        if (template != null && template.remoteMountId() != null) {
            return new Descriptor(
                    template.name(),
                    template.remoteMountId(),
                    "RESOURCE_TEMPLATE",
                    template.configuration()
            );
        }
        throw McpResourceDriver.rejected(
                "remote MCP resource descriptor was not found"
        );
    }

    private Map<String, Object> stringObjectMap(Map<?, ?> source) {
        java.util.LinkedHashMap<String, Object> result =
                new java.util.LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key instanceof String name) {
                result.put(name, value);
            }
        });
        return Map.copyOf(result);
    }

    private CompiledMcpRules active() {
        CompiledMcpRules value = rules.get();
        return value == null ? CompiledMcpRules.empty() : value;
    }

    private McpSecurityGate.IdentityContext identity(
            Map<String, Object> attributes) {
        return McpSecurityGate.IdentityContext.from(attributes);
    }

    private RemoteAuthProvider.AuthContext auth(
            McpSecurityGate.IdentityContext identity) {
        return new RemoteAuthProvider.AuthContext(
                identity.subjectId(),
                identity.tenantId(),
                identity.clientId()
        );
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

    private record Descriptor(
            String name,
            String remoteMountId,
            String primitiveType,
            Map<String, String> configuration
    ) {
    }
}
