package top.egon.cola.platform.rbac3.admin.snapshot.application;

import top.egon.cola.platform.rbac3.admin.activation.application.RoleActivationCandidateService;
import top.egon.cola.platform.rbac3.contract.authorization.AppAuthorizationContext;
import top.egon.cola.platform.rbac3.contract.authorization.DataScopeDecision;
import top.egon.cola.platform.rbac3.contract.authorization.Decision;
import top.egon.cola.platform.rbac3.contract.authorization.FieldPolicyDecision;
import top.egon.cola.platform.rbac3.contract.manifest.ManifestResource;
import top.egon.cola.platform.rbac3.contract.authorization.SessionAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.core.activation.AuthorizationRuleFacts;
import top.egon.cola.platform.rbac3.core.activation.RoleActivationResolution;
import top.egon.cola.platform.rbac3.core.decision.DataScopeMerger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Maps the canonical Core activation result to the public runtime snapshot contract.
 */
public final class SessionSnapshotProjector {

    public Projection project(ProjectionCommand command) {
        RoleActivationResolution resolution = command.resolution();
        var contexts = new ArrayList<AppAuthorizationContext>();
        resolution.activeRoleSet().rootsByApplication().forEach(
                (applicationId, roots) -> contexts.add(appContext(
                        applicationId, roots, command)));
        contexts.sort(java.util.Comparator.comparing(
                AppAuthorizationContext::applicationCode));
        SessionAuthorizationSnapshot snapshot = new SessionAuthorizationSnapshot(
                command.sessionId(),
                command.authVersion(),
                command.sessionVersion(),
                command.policyVersion(),
                contexts,
                resolution.snapshot().checksum(),
                command.generatedAt());
        RuntimeSession session = new RuntimeSession(
                command.tenantId(),
                command.userId(),
                command.sessionId(),
                "ACTIVE",
                command.authVersion(),
                command.sessionVersion(),
                command.policyVersion(),
                command.expiresAt());
        return new Projection(session, snapshot);
    }

    private AppAuthorizationContext appContext(
            String applicationId,
            Set<String> roots,
            ProjectionCommand command
    ) {
        var facts = command.facts();
        var snapshot = command.resolution().snapshot();
        var effectiveRoles = new TreeSet<String>();
        for (String roleId : snapshot.effectiveRoleIds()) {
            if (applicationId.equals(facts.hierarchy().requireNode(roleId).applicationId())) {
                effectiveRoles.add(roleId);
            }
        }
        var permissions = new TreeSet<String>();
        for (AuthorizationRuleFacts.PermissionBinding binding
                : facts.authorizationFacts().permissionBindings()) {
            if (effectiveRoles.contains(binding.roleId())) {
                permissions.add(binding.permissionCode());
            }
        }
        var assignmentIds = new TreeSet<String>();
        for (var assignment : facts.assignments()) {
            if (effectiveRoles.contains(assignment.roleId())) {
                assignmentIds.add(assignment.id());
            }
        }
        Map<String, DataScopeDecision> scopes = new TreeMap<>();
        snapshot.dataScopes().forEach((permission, scope) -> {
            if (permissions.contains(permission)) {
                scopes.put(permission, dataScope(permission, scope, command));
            }
        });
        Map<String, FieldPolicyDecision> fieldPolicies = fieldPolicies(
                applicationId, permissions, command);
        List<ManifestResource> resources = facts.authorizationFacts().resources().stream()
                .filter(resource -> permissions.contains(resource.requiredPermissionCode()))
                .filter(resource -> snapshot.resourceCodes().contains(resource.code()))
                .map(resource -> new ManifestResource(
                        resource.code(), null, resource.code(), null,
                        null, null, resource.requiredPermissionCode(), null,
                        null, null, null, null, null, null, null, Map.of()))
                .toList();
        RoleActivationCandidateService.ApplicationFact application =
                facts.applications().get(applicationId);
        if (application == null) {
            throw new IllegalArgumentException("missing application fact: " + applicationId);
        }
        String landingRouteCode = effectiveRoles.stream()
                .map(roleId -> facts.hierarchy().requireNode(roleId).landingRouteCode())
                .filter(routeCode -> routeCode != null
                        && routeCode.equals(snapshot.landingRouteCode()))
                .findFirst()
                .orElse(null);
        return new AppAuthorizationContext(
                applicationId,
                application.code(),
                new ArrayList<>(new TreeSet<>(roots)),
                new ArrayList<>(assignmentIds),
                new ArrayList<>(effectiveRoles),
                permissions,
                scopes,
                fieldPolicies,
                resources,
                landingRouteCode);
    }

