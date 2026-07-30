package top.egon.cola.component.gateway.engine.discovery;

import java.util.Set;

public record ProviderPolicyOverride(
        Boolean enabled,
        Integer weight,
        String zone,
        String region,
        Set<String> tags
) {

    public ProviderPolicyOverride {
        if (weight != null && (weight < 0 || weight > 10000)) {
            throw new IllegalArgumentException(
                    "override weight must be between 0 and 10000"
            );
        }
        zone = normalized(zone);
        region = normalized(region);
        tags = tags == null
                ? null
                : tags.stream()
                .map(ProviderPolicyOverride::normalizedRequired)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public static ProviderPolicyOverride none() {
        return new ProviderPolicyOverride(null, null, null, null, null);
    }

    private static String normalized(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String normalizedRequired(String value) {
        String normalized = normalized(value);
        if (normalized == null) {
            throw new IllegalArgumentException("provider tag is required");
        }
        return normalized;
    }
}
