package top.egon.cola.platform.rbac3.admin.simulation.application;

import top.egon.cola.platform.rbac3.admin.application.port.AuditPort;
import top.egon.cola.platform.rbac3.admin.authorization.application.AuthorizationDecisionService;
import top.egon.cola.platform.rbac3.admin.role.application.RoleFacade;
import top.egon.cola.platform.rbac3.admin.security.CurrentRbac3Principal;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Read-only authorization what-if analysis over one consistent snapshot.
 */
public final class AuthorizationSimulationService {

    private static final long RESULT_TTL_SECONDS = 300;

    private final AuthorizationDecisionService decisionService;
    private final RoleImpactSource roleImpactSource;
    private final AuditPort auditPort;
    private final Clock clock;

    public AuthorizationSimulationService(
            AuthorizationDecisionService decisionService,
            RoleImpactSource roleImpactSource,
            AuditPort auditPort,
            Clock clock) {
        this.decisionService = Objects.requireNonNull(decisionService, "decisionService");
        this.roleImpactSource = Objects.requireNonNull(roleImpactSource, "roleImpactSource");
        this.auditPort = Objects.requireNonNull(auditPort, "auditPort");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public SimulationResult simulate(
            CurrentRbac3Principal caller,
            SimulationRequest request) {
        Objects.requireNonNull(caller, "caller");
        Objects.requireNonNull(request, "request");
        AuthorizationDecisionService.SnapshotRecord snapshot =
                decisionService.consistentSnapshot(caller, request.decisionRequest());
        var current = decisionService.evaluateConsistentSnapshot(
                snapshot, request.decisionRequest(), Set.of(), Set.of());
        var hypothetical = decisionService.evaluateConsistentSnapshot(
                snapshot, request.decisionRequest(),
                request.hypothesis().addedPermissions(),
                request.hypothesis().removedPermissions());
        Instant expiresAt = clock.instant().plusSeconds(RESULT_TTL_SECONDS);
        auditPort.append(new AuditPort.AuditEvent(
                caller.tenantId(), "AUTHORIZATION_SIMULATED", caller.userId(),
                "SESSION", request.decisionRequest().subject().sessionId(),
                request.requestId(), request.traceId(),
                Map.of(
                        "applicationCode",
                        request.decisionRequest().resource().applicationCode(),
                        "permissionCode", request.decisionRequest().permissionCode(),
                        "snapshotChecksum", snapshot.snapshot().checksum()),
                clock.instant()));
        return new SimulationResult(
                current, hypothetical, snapshot.snapshot().authVersion(),
                snapshot.snapshot().sessionVersion(), snapshot.snapshot().policyVersion(),
                snapshot.snapshot().checksum(), expiresAt);
    }

    public RoleChangeImpactResult simulateRoleChangeImpact(
            CurrentRbac3Principal caller,
            RoleChangeImpactRequest request) {
        Objects.requireNonNull(caller, "caller");
        Objects.requireNonNull(request, "request");
        RoleImpactSnapshot snapshot = roleImpactSource.load(
                caller.tenantId(), request.roleId());
        Instant now = clock.instant();
        auditPort.append(new AuditPort.AuditEvent(
                caller.tenantId(), "ROLE_CHANGE_IMPACT_SIMULATED", caller.userId(),
                "ROLE", request.roleId(), request.requestId(), request.traceId(),
                Map.of(
                        "policyVersion", Long.toString(snapshot.policyVersion()),
                        "evidenceChecksum", snapshot.evidenceChecksum()),
                now));
        return new RoleChangeImpactResult(
                snapshot.impact(), snapshot.policyVersion(),
                snapshot.evidenceChecksum(), now.plusSeconds(RESULT_TTL_SECONDS));
    }

    public record SimulationRequest(
            AuthorizationDecisionService.DecisionRequest decisionRequest,
            Hypothesis hypothesis,
            Instant at,
            String requestId,
            String traceId) {
        public SimulationRequest {
            decisionRequest = Objects.requireNonNull(decisionRequest, "decisionRequest");
            hypothesis = Objects.requireNonNull(hypothesis, "hypothesis");
            at = Objects.requireNonNull(at, "at");
            requestId = required(requestId, "requestId");
            traceId = required(traceId, "traceId");
        }
    }

    public record Hypothesis(
            Set<String> addedPermissions,
            Set<String> removedPermissions) {
        public Hypothesis {
            addedPermissions = Set.copyOf(addedPermissions);
            removedPermissions = Set.copyOf(removedPermissions);
        }
    }

    public record SimulationResult(
            AuthorizationDecisionService.DecisionBundle current,
            AuthorizationDecisionService.DecisionBundle hypothetical,
            long authVersion,
            long sessionVersion,
            long policyVersion,
            String snapshotChecksum,
            Instant expiresAt) {
    }

    public record RoleChangeImpactRequest(
            String roleId,
            Instant at,
            String requestId,
            String traceId) {
        public RoleChangeImpactRequest {
            roleId = required(roleId, "roleId");
            at = Objects.requireNonNull(at, "at");
            requestId = required(requestId, "requestId");
            traceId = required(traceId, "traceId");
        }
    }

    public record RoleImpactSnapshot(
            RoleFacade.RoleImpactView impact,
            long policyVersion,
            String evidenceChecksum) {
        public RoleImpactSnapshot {
            impact = Objects.requireNonNull(impact, "impact");
            if (policyVersion < 0) {
                throw new IllegalArgumentException("policyVersion must not be negative");
            }
            evidenceChecksum = required(evidenceChecksum, "evidenceChecksum");
        }
    }

    public record RoleChangeImpactResult(
            RoleFacade.RoleImpactView impact,
            long policyVersion,
            String evidenceChecksum,
            Instant expiresAt) {
    }

    @FunctionalInterface
    public interface RoleImpactSource {
        RoleImpactSnapshot load(String tenantId, String roleId);
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
