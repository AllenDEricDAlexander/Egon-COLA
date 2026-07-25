package top.egon.cola.component.gateway.engine.discovery;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.core.provider.ProviderHealthState;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.provider.ProviderRegistryState;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProviderCandidateFilterTest {

    private static final Instant NOW = Instant.parse("2026-07-25T00:00:00Z");

    @Test
    void appliesOverridePrecedenceAndFixedCandidateStages() {
        ProviderInstance allowed = provider(
                "allowed",
                Map.of(
                        "gateway.zone", "provider-zone",
                        "gateway.tags", "blue,legacy",
                        "gateway.weight", "10"
                ),
                ProviderHealthState.HEALTHY,
                ProviderHealthState.HEALTHY
        );
        ProviderInstance disabled = provider(
                "disabled",
                Map.of("gateway.zone", "az-a", "gateway.tags", "blue"),
                ProviderHealthState.HEALTHY,
                ProviderHealthState.HEALTHY
        );
        ProviderSelectionPolicy policy = new ProviderSelectionPolicy(
                true,
                false,
                "az-a",
                null,
                Set.of("blue"),
                new ProviderPolicyOverride(
                        null,
                        20,
                        "service-zone",
                        null,
                        Set.of("service")
                ),
                Map.of(
                        "allowed",
                        new ProviderPolicyOverride(
                                true,
                                30,
                                "az-a",
                                null,
                                Set.of("blue", "canary")
                        ),
                        "disabled",
                        new ProviderPolicyOverride(
                                false,
                                null,
                                null,
                                null,
                                Set.of()
                        )
                )
        );
        ProviderCandidateFilter filter = new ProviderCandidateFilter(
                Clock.fixed(NOW, ZoneOffset.UTC),
                ignored -> true
        );

        ProviderCandidateFilterResult result = filter.filter(
                key(),
                java.util.List.of(allowed, disabled),
                policy
        );

        assertEquals(1, result.candidates().size());
        ProviderInstance selected = result.candidates().getFirst();
        assertEquals("allowed", selected.instanceId());
        assertEquals(30, selected.weight());
        assertEquals("az-a", selected.metadata().get("gateway.zone"));
        assertEquals(1, result.counts().get(
                ProviderCandidateStage.ADMIN_ENABLED
        ));
        assertEquals(
                "ADMIN_DISABLED",
                result.rejectedReasons().get("disabled:lease")
        );
    }

    @Test
    void rejectsExpiredUnhealthyAndInvalidMetadataWithoutFallback() {
        ProviderInstance expired = new ProviderInstance(
                key(),
                "expired",
                "lease",
                "127.0.0.1",
                8080,
                false,
                Map.of(),
                NOW,
                ProviderRegistryState.REGISTERED,
                ProviderHealthState.HEALTHY,
                ProviderHealthState.HEALTHY
        );
        ProviderInstance unhealthy = provider(
                "unhealthy",
                Map.of(),
                ProviderHealthState.UNHEALTHY,
                ProviderHealthState.HEALTHY
        );
        ProviderInstance invalidWeight = provider(
                "invalid",
                Map.of("gateway.weight", "not-a-number"),
                ProviderHealthState.HEALTHY,
                ProviderHealthState.HEALTHY
        );
        ProviderCandidateFilter filter = new ProviderCandidateFilter(
                Clock.fixed(NOW, ZoneOffset.UTC),
                ignored -> true
        );

        ProviderCandidateFilterResult result = filter.filter(
                key(),
                java.util.List.of(expired, unhealthy, invalidWeight),
                ProviderSelectionPolicy.defaults(false)
        );

        assertTrue(result.candidates().isEmpty());
        assertEquals(0, result.counts().get(
                ProviderCandidateStage.POSITIVE_WEIGHT
        ));
        assertEquals(
                "INVALID_METADATA",
                result.rejectedReasons().get("invalid:lease")
        );
    }

    private ProviderInstance provider(
            String instanceId,
            Map<String, String> metadata,
            ProviderHealthState active,
            ProviderHealthState passive) {
        return new ProviderInstance(
                key(),
                instanceId,
                "lease",
                "127.0.0.1",
                8080,
                false,
                metadata,
                NOW.plusSeconds(60),
                ProviderRegistryState.REGISTERED,
                active,
                passive
        );
    }

    private ProviderServiceKey key() {
        return new ProviderServiceKey(
                "local",
                "default",
                ProviderProtocolType.HTTP,
                "orders",
                "default",
                "v1",
                "http"
        );
    }
}
