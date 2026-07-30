package top.egon.cola.component.gateway.engine.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import top.egon.cola.component.gateway.contract.observability.GatewayCallEventV1;

import java.time.Duration;
import java.util.Objects;

public final class GatewayCallMetricsListener
        implements GatewayCallCompletionListener {

    private final MeterRegistry registry;

    public GatewayCallMetricsListener(MeterRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public void onComplete(GatewayCallEventV1 event) {
        String protocol = bounded(event.request().protocol());
        String zone = bounded(event.request().accessZone());
        String result = bounded(event.result().category());
        Counter.builder("gateway.calls")
                .tag("protocol", protocol)
                .tag("zone", zone)
                .tag("result", result)
                .register(registry)
                .increment();
        Timer.builder("gateway.call.duration")
                .tag("protocol", protocol)
                .tag("zone", zone)
                .tag("result", result)
                .register(registry)
                .record(Duration.ofMillis(event.result().durationMs()));
    }

    private static String bounded(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.length() > 48 ? "other" : value;
    }
}
