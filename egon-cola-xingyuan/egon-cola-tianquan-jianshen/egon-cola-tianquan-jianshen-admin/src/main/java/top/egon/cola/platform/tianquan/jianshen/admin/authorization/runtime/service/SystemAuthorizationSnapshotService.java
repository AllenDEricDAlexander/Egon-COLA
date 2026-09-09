package top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.service;

import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.decision.domain.vo.SnapshotRecordVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.decision.repository.AuthorizationSnapshotRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.repository.InitialAuthorizationContextRepository;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.AppAuthorizationContext;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.tianquan.jianshen.core.rule.Rbac3RuleViolation;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.LinkedHashSet;
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
            "system:about:read",
            "system:role-activation:read",
            "system:role-activation:use"
    );
    private static final String DDC_ADMIN_SYSTEM = "ddc-admin";
    private static final Set<String> DDC_BOOTSTRAP_PERMISSIONS = Set.of(
            "DDC_READ",
            "DDC_WRITE",
            "DDC_PUBLISH",
            "DDC_CACHE"
    );
    private final AuthorizationSnapshotRepository snapshots;
    private final InitialAuthorizationContextRepository initialContexts;
    private final Clock clock;
    private final boolean ddcInitialContextEnabled;

    public SystemAuthorizationSnapshotService(
            AuthorizationSnapshotRepository snapshots,
            Clock clock) {
        this(snapshots, (tenantId, identitySub) -> Optional.empty(), clock);
    }

    public SystemAuthorizationSnapshotService(
            AuthorizationSnapshotRepository snapshots,
            InitialAuthorizationContextRepository initialContexts,
            Clock clock) {
        this(snapshots, initialContexts, clock, false);
    }

    /**
     * Creates the snapshot projector with an optional local DDC bootstrap context.
     * The bootstrap context is intentionally disabled by default so a deployed
     * RBAC3 instance still fails closed until its runtime snapshot is published.
     */
    public SystemAuthorizationSnapshotService(
            AuthorizationSnapshotRepository snapshots,
            InitialAuthorizationContextRepository initialContexts,
            Clock clock,
            boolean ddcInitialContextEnabled) {
        this.snapshots = Objects.requireNonNull(snapshots, "snapshots");
        this.initialContexts = Objects.requireNonNull(
                initialContexts, "initialContexts");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.ddcInitialContextEnabled = ddcInitialContextEnabled;
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
                .orElse(null);
        if (app == null) {
            // Selecting a business role must not remove an active member's own role-selection entry.
            // This does not bootstrap DDC or grant any RBAC management capability.
            if (RBAC3_ADMIN_SYSTEM.equals(systemCode)) {
                return initialSnapshot(tenantId, identitySub, systemCode)
                        .orElseThrow(() -> new Rbac3RuleViolation("AUTHORIZATION_DENIED"));
            }
            throw new Rbac3RuleViolation("AUTHORIZATION_DENIED");
        }
        Set<String> permissions = app.permissions();
        if (RBAC3_ADMIN_SYSTEM.equals(systemCode)) {
            permissions = new LinkedHashSet<>(permissions);
            permissions.addAll(ROLE_ACTIVATION_PERMISSIONS);
        }
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
                permissions,
                Set.copyOf(app.resourceCodes()),
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
        Set<String> permissions;
        if (RBAC3_ADMIN_SYSTEM.equals(systemCode)) {
            permissions = ROLE_ACTIVATION_PERMISSIONS;
        } else if (DDC_ADMIN_SYSTEM.equals(systemCode)
                && ddcInitialContextEnabled) {
            permissions = DDC_BOOTSTRAP_PERMISSIONS;
        } else {
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
                        permissions,
                        Set.of(),
                        Map.of(),
                        Map.of(),
                        "initial:" + context.authVersion()
                                + ':' + context.policyVersion(),
                        generatedAt,
                        generatedAt.plus(DEFAULT_TTL)
                ));
    }
}
