package top.egon.cola.component.gateway.admin.application.observability;

import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.gateway.contract.observability.GatewayCallEventV1;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public class GatewayCallEventIngestService {

    private final GatewayObservabilityStore store;

    private final Clock clock;

    private final Duration retention;

    public GatewayCallEventIngestService(
            GatewayObservabilityStore store,
            Clock clock,
            Duration retention) {
        this.store = Objects.requireNonNull(store, "store");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.retention = Objects.requireNonNull(retention, "retention");
    }

    @Transactional
    public boolean ingest(GatewayCallEventV1 event) {
        return store.project(
                event,
                Instant.ofEpochMilli(event.occurredAt()).plus(retention)
        );
    }

    @Transactional
    public void poison(
            GatewayObservabilityStore.ConsumeFailure failure) {
        store.recordFailure(failure);
    }

    @Transactional
    public int purgeExpired() {
        return store.deleteExpired(clock.instant());
    }
}
