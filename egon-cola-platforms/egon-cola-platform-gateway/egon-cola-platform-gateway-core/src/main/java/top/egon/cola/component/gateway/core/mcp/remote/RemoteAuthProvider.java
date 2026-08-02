package top.egon.cola.component.gateway.core.mcp.remote;

import org.reactivestreams.Publisher;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeRemoteProvider;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Resolves outbound-only authentication from reviewed secret references.
 */
@FunctionalInterface
public interface RemoteAuthProvider {

    Publisher<OutboundAuthentication> resolve(AuthRequest request);

    record AuthRequest(
            McpRuntimeRemoteProvider provider,
            AuthContext context
    ) {

        public AuthRequest {
            provider = Objects.requireNonNull(provider, "provider");
            context = Objects.requireNonNull(context, "context");
        }
    }

    record AuthContext(String subjectId, String tenantId, String clientId) {

        public AuthContext {
            subjectId = required(subjectId, "subjectId");
            tenantId = required(tenantId, "tenantId");
            clientId = required(clientId, "clientId");
        }

        public static AuthContext system() {
            return new AuthContext(
                    "gateway-mcp-capability-sync",
                    "system",
                    "gateway-mcp"
            );
        }
    }

    record OutboundAuthentication(
            Map<String, String> headers,
            String tlsProfileReference
    ) {

        private static final Set<String> FORBIDDEN_HEADERS = Set.of(
                "host",
                "cookie",
                "proxy-authorization",
                "mcp-session-id",
                "x-forwarded-for",
                "x-forwarded-host",
                "x-forwarded-proto",
                "x-egon-principal"
        );

        public OutboundAuthentication {
            LinkedHashMap<String, String> checked = new LinkedHashMap<>();
            if (headers != null) {
                headers.forEach((name, value) -> {
                    String normalized = required(name, "header name")
                            .toLowerCase(Locale.ROOT);
                    String content = required(value, "header value");
                    if (FORBIDDEN_HEADERS.contains(normalized)
                            || normalized.contains("\r")
                            || normalized.contains("\n")
                            || content.contains("\r")
                            || content.contains("\n")) {
                        throw new IllegalArgumentException(
                                "remote authentication header is forbidden"
                        );
                    }
                    if (checked.putIfAbsent(normalized, content) != null) {
                        throw new IllegalArgumentException(
                                "duplicate remote authentication header"
                        );
                    }
                });
            }
            headers = Map.copyOf(checked);
            tlsProfileReference = tlsProfileReference == null
                    || tlsProfileReference.isBlank()
                    ? null
                    : tlsProfileReference.trim();
        }

        public static OutboundAuthentication none() {
            return new OutboundAuthentication(Map.of(), null);
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "remote authentication " + field + " is required"
            );
        }
        return value.trim();
    }
}
