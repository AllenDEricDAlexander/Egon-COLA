package top.egon.cola.component.gateway.engine.http;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Removes fixed and Connection-declared hop-by-hop fields without changing
 * end-to-end payload metadata.
 */
public final class GatewayHeaderFilter {

    private static final Set<String> HOP_BY_HOP = Set.of(
            "connection",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade",
            "proxy-connection"
    );

    public Map<String, List<String>> requestHeaders(
            Map<String, List<String>> source) {
        return filter(source);
    }

    public Map<String, List<String>> responseHeaders(
            Map<String, List<String>> source) {
        return filter(source);
    }

    private Map<String, List<String>> filter(
            Map<String, List<String>> source) {
        Objects.requireNonNull(source, "source");
        Set<String> removals = new LinkedHashSet<>(HOP_BY_HOP);
        source.forEach((name, values) -> {
            if ("connection".equals(normalizedName(name))) {
                values.forEach(value -> connectionTokens(value, removals));
            }
        });
        Map<String, List<String>> result = new LinkedHashMap<>();
        source.forEach((name, values) -> {
            String normalized = normalizedName(name);
            if (!removals.contains(normalized)) {
                result.computeIfAbsent(
                        normalized,
                        ignored -> new ArrayList<>()
                ).addAll(List.copyOf(values));
            }
        });
        result.replaceAll((ignored, values) -> List.copyOf(values));
        return Map.copyOf(result);
    }

    private void connectionTokens(String value, Set<String> target) {
        if (value == null) {
            return;
        }
        for (String token : value.split(",")) {
            String normalized = token.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                target.add(normalized);
            }
        }
    }

    private String normalizedName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("invalid HTTP header name");
        }
        return value.toLowerCase(Locale.ROOT);
    }
}
