package top.egon.cola.platform.rbac3.admin.authorization.application;

import top.egon.cola.platform.rbac3.admin.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.security.CurrentRbac3ServicePrincipal;
import top.egon.cola.platform.rbac3.contract.authorization.AppAuthorizationContext;
import top.egon.cola.platform.rbac3.contract.authorization.AuthorizationDecision;
import top.egon.cola.platform.rbac3.contract.authorization.DataScopeDecision;
import top.egon.cola.platform.rbac3.contract.authorization.Decision;
import top.egon.cola.platform.rbac3.contract.authorization.FieldPolicyDecision;
import top.egon.cola.platform.rbac3.contract.authorization.SessionAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Remote decision facade over one immutable session authorization snapshot.
 */
public final class AuthorizationDecisionService {

    private final SnapshotSource snapshotSource;
    private final FenceVerifier fenceVerifier;
    private final Clock clock;

    public AuthorizationDecisionService(
            SnapshotSource snapshotSource,
            FenceVerifier fenceVerifier,
            Clock clock) {
        this.snapshotSource = Objects.requireNonNull(snapshotSource, "snapshotSource");
        this.fenceVerifier = Objects.requireNonNull(fenceVerifier, "fenceVerifier");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public DecisionBundle decide(
            CurrentRbac3ServicePrincipal caller,
            DecisionRequest request) {
        requireServiceTenant(caller, request.subject().tenantId());
        requireApplication(caller, request.resource().applicationCode());
        requireUnfenced(request.subject().tenantId(), request.subject().sessionId());
        SnapshotRecord snapshot = load(request.subject());
        return evaluateConsistentSnapshot(snapshot, request, Set.of(), Set.of());
    }

    public SessionAuthorizationSnapshot snapshot(
            CurrentRbac3ServicePrincipal caller,
            String tenantId,
            String sessionId) {
        requireServiceTenant(caller, tenantId);
        requireUnfenced(tenantId, sessionId);
        return boundSnapshot(caller, tenantId, sessionId);
    }

    private SessionAuthorizationSnapshot boundSnapshot(
            CurrentRbac3ServicePrincipal caller,
            String tenantId,
            String sessionId) {
        SnapshotRecord record = snapshotSource.load(tenantId, required(sessionId, "sessionId"));
        if (!record.tenantId().equals(tenantId)
                || !record.snapshot().sessionId().equals(sessionId)) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        AppAuthorizationContext application = application(
                record.snapshot(), caller.applicationCode());
        return new SessionAuthorizationSnapshot(
                record.snapshot().sessionId(), record.snapshot().authVersion(),
                record.snapshot().sessionVersion(), record.snapshot().policyVersion(),
                List.of(application), record.snapshot().checksum(),
                record.snapshot().generatedAt());
    }

    public FenceVerification verifyFence(
            CurrentRbac3ServicePrincipal caller,
            String tenantId,
            String sessionId) {
        requireServiceTenant(caller, tenantId);
        boundSnapshot(caller, tenantId, sessionId);
        boolean fenced = fenceVerifier.isFenced(tenantId, sessionId);
        return new FenceVerification(
                fenced ? Decision.DENY : Decision.ALLOW,
                fenced ? "AUTH_PROPAGATION_PENDING" : "ALLOW",
                sessionId,
                clock.instant());
    }

    private void requireUnfenced(String tenantId, String sessionId) {
        if (fenceVerifier.isFenced(
                required(tenantId, "tenantId"), required(sessionId, "sessionId"))) {
            throw new Rbac3RuleViolation("AUTH_PROPAGATION_PENDING");
        }
    }

    public SnapshotRecord consistentSnapshot(
            CurrentRbac3Principal caller,
            DecisionRequest request) {
        Objects.requireNonNull(caller, "caller");
        if (!caller.tenantId().equals(request.subject().tenantId())) {
            throw new Rbac3RuleViolation("PERMISSION_DENIED");
        }
        return load(request.subject());
    }

    public DecisionBundle evaluateConsistentSnapshot(
            SnapshotRecord record,
            DecisionRequest request,
            Set<String> addedPermissions,
            Set<String> removedPermissions) {
        Objects.requireNonNull(record, "record");
        Objects.requireNonNull(request, "request");
        validateSubject(record, request.subject());
        validateVersions(record.snapshot(), request.tokenVersions());
        AppAuthorizationContext application = application(
                record.snapshot(), request.resource().applicationCode());
        var permissions = new LinkedHashSet<>(application.permissions());
        permissions.addAll(Objects.requireNonNull(addedPermissions, "addedPermissions"));
        permissions.removeAll(Objects.requireNonNull(removedPermissions, "removedPermissions"));
        boolean allowed = permissions.contains(request.permissionCode());
        Instant now = clock.instant();
        AuthorizationDecision function = new AuthorizationDecision(
                allowed ? Decision.ALLOW : Decision.DENY,
                allowed ? "ALLOW" : "PERMISSION_DENIED",
                record.tenantId(), record.userId(), request.permissionCode(),
                record.snapshot().authVersion(), record.snapshot().sessionVersion(),
                record.snapshot().policyVersion(), application.effectiveRoleIds(), now);
        if (!allowed) {
            return new DecisionBundle(function, null, null, record.snapshot().checksum());
        }
        DataScopeDecision dataScope = request.requestedDecisions().contains(
                DecisionType.DATA_SCOPE)
                ? application.dataScopes().getOrDefault(
                        request.permissionCode(), missingDataScope(record, request, now))
                : null;
        String fieldKey = request.permissionCode() + ':'
                + request.resource().applicationCode() + ':'
                + request.resource().resourceCode();
        FieldPolicyDecision field = request.requestedDecisions().contains(DecisionType.FIELD)
                ? application.fieldPolicies().getOrDefault(
                        fieldKey, missingFieldPolicy(record, request, now))
                : null;
        return new DecisionBundle(function, dataScope, field, record.snapshot().checksum());
    }

    private SnapshotRecord load(Subject subject) {
        SnapshotRecord record = snapshotSource.load(subject.tenantId(), subject.sessionId());
        validateSubject(record, subject);
        return record;
    }

    private void validateSubject(SnapshotRecord record, Subject subject) {
        if (!record.tenantId().equals(subject.tenantId())
                || !record.userId().equals(subject.userId())
                || !record.snapshot().sessionId().equals(subject.sessionId())) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
    }

    private void validateVersions(
            SessionAuthorizationSnapshot snapshot,
            TokenVersions versions) {
        if (snapshot.authVersion() != versions.authVersion()) {
            throw new Rbac3RuleViolation("AUTH_VERSION_MISMATCH");
        }
        if (snapshot.sessionVersion() != versions.sessionVersion()) {
            throw new Rbac3RuleViolation("SESSION_VERSION_MISMATCH");
        }
        if (snapshot.policyVersion() != versions.policyVersion()) {
            throw new Rbac3RuleViolation("POLICY_VERSION_MISMATCH");
        }
    }

    private AppAuthorizationContext application(
            SessionAuthorizationSnapshot snapshot,
            String applicationCode) {
        return snapshot.appContexts().stream()
                .filter(context -> context.applicationCode().equals(applicationCode))
                .findFirst()
                .orElseThrow(() -> new Rbac3RuleViolation("APPLICATION_BINDING_DENIED"));
    }

    private DataScopeDecision missingDataScope(
            SnapshotRecord record,
            DecisionRequest request,
            Instant now) {
        return new DataScopeDecision(
                Decision.DENY, "DATA_SCOPE_DENIED", record.tenantId(), record.userId(),
                request.permissionCode(), "NONE", false, Set.of(), false,
                Set.of(), false, Set.of(), false, null, "UNKNOWN",
                record.snapshot().policyVersion(), record.snapshot().authVersion(),
                record.snapshot().sessionVersion(), record.snapshot().policyVersion(),
                List.of(), now);
    }

    private FieldPolicyDecision missingFieldPolicy(
            SnapshotRecord record,
            DecisionRequest request,
            Instant now) {
        return new FieldPolicyDecision(
                Decision.DENY, "FIELD_ACCESS_DENIED", record.tenantId(), record.userId(),
                request.permissionCode(), request.resource().applicationCode(),
                request.resource().resourceCode(), Map.of(),
                record.snapshot().authVersion(), record.snapshot().sessionVersion(),
                record.snapshot().policyVersion(), List.of(), now);
    }

    private void requireServiceTenant(
            CurrentRbac3ServicePrincipal caller,
            String tenantId) {
        Objects.requireNonNull(caller, "caller");
        if (!caller.tenantId().equals(required(tenantId, "tenantId"))) {
            throw new Rbac3RuleViolation("SERVICE_IDENTITY_DENIED");
        }
    }

    private void requireApplication(
            CurrentRbac3ServicePrincipal caller,
            String applicationCode) {
        if (!caller.applicationCode().equals(required(applicationCode, "applicationCode"))) {
            throw new Rbac3RuleViolation("APPLICATION_BINDING_DENIED");
        }
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    @FunctionalInterface
    public interface SnapshotSource {
        SnapshotRecord load(String tenantId, String sessionId);
    }

    @FunctionalInterface
    public interface FenceVerifier {
        boolean isFenced(String tenantId, String sessionId);
    }

    public record SnapshotRecord(
            String tenantId,
            String userId,
            SessionAuthorizationSnapshot snapshot) {
        public SnapshotRecord {
            tenantId = required(tenantId, "tenantId");
            userId = required(userId, "userId");
            snapshot = Objects.requireNonNull(snapshot, "snapshot");
        }
    }

    public record Subject(String tenantId, String userId, String sessionId) {
        public Subject {
            tenantId = required(tenantId, "tenantId");
            userId = required(userId, "userId");
            sessionId = required(sessionId, "sessionId");
        }
    }

    public record Resource(String applicationCode, String resourceCode) {
        public Resource {
            applicationCode = required(applicationCode, "applicationCode");
            resourceCode = required(resourceCode, "resourceCode");
        }
    }

    public record TokenVersions(long authVersion, long sessionVersion, long policyVersion) {
        public TokenVersions {
            if (authVersion < 0 || sessionVersion < 0 || policyVersion < 0) {
                throw new IllegalArgumentException("token versions must not be negative");
            }
        }
    }

    public record DecisionRequest(
            Subject subject,
            String permissionCode,
            Resource resource,
            Set<DecisionType> requestedDecisions,
            TokenVersions tokenVersions) {
        public DecisionRequest {
            subject = Objects.requireNonNull(subject, "subject");
            permissionCode = required(permissionCode, "permissionCode");
            resource = Objects.requireNonNull(resource, "resource");
            requestedDecisions = Set.copyOf(Objects.requireNonNull(
                    requestedDecisions, "requestedDecisions"));
            if (requestedDecisions.isEmpty()) {
                throw new IllegalArgumentException("requestedDecisions must not be empty");
            }
            tokenVersions = Objects.requireNonNull(tokenVersions, "tokenVersions");
        }
    }

    public record DecisionBundle(
            AuthorizationDecision functionDecision,
            DataScopeDecision dataScopeDecision,
            FieldPolicyDecision fieldPolicyDecision,
            String snapshotChecksum) {
    }

    public record FenceVerification(
            Decision decision,
            String reasonCode,
            String sessionId,
            Instant verifiedAt) {
    }

    public enum DecisionType {
        FUNCTION,
        DATA_SCOPE,
        FIELD,
        PARTICIPATION,
        FENCE
    }
}
