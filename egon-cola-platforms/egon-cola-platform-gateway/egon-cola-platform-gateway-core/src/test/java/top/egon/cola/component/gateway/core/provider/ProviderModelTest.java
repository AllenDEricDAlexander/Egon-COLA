package top.egon.cola.component.gateway.core.provider;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProviderModelTest {

    @Test
    void treatsInstanceAndLeaseAsRuntimeIdentityAndExpiresLocally() {
        ProviderInstance instance = instance(
                Instant.parse("2026-07-25T00:00:30Z"),
                Map.of("gateway.weight", "200")
        );

        assertTrue(instance.availableAt(
                Instant.parse("2026-07-25T00:00:29Z")
        ));
        assertFalse(instance.availableAt(
                Instant.parse("2026-07-25T00:00:30Z")
        ));
        assertTrue(instance.runtimeIdentity().contains("lease-a"));
    }

    @Test
    void rejectsSecretsAndInvalidWeights() {
        assertThrows(
                IllegalArgumentException.class,
                () -> instance(
                        Instant.parse("2026-07-25T00:00:30Z"),
                        Map.of("gateway.token", "secret")
                )
        );
        ProviderInstance invalid = instance(
                Instant.parse("2026-07-25T00:00:30Z"),
                Map.of("gateway.weight", "0")
        );
        assertThrows(IllegalArgumentException.class, invalid::weight);
    }

    private ProviderInstance instance(
            Instant expiresAt,
            Map<String, String> metadata) {
        return new ProviderInstance(
                new ProviderServiceKey(
                        "local",
                        "default",
                        ProviderProtocolType.HTTP,
                        "orders",
                        "default",
                        "v1",
                        "http"
                ),
                "provider-a",
                "lease-a",
                "127.0.0.1",
                8080,
                false,
                metadata,
                expiresAt,
                ProviderRegistryState.REGISTERED,
                ProviderHealthState.UNKNOWN,
                ProviderHealthState.HEALTHY
        );
    }
}
