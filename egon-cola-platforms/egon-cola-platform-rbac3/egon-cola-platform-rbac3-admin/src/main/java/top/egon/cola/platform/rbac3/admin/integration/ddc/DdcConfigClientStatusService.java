package top.egon.cola.platform.rbac3.admin.integration.ddc;

import org.springframework.beans.factory.ObjectProvider;
import top.egon.cola.component.ddc.model.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.service.lifecycle.DdcRuntimeCoordinator;
import top.egon.cola.platform.rbac3.admin.runtime.application.ControlPlaneRuntimeStatusPort;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Projects the independent DDC configuration-client lease without exposing its credential.
 */
public final class DdcConfigClientStatusService {

    private static final int FINGERPRINT_LENGTH = 12;

    private final Supplier<DdcRuntimeCoordinator> coordinator;
    private final AtomicRbac3RuntimePolicy policy;

    public DdcConfigClientStatusService(
            DdcRuntimeCoordinator coordinator,
            AtomicRbac3RuntimePolicy policy) {
        this(() -> Objects.requireNonNull(coordinator, "coordinator"), policy);
    }

    public DdcConfigClientStatusService(
            ObjectProvider<DdcRuntimeCoordinator> coordinator,
            AtomicRbac3RuntimePolicy policy) {
        this(coordinator::getIfAvailable, policy);
    }

    private DdcConfigClientStatusService(
            Supplier<DdcRuntimeCoordinator> coordinator,
            AtomicRbac3RuntimePolicy policy) {
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator");
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    public ControlPlaneRuntimeStatusPort.DdcConfigClientStatus status() {
        DdcRuntimeCoordinator runtime = coordinator.get();
        String state = runtime == null ? "UNKNOWN" : runtime.state().name();
        Optional<DdcLeaseSession> session = runtime == null
                ? Optional.empty()
                : runtime.currentSession().filter(value -> value.role() == DdcLeaseRole.CONFIG_CLIENT);
        AtomicRbac3RuntimePolicy.ApplyFailure failure = policy.lastApplyFailure().orElse(null);
        return new ControlPlaneRuntimeStatusPort.DdcConfigClientStatus(
                state,
                session.map(DdcLeaseSession::instanceId).orElse(null),
                session.map(DdcLeaseSession::leaseId).map(this::fingerprint).orElse(null),
                session.map(DdcLeaseSession::leaseExpireAt).orElse(null),
                policy.current().configVersions(),
                failure == null ? null : failure.key(),
                failure == null ? null : failure.targetVersion(),
                failure == null ? null : failure.errorCode());
    }

    public boolean ready() {
        ControlPlaneRuntimeStatusPort.DdcConfigClientStatus status = status();
        return "READY".equals(status.state()) && status.instanceId() != null;
    }

    private String fingerprint(String leaseId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(leaseId.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, FINGERPRINT_LENGTH);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
