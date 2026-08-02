package top.egon.cola.platform.rbac3.admin.snapshot.application;

import top.egon.cola.platform.rbac3.admin.authorization.application.AuthorizationDecisionService;
import top.egon.cola.platform.rbac3.admin.session.application.AuthorizationContextFacade;
import top.egon.cola.platform.rbac3.contract.authorization.AppAuthorizationContext;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Projects an immutable system-only view from the canonical session snapshot. */
public final class SystemAuthorizationSnapshotService {

    private static final Duration DEFAULT_CONTEXT_TTL = Duration.ofHours(12);

    private final AuthorizationContextFacade.ContextOpener contexts;
    private final AuthorizationDecisionService.SnapshotSource snapshots;
    private final Clock clock;

    public SystemAuthorizationSnapshotService(
            AuthorizationContextFacade.ContextOpener contexts,
            AuthorizationDecisionService.SnapshotSource snapshots,
            Clock clock) {
        this.contexts = Objects.requireNonNull(contexts, "contexts");
        this.snapshots = Objects.requireNonNull(snapshots, "snapshots");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public SystemAuthorizationSnapshot snapshot(
            String tenantId,
            String sessionId,
            String systemCode,
            String identitySub) {
        Instant now = clock.instant();
        AuthorizationContextFacade.AuthorizationContext context = contexts.open(
                tenantId, sessionId, identitySub, now, now.plus(DEFAULT_CONTEXT_TTL));
        if (context.activationRequired()) {
            return empty(context, systemCode, now);
        }
        AuthorizationDecisionService.SnapshotRecord record = snapshots.load(
                context.tenantId(), context.sessionId());
        if (!record.identitySub().equals(context.identitySub())
                || !record.userId().equals(context.rbac3UserId())) {
            throw new Rbac3RuleViolation("AUTHORIZATION_CONTEXT_MISMATCH");
        }
        AppAuthorizationContext application = record.snapshot().appContexts().stream()
                .filter(candidate -> candidate.applicationCode().equals(systemCode))
                .findFirst()
                .orElseThrow(() -> new Rbac3RuleViolation("APPLICATION_BINDING_DENIED"));
        return new SystemAuthorizationSnapshot(
                context.tenantId(), context.identitySub(), context.rbac3UserId(),
                context.sessionId(), systemCode, record.snapshot().authVersion(),
                record.snapshot().sessionVersion(), record.snapshot().policyVersion(),
                application.effectiveRoleIds(), application.permissions(),
                application.dataScopes(), application.fieldPolicies(),
                record.snapshot().checksum(), record.snapshot().generatedAt(),
                context.expiresAt());
    }

    private SystemAuthorizationSnapshot empty(
            AuthorizationContextFacade.AuthorizationContext context,
            String systemCode,
            Instant now) {
        return new SystemAuthorizationSnapshot(
                context.tenantId(), context.identitySub(), context.rbac3UserId(),
                context.sessionId(), systemCode, context.authVersion(),
                context.contextVersion(), context.policyVersion(), List.of(), Set.of(),
                Map.of(), Map.of(), "empty:" + context.contextVersion(), now,
                context.expiresAt());
    }
}
