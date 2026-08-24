package top.egon.cola.platform.rbac3.admin.runtime.service;

import top.egon.cola.platform.rbac3.admin.authorization.domain.vo.SnapshotRecordVO;
import top.egon.cola.platform.rbac3.admin.authorization.repository.AuthorizationSnapshotRepository;
import top.egon.cola.platform.rbac3.admin.runtime.repository.InitialAuthorizationContextRepository;
import top.egon.cola.platform.rbac3.contract.authorization.AppAuthorizationContext;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Projects one user snapshot into a system-specific authorization view.
 */
public final class SystemAuthorizationSnapshotService {

    private static final Duration DEFAULT_TTL = Duration.ofHours(12);
    private static final String RBAC3_ADMIN_SYSTEM = "rbac3-admin";
    private static final Set<String> ROLE_ACTIVATION_PERMISSIONS = Set.of(
            "system:role-activation:read",
            "system:role-activation:use"
    );
    private final AuthorizationSnapshotRepository snapshots;
    private final InitialAuthorizationContextRepository initialContexts;
    private final Clock clock;

    public SystemAuthorizationSnapshotService(
            AuthorizationSnapshotRepository snapshots,
            Clock clock) {
        this(snapshots, (tenantId, identitySub) -> Optional.empty(), clock);
    }

    public SystemAuthorizationSnapshotService(
            AuthorizationSnapshotRepository snapshots,
            InitialAuthorizationContextRepository initialContexts,
            Clock clock) {
        this.snapshots = Objects.requireNonNull(snapshots, "snapshots");
        this.initialContexts = Objects.requireNonNull(
                initialContexts, "initialContexts");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public SystemAuthorizationSnapshot snapshot(
            String tenantId,
            String identitySub,
            String systemCode) {
        SnapshotRecordVO record;
        try {
            record = snapshots.load(tenantId, identitySub);
        } catch (Rbac3RuleViolation unavailable) {
            if ("AUTH_SNAPSHOT_NOT_READY".equals(unavailable.reasonCode())) {
                return initialSnapshot(tenantId, identitySub, systemCode)
                        .orElseThrow(() -> unavailable);
            }
            throw unavailable;
        }
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

    private Optional<SystemAuthorizationSnapshot> initialSnapshot(
            String tenantId,
            String identitySub,
            String systemCode
    ) {
        if (!RBAC3_ADMIN_SYSTEM.equals(systemCode)) {
            return Optional.empty();
        }
        Instant generatedAt = clock.instant();
        return initialContexts.find(tenantId, identitySub).map(context ->
                new SystemAuthorizationSnapshot(
                        tenantId,
                        identitySub,
                        context.userId(),
                        systemCode,
                        context.authVersion(),
                        context.policyVersion(),
                        List.of(),
                        null,
                        null,
                        ROLE_ACTIVATION_PERMISSIONS,
                        Map.of(),
                        Map.of(),
                        "initial:" + context.authVersion()
                                + ':' + context.policyVersion(),
                        generatedAt,
                        generatedAt.plus(DEFAULT_TTL)
                ));
    }
}
