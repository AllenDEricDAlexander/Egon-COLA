package top.egon.cola.component.gateway.mcp.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeRemoteProvider;
import top.egon.cola.component.gateway.core.mcp.remote.RemoteAuthProvider;

import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Discovers canonical remote descriptors and persists one immutable snapshot.
 */
public final class McpCapabilitySynchronizer {

    private static final List<DiscoveryMethod> METHODS = List.of(
            new DiscoveryMethod("TOOL", "tools/list", "tools"),
            new DiscoveryMethod("RESOURCE", "resources/list", "resources"),
            new DiscoveryMethod(
                    "RESOURCE_TEMPLATE",
                    "resources/templates/list",
                    "resourceTemplates"
            ),
            new DiscoveryMethod("PROMPT", "prompts/list", "prompts")
    );

    private final McpRemoteClientPool clients;

    private final McpDialectTranslator translator;

    private final ObjectMapper objectMapper;

    private final Clock clock;

    private final SnapshotSink sink;

    public McpCapabilitySynchronizer(
            McpRemoteClientPool clients,
            McpDialectTranslator translator,
            ObjectMapper objectMapper,
            Clock clock,
            SnapshotSink sink) {
        this.clients = Objects.requireNonNull(clients, "clients");
        this.translator = Objects.requireNonNull(translator, "translator");
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        ).copy()
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        this.clock = Objects.requireNonNull(clock, "clock");
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    public Publisher<CapabilitySnapshot> synchronize(
            McpRuntimeRemoteProvider provider) {
        Objects.requireNonNull(provider, "provider");
        return initialize(provider)
                .thenMany(Flux.fromIterable(METHODS)
                        .concatMap(method -> discover(provider, method)))
                .collectList()
                .map(capabilities -> snapshot(provider, capabilities))
                .doOnNext(sink::persist);
    }

    public void requireReviewed(
            McpRuntimeRemoteProvider provider,
            CapabilitySnapshot snapshot) {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(snapshot, "snapshot");
        if (!provider.providerId().equals(snapshot.providerId())
                || !snapshot.matchesReviewedFingerprint()) {
            throw new IllegalStateException(
                    "remote MCP capabilities changed; preview and publish "
                            + "a new Release"
            );
        }
    }

    private Mono<Void> initialize(McpRuntimeRemoteProvider provider) {
        Map<String, Object> params = Map.of(
                "protocolVersion", provider.dialect().protocolVersion(),
                "capabilities", Map.of(),
                "clientInfo", Map.of(
                        "name", "egon-cola-gateway",
                        "version", "5.3.2"
                )
        );
        return call(provider, "initialize", params)
                .then();
    }

    private Flux<Capability> discover(
            McpRuntimeRemoteProvider provider,
            DiscoveryMethod method) {
        return call(provider, method.method(), Map.of())
                .flatMapMany(result -> Flux.fromIterable(
                        descriptors(result.get(method.resultField()))
                ))
                .map(descriptor -> new Capability(
                        method.primitiveType(),
                        name(descriptor),
                        descriptor
                ));
    }

    private Mono<Map<String, Object>> call(
            McpRuntimeRemoteProvider provider,
            String method,
            Map<String, Object> params) {
        McpDialectTranslator.OutboundCall call = translator.outbound(
                provider.dialect(),
                provider.dialect(),
                method,
                params,
                Map.of("purpose", "capability-sync"),
                Map.of()
        );
        return Mono.from(clients.exchange(
                        provider,
                        call,
                        RemoteAuthProvider.AuthContext.system()
                ))
                .map(translator::result);
    }

    private CapabilitySnapshot snapshot(
            McpRuntimeRemoteProvider provider,
            List<Capability> values) {
        List<Capability> sorted = values.stream()
                .sorted(Comparator.comparing(Capability::primitiveType)
                        .thenComparing(Capability::remoteName))
                .toList();
        String fingerprint = fingerprint(sorted);
        return new CapabilitySnapshot(
                provider.providerId(),
                fingerprint,
                fingerprint.equals(provider.capabilityFingerprint()),
                sorted,
                clock.instant()
        );
    }

    private String fingerprint(List<Capability> capabilities) {
        try {
            ArrayList<Map<String, Object>> canonical = new ArrayList<>();
            capabilities.forEach(capability -> {
                LinkedHashMap<String, Object> item = new LinkedHashMap<>();
                item.put("primitiveType", capability.primitiveType());
                item.put("remoteName", capability.remoteName());
                item.put("descriptor", capability.descriptor());
                canonical.add(item);
            });
            return HexFormat.of().formatHex(MessageDigest.getInstance(
                    "SHA-256"
            ).digest(objectMapper.writeValueAsBytes(canonical)));
        } catch (Exception failure) {
            throw new IllegalStateException(
                    "remote MCP capability fingerprint failed",
                    failure
            );
        }
    }

    private List<Map<String, Object>> descriptors(Object value) {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> source)) {
            throw new IllegalStateException(
                    "remote MCP capability list is invalid"
            );
        }
        ArrayList<Map<String, Object>> result = new ArrayList<>();
        source.forEach(item -> {
            if (!(item instanceof Map<?, ?> map)) {
                throw new IllegalStateException(
                        "remote MCP capability descriptor is invalid"
                );
            }
            LinkedHashMap<String, Object> descriptor = new LinkedHashMap<>();
            map.forEach((key, content) -> {
                if (!(key instanceof String name)) {
                    throw new IllegalStateException(
                            "remote MCP capability field is invalid"
                    );
                }
                descriptor.put(name, content);
            });
            result.add(Map.copyOf(descriptor));
        });
        return List.copyOf(result);
    }

    private String name(Map<String, Object> descriptor) {
        Object value = descriptor.get("name");
        if (!(value instanceof String text) || text.isBlank()) {
            value = descriptor.get("uri");
        }
        if (!(value instanceof String text) || text.isBlank()) {
            value = descriptor.get("uriTemplate");
        }
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalStateException(
                    "remote MCP capability name is missing"
            );
        }
        return text.trim();
    }

    @FunctionalInterface
    public interface SnapshotSink {

        void persist(CapabilitySnapshot snapshot);
    }

    public record Capability(
            String primitiveType,
            String remoteName,
            Map<String, Object> descriptor
    ) {

        public Capability {
            primitiveType = required(primitiveType, "primitiveType");
            remoteName = required(remoteName, "remoteName");
            descriptor = Map.copyOf(Objects.requireNonNull(
                    descriptor,
                    "descriptor"
            ));
        }
    }

    public record CapabilitySnapshot(
            String providerId,
            String fingerprint,
            boolean matchesReviewedFingerprint,
            List<Capability> capabilities,
            Instant synchronizedAt
    ) {

        public CapabilitySnapshot {
            providerId = required(providerId, "providerId");
            fingerprint = required(fingerprint, "fingerprint");
            capabilities = List.copyOf(Objects.requireNonNull(
                    capabilities,
                    "capabilities"
            ));
            synchronizedAt = Objects.requireNonNull(
                    synchronizedAt,
                    "synchronizedAt"
            );
        }
    }

    private record DiscoveryMethod(
            String primitiveType,
            String method,
            String resultField
    ) {
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "remote MCP " + field + " is required"
            );
        }
        return value.trim();
    }
}
