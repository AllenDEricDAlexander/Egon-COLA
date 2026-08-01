package top.egon.cola.platform.rbac3.admin.integration.runtime;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Separates application readiness from Gateway release routeability.
 */
public final class Rbac3ReadinessIndicator implements HealthIndicator {

    private final List<ReadinessCheck> applicationChecks;
    private final Supplier<String> gatewayRouteability;
    private final AtomicBoolean acceptingTraffic = new AtomicBoolean(true);

    public Rbac3ReadinessIndicator(
            List<ReadinessCheck> applicationChecks,
            Supplier<String> gatewayRouteability) {
        this.applicationChecks = List.copyOf(applicationChecks);
        this.gatewayRouteability = Objects.requireNonNull(
                gatewayRouteability, "gatewayRouteability");
        if (this.applicationChecks.isEmpty()) {
            throw new IllegalArgumentException(
                    "at least one application readiness check is required");
        }
    }

    @Override
    public Health health() {
        String routeability = safeRouteability();
        if (!acceptingTraffic.get()) {
            return Health.down()
                    .withDetail("failedCheck", "trafficAcceptance")
                    .withDetail("gatewayRouteability", routeability)
                    .build();
        }
        for (ReadinessCheck check : applicationChecks) {
            if (!safeReady(check)) {
                return Health.down()
                        .withDetail("failedCheck", check.name())
                        .withDetail("gatewayRouteability", routeability)
                        .build();
            }
        }
        return Health.up()
                .withDetail("gatewayRouteability", routeability)
                .build();
    }

    public void stopAcceptingTraffic() {
        acceptingTraffic.set(false);
    }

    private boolean safeReady(ReadinessCheck check) {
        try {
            return check.ready().getAsBoolean();
        } catch (RuntimeException unavailable) {
            return false;
        }
    }

    private String safeRouteability() {
        try {
            String value = gatewayRouteability.get();
            return value == null || value.isBlank() ? "UNKNOWN" : value;
        } catch (RuntimeException unavailable) {
            return "UNKNOWN";
        }
    }

    public record ReadinessCheck(String name, BooleanSupplier ready) {

        public ReadinessCheck {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("readiness check name is required");
            }
            name = name.trim();
            ready = Objects.requireNonNull(ready, "ready");
        }
    }
}
