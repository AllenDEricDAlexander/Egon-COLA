package top.egon.cola.platform.rbac3.admin.runtime;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.runtime.service.ControlPlaneRuntimeStatusPort;
import top.egon.cola.platform.rbac3.admin.runtime.service.RuntimeQueryService;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.RuntimeStatusVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.DdcConfigClientStatusVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.DefinitionStatusVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.ProviderLeaseStatusVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.GatewayReleaseStatusVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.AuthorizationMutationPageVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.RetryResultVO;

class RuntimeQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-30T12:00:00Z");

    @Test
    void exposesDefinitionLeaseAndReleaseAsThreeIndependentStates() {
        var expected = new RuntimeStatusVO(
                new DdcConfigClientStatusVO(
                        "READY", "instance-config", "a1b2c3d4e5f6",
                        NOW.plusSeconds(30), Map.of("rbac3.maximum-active-roots", 3L),
                        null, null, null),
                new DefinitionStatusVO(
                        "ACCEPTED_WITH_WARNINGS", "definition-7", List.of("deprecated field")),
                new ProviderLeaseStatusVO(
                        "RECOVERING", "instance-9", NOW.plusSeconds(30)),
                new GatewayReleaseStatusVO(
                        "release-3", "ACTIVATING", "engine-5"),
                NOW);
        RuntimeQueryService service = new RuntimeQueryService(
                () -> expected,
                (tenantId, status, cursor, pageSize) ->
                        new AuthorizationMutationPageVO(List.of(), null),
                (tenantId, mutationId, actorId) ->
                        new RetryResultVO(mutationId, "RECOVERY_REQUESTED"));

        assertThat(service.status()).isEqualTo(expected);
        assertThat(service.gatewayDdcStatus().ddcConfigClient())
                .isEqualTo(expected.ddcConfigClient());
        assertThat(service.gatewayDdcStatus().definition()).isEqualTo(expected.definition());
        assertThat(service.gatewayDdcStatus().providerLease()).isEqualTo(expected.providerLease());
        assertThat(service.gatewayDdcStatus().gatewayRelease()).isEqualTo(expected.gatewayRelease());
        assertThat(service.gatewayDdcStatus().ddcConfigClient().instanceId())
                .isNotEqualTo(service.gatewayDdcStatus().providerLease().instanceId());
        assertThat(service.gatewayDdcStatus().ddcConfigClient().state()).isEqualTo("READY");
        assertThat(service.gatewayDdcStatus().providerLease().state())
                .isEqualTo("RECOVERING");
    }

    @Test
    void retryAcceptsOnlyMutationIdentityAndActorContext() {
        AtomicReference<String> retried = new AtomicReference<>();
        RuntimeQueryService service = new RuntimeQueryService(
                () -> new RuntimeStatusVO(
                        new DefinitionStatusVO(
                                "UNKNOWN", null, List.of()),
                        new ProviderLeaseStatusVO(
                                "UNKNOWN", null, null),
                        new GatewayReleaseStatusVO(
                                null, "UNKNOWN", null), NOW),
                (tenantId, status, cursor, pageSize) ->
                        new AuthorizationMutationPageVO(List.of(), null),
                (tenantId, mutationId, actorId) -> {
                    retried.set(tenantId + ':' + mutationId + ':' + actorId);
                    return new RetryResultVO(
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
