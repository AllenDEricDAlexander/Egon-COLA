package top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.activation.service;

import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.activation.domain.dto.ReplaceCommandDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.activation.domain.vo.ActivationFactsVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.activation.domain.vo.ApplicationFactVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.activation.domain.vo.CurrentStateVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.activation.domain.vo.ResolvedActivationVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.activation.domain.vo.RuntimePublicationVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.activation.domain.vo.TransactionResultVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.activation.repository.ActivationTransaction;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.activation.repository.RoleActivationFactRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.activation.repository.RoleActivationRuntimeRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.domain.dto.ProjectionCommandDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.domain.vo.UserSnapshotProjectionVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.repository.Rbac3RuntimePolicy;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.service.UserAuthorizationSnapshotProjector;
import top.egon.cola.platform.tianquan.jianshen.contract.activation.ActiveRoleSetView;
import top.egon.cola.platform.tianquan.jianshen.contract.activation.ReplaceActiveRolesResult;
import top.egon.cola.platform.tianquan.jianshen.core.activation.DefaultRoleActivationResolver;
import top.egon.cola.platform.tianquan.jianshen.core.activation.RoleActivationInput;
import top.egon.cola.platform.tianquan.jianshen.core.activation.RoleActivationResolution;
import top.egon.cola.platform.tianquan.jianshen.core.activation.RoleActivationResolver;
import top.egon.cola.platform.tianquan.jianshen.core.rule.Rbac3RuleViolation;

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

/**
 * Replaces user active roots and publishes a user authorization snapshot.
 */
public final class RoleActivationFacade {

    private static final Duration FENCE_TTL = Duration.ofSeconds(30);
    private static final Duration SNAPSHOT_TTL = Duration.ofHours(12);

    private final RoleActivationFactRepository factSource;
    private final ActivationTransaction transaction;
    private final RoleActivationResolver resolver;
    private final UserAuthorizationSnapshotProjector snapshotProjector;
    private final RoleActivationRuntimeRepository runtimeStore;
    private final Rbac3RuntimePolicy runtimePolicy;
    private final Clock clock;

    public RoleActivationFacade(
            RoleActivationFactRepository factSource,
            ActivationTransaction transaction,
            UserAuthorizationSnapshotProjector snapshotProjector,
            RoleActivationRuntimeRepository runtimeStore,
            Rbac3RuntimePolicy runtimePolicy,
            Clock clock) {
        this(factSource, transaction, new DefaultRoleActivationResolver(),
                snapshotProjector, runtimeStore, runtimePolicy, clock);
    }

