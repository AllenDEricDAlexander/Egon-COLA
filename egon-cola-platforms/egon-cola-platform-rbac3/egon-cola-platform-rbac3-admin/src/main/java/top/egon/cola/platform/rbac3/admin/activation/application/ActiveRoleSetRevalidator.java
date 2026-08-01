package top.egon.cola.platform.rbac3.admin.activation.application;

import top.egon.cola.platform.rbac3.core.activation.DefaultRoleActivationResolver;
import top.egon.cola.platform.rbac3.core.activation.RoleActivationInput;
import top.egon.cola.platform.rbac3.core.activation.RoleActivationResolver;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Revalidates persisted roots after policy or assignment facts change.
 */
public final class ActiveRoleSetRevalidator {

    private final RoleActivationCandidateService.ActivationFactSource factSource;
    private final CurrentActivationSource currentSource;
    private final ReselectionStore reselectionStore;
    private final RoleActivationResolver resolver;

    public ActiveRoleSetRevalidator(
            RoleActivationCandidateService.ActivationFactSource factSource,
            CurrentActivationSource currentSource,
            ReselectionStore reselectionStore
    ) {
        this(factSource, currentSource, reselectionStore,
                new DefaultRoleActivationResolver());
    }

    ActiveRoleSetRevalidator(
            RoleActivationCandidateService.ActivationFactSource factSource,
            CurrentActivationSource currentSource,
            ReselectionStore reselectionStore,
            RoleActivationResolver resolver
    ) {
        this.factSource = Objects.requireNonNull(factSource, "factSource");
        this.currentSource = Objects.requireNonNull(currentSource, "currentSource");
        this.reselectionStore = Objects.requireNonNull(reselectionStore, "reselectionStore");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
    }

    public RevalidationResult revalidate(RevalidationCommand command) {
        CurrentActivation current = currentSource.current(
                command.tenantId(), command.userId(), command.sessionId());
        if (current.rootRoleIds().isEmpty()) {
            return new RevalidationResult(false, true, "ROLE_ACTIVATION_REQUIRED");
        }
        var facts = factSource.load(
                command.tenantId(), command.userId(), command.databaseNow());
        try {
            resolver.resolve(new RoleActivationInput(
                    command.tenantId(), command.userId(), command.sessionId(),
                    current.rootRoleIds(), facts.assignments(), facts.hierarchy(),
                    facts.dsdSets(), facts.authorizationFacts(), facts.authVersion(),
                    current.sessionVersion(), facts.policyVersion(), command.databaseNow()));
            return new RevalidationResult(true, false, "ALLOW");
        } catch (Rbac3RuleViolation violation) {
            reselectionStore.requireReselection(
                    command.tenantId(), command.sessionId(),
                    current.sessionVersion(), command.databaseNow(), command.actorId());
            return new RevalidationResult(false, true, "ROLE_RESELECTION_REQUIRED");
        }
    }

    @FunctionalInterface
    public interface CurrentActivationSource {
        CurrentActivation current(String tenantId, String userId, String sessionId);
    }

    @FunctionalInterface
    public interface ReselectionStore {
        void requireReselection(
                String tenantId,
                String sessionId,
                long expectedSessionVersion,
                Instant now,
                String actorId);
    }

    public record RevalidationCommand(
            String tenantId,
            String userId,
            String sessionId,
            Instant databaseNow,
            String actorId
    ) {
    }

    public record CurrentActivation(List<String> rootRoleIds, long sessionVersion) {
        public CurrentActivation {
            rootRoleIds = List.copyOf(rootRoleIds);
        }
    }

    public record RevalidationResult(
            boolean valid,
            boolean activationRequired,
            String reasonCode
    ) {
    }
}
