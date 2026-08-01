package top.egon.cola.platform.rbac3.admin.activation.application;

import top.egon.cola.platform.rbac3.admin.auth.application.JwtTokenService;
import top.egon.cola.platform.rbac3.admin.snapshot.application.SessionSnapshotProjector;
import top.egon.cola.platform.rbac3.contract.activation.ActiveRoleSetView;
import top.egon.cola.platform.rbac3.contract.activation.ReplaceActiveRolesResult;
import top.egon.cola.platform.rbac3.core.activation.DefaultRoleActivationResolver;
import top.egon.cola.platform.rbac3.core.activation.RoleActivationInput;
import top.egon.cola.platform.rbac3.core.activation.RoleActivationResolution;
import top.egon.cola.platform.rbac3.core.activation.RoleActivationResolver;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * Replaces the current session's canonical activation roots and publishes one snapshot.
 */
public final class RoleActivationFacade {

    private static final Duration FENCE_TTL = Duration.ofMinutes(5);
    private static final Duration STRONG_AUTHENTICATION_MAX_AGE = Duration.ofMinutes(10);

    private final RoleActivationCandidateService.ActivationFactSource factSource;
    private final ActivationTransaction transaction;
    private final RoleActivationResolver resolver;
    private final SessionSnapshotProjector snapshotProjector;
    private final RuntimeStore runtimeStore;
    private final AccessTokenIssuer accessTokenIssuer;
    private final Clock clock;

    public RoleActivationFacade(
            RoleActivationCandidateService.ActivationFactSource factSource,
            ActivationTransaction transaction,
            SessionSnapshotProjector snapshotProjector,
            RuntimeStore runtimeStore,
            AccessTokenIssuer accessTokenIssuer,
            Clock clock
    ) {
        this(factSource, transaction, new DefaultRoleActivationResolver(),
                snapshotProjector, runtimeStore, accessTokenIssuer, clock);
    }

