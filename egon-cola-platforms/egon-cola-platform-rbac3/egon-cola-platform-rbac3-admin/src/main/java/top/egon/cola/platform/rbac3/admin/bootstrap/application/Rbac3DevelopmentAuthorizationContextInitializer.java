package top.egon.cola.platform.rbac3.admin.bootstrap.application;

import top.egon.cola.platform.rbac3.admin.activation.application.RoleActivationCandidateService;
import top.egon.cola.platform.rbac3.admin.activation.application.RoleActivationFacade;
import top.egon.cola.platform.rbac3.admin.session.application.AuthorizationContextFacade;
import top.egon.cola.platform.rbac3.admin.snapshot.application.SystemAuthorizationSnapshotService;
import top.egon.cola.platform.rbac3.contract.activation.RoleActivationCandidateView;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Activates the generated local administrator roles for an opted-in development stack. */
public final class Rbac3DevelopmentAuthorizationContextInitializer
        implements SystemAuthorizationSnapshotService.ContextInitializer {

    private static final String DEVELOPMENT_ACTOR = "development-bootstrap";
    private static final Map<String, String> DEVELOPMENT_ROLES_BY_APPLICATION =
            Rbac3DevelopmentTopology.applications().stream()
                    .collect(Collectors.toUnmodifiableMap(
                            Rbac3DevelopmentTopology.ApplicationDefinition::applicationCode,
                            Rbac3DevelopmentTopology.ApplicationDefinition::roleCode));

    private final boolean enabled;
    private final CandidateSource candidates;
    private final RoleActivator activator;

    public Rbac3DevelopmentAuthorizationContextInitializer(
            boolean enabled,
            RoleActivationCandidateService candidates,
            RoleActivationFacade activator) {
        this(enabled, candidates::candidates, activator::replace);
    }

    Rbac3DevelopmentAuthorizationContextInitializer(
            boolean enabled,
            CandidateSource candidates,
            RoleActivator activator) {
        this.enabled = enabled;
        this.candidates = Objects.requireNonNull(candidates, "candidates");
        this.activator = Objects.requireNonNull(activator, "activator");
    }

    @Override
    public SystemAuthorizationSnapshotService.ContextInitialization initialize(
            AuthorizationContextFacade.AuthorizationContext context,
            Instant now) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(now, "now");
        if (!enabled || !context.activationRequired()) {
            return SystemAuthorizationSnapshotService.ContextInitialization.UNCHANGED;
        }
        RoleActivationCandidateView view = candidates.load(
                context.tenantId(), context.rbac3UserId(), now);
        List<String> roleIds = view.applications().stream()
                .flatMap(application -> application.candidates().stream()
                        .filter(candidate -> Objects.equals(
                                DEVELOPMENT_ROLES_BY_APPLICATION.get(
                                        application.applicationCode()),
                                candidate.rootRoleCode())))
                .filter(candidate -> "PASSWORD".equals(candidate.requiredAuthStrength()))
                .map(candidate -> candidate.rootRoleId())
                .sorted()
                .toList();
        if (roleIds.isEmpty()) {
            return SystemAuthorizationSnapshotService.ContextInitialization.UNCHANGED;
        }
        try {
            activator.replace(new RoleActivationFacade.ReplaceCommand(
                    context.tenantId(), context.identitySub(),
                    context.rbac3UserId(), context.sessionId(), roleIds,
                    context.contextVersion(), DEVELOPMENT_ACTOR,
                    "development-bootstrap:auto-activate-local-admin:"
                            + context.tenantId() + ':' + context.sessionId()
                            + ':' + context.contextVersion()));
        } catch (Rbac3RuleViolation violation) {
            if (!"ROLE_ACTIVATION_VERSION_CONFLICT".equals(
                    violation.reasonCode())) {
                throw violation;
            }
            return SystemAuthorizationSnapshotService.ContextInitialization.CONCURRENT;
        }
        return SystemAuthorizationSnapshotService.ContextInitialization.COMPLETED;
    }

    @FunctionalInterface
    interface CandidateSource {

        RoleActivationCandidateView load(
                String tenantId,
                String userId,
                Instant now);
    }

    @FunctionalInterface
    interface RoleActivator {

        void replace(RoleActivationFacade.ReplaceCommand command);
    }
}
