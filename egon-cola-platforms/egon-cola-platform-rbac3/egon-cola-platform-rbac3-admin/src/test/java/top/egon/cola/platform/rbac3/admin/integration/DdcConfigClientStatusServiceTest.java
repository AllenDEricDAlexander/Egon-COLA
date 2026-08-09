package top.egon.cola.platform.rbac3.admin.integration;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.model.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.service.lifecycle.DdcRuntimeCoordinator;
import top.egon.cola.component.ddc.model.instance.DdcRuntimeState;
import top.egon.cola.platform.rbac3.admin.config.Rbac3AdminProperties;
import top.egon.cola.platform.rbac3.admin.integration.ddc.AtomicRbac3RuntimePolicy;
import top.egon.cola.platform.rbac3.admin.integration.ddc.DdcConfigClientStatusService;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DdcConfigClientStatusServiceTest {

    @Test
    void exposesConfigLeaseAndPolicyMetadataWithoutTheLeaseOrRawValue() {
        String leaseId = "config-client-lease-secret-value";
        DdcRuntimeCoordinator coordinator = mock(DdcRuntimeCoordinator.class);
        when(coordinator.state()).thenReturn(DdcRuntimeState.READY);
        when(coordinator.currentSession()).thenReturn(Optional.of(new DdcLeaseSession(
                "rbac3-1", leaseId, DdcLeaseRole.CONFIG_CLIENT, 30, 10,
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-08-01T00:00:30Z"))));
        AtomicRbac3RuntimePolicy policy = new AtomicRbac3RuntimePolicy(
                new Rbac3AdminProperties());
        policy.apply(AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY, "1200", 7L);
        String rejectedRawValue = "invalid-secret-like-value";
        try {
            policy.apply(AtomicRbac3RuntimePolicy.SESSION_IDLE_TIMEOUT_KEY,
                    rejectedRawValue, 8L);
        } catch (IllegalArgumentException ignored) {
        }

        var status = new DdcConfigClientStatusService(coordinator, policy).status();

        assertThat(status.state()).isEqualTo("READY");
        assertThat(status.instanceId()).isEqualTo("rbac3-1");
        assertThat(status.leaseIdFingerprint())
                .hasSize(12)
                .doesNotContain(leaseId);
        assertThat(status.leaseExpireAt())
                .isEqualTo(Instant.parse("2026-08-01T00:00:30Z"));
        assertThat(status.configVersions())
                .containsEntry(AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY, 7L);
        assertThat(status.lastApplyFailureKey())
                .isEqualTo(AtomicRbac3RuntimePolicy.SESSION_IDLE_TIMEOUT_KEY);
        assertThat(status.lastApplyFailureVersion()).isEqualTo(8L);
        assertThat(status.lastApplyFailureCode()).isEqualTo("INVALID_INTEGER");
        assertThat(status.toString())
                .doesNotContain(leaseId, rejectedRawValue);
    }

    @Test
    void preservesRuntimeStateWhenThereIsNoCurrentSession() {
        DdcRuntimeCoordinator coordinator = mock(DdcRuntimeCoordinator.class);
        when(coordinator.state()).thenReturn(DdcRuntimeState.RECOVERING);
        when(coordinator.currentSession()).thenReturn(Optional.empty());

        var status = new DdcConfigClientStatusService(
                coordinator,
                new AtomicRbac3RuntimePolicy(new Rbac3AdminProperties())).status();

        assertThat(status.state()).isEqualTo("RECOVERING");
        assertThat(status.instanceId()).isNull();
        assertThat(status.leaseIdFingerprint()).isNull();
        assertThat(status.leaseExpireAt()).isNull();
    }
}