    RoleActivationFacade(
            RoleActivationCandidateService.ActivationFactSource factSource,
            ActivationTransaction transaction,
            RoleActivationResolver resolver,
            SessionSnapshotProjector snapshotProjector,
            RuntimeStore runtimeStore,
            AccessTokenIssuer accessTokenIssuer,
            Clock clock
    ) {
        this.factSource = Objects.requireNonNull(factSource, "factSource");
        this.transaction = Objects.requireNonNull(transaction, "transaction");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.snapshotProjector = Objects.requireNonNull(
                snapshotProjector, "snapshotProjector");
        this.runtimeStore = Objects.requireNonNull(runtimeStore, "runtimeStore");
        this.accessTokenIssuer = Objects.requireNonNull(
                accessTokenIssuer, "accessTokenIssuer");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public ReplaceActiveRolesResult replace(ReplaceCommand command) {
        Instant now = clock.instant();
        TransactionResult result = transaction.replace(command, now, session -> {
            RoleActivationCandidateService.ActivationFacts facts = factSource.load(
                    command.tenantId(), command.userId(), now);
            RoleActivationResolution resolution = resolver.resolve(new RoleActivationInput(
                    command.tenantId(),
                    command.userId(),
                    command.sessionId(),
                    command.requestedRoleIds(),
                    facts.assignments(),
                    facts.hierarchy(),
                    facts.dsdSets(),
                    facts.authorizationFacts(),
                    facts.authVersion(),
                    session.sessionVersion(),
                    facts.policyVersion(),
                    now));
            requireAuthenticationStrength(session, resolution, facts, now);
            return new ResolvedActivation(resolution, facts);
        });
        if (result.changed()) {
            try {
                runtimeStore.createFence(
                        command.tenantId(), command.sessionId(),
                        result.mutationId(), FENCE_TTL);
                transaction.markFenced(result.mutationId(), now);
                SessionSnapshotProjector.Projection projection = snapshotProjector.project(
                        new SessionSnapshotProjector.ProjectionCommand(
                                command.tenantId(), command.userId(), command.sessionId(),
                                result.authVersion(), result.sessionVersion(),
                                result.policyVersion(), result.expiresAt(),
                                result.resolved().resolution(),
                                result.resolved().facts(), now));
                runtimeStore.publish(new RuntimePublication(
                        command.tenantId(), command.userId(), command.sessionId(),
                        result.authVersion(), result.sessionVersion(),
                        result.policyVersion(), projection));
                transaction.markCompleted(result.mutationId(), now);
            } catch (RuntimeException exception) {
                try {
                    transaction.markRecoveryRequired(
                            result.mutationId(), "AUTH_PROPAGATION_PENDING", now);
                } catch (RuntimeException recoveryFailure) {
                    exception.addSuppressed(recoveryFailure);
                }
                throw new Rbac3RuleViolation(
                        "AUTH_PROPAGATION_PENDING", List.of(result.mutationId()));
            }
        }
        IssuedToken token = accessTokenIssuer.issue(
                command.tenantId(), command.userId(), command.sessionId(),
                result.authVersion(), result.sessionVersion(), result.policyVersion(), now);
        return new ReplaceActiveRolesResult(
                activeRoles(result.rootsByApplication(), result.resolved().facts()),
                result.changed(),
                result.sessionVersion(),
                result.authVersion(),
                result.policyVersion(),
                token.token(),
                Math.max(0L, Duration.between(now, token.expiresAt()).toSeconds()),
                false,
                false,
                result.snapshotChecksum());
    }

    public ActiveRoleSetView current(
            String tenantId,
            String userId,
            String sessionId
    ) {
        Instant now = clock.instant();
        RoleActivationCandidateService.ActivationFacts facts = factSource.load(
                tenantId, userId, now);
        CurrentState state = transaction.current(tenantId, userId, sessionId, now);
        return new ActiveRoleSetView(
                sessionId,
                activeRoles(state.rootsByApplication(), facts),
                state.activationRequired(),
                state.authVersion(),
                state.sessionVersion(),
                state.policyVersion(),
                state.snapshotChecksum() == null ? "unavailable" : state.snapshotChecksum());
    }

    private List<ActiveRoleSetView.ApplicationActiveRoles> activeRoles(
            Map<String, Set<String>> rootsByApplication,
            RoleActivationCandidateService.ActivationFacts facts
    ) {
        var result = new ArrayList<ActiveRoleSetView.ApplicationActiveRoles>();
        new TreeMap<>(rootsByApplication).forEach((applicationId, roots) -> {
            RoleActivationCandidateService.ApplicationFact application =
                    facts.applications().get(applicationId);
            if (application == null) {
                throw new IllegalStateException("missing application fact: " + applicationId);
            }
            result.add(new ActiveRoleSetView.ApplicationActiveRoles(
                    application.code(), new ArrayList<>(new TreeSet<>(roots))));
        });
        return result;
    }

    private void requireAuthenticationStrength(
            SessionState session,
            RoleActivationResolution resolution,
            RoleActivationCandidateService.ActivationFacts facts,
            Instant now) {
        int required = resolution.snapshot().effectiveRoleIds().stream()
                .map(facts.hierarchy()::requireNode)
                .map(node -> node.riskLevel())
                .mapToInt(risk -> switch (risk) {
                    case LOW, MEDIUM -> 0;
                    case HIGH -> 1;
                    case CRITICAL -> 2;
                })
                .max()
                .orElse(0);
        int actual = switch (session.authenticationStrength()) {
            case "PASSWORD" -> 0;
            case "MFA" -> 1;
            case "STRONG" -> session.strongAuthenticatedAt() != null
                    && session.strongAuthenticatedAt()
                    .plus(STRONG_AUTHENTICATION_MAX_AGE).isAfter(now) ? 2 : 0;
            default -> 0;
        };
        if (actual < required) {
            throw new Rbac3RuleViolation("STEP_UP_REQUIRED");
        }
    }

    public static AccessTokenIssuer jwtIssuer(JwtTokenService tokenService) {
        Objects.requireNonNull(tokenService, "tokenService");
        return (tenantId, userId, sessionId, authVersion, sessionVersion,
                policyVersion, now) -> {
            JwtTokenService.IssuedAccessToken token = tokenService.issue(
                    new JwtTokenService.AccessTokenSubject(
                            tenantId, userId, sessionId,
                            authVersion, sessionVersion, policyVersion), now);
            return new IssuedToken(token.token(), token.expiresAt());
        };
    }

    public interface ActivationTransaction {

        TransactionResult replace(
                ReplaceCommand command,
                Instant now,
                Function<SessionState, ResolvedActivation> resolutionFactory);

        CurrentState current(
                String tenantId,
                String userId,
                String sessionId,
                Instant now);

        default void markFenced(String mutationId, Instant now) {
        }

        default void markCompleted(String mutationId, Instant now) {
        }

        default void markRecoveryRequired(
                String mutationId,
                String reasonCode,
                Instant now
        ) {
        }
    }

    public interface RuntimeStore {

        void createFence(
                String tenantId,
                String sessionId,
                String mutationId,
                Duration ttl);

        void publish(RuntimePublication publication);
    }

    @FunctionalInterface
    public interface AccessTokenIssuer {

        IssuedToken issue(
                String tenantId,
                String userId,
                String sessionId,
                long authVersion,
                long sessionVersion,
                long policyVersion,
                Instant now);
    }

    public record ReplaceCommand(
            String tenantId,
            String userId,
            String sessionId,
            List<String> requestedRoleIds,
            long expectedSessionVersion,
            String actorId,
            String commandId
    ) {

        public ReplaceCommand {
            requestedRoleIds = List.copyOf(requestedRoleIds);
            if (expectedSessionVersion < 0) {
                throw new IllegalArgumentException(
                        "expectedSessionVersion must not be negative");
            }
            Objects.requireNonNull(commandId, "commandId");
        }
    }

    public record SessionState(
            String tenantId,
            String userId,
            String sessionId,
            Map<String, Set<String>> rootsByApplication,
            long authVersion,
            long sessionVersion,
            long policyVersion,
            String snapshotChecksum,
            boolean activationRequired,
            Instant expiresAt,
            String authenticationStrength,
            Instant strongAuthenticatedAt
    ) {

        public SessionState(
                String tenantId,
                String userId,
                String sessionId,
                Map<String, Set<String>> rootsByApplication,
                long authVersion,
                long sessionVersion,
                long policyVersion,
                String snapshotChecksum,
                boolean activationRequired,
                Instant expiresAt) {
            this(tenantId, userId, sessionId, rootsByApplication, authVersion,
                    sessionVersion, policyVersion, snapshotChecksum,
                    activationRequired, expiresAt, "PASSWORD", null);
        }
    }

    public record ResolvedActivation(
            RoleActivationResolution resolution,
            RoleActivationCandidateService.ActivationFacts facts
    ) {
    }

    public record TransactionResult(
            ResolvedActivation resolved,
            boolean changed,
            String mutationId,
            Map<String, Set<String>> rootsByApplication,
            long authVersion,
            long sessionVersion,
            long policyVersion,
            String snapshotChecksum,
            Instant expiresAt
    ) {
    }

    public record CurrentState(
            Map<String, Set<String>> rootsByApplication,
            long authVersion,
            long sessionVersion,
            long policyVersion,
            String snapshotChecksum,
            boolean activationRequired
    ) {
    }

    public record RuntimePublication(
            String tenantId,
            String userId,
            String sessionId,
            long authVersion,
            long sessionVersion,
            long policyVersion,
            SessionSnapshotProjector.Projection projection
    ) {
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }
}
