package top.egon.cola.platform.rbac3.admin.snapshot.application;

import top.egon.cola.platform.rbac3.admin.activation.service.RoleActivationCandidateService;
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
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.ActivationFactsVO;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.ApplicationFactVO;

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
                command.identitySub(),
                command.userId(),
                command.sessionId(),
                "ACTIVE",
                command.authVersion(),
                command.sessionVersion(),
                command.policyVersion(),
                command.expiresAt());
        return new Projection(session, snapshot);
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

    /**
     * 类型 `ProjectionCommand` 位于 `SessionSnapshotProjector` 内，是记录类型，用于承载 `Projection Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ProjectionCommand` is a record inside `SessionSnapshotProjector` and carries the responsibility, state, or contract for `Projection Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ProjectionCommand` 作为 `SessionSnapshotProjector` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ProjectionCommand` as the responsibility boundary of `SessionSnapshotProjector`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param identitySub 记录组件 `identitySub` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `identitySub` carries constructor data whose meaning is defined by the record contract.
     * @param userId 记录组件 `userId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `userId` carries constructor data whose meaning is defined by the record contract.
     * @param sessionId 记录组件 `sessionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionId` carries constructor data whose meaning is defined by the record contract.
     * @param authVersion 记录组件 `authVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authVersion` carries constructor data whose meaning is defined by the record contract.
     * @param sessionVersion 记录组件 `sessionVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionVersion` carries constructor data whose meaning is defined by the record contract.
     * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
     * @param expiresAt 记录组件 `expiresAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expiresAt` carries constructor data whose meaning is defined by the record contract.
     * @param resolution 记录组件 `resolution` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `resolution` carries constructor data whose meaning is defined by the record contract.
     * @param facts 记录组件 `facts` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `facts` carries constructor data whose meaning is defined by the record contract.
     * @param generatedAt 记录组件 `generatedAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `generatedAt` carries constructor data whose meaning is defined by the record contract.
     */
    public record ProjectionCommand(
            /**
             * 字段 `tenantId` 表示 `ProjectionCommand` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `ProjectionCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `ProjectionCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `ProjectionCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `identitySub` 表示 `ProjectionCommand` 中与 `identity Sub` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `identitySub` stores the `identity Sub`-related state, dependency, configuration, or result of `ProjectionCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `identitySub` 时应保持 `ProjectionCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `identitySub`, preserve `ProjectionCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String identitySub,
            /**
             * 字段 `userId` 表示 `ProjectionCommand` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `ProjectionCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userId` 时应保持 `ProjectionCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userId`, preserve `ProjectionCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String userId,
            /**
             * 字段 `sessionId` 表示 `ProjectionCommand` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `ProjectionCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `ProjectionCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `ProjectionCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sessionId,
            /**
             * 字段 `authVersion` 表示 `ProjectionCommand` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `ProjectionCommand` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `ProjectionCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `ProjectionCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            long authVersion,
            /**
             * 字段 `sessionVersion` 表示 `ProjectionCommand` 中与 `session Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionVersion` stores the `session Version`-related state, dependency, configuration, or result of `ProjectionCommand` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionVersion` 时应保持 `ProjectionCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionVersion`, preserve `ProjectionCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            long sessionVersion,
            /**
             * 字段 `policyVersion` 表示 `ProjectionCommand` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `ProjectionCommand` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `ProjectionCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `ProjectionCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            long policyVersion,
            /**
             * 字段 `expiresAt` 表示 `ProjectionCommand` 中与 `expires At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expiresAt` stores the `expires At`-related state, dependency, configuration, or result of `ProjectionCommand` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expiresAt` 时应保持 `ProjectionCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expiresAt`, preserve `ProjectionCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant expiresAt,
            /**
             * 字段 `resolution` 表示 `ProjectionCommand` 中与 `resolution` 相关的状态、依赖、配置或结果（声明类型 `RoleActivationResolution`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `resolution` stores the `resolution`-related state, dependency, configuration, or result of `ProjectionCommand` (declared type `RoleActivationResolution`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `resolution` 时应保持 `ProjectionCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `resolution`, preserve `ProjectionCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            RoleActivationResolution resolution,
            /**
             * 字段 `facts` 表示 `ProjectionCommand` 中与 `facts` 相关的状态、依赖、配置或结果（声明类型 `ActivationFactsVO`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `facts` stores the `facts`-related state, dependency, configuration, or result of `ProjectionCommand` (declared type `ActivationFactsVO`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `facts` 时应保持 `ProjectionCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `facts`, preserve `ProjectionCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            ActivationFactsVO facts,
            /**
             * 字段 `generatedAt` 表示 `ProjectionCommand` 中与 `generated At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `generatedAt` stores the `generated At`-related state, dependency, configuration, or result of `ProjectionCommand` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `generatedAt` 时应保持 `ProjectionCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `generatedAt`, preserve `ProjectionCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant generatedAt
    ) {
    }

    /**
     * 类型 `RuntimeSession` 位于 `SessionSnapshotProjector` 内，是记录类型，用于承载 `Runtime Session` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RuntimeSession` is a record inside `SessionSnapshotProjector` and carries the responsibility, state, or contract for `Runtime Session`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RuntimeSession` 作为 `SessionSnapshotProjector` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RuntimeSession` as the responsibility boundary of `SessionSnapshotProjector`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param identitySub 记录组件 `identitySub` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `identitySub` carries constructor data whose meaning is defined by the record contract.
     * @param userId 记录组件 `userId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `userId` carries constructor data whose meaning is defined by the record contract.
     * @param sessionId 记录组件 `sessionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionId` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param authVersion 记录组件 `authVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authVersion` carries constructor data whose meaning is defined by the record contract.
     * @param sessionVersion 记录组件 `sessionVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionVersion` carries constructor data whose meaning is defined by the record contract.
     * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
     * @param expiresAt 记录组件 `expiresAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expiresAt` carries constructor data whose meaning is defined by the record contract.
     */
    public record RuntimeSession(
            /**
             * 字段 `tenantId` 表示 `RuntimeSession` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `RuntimeSession` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `RuntimeSession` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `RuntimeSession`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `identitySub` 表示 `RuntimeSession` 中与 `identity Sub` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `identitySub` stores the `identity Sub`-related state, dependency, configuration, or result of `RuntimeSession` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `identitySub` 时应保持 `RuntimeSession` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `identitySub`, preserve `RuntimeSession`'s lifecycle, immutability, and thread-safety constraints.
             */
            String identitySub,
            /**
             * 字段 `userId` 表示 `RuntimeSession` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `RuntimeSession` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userId` 时应保持 `RuntimeSession` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userId`, preserve `RuntimeSession`'s lifecycle, immutability, and thread-safety constraints.
             */
            String userId,
            /**
             * 字段 `sessionId` 表示 `RuntimeSession` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `RuntimeSession` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `RuntimeSession` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `RuntimeSession`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sessionId,
            /**
             * 字段 `status` 表示 `RuntimeSession` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `RuntimeSession` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `RuntimeSession` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `RuntimeSession`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status,
            /**
             * 字段 `authVersion` 表示 `RuntimeSession` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `RuntimeSession` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `RuntimeSession` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `RuntimeSession`'s lifecycle, immutability, and thread-safety constraints.
             */
            long authVersion,
            /**
             * 字段 `sessionVersion` 表示 `RuntimeSession` 中与 `session Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionVersion` stores the `session Version`-related state, dependency, configuration, or result of `RuntimeSession` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionVersion` 时应保持 `RuntimeSession` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionVersion`, preserve `RuntimeSession`'s lifecycle, immutability, and thread-safety constraints.
             */
            long sessionVersion,
            /**
             * 字段 `policyVersion` 表示 `RuntimeSession` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `RuntimeSession` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `RuntimeSession` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `RuntimeSession`'s lifecycle, immutability, and thread-safety constraints.
             */
            long policyVersion,
            /**
             * 字段 `expiresAt` 表示 `RuntimeSession` 中与 `expires At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expiresAt` stores the `expires At`-related state, dependency, configuration, or result of `RuntimeSession` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expiresAt` 时应保持 `RuntimeSession` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expiresAt`, preserve `RuntimeSession`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant expiresAt
    ) {
    }

    /**
     * 类型 `Projection` 位于 `SessionSnapshotProjector` 内，是记录类型，用于承载 `Projection` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Projection` is a record inside `SessionSnapshotProjector` and carries the responsibility, state, or contract for `Projection`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Projection` 作为 `SessionSnapshotProjector` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Projection` as the responsibility boundary of `SessionSnapshotProjector`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param session 记录组件 `session` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `session` carries constructor data whose meaning is defined by the record contract.
     * @param snapshot 记录组件 `snapshot` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `snapshot` carries constructor data whose meaning is defined by the record contract.
     */
    public record Projection(
            /**
             * 字段 `session` 表示 `Projection` 中与 `session` 相关的状态、依赖、配置或结果（声明类型 `RuntimeSession`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `session` stores the `session`-related state, dependency, configuration, or result of `Projection` (declared type `RuntimeSession`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `session` 时应保持 `Projection` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `session`, preserve `Projection`'s lifecycle, immutability, and thread-safety constraints.
             */
            RuntimeSession session,
            /**
             * 字段 `snapshot` 表示 `Projection` 中与 `snapshot` 相关的状态、依赖、配置或结果（声明类型 `SessionAuthorizationSnapshot`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `snapshot` stores the `snapshot`-related state, dependency, configuration, or result of `Projection` (declared type `SessionAuthorizationSnapshot`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `snapshot` 时应保持 `Projection` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `snapshot`, preserve `Projection`'s lifecycle, immutability, and thread-safety constraints.
             */
            SessionAuthorizationSnapshot snapshot
    ) {
    }
}
