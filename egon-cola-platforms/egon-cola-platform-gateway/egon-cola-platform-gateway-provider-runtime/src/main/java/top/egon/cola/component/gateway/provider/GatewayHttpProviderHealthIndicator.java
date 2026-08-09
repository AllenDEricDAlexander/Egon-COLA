package top.egon.cola.component.gateway.provider;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;

public final class GatewayHttpProviderHealthIndicator
        implements HealthIndicator {

    private final HttpProviderLeaseRuntime runtime;

    public GatewayHttpProviderHealthIndicator(
            HttpProviderLeaseRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public Health health() {
        HttpProviderRuntimeState state = runtime.state();
        Health.Builder builder = switch (state) {
            case REGISTERED -> Health.up();
            case RECOVERING -> Health.outOfService();
            case FAILED, STOPPED -> Health.down();
            default -> Health.unknown();
        };
        builder.withDetail("state", state.name())
                .withDetail("instanceId", runtime.instanceId());
        runtime.lease().ifPresent(current -> addLeaseDetails(
                builder,
                current
        ));
        return builder.build();
    }

    private void addLeaseDetails(
            Health.Builder builder,
            DdcLeaseSession lease) {
        builder.withDetail("leaseId", lease.leaseId())
                .withDetail("leaseExpireAt", lease.leaseExpireAt());
    }
}
