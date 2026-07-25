package top.egon.cola.component.gateway.core.provider;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public record ProviderInstance(
        ProviderServiceKey serviceKey,
        String instanceId,
        String leaseId,
        String host,
        int port,
        boolean secure,
        Map<String, String> metadata,
        Instant leaseExpireAt,
        ProviderRegistryState registryState,
        ProviderHealthState activeHealth,
        ProviderHealthState passiveHealth
) {

    public ProviderInstance {
        serviceKey = Objects.requireNonNull(serviceKey, "serviceKey");
        instanceId = required(instanceId, "instanceId");
        leaseId = required(leaseId, "leaseId");
        host = required(host, "host").toLowerCase(Locale.ROOT);
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        metadata = immutableMetadata(metadata);
        leaseExpireAt = Objects.requireNonNull(leaseExpireAt, "leaseExpireAt");
        registryState = Objects.requireNonNull(registryState, "registryState");
        activeHealth = Objects.requireNonNull(activeHealth, "activeHealth");
        passiveHealth = Objects.requireNonNull(passiveHealth, "passiveHealth");
    }

    public boolean availableAt(Instant now) {
        Objects.requireNonNull(now, "now");
        return registryState == ProviderRegistryState.REGISTERED
                && leaseExpireAt.isAfter(now)
                && activeHealth != ProviderHealthState.UNHEALTHY
                && activeHealth != ProviderHealthState.EJECTED
                && passiveHealth != ProviderHealthState.UNHEALTHY
                && passiveHealth != ProviderHealthState.EJECTED;
    }

    public String runtimeIdentity() {
        return instanceId + ":" + leaseId;
    }

    public int weight() {
        String raw = metadata.getOrDefault("gateway.weight", "100");
        try {
            int value = Integer.parseInt(raw);
            if (value < 1 || value > 10000) {
                throw new IllegalArgumentException(
                        "gateway.weight must be between 1 and 10000"
                );
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "gateway.weight must be an integer",
                    exception
            );
        }
    }

    private static Map<String, String> immutableMetadata(
            Map<String, String> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, String> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            String normalizedKey = required(key, "metadata key");
            String normalizedValue = required(value, "metadata value");
            String lowerKey = normalizedKey.toLowerCase(Locale.ROOT);
            if (lowerKey.contains("secret")
                    || lowerKey.contains("password")
                    || lowerKey.contains("token")
                    || lowerKey.contains("credential")) {
                throw new IllegalArgumentException(
                        "secret-like provider metadata is forbidden"
                );
            }
            copy.put(normalizedKey, normalizedValue);
        });
        return Map.copyOf(copy);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
