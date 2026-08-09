package top.egon.cola.platform.rbac3.admin.integration.ddc;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import top.egon.cola.component.ddc.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.configuration.runtime.DdcRuntimeCoordinator;
import top.egon.cola.component.ddc.configuration.runtime.DdcRuntimeState;
import top.egon.cola.component.gateway.starter.reporting.GatewayReportingState;

import java.util.Objects;
import java.util.Set;

/**
 * Registers the bounded RBAC3 DDC and Gateway integration metrics.
 */
public final class Rbac3IntegrationMetrics
        implements Rbac3DdcPolicyApplier.ApplyObserver {

    private static final Set<String> APPLY_STATUSES = Set.of("success", "failed");

    private final MeterRegistry registry;
    private final AtomicRbac3RuntimePolicy policy;
    private final ObjectProvider<DdcRuntimeCoordinator> coordinator;
    private final ObjectProvider<GatewayReportingState> reportingState;

    public Rbac3IntegrationMetrics(
            MeterRegistry registry,
            AtomicRbac3RuntimePolicy policy,
            ObjectProvider<DdcRuntimeCoordinator> coordinator,
            ObjectProvider<GatewayReportingState> reportingState) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.policy = Objects.requireNonNull(policy, "policy");
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator");
        this.reportingState = Objects.requireNonNull(reportingState, "reportingState");
        AtomicRbac3RuntimePolicy.CONFIG_KEYS.stream().sorted().forEach(this::registerVersionGauge);
        Gauge.builder("rbac3_ddc_config_ready", this, Rbac3IntegrationMetrics::ready)
                .register(registry);
        Gauge.builder("rbac3_gateway_definition_operation_count", this,
                        Rbac3IntegrationMetrics::operationCount)
                .register(registry);
    }

    @Override
    public void recordApply(String key, String status) {
        if (!AtomicRbac3RuntimePolicy.CONFIG_KEYS.contains(key)) {
            throw new IllegalArgumentException("unknown RBAC3 metric key");
        }
        if (!APPLY_STATUSES.contains(status)) {
            throw new IllegalArgumentException("unknown RBAC3 metric status");
        }
        Counter.builder("rbac3_ddc_config_apply_total")
                .tag("key", key)
                .tag("status", status)
                .register(registry)
                .increment();
    }

    private void registerVersionGauge(String key) {
        Gauge.builder("rbac3_ddc_config_snapshot_version", policy,
                        value -> value.current().configVersions().getOrDefault(key, 0L))
                .tag("key", key)
                .register(registry);
    }

    private double ready() {
        try {
            DdcRuntimeCoordinator runtime = coordinator.getIfAvailable();
            return runtime != null
                    && runtime.state() == DdcRuntimeState.READY
                    && runtime.currentSession()
                    .filter(session -> session.role() == DdcLeaseRole.CONFIG_CLIENT)
                    .isPresent() ? 1.0d : 0.0d;
        } catch (RuntimeException unavailable) {
            return 0.0d;
        }
    }

    private double operationCount() {
        try {
            GatewayReportingState state = reportingState.getIfAvailable();
            GatewayReportingState.Snapshot snapshot = state == null
                    ? null : state.snapshot();
            if (snapshot == null || snapshot.result() == null) {
                return 0.0d;
            }
            return snapshot.result().counts().operations();
        } catch (RuntimeException unavailable) {
            return 0.0d;
        }
    }
}
