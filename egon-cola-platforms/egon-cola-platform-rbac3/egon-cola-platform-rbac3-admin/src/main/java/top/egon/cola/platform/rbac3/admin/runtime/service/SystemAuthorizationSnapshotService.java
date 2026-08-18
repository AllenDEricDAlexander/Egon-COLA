package top.egon.cola.platform.rbac3.admin.runtime.service;

import top.egon.cola.platform.rbac3.admin.authorization.domain.vo.SnapshotRecordVO;
import top.egon.cola.platform.rbac3.admin.authorization.repository.AuthorizationSnapshotRepository;
import top.egon.cola.platform.rbac3.contract.authorization.AppAuthorizationContext;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Projects one user snapshot into a system-specific authorization view.
 */
public final class SystemAuthorizationSnapshotService {

    private static final Duration DEFAULT_TTL = Duration.ofHours(12);
    private final AuthorizationSnapshotRepository snapshots;
    private final Clock clock;

    public SystemAuthorizationSnapshotService(
            AuthorizationSnapshotRepository snapshots,
            Clock clock) {
        this.snapshots = Objects.requireNonNull(snapshots, "snapshots");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** Compatibility constructor; context initializers are no longer used. */
    public SystemAuthorizationSnapshotService(
            AuthorizationSnapshotRepository snapshots,
            Clock clock,
            Object ignored) {
        this(snapshots, clock);
    }

    public SystemAuthorizationSnapshot snapshot(
            String tenantId,
            String identitySub,
            String systemCode) {
        SnapshotRecordVO record = snapshots.load(tenantId, identitySub);
        if (!identitySub.equals(record.identitySub())) {
            throw new Rbac3RuleViolation("IDENTITY_SUBJECT_MISMATCH");
        }
        var user = record.snapshot();
        AppAuthorizationContext app = user.appContexts().stream()
                .filter(context -> systemCode.equals(context.applicationCode())
                        || systemCode.equals(context.applicationId()))
                .findFirst()
                .orElseThrow(() -> new Rbac3RuleViolation("AUTHORIZATION_DENIED"));
        Instant generatedAt = clock.instant();
        Instant expiresAt = user.expiresAt().isAfter(generatedAt)
                ? user.expiresAt() : generatedAt.plus(DEFAULT_TTL);
        return new SystemAuthorizationSnapshot(
                tenantId,
                identitySub,
                record.userId(),
                systemCode,
                user.authVersion(),
                user.policyVersion(),
                app.effectiveRoleIds(),
                null,
                app.landingRouteCode(),
                app.permissions(),
                app.dataScopes(),
                app.fieldPolicies(),
                user.checksum(),
                generatedAt,
                expiresAt);
    }
}
