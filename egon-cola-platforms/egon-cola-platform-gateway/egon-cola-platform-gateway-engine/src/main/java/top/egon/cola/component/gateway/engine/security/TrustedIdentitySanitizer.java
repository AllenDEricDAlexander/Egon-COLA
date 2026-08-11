package top.egon.cola.component.gateway.engine.security;

import top.egon.cola.component.gateway.core.security.TrustedIdentity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class TrustedIdentitySanitizer {

    private static final Set<String> FIXED_SENSITIVE = Set.of(
            "authorization",
            "proxy-authorization",
            "cookie",
            "set-cookie",
            "x-gateway-principal-id",
            "x-gateway-tenant-id",
            "x-gateway-authenticated",
            "x-gateway-auth-provider",
            "x-gateway-access-zone",
            "x-internal-request",
            "x-forwarded-internal",
            "gateway-access-zone",
            "gateway-principal-id",
            "gateway-tenant-id"
    );

    private static final Set<String> IDP_TRUSTED_HTTP = Set.of(
            "x-egon-principal-type",
            "x-egon-identity-sub",
            "x-egon-tenant-id",
            "x-egon-session-id",
            "x-egon-client-id",
            "x-egon-token-id",
            "x-egon-token-version",
            "x-egon-resource-uri",
            "x-egon-resource-version",
            "x-egon-source-biz",
            "x-egon-source-app",
            "x-egon-source-env",
            "x-egon-service-scopes",
            "x-egon-credential-id"
    );

    private static final Set<String> HOP_BY_HOP = Set.of(
            "connection",
            "keep-alive",
            "proxy-authenticate",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade"
    );

    public Map<String, List<String>> sanitizeHttp(
            Map<String, List<String>> source,
            Set<String> fieldsToRemove,
            TrustedIdentity identity) {
        return sanitizeHttp(source, fieldsToRemove, identity, false);
    }

    public Map<String, List<String>> sanitizeHttp(
            Map<String, List<String>> source,
            Set<String> fieldsToRemove,
            TrustedIdentity identity,
            boolean authorizationForwardingAllowed) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(identity, "identity");
        Set<String> removals = normalized(fieldsToRemove);
        Map<String, List<String>> result = new LinkedHashMap<>();
        source.forEach((name, values) -> {
            String lower = normalizedName(name);
            if (safeInbound(
                    lower,
                    removals,
                    authorizationForwardingAllowed
            )) {
                result.put(lower, List.copyOf(values));
            }
        });
        if (identity.httpHeaders().size() > 16) {
            throw new IllegalArgumentException(
                    "trusted HTTP identity field count exceeds 16"
            );
        }
        identity.httpHeaders().forEach((name, value) -> {
            String lower = normalizedName(name);
            if (!lower.startsWith("x-egon-gateway-")
                    && !IDP_TRUSTED_HTTP.contains(lower)) {
                throw new IllegalArgumentException(
                        "untrusted HTTP identity field " + name
                );
            }
            result.put(lower, List.of(safeValue(value)));
        });
        return Map.copyOf(result);
    }

    public Map<String, String> sanitizeRpc(
            Map<String, String> source,
            Set<String> fieldsToRemove,
            TrustedIdentity identity) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(identity, "identity");
        Set<String> removals = normalized(fieldsToRemove);
        Map<String, String> result = new LinkedHashMap<>();
        source.forEach((name, value) -> {
            String lower = normalizedName(name);
            if (safeInbound(lower, removals, false)
                    && !lower.startsWith("egon-gateway-")) {
                result.put(lower, safeValue(value));
            }
        });
        if (identity.rpcMetadata().size() > 16) {
            throw new IllegalArgumentException(
                    "trusted RPC identity field count exceeds 16"
            );
        }
        identity.rpcMetadata().forEach((name, value) -> {
            String lower = normalizedName(name);
            if (!lower.startsWith("egon-gateway-")) {
                throw new IllegalArgumentException(
                        "untrusted RPC identity field " + name
                );
            }
            result.put(lower, safeValue(value));
        });
        return Map.copyOf(result);
    }

    private boolean safeInbound(
            String name,
            Set<String> removals,
            boolean authorizationForwardingAllowed) {
        boolean fixedSensitive = FIXED_SENSITIVE.contains(name)
                || IDP_TRUSTED_HTTP.contains(name);
        fixedSensitive = fixedSensitive
                && !(authorizationForwardingAllowed
                && "authorization".equals(name));
        return !fixedSensitive
                && !HOP_BY_HOP.contains(name)
                && !removals.contains(name)
                && !name.startsWith("x-egon-gateway-")
                && !name.startsWith("x-forwarded-");
    }

    private Set<String> normalized(Set<String> names) {
        if (names == null || names.isEmpty()) {
            return Set.of();
        }
        return names.stream()
                .map(this::normalizedName)
                .collect(Collectors.toUnmodifiableSet());
    }

    private String normalizedName(String value) {
        if (value == null || value.isBlank() || value.length() > 128) {
            throw new IllegalArgumentException("invalid metadata name");
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9!#$%&'*+.^_`|~-]+")) {
            throw new IllegalArgumentException("invalid metadata name");
        }
        return normalized;
    }

    private String safeValue(String value) {
        if (value == null
                || value.length() > 1024
                || value.indexOf('\r') >= 0
                || value.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("invalid metadata value");
        }
        return value;
    }
}
