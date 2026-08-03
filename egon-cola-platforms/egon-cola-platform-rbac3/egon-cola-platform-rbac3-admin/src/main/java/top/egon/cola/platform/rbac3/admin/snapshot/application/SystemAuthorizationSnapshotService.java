package top.egon.cola.platform.rbac3.admin.snapshot.application;

import top.egon.cola.platform.rbac3.admin.authorization.application.AuthorizationDecisionService;
import top.egon.cola.platform.rbac3.admin.session.application.AuthorizationContextFacade;
import top.egon.cola.platform.rbac3.contract.authorization.AppAuthorizationContext;
import top.egon.cola.platform.rbac3.contract.authorization.SessionAuthorizationSnapshot;
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
    private static final Duration CONCURRENT_SNAPSHOT_RETRY_PAUSE =
            Duration.ofMillis(25);
    private static final int CONCURRENT_SNAPSHOT_READ_ATTEMPTS = 20;
    private static final String RBAC3_ADMIN_SYSTEM = "rbac3-admin";
    private static final Set<String> RETRYABLE_SNAPSHOT_REASONS = Set.of(
            "AUTH_SNAPSHOT_NOT_READY",
            "AUTH_PROPAGATION_PENDING");
    private static final Set<String> ROLE_ACTIVATION_PERMISSIONS = Set.of(
            "system:role-activation:read",
            "system:role-activation:use");

    private final AuthorizationContextFacade.ContextOpener contexts;
    private final AuthorizationDecisionService.SnapshotSource snapshots;
    private final Clock clock;
    private final ContextInitializer contextInitializer;
    private final RetryPause retryPause;

    public SystemAuthorizationSnapshotService(
            AuthorizationContextFacade.ContextOpener contexts,
            AuthorizationDecisionService.SnapshotSource snapshots,
            Clock clock) {
        this(contexts, snapshots, clock,
                (context, now) -> ContextInitialization.UNCHANGED);
    }

    public SystemAuthorizationSnapshotService(
            AuthorizationContextFacade.ContextOpener contexts,
            AuthorizationDecisionService.SnapshotSource snapshots,
            Clock clock,
            ContextInitializer contextInitializer) {
        this(contexts, snapshots, clock, contextInitializer,
                SystemAuthorizationSnapshotService::pause);
    }

    SystemAuthorizationSnapshotService(
            AuthorizationContextFacade.ContextOpener contexts,
            AuthorizationDecisionService.SnapshotSource snapshots,
            Clock clock,
            ContextInitializer contextInitializer,
            RetryPause retryPause) {
        this.contexts = Objects.requireNonNull(contexts, "contexts");
        this.snapshots = Objects.requireNonNull(snapshots, "snapshots");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.contextInitializer = Objects.requireNonNull(
                contextInitializer, "contextInitializer");
        this.retryPause = Objects.requireNonNull(retryPause, "retryPause");
    }

    public SystemAuthorizationSnapshot snapshot(
            String tenantId,
            String sessionId,
            String systemCode,
            String identitySub) {
        Instant now = clock.instant();
        AuthorizationContextFacade.AuthorizationContext context = contexts.open(
                tenantId, sessionId, identitySub, now, now.plus(DEFAULT_CONTEXT_TTL));
        ContextInitialization initialization = ContextInitialization.UNCHANGED;
        if (context.activationRequired()) {
            initialization = contextInitializer.initialize(context, now);
        }
        if (initialization != ContextInitialization.UNCHANGED) {
            context = contexts.open(
                    tenantId, sessionId, identitySub, now,
                    now.plus(DEFAULT_CONTEXT_TTL));
        }
        if (context.activationRequired()) {
            return empty(context, systemCode, now);
        }
        AuthorizationDecisionService.SnapshotRecord record = loadSnapshot(
                context, initialization);
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

    private AuthorizationDecisionService.SnapshotRecord loadSnapshot(
            AuthorizationContextFacade.AuthorizationContext context,
            ContextInitialization initialization) {
        for (int attempt = 1; attempt <= CONCURRENT_SNAPSHOT_READ_ATTEMPTS; attempt++) {
            try {
                AuthorizationDecisionService.SnapshotRecord record = snapshots.load(
                        context.tenantId(), context.sessionId());
                requireCurrentVersions(context, record.snapshot());
                return record;
            } catch (Rbac3RuleViolation violation) {
                boolean retry = initialization == ContextInitialization.CONCURRENT
                        && RETRYABLE_SNAPSHOT_REASONS.contains(violation.reasonCode())
                        && attempt < CONCURRENT_SNAPSHOT_READ_ATTEMPTS;
                if (!retry) {
                    throw violation;
                }
                retryPause.pause(CONCURRENT_SNAPSHOT_RETRY_PAUSE);
            }
        }
        throw new IllegalStateException("concurrent snapshot retry exhausted");
    }

    private static void requireCurrentVersions(
            AuthorizationContextFacade.AuthorizationContext context,
            SessionAuthorizationSnapshot snapshot) {
        if (snapshot.authVersion() != context.authVersion()
                || snapshot.sessionVersion() != context.contextVersion()
                || snapshot.policyVersion() != context.policyVersion()) {
            throw new Rbac3RuleViolation("AUTH_PROPAGATION_PENDING");
        }
    }

    private static void pause(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new Rbac3RuleViolation("AUTH_PROPAGATION_PENDING");
        }
    }

    private SystemAuthorizationSnapshot empty(
            AuthorizationContextFacade.AuthorizationContext context,
            String systemCode,
            Instant now) {
        Set<String> permissions = RBAC3_ADMIN_SYSTEM.equals(systemCode)
                ? ROLE_ACTIVATION_PERMISSIONS : Set.of();
        return new SystemAuthorizationSnapshot(
                context.tenantId(), context.identitySub(), context.rbac3UserId(),
                context.sessionId(), systemCode, context.authVersion(),
                context.contextVersion(), context.policyVersion(), List.of(), permissions,
                Map.of(), Map.of(), "empty:" + context.contextVersion(), now,
                context.expiresAt());
    }

    @FunctionalInterface
    public interface ContextInitializer {

        ContextInitialization initialize(
                AuthorizationContextFacade.AuthorizationContext context,
                Instant now);
    }

    public enum ContextInitialization {
        UNCHANGED,
        COMPLETED,
        CONCURRENT
    }

    @FunctionalInterface
    interface RetryPause {

        void pause(Duration duration);
    }
}
