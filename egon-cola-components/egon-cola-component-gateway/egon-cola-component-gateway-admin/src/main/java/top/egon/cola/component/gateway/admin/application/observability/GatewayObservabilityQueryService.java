package top.egon.cola.component.gateway.admin.application.observability;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

public class GatewayObservabilityQueryService {

    private final GatewayObservabilityStore store;

    private final Clock clock;

    public GatewayObservabilityQueryService(
            GatewayObservabilityStore store,
            Clock clock) {
        this.store = Objects.requireNonNull(store, "store");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public GatewayObservabilityStore.Page<
            GatewayObservabilityStore.TraceSummary> traces(
            GatewayObservabilityStore.TraceQuery query) {
        return store.traces(query);
    }

    public GatewayObservabilityStore.DashboardSummary dashboard(
            String env,
            String namespace) {
        return store.dashboard(
                env,
                namespace,
                clock.instant().minus(Duration.ofHours(1))
        );
    }

    public GatewayObservabilityStore.Page<
            GatewayObservabilityStore.AuditSummary> audits(
            GatewayObservabilityStore.AuditQuery query) {
        return store.audits(query);
    }
}
