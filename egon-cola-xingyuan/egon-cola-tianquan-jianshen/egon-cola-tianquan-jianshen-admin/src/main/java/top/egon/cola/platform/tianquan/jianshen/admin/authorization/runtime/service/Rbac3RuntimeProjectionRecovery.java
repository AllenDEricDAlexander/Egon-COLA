package top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.service;

import org.springframework.stereotype.Component;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.activation.domain.dto.ReplaceCommandDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.activation.repository.ActivationTransaction;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.activation.repository.ReselectionRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.activation.service.RoleActivationFacade;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.domain.dto.MutationWorkDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.domain.vo.EventEnvelopeVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.repository.RuntimeProjectionTargetRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.repository.RuntimeProjectionTargetRepository.ProjectionTarget;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.repository.RuntimePublicationRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.state.repository.TenantAuthorizationStateRepository;
import top.egon.cola.platform.tianquan.jianshen.core.rule.Rbac3RuleViolation;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Rebuilds publications from current database facts and previously selected roots.
 * Durable mutation/outbox workers retain failed work for retry; an event payload
 * is only a repair trigger, never a source of granted roles or permissions.
 */
@Component
public final class Rbac3RuntimeProjectionRecovery implements
        RuntimeProjectionExecutor,
        RuntimeSnapshotRebuildService {

    private static final int PAGE_SIZE = 200;
    private static final String ACTOR = "tianquan-jianshen-runtime-recovery";
    private static final Set<String> RESELECTION_REASONS = Set.of(
            "ROLE_ACTIVATION_ASSIGNMENT_REQUIRED", "ROLE_ACTIVATION_SET_INVALID",
            "APP_ROLE_ACTIVATION_MUTEX_VIOLATION", "ACTIVE_ROLE_ROOT_LIMIT_EXCEEDED");

    private final RuntimeProjectionTargetRepository targets;
    private final ActivationTransaction activeRoles;
    private final RoleActivationFacade activation;
    private final ReselectionRepository reselection;
    private final RuntimePublicationRepository runtime;
    private final TenantAuthorizationStateRepository authorizationState;
    private final Clock clock;

    public Rbac3RuntimeProjectionRecovery(RuntimeProjectionTargetRepository targets,
                                         ActivationTransaction activeRoles,
                                         RoleActivationFacade activation,
                                         ReselectionRepository reselection,
                                         RuntimePublicationRepository runtime,
                                         TenantAuthorizationStateRepository authorizationState,
                                         Clock clock) {
        this.targets = Objects.requireNonNull(targets, "targets");
        this.activeRoles = Objects.requireNonNull(activeRoles, "activeRoles");
        this.activation = Objects.requireNonNull(activation, "activation");
        this.reselection = Objects.requireNonNull(reselection, "reselection");
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.authorizationState = Objects.requireNonNull(authorizationState, "authorizationState");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void project(MutationWorkDTO mutation) {
        Objects.requireNonNull(mutation, "mutation");
        if ("USER".equals(mutation.scopeType())) {
            targets.find(mutation.tenantId(), mutation.scopeId())
                    .ifPresent(target -> projectUser(mutation.tenantId(), target, mutation.mutationId()));
        } else if ("TENANT".equals(mutation.scopeType())) {
            projectTenant(mutation.tenantId(), mutation.mutationId());
        } else {
            throw new IllegalArgumentException("unsupported runtime projection scope: " + mutation.scopeType());
        }
    }

    @Override
    public void rebuild(EventEnvelopeVO event) {
        Objects.requireNonNull(event, "event");
        // Policy versions are tenant-wide; unaffected users also need a snapshot
        // at the new version. Keyset paging avoids loading the tenant into memory.
        projectTenant(event.tenantId(), event.eventId());
    }

    private void projectTenant(String tenantId, String repairId) {
        long afterUserId = 0L;
        RuntimeException failure = null;
        while (true) {
            List<ProjectionTarget> page = targets.page(tenantId, afterUserId, PAGE_SIZE);
            if (page.isEmpty()) {
                break;
            }
            for (ProjectionTarget target : page) {
                try {
                    projectUser(tenantId, target, repairId);
                } catch (RuntimeException exception) {
                    // One concurrently changing user must not starve the rest of the tenant.
                    if (failure == null) {
                        failure = exception;
                    }
                }
            }
            afterUserId = Long.parseLong(page.getLast().userId());
        }
        if (failure != null) {
            throw failure;
        }
    }

    private void projectUser(String tenantId, ProjectionTarget target, String repairId) {
        long policyVersion = authorizationState.require(Long.valueOf(tenantId)).getPolicyVersion();
        if (!target.active()) {
            runtime.invalidate(tenantId, target.identitySub(), target.userId(), target.authVersion(), policyVersion);
            return;
        }
        var current = activeRoles.current(tenantId, target.identitySub(), target.userId(), clock.instant());
        List<String> roots = current.rootsByApplication().values().stream()
                .flatMap(Set::stream).distinct().sorted().toList();
        if (roots.isEmpty()) {
            runtime.invalidate(tenantId, target.identitySub(), target.userId(), current.authVersion(), policyVersion);
            return;
        }
        try {
            activation.refresh(new ReplaceCommandDTO(tenantId, target.identitySub(), target.userId(),
                    roots, current.authVersion(), ACTOR, repairId + '-' + target.userId()));
        } catch (Rbac3RuleViolation exception) {
            if (!RESELECTION_REASONS.contains(exception.reasonCode())) {
                throw exception;
            }
            // An expired/revoked assignment or newly conflicting roots require an
            // explicit user choice, not silent activation of other eligible roles.
            reselection.requireReselection(tenantId, target.userId(), current.authVersion(), clock.instant(), ACTOR);
            runtime.invalidate(tenantId, target.identitySub(), target.userId(), current.authVersion() + 1L, policyVersion);
        }
    }
}
