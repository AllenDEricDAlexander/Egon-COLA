package top.egon.cola.platform.rbac3.admin.runtime;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.runtime.application.ControlPlaneRuntimeStatusPort;
import top.egon.cola.platform.rbac3.admin.runtime.application.RuntimeQueryService;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuntimeQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-30T12:00:00Z");

    @Test
    void exposesDefinitionLeaseAndReleaseAsThreeIndependentStates() {
        var expected = new ControlPlaneRuntimeStatusPort.RuntimeStatus(
                new ControlPlaneRuntimeStatusPort.DefinitionStatus(
                        "ACCEPTED_WITH_WARNINGS", "definition-7", List.of("deprecated field")),
                new ControlPlaneRuntimeStatusPort.ProviderLeaseStatus(
                        "RECOVERING", "instance-9", NOW.plusSeconds(30)),
                new ControlPlaneRuntimeStatusPort.GatewayReleaseStatus(
                        "release-3", "ACTIVATING", "engine-5"),
                NOW);
        RuntimeQueryService service = new RuntimeQueryService(
                () -> expected,
                (tenantId, status, cursor, pageSize) ->
                        new RuntimeQueryService.MutationPage(List.of(), null),
                (tenantId, mutationId, actorId) ->
                        new RuntimeQueryService.RetryResult(mutationId, "RECOVERY_REQUESTED"));

        assertThat(service.status()).isEqualTo(expected);
        assertThat(service.gatewayDdcStatus().definition()).isEqualTo(expected.definition());
        assertThat(service.gatewayDdcStatus().providerLease()).isEqualTo(expected.providerLease());
        assertThat(service.gatewayDdcStatus().gatewayRelease()).isEqualTo(expected.gatewayRelease());
    }

    @Test
    void retryAcceptsOnlyMutationIdentityAndActorContext() {
        AtomicReference<String> retried = new AtomicReference<>();
        RuntimeQueryService service = new RuntimeQueryService(
                () -> new ControlPlaneRuntimeStatusPort.RuntimeStatus(
                        new ControlPlaneRuntimeStatusPort.DefinitionStatus(
                                "UNKNOWN", null, List.of()),
                        new ControlPlaneRuntimeStatusPort.ProviderLeaseStatus(
                                "UNKNOWN", null, null),
                        new ControlPlaneRuntimeStatusPort.GatewayReleaseStatus(
                                null, "UNKNOWN", null), NOW),
                (tenantId, status, cursor, pageSize) ->
                        new RuntimeQueryService.MutationPage(List.of(), null),
                (tenantId, mutationId, actorId) -> {
                    retried.set(tenantId + ':' + mutationId + ':' + actorId);
                    return new RuntimeQueryService.RetryResult(
                            mutationId, "RECOVERY_REQUESTED");
                });

        var result = service.retry("tenant-1", "mutation-7", "operator-1");

        assertThat(result.status()).isEqualTo("RECOVERY_REQUESTED");
        assertThat(retried).hasValue("tenant-1:mutation-7:operator-1");
        assertThatThrownBy(() -> service.retry("tenant-1", " ", "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mutationId");
    }
}
