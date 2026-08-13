package top.egon.cola.platform.rbac3.admin.runtime.service;

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
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.ApplicationFactVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.dto.ProjectionCommandDTO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.RuntimeSessionVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.SessionSnapshotProjectionVO;

/**
 * 类型 `SessionSnapshotProjector` 位于当前包内，是类型，用于承载 `Session Snapshot Projector` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `SessionSnapshotProjector` is a type in its package and carries the responsibility, state, or contract for `Session Snapshot Projector`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Maps the canonical Core activation result to the public runtime snapshot contract.
 */
public final class SessionSnapshotProjector {

    /**
     * 方法 `project` 按照 `SessionSnapshotProjector` 的职责处理输入，完成 `project` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `project` processes its inputs according to `SessionSnapshotProjector`'s responsibility, performs the `project` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `project` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `project`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public SessionSnapshotProjectionVO project(ProjectionCommandDTO command) {
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
        RuntimeSessionVO session = new RuntimeSessionVO(
                command.tenantId(),
                command.identitySub(),
                command.userId(),
                command.sessionId(),
                "ACTIVE",
                command.authVersion(),
                command.sessionVersion(),
                command.policyVersion(),
                command.expiresAt());
        return new SessionSnapshotProjectionVO(session, snapshot);
    }

    /**
     * 方法 `appContext` 按照 `SessionSnapshotProjector` 的职责处理输入，完成 `app Context` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `appContext` processes its inputs according to `SessionSnapshotProjector`'s responsibility, performs the `app Context` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `appContext` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `appContext`, then continue the business flow using its result, exception, or side effect.
     *
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param roots 输入参数 `roots`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private AppAuthorizationContext appContext(
            String applicationId,
            Set<String> roots,
            ProjectionCommandDTO command
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
        ApplicationFactVO application =
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

    /**
     * 方法 `dataScope` 按照 `SessionSnapshotProjector` 的职责处理输入，完成 `data Scope` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `dataScope` processes its inputs according to `SessionSnapshotProjector`'s responsibility, performs the `data Scope` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `dataScope` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `dataScope`, then continue the business flow using its result, exception, or side effect.
     *
     * @param permission 输入参数 `permission`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param scope 输入参数 `scope`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private DataScopeDecision dataScope(
            String permission,
            DataScopeMerger.NormalizedDataScope scope,
            ProjectionCommandDTO command
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

    /**
     * 方法 `fieldPolicies` 按照 `SessionSnapshotProjector` 的职责处理输入，完成 `field Policies` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `fieldPolicies` processes its inputs according to `SessionSnapshotProjector`'s responsibility, performs the `field Policies` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `fieldPolicies` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `fieldPolicies`, then continue the business flow using its result, exception, or side effect.
     *
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param permissions 输入参数 `permissions`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private Map<String, FieldPolicyDecision> fieldPolicies(
            String applicationId,
            Set<String> permissions,
            ProjectionCommandDTO command
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

    /**
     * 方法 `values` 按照 `SessionSnapshotProjector` 的职责处理输入，完成 `values` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `values` processes its inputs according to `SessionSnapshotProjector`'s responsibility, performs the `values` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `values` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `values`, then continue the business flow using its result, exception, or side effect.
     *
     * @param source 输入参数 `source`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param keys 输入参数 `keys`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private Set<String> values(Map<String, Set<String>> source, String... keys) {
        var result = new TreeSet<String>();
        for (String key : keys) {
            result.addAll(source.getOrDefault(key, Set.of()));
        }
        return result;
    }



    }
