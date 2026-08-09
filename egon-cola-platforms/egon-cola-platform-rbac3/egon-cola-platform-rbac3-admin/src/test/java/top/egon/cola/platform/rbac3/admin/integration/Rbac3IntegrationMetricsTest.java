package top.egon.cola.platform.rbac3.admin.integration;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import top.egon.cola.component.ddc.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.configuration.runtime.DdcRuntimeCoordinator;
import top.egon.cola.component.ddc.configuration.runtime.DdcRuntimeState;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReportResult;
import top.egon.cola.component.gateway.starter.reporting.GatewayReportingState;
import top.egon.cola.platform.rbac3.admin.config.Rbac3AdminProperties;
import top.egon.cola.platform.rbac3.admin.integration.ddc.AtomicRbac3RuntimePolicy;
import top.egon.cola.platform.rbac3.admin.integration.ddc.Rbac3DdcPolicyApplier;
import top.egon.cola.platform.rbac3.admin.integration.ddc.Rbac3IntegrationMetrics;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Rbac3IntegrationMetricsTest {

    @Test
    void exposesOnlyFixedKeyStatusAndAggregateStateDimensions() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AtomicRbac3RuntimePolicy policy = new AtomicRbac3RuntimePolicy(
                new Rbac3AdminProperties());
        DdcRuntimeCoordinator coordinator = mock(DdcRuntimeCoordinator.class);
        when(coordinator.state()).thenReturn(DdcRuntimeState.READY);
        when(coordinator.currentSession()).thenReturn(Optional.of(new DdcLeaseSession(
                "rbac3-1", "lease-secret", DdcLeaseRole.CONFIG_CLIENT, 30, 10,
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-08-01T00:00:30Z"))));
        GatewayReportingState reporting = mock(GatewayReportingState.class);
        when(reporting.snapshot()).thenReturn(new GatewayReportingState.Snapshot(
                "SUCCESS", Instant.parse("2026-08-01T00:00:00Z"),
                reportResult(78), null, 1));
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        beans.addBean("coordinator", coordinator);
        beans.addBean("reporting", reporting);
        Rbac3IntegrationMetrics metrics = new Rbac3IntegrationMetrics(
                registry,
                policy,
                beans.getBeanProvider(DdcRuntimeCoordinator.class),
                beans.getBeanProvider(GatewayReportingState.class));
        Rbac3DdcPolicyApplier applier = new Rbac3DdcPolicyApplier(
                AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY, 0, policy, metrics);

        applier.apply(AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY, "1200", 7L);
        assertThatIllegalArgumentException().isThrownBy(() -> applier.apply(
                AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY, "invalid", 8L));

        assertThat(registry.get("rbac3_ddc_config_apply_total")
                .tag("key", AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY)
                .tag("status", "success").counter().count()).isEqualTo(1.0d);
        assertThat(registry.get("rbac3_ddc_config_apply_total")
                .tag("key", AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY)
                .tag("status", "failed").counter().count()).isEqualTo(1.0d);
        assertThat(registry.get("rbac3_ddc_config_snapshot_version")
                .tag("key", AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY)
                .gauge().value()).isEqualTo(7.0d);
        assertThat(registry.get("rbac3_ddc_config_ready").gauge().value())
                .isEqualTo(1.0d);
        assertThat(registry.get("rbac3_gateway_definition_operation_count")
                .gauge().value()).isEqualTo(78.0d);
        assertThat(registry.getMeters()).hasSize(5 + 2 + 1 + 1);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> metrics.recordApply("rbac3.unknown", "success"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> metrics.recordApply(
                        AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY, "version-7"));
        assertThat(registry.getMeters()).allSatisfy(meter ->
                assertThat(meter.getId().getTags().toString())
                        .doesNotContain("lease-secret", "1200", "rbac3-1"));
    }

    private GatewayInterfaceDefinitionReportResult reportResult(int operations) {
        return new GatewayInterfaceDefinitionReportResult(
                "report-1", "definition-1",
                GatewayInterfaceDefinitionReportResult.Status.ACCEPTED,
                "application-1",
                new GatewayInterfaceDefinitionReportResult.Counts(
                        1, 1, 1, operations, operations, 0, 0),
                List.of(), List.of(), Instant.parse("2026-08-01T00:00:00Z"));
    }
}