    private DataScopeDecision dataScope(
            String permission,
            DataScopeMerger.NormalizedDataScope scope,
            ProjectionCommand command
    ) {
        Map<String, Set<String>> references = scope.referencesByDimension();
        Set<String> orgs = values(references, "ORG", "ORG_TREE");
        Set<String> departments = values(references, "DEPT", "DEPT_TREE");
        Set<String> users = values(references, "USER");
        boolean includeSelf = users.contains(command.userId());
        boolean concrete = scope.allInTenant() || !orgs.isEmpty()
                || !departments.isEmpty() || !users.isEmpty() || includeSelf;
        String scopeType = !concrete ? "NONE" : scope.allInTenant() ? "ALL"
                : includeSelf && references.size() == 1 ? "SELF" : "CUSTOM";
        return new DataScopeDecision(
                concrete ? Decision.ALLOW : Decision.DENY,
                concrete ? "ALLOW" : "DATA_SCOPE_MISSING",
                command.tenantId(),
                command.userId(),
                permission,
                scopeType,
                scope.allInTenant(),
                orgs,
                references.containsKey("ORG_TREE"),
                departments,
                references.containsKey("DEPT_TREE"),
                users,
                includeSelf,
                includeSelf ? command.userId() : null,
                Long.toString(scope.directorySnapshotVersion()),
                command.policyVersion(),
                command.authVersion(),
                command.sessionVersion(),
                command.policyVersion(),
                List.of(),
                command.generatedAt());
    }

    private Map<String, FieldPolicyDecision> fieldPolicies(
            String applicationId,
            Set<String> permissions,
            ProjectionCommand command
    ) {
        var fieldsByResource = new TreeMap<String, Map<String, FieldPolicyDecision.FieldAccess>>();
        command.resolution().snapshot().fieldPolicies().forEach((key, level) -> {
            int separator = key.indexOf('#');
            if (separator > 0) {
                fieldsByResource.computeIfAbsent(
                        key.substring(0, separator), ignored -> new TreeMap<>()).put(
                        key.substring(separator + 1),
                        new FieldPolicyDecision.FieldAccess(level, null));
            }
        });
        var result = new TreeMap<String, FieldPolicyDecision>();
        String applicationCode = command.facts().applications().get(applicationId).code();
        fieldsByResource.forEach((resource, fields) -> permissions.forEach(permission -> {
            String key = permission + ':' + applicationCode + ':' + resource;
            result.put(key, new FieldPolicyDecision(
                    Decision.ALLOW,
                    "ALLOW",
                    command.tenantId(),
                    command.userId(),
                    permission,
                    applicationCode,
                    resource,
                    fields,
                    command.authVersion(),
                    command.sessionVersion(),
                    command.policyVersion(),
                    List.of(),
                    command.generatedAt()));
        }));
        return result;
    }

    private Set<String> values(Map<String, Set<String>> source, String... keys) {
        var result = new TreeSet<String>();
        for (String key : keys) {
            result.addAll(source.getOrDefault(key, Set.of()));
        }
        return result;
    }

    public record ProjectionCommand(
            String tenantId,
            String userId,
            String sessionId,
            long authVersion,
            long sessionVersion,
            long policyVersion,
            Instant expiresAt,
            RoleActivationResolution resolution,
            RoleActivationCandidateService.ActivationFacts facts,
            Instant generatedAt
    ) {
    }

    public record RuntimeSession(
            String tenantId,
            String userId,
            String sessionId,
            String status,
            long authVersion,
            long sessionVersion,
            long policyVersion,
            Instant expiresAt
    ) {
    }

    public record Projection(
            RuntimeSession session,
            SessionAuthorizationSnapshot snapshot
    ) {
    }
}
