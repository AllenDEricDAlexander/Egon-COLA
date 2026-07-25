package top.egon.cola.component.gateway.engine.discovery;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record ProviderSelectionPolicy(
        boolean serviceEnabled,
        Boolean secureRequired,
        String requiredZone,
        String requiredRegion,
        Set<String> requiredTags,
        ProviderPolicyOverride serviceOverride,
        Map<String, ProviderPolicyOverride> instanceOverrides
) {

    public ProviderSelectionPolicy {
        requiredZone = normalized(requiredZone);
        requiredRegion = normalized(requiredRegion);
        requiredTags = requiredTags == null
                ? Set.of()
                : Set.copyOf(requiredTags);
        serviceOverride = Objects.requireNonNull(
                serviceOverride,
                "serviceOverride"
        );
        Map<String, ProviderPolicyOverride> copy = new LinkedHashMap<>();
        Objects.requireNonNull(instanceOverrides, "instanceOverrides")
                .forEach((instanceId, override) -> copy.put(
                        required(instanceId),
                        Objects.requireNonNull(override, "instance override")
                ));
        instanceOverrides = Map.copyOf(copy);
    }

    public static ProviderSelectionPolicy defaults(boolean secure) {
        return new ProviderSelectionPolicy(
                true,
                secure,
                null,
                null,
                Set.of(),
                ProviderPolicyOverride.none(),
                Map.of()
        );
    }

    private static String normalized(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String required(String value) {
        String normalized = normalized(value);
        if (normalized == null) {
            throw new IllegalArgumentException("instanceId is required");
        }
        return normalized;
    }
}
