package top.egon.cola.platform.rbac3.admin.integration.ddc;

import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.http.registration.DdcHttpRegistrationRuntime;
import top.egon.cola.platform.rbac3.admin.integration.runtime.GatewayDdcRuntimeStatusService;

import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Maps the existing provider lease state machine to the RBAC3 status contract.
 */
public final class DdcProviderLeaseStatusService {

    private final Supplier<ProviderLeaseStatus> status;

    public DdcProviderLeaseStatusService(
            DdcHttpRegistrationRuntime runtime,
            GatewayDdcRuntimeStatusService.ServiceIdentity identity) {
        Objects.requireNonNull(runtime, "runtime");
        Objects.requireNonNull(identity, "identity");
        this.status = () -> {
            DdcLeaseSession lease = runtime.lease().orElse(null);
            return new ProviderLeaseStatus(
                    runtime.state().name(), runtime.instanceId(),
                    lease == null ? null : lease.leaseExpireAt(), identity);
        };
    }

    public DdcProviderLeaseStatusService(Supplier<ProviderLeaseStatus> status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public ProviderLeaseStatus status() {
        return status.get();
    }

    public record ProviderLeaseStatus(
            String state,
            String instanceId,
            Instant leaseExpireAt,
            GatewayDdcRuntimeStatusService.ServiceIdentity identity) {
    }
}
