package top.egon.cola.component.gateway.admin.application.observability;

import top.egon.cola.component.gateway.admin.application.projection.GatewayProjectionService;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

public class GatewayObservabilityQueryService {

    private final GatewayObservabilityStore store;

    private final Clock clock;

    private final GatewayProjectionService projections;

    public GatewayObservabilityQueryService(
            GatewayObservabilityStore store,
            Clock clock) {
        this(store, clock, null);
    }

    public GatewayObservabilityQueryService(
            GatewayObservabilityStore store,
            Clock clock,
            GatewayProjectionService projections) {
        this.store = Objects.requireNonNull(store, "store");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.projections = projections;
    }

    public GatewayObservabilityStore.Page<
            GatewayObservabilityStore.TraceSummary> traces(
            GatewayObservabilityStore.TraceQuery query) {
        return store.traces(query);
    }

    public GatewayObservabilityStore.DashboardSummary dashboard(
            String bizCode,
            String appCode,
            String env,
            String namespace) {
        GatewayObservabilityStore.DashboardSummary summary = store.dashboard(
                env,
                namespace,
                clock.instant().minus(Duration.ofHours(1))
        );
        if (projections == null) {
            return summary;
        }
        try {
            GatewayProjectionService.ProjectionCounts counts =
                    projections.scopeCounts(
                            bizCode,
                            appCode,
                            env,
                            namespace
                    );
            return new GatewayObservabilityStore.DashboardSummary(
                    summary.gatewayGroups(),
                    counts.readyEngines(),
                    counts.totalEngines(),
                    counts.inconsistentGroups(),
                    counts.activeProviders(),
                    counts.abnormalProviders(),
                    summary.releaseSuccessRate(),
                    summary.requestSeries(),
                    summary.protocolCalls(),
                    counts.stale()
                            ? "PROJECTION_STALE"
                            : summary.observabilityState()
            );
        } catch (RuntimeException ignored) {
            return new GatewayObservabilityStore.DashboardSummary(
                    summary.gatewayGroups(),
                    summary.readyEngines(),
                    summary.totalEngines(),
                    summary.inconsistentGroups(),
                    summary.activeProviders(),
                    summary.abnormalProviders(),
                    summary.releaseSuccessRate(),
                    summary.requestSeries(),
                    summary.protocolCalls(),
                    "PROJECTION_UNAVAILABLE"
            );
        }
    }

    public GatewayObservabilityStore.Page<
            GatewayObservabilityStore.AuditSummary> audits(
            GatewayObservabilityStore.AuditQuery query) {
        return store.audits(query);
    }
}