    RoleActivationFacade(
            RoleActivationFactRepository factSource,
            ActivationTransaction transaction,
            RoleActivationResolver resolver,
            UserAuthorizationSnapshotProjector snapshotProjector,
            RoleActivationRuntimeRepository runtimeStore,
            Rbac3RuntimePolicy runtimePolicy,
            Clock clock) {
        this.factSource = Objects.requireNonNull(factSource, "factSource");
        this.transaction = Objects.requireNonNull(transaction, "transaction");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.snapshotProjector = Objects.requireNonNull(snapshotProjector, "snapshotProjector");
        this.runtimeStore = Objects.requireNonNull(runtimeStore, "runtimeStore");
        this.runtimePolicy = Objects.requireNonNull(runtimePolicy, "runtimePolicy");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public ReplaceActiveRolesResult replace(ReplaceCommandDTO command) {
        return replace(command, false);
    }

    /** Reprojects an existing selection without allowing hierarchy normalization to select new roots. */
    public ReplaceActiveRolesResult refresh(ReplaceCommandDTO command) {
        return replace(command, true);
    }

    private ReplaceActiveRolesResult replace(ReplaceCommandDTO command, boolean preserveRoots) {
        Instant now = clock.instant();
        TransactionResultVO result = transaction.replace(command, now, state -> {
            ActivationFactsVO facts = factSource.load(command.tenantId(), command.userId(), now);
            RoleActivationResolution resolution = resolver.resolve(new RoleActivationInput(
                    command.tenantId(),
                    command.userId(),
                    command.requestedRoleIds(),
                    facts.assignments(),
                    facts.hierarchy(),
                    facts.dsdSets(),
                    facts.authorizationFacts(),
                    facts.authVersion(),
                    facts.policyVersion(),
                    now));
            requireWithinRootLimit(resolution);
            if (preserveRoots && !resolution.activeRoleSet().rootIds().equals(Set.copyOf(command.requestedRoleIds()))) {
                throw new Rbac3RuleViolation("ROLE_ACTIVATION_SET_INVALID");
            }
            return new ResolvedActivationVO(resolution, facts);
        });

        boolean changed = result.changed();
        if (changed) {
            runtimeStore.createFence(command.tenantId(), command.identitySub(),
                    result.mutationId(), FENCE_TTL);
            transaction.markFenced(result.mutationId(), now);
        }
        try {
            // An idempotent activation can still be the first request after a runtime
            // restart, so publish the same resolved snapshot when the role set is unchanged.
            UserSnapshotProjectionVO projection = snapshotProjector.project(
                    new ProjectionCommandDTO(
                            command.tenantId(), command.identitySub(), command.userId(),
                            result.authVersion(), result.policyVersion(),
                            result.expiresAt(), result.resolved().resolution(),
                            result.resolved().facts(), now));
            runtimeStore.publish(new RuntimePublicationVO(
                    command.tenantId(), command.identitySub(), command.userId(),
                    result.authVersion(), result.policyVersion(), projection));
            if (changed) {
                transaction.markCompleted(result.mutationId(), now);
            }
        } catch (RuntimeException exception) {
            if (changed) {
                try {
                    transaction.markRecoveryRequired(
                            result.mutationId(), "AUTH_PROPAGATION_PENDING", now);
                } catch (RuntimeException recoveryFailure) {
                    exception.addSuppressed(recoveryFailure);
                }
            }
            throw new Rbac3RuleViolation(
                    "AUTH_PROPAGATION_PENDING", List.of(result.mutationId()));
        }

        return new ReplaceActiveRolesResult(
                activeRoles(result.rootsByApplication(), result.resolved().facts()),
                result.changed(),
                result.authVersion(),
                result.policyVersion(),
                false,
                result.snapshotChecksum());
    }

    public ActiveRoleSetView current(
            String tenantId,
            String identitySub,
            String userId) {
        Instant now = clock.instant();
        ActivationFactsVO facts = factSource.load(tenantId, userId, now);
        CurrentStateVO state = transaction.current(tenantId, identitySub, userId, now);
        return new ActiveRoleSetView(
                activeRoles(state.rootsByApplication(), facts),
                state.rootsByApplication().isEmpty(),
                state.authVersion(),
                facts.policyVersion(),
                state.snapshotChecksum() == null ? "unavailable" : state.snapshotChecksum());
    }

    private List<ActiveRoleSetView.ApplicationActiveRoles> activeRoles(
            Map<String, Set<String>> rootsByApplication,
            ActivationFactsVO facts) {
        var result = new ArrayList<ActiveRoleSetView.ApplicationActiveRoles>();
        new TreeMap<>(rootsByApplication).forEach((applicationId, roots) -> {
            ApplicationFactVO application = facts.applications().get(applicationId);
            if (application == null) {
                throw new IllegalStateException("missing application fact: " + applicationId);
            }
            result.add(new ActiveRoleSetView.ApplicationActiveRoles(
                    application.code(), new ArrayList<>(new TreeSet<>(roots))));
        });
        return result;
    }

    private void requireWithinRootLimit(RoleActivationResolution resolution) {
        int actual = resolution.activeRoleSet().rootIds().size();
        int maximum = runtimePolicy.current().maximumActiveRoots();
        if (actual > maximum) {
            throw new Rbac3RuleViolation(
                    "ACTIVE_ROLE_ROOT_LIMIT_EXCEEDED",
                    List.of(Integer.toString(actual), Integer.toString(maximum)));
        }
    }
}
