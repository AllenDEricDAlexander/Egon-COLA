package top.egon.cola.platform.rbac3.admin.authorization.application;

import top.egon.cola.platform.rbac3.admin.security.CurrentRbac3Principal;
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;
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
 * 基于单个不可变会话授权快照提供远程授权判定门面。
 * Remote authorization-decision facade over one immutable session authorization snapshot.
 * 语义与用法：将 `AuthorizationDecisionService` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `AuthorizationDecisionService` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
public final class AuthorizationDecisionService {

    /** 授权快照来源。 / Authorization snapshot source.
     * 含义与用法：读取、传递或更新 `snapshotSource` 时应保持 `AuthorizationDecisionService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `snapshotSource`, preserve `AuthorizationDecisionService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final SnapshotSource snapshotSource;
    /** 会话传播 Fence 校验器。 / Session propagation-fence verifier.
     * 含义与用法：读取、传递或更新 `fenceVerifier` 时应保持 `AuthorizationDecisionService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `fenceVerifier`, preserve `AuthorizationDecisionService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final FenceVerifier fenceVerifier;
    /** 产生可测试审计时间的时钟。 / Clock used for testable audit timestamps.
     * 含义与用法：读取、传递或更新 `clock` 时应保持 `AuthorizationDecisionService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `clock`, preserve `AuthorizationDecisionService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Clock clock;

    /**
     * 创建授权判定服务。
     * Creates the authorization-decision service.
     *
     * @param snapshotSource 授权快照来源 / authorization snapshot source
     * @param fenceVerifier 会话 Fence 校验器 / session-fence verifier
     * @param clock 审计时钟 / audit clock
     * 用法：通过 `AuthorizationDecisionService` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `AuthorizationDecisionService`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    public AuthorizationDecisionService(
            SnapshotSource snapshotSource,
            FenceVerifier fenceVerifier,
            Clock clock) {
        this.snapshotSource = Objects.requireNonNull(snapshotSource, "snapshotSource");
        this.fenceVerifier = Objects.requireNonNull(fenceVerifier, "fenceVerifier");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * 使用调用服务绑定的租户和应用执行完整的类型化授权判定。
     * Executes a complete typed authorization decision for the caller-bound tenant and application.
     *
     * @param caller 已认证调用服务 / authenticated calling service
     * @param request 类型化判定请求 / typed decision request
     * @return 函数、数据和字段判定组合 / function, data, and field decision bundle
     */
    public DecisionBundle decide(
            ServiceIdentityPrincipal caller,
            DecisionRequest request) {
        requireServiceTenant(caller, request.subject().tenantId());
        requireApplication(caller, request.resource().applicationCode());
        requireUnfenced(request.subject().tenantId(), request.subject().sessionId());
        SnapshotRecord snapshot = load(request.subject());
        return evaluateConsistentSnapshot(snapshot, request, Set.of(), Set.of());
    }

    /**
     * 判定 IdP 用户是否具备进入目标 Resource Server 应用的入口权限。
     * Decides whether an IdP user has the entry permission for a target Resource Server application.
     *
     * <p>该接口只返回最小判定和授权版本，不返回角色、权限集合、数据范围或字段策略。
     * This API returns only the minimal decision and authorization versions; roles, permission
     * sets, data scopes, and field policies are never exposed.</p>
     *
     * @param caller 已认证的调用服务 / authenticated calling service
     * @param request 用户资源入口判定请求 / user resource-entry decision request
     * @return 最小资源入口判定 / minimal resource-entry decision
     * 用法：调用 `decideResourceAccess` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `decideResourceAccess`, then continue the business flow using its result, exception, or side effect.
     */
    public ResourceAccessDecision decideResourceAccess(
            ServiceIdentityPrincipal caller,
            ResourceAccessRequest request) {
        Objects.requireNonNull(request, "request");
        requireResourceDecisionTenant(caller, request.tenantId());
        if (fenceVerifier.isFenced(request.tenantId(), request.sessionId())) {
            return resourceAccessDeny("AUTH_PROPAGATION_PENDING", null);
        }
        SnapshotRecord record;
        try {
            record = snapshotSource.load(request.tenantId(), request.sessionId());
        } catch (Rbac3RuleViolation violation) {
            if (isInactiveIdentitySession(violation.reasonCode())) {
                return resourceAccessDeny("IDENTITY_SESSION_INACTIVE", null);
            }
            if ("AUTH_PROPAGATION_PENDING".equals(violation.reasonCode())) {
                return resourceAccessDeny("AUTH_PROPAGATION_PENDING", null);
            }
            throw violation;
        }
        if (!record.tenantId().equals(request.tenantId())
                || !record.identitySub().equals(request.identitySub())
                || !record.snapshot().sessionId().equals(request.sessionId())) {
            return resourceAccessDeny("IDENTITY_SESSION_INACTIVE", null);
        }
        AppAuthorizationContext application = record.snapshot().appContexts().stream()
                .filter(context -> context.applicationCode().equals(
                        request.rbacApplicationCode()))
                .findFirst()
                .orElse(null);
        if (application == null) {
            return resourceAccessDeny("APPLICATION_BINDING_DENIED", record.snapshot());
        }
        if (!application.permissions().contains(request.entryPermissionCode())) {
            return resourceAccessDeny("ENTRY_PERMISSION_DENIED", record.snapshot());
        }
        return resourceAccessDecision(Decision.ALLOW, "ALLOW", record.snapshot());
    }

    /**
     * 判断规则异常是否表示用户身份、成员关系或会话已经不可用。
     * Determines whether a rule violation represents an inactive identity, membership, or session.
     *
     * @param reasonCode RBAC3 原因码 / RBAC3 reason code
     * @return 若主体上下文不可用则为 {@code true} / {@code true} when the subject context is inactive
     * 用法：调用 `isInactiveIdentitySession` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `isInactiveIdentitySession`, then continue the business flow using its result, exception, or side effect.
     */
    private boolean isInactiveIdentitySession(String reasonCode) {
        return "RESOURCE_NOT_FOUND".equals(reasonCode)
                || "SESSION_INVALIDATED".equals(reasonCode);
    }

    /**
     * 创建最小拒绝结果。
     * Creates a minimal denial result.
     *
     * @param reasonCode 稳定拒绝原因 / stable denial reason
     * @param snapshot 可选授权快照 / optional authorization snapshot
     * @return 拒绝结果 / denial result
     * 用法：调用 `resourceAccessDeny` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `resourceAccessDeny`, then continue the business flow using its result, exception, or side effect.
     */
    private ResourceAccessDecision resourceAccessDeny(
            String reasonCode,
            SessionAuthorizationSnapshot snapshot) {
        return resourceAccessDecision(Decision.DENY, reasonCode, snapshot);
    }

    /**
     * 创建只包含判定、原因和授权版本的资源入口结果。
     * Creates a resource-entry result containing only the decision, reason, and authorization versions.
     *
     * @param decision 授权判定 / authorization decision
     * @param reasonCode 稳定原因码 / stable reason code
     * @param snapshot 可选授权快照 / optional authorization snapshot
     * @return 最小资源入口结果 / minimal resource-entry result
     * 用法：调用 `resourceAccessDecision` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `resourceAccessDecision`, then continue the business flow using its result, exception, or side effect.
     */
    private ResourceAccessDecision resourceAccessDecision(
            Decision decision,
            String reasonCode,
            SessionAuthorizationSnapshot snapshot) {
        return new ResourceAccessDecision(
                decision, reasonCode,
                snapshot == null ? null : snapshot.authVersion(),
                snapshot == null ? null : snapshot.sessionVersion(),
                snapshot == null ? null : snapshot.policyVersion(),
                clock.instant());
    }

    /**
     * 校验资源入口判定调用服务是否允许访问目标租户。
     * Validates that the resource-entry caller may access the target tenant.
     *
     * @param caller 已认证调用服务 / authenticated calling service
     * @param tenantId 目标租户 / target tenant
     * 用法：调用 `requireResourceDecisionTenant` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireResourceDecisionTenant`, then continue the business flow using its result, exception, or side effect.
     */
    private void requireResourceDecisionTenant(
            ServiceIdentityPrincipal caller,
            String tenantId) {
        Objects.requireNonNull(caller, "caller");
        String targetTenant = required(tenantId, "tenantId");
        if (!caller.tenantId().equals(targetTenant)) {
            throw new Rbac3RuleViolation("SERVICE_IDENTITY_DENIED");
        }
    }

    /**
     * 读取仅包含调用服务绑定应用的会话授权快照。
     * Loads a session authorization snapshot containing only the caller-bound application.
     *
     * @param caller 已认证调用服务 / authenticated calling service
     * @param tenantId 租户标识 / tenant identifier
     * @param sessionId 会话标识 / session identifier
     * @return 应用受限的会话快照 / application-bound session snapshot
     * 用法：调用 `snapshot` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `snapshot`, then continue the business flow using its result, exception, or side effect.
     */
    public SessionAuthorizationSnapshot snapshot(
            ServiceIdentityPrincipal caller,
            String tenantId,
            String sessionId) {
        requireServiceTenant(caller, tenantId);
        requireUnfenced(tenantId, sessionId);
        return boundSnapshot(caller, tenantId, sessionId);
    }

    /**
     * 从完整快照提取调用服务绑定的单应用上下文。
     * Extracts the single caller-bound application context from a full snapshot.
     *
     * @param caller 已认证调用服务 / authenticated calling service
     * @param tenantId 租户标识 / tenant identifier
     * @param sessionId 会话标识 / session identifier
     * @return 单应用授权快照 / single-application authorization snapshot
     * 用法：调用 `boundSnapshot` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `boundSnapshot`, then continue the business flow using its result, exception, or side effect.
     */
    private SessionAuthorizationSnapshot boundSnapshot(
            ServiceIdentityPrincipal caller,
            String tenantId,
            String sessionId) {
        SnapshotRecord record = snapshotSource.load(tenantId, required(sessionId, "sessionId"));
        if (!record.tenantId().equals(tenantId)
                || !record.snapshot().sessionId().equals(sessionId)) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        AppAuthorizationContext application = application(
                record.snapshot(), caller.sourceAppCode());
        return new SessionAuthorizationSnapshot(
                record.snapshot().sessionId(), record.snapshot().authVersion(),
                record.snapshot().sessionVersion(), record.snapshot().policyVersion(),
                List.of(application), record.snapshot().checksum(),
                record.snapshot().generatedAt());
    }

    /**
     * 校验指定会话是否仍处于授权传播 Fence 中。
     * Verifies whether the specified session is still behind an authorization propagation fence.
     *
     * @param caller 已认证调用服务 / authenticated calling service
     * @param tenantId 租户标识 / tenant identifier
     * @param sessionId 会话标识 / session identifier
     * @return Fence 判定 / fence decision
     * 用法：调用 `verifyFence` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `verifyFence`, then continue the business flow using its result, exception, or side effect.
     */
    public FenceVerification verifyFence(
            ServiceIdentityPrincipal caller,
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

    /**
     * 要求会话不存在传播 Fence。
     * Requires the session to be free of a propagation fence.
     *
     * @param tenantId 租户标识 / tenant identifier
     * @param sessionId 会话标识 / session identifier
     * 用法：调用 `requireUnfenced` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireUnfenced`, then continue the business flow using its result, exception, or side effect.
     */
    private void requireUnfenced(String tenantId, String sessionId) {
        if (fenceVerifier.isFenced(
                required(tenantId, "tenantId"), required(sessionId, "sessionId"))) {
            throw new Rbac3RuleViolation("AUTH_PROPAGATION_PENDING");
        }
    }

    /**
     * 为当前用户读取租户一致的授权快照。
     * Loads a tenant-consistent authorization snapshot for the current user.
     *
     * @param caller 当前用户主体 / current user principal
     * @param request 判定请求 / decision request
     * @return 一致授权快照记录 / consistent authorization snapshot record
     * 用法：调用 `consistentSnapshot` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `consistentSnapshot`, then continue the business flow using its result, exception, or side effect.
     */
    public SnapshotRecord consistentSnapshot(
            CurrentRbac3Principal caller,
            DecisionRequest request) {
        Objects.requireNonNull(caller, "caller");
        if (!caller.tenantId().equals(request.subject().tenantId())) {
            throw new Rbac3RuleViolation("PERMISSION_DENIED");
        }
        return load(request.subject());
    }

    /**
     * 在已加载的一致快照上应用可选权限增删后执行判定。
     * Evaluates an already loaded consistent snapshot after optional permission additions/removals.
     *
     * @param record 一致快照记录 / consistent snapshot record
     * @param request 判定请求 / decision request
     * @param addedPermissions 临时增加的权限 / permissions added for this evaluation
     * @param removedPermissions 临时移除的权限 / permissions removed for this evaluation
     * @return 类型化判定组合 / typed decision bundle
     * 用法：调用 `evaluateConsistentSnapshot` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `evaluateConsistentSnapshot`, then continue the business flow using its result, exception, or side effect.
     */
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

    /**
     * 加载并校验请求主体的快照记录。
     * Loads and validates the snapshot record for the requested subject.
     *
     * @param subject 请求主体 / requested subject
     * @return 已校验快照记录 / validated snapshot record
     * 用法：调用 `load` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `load`, then continue the business flow using its result, exception, or side effect.
     */
    private SnapshotRecord load(Subject subject) {
        SnapshotRecord record = snapshotSource.load(subject.tenantId(), subject.sessionId());
        validateSubject(record, subject);
        return record;
    }

    /**
     * 校验快照记录与请求主体完全一致。
     * Validates that the snapshot record exactly matches the requested subject.
     *
     * @param record 快照记录 / snapshot record
     * @param subject 请求主体 / requested subject
     * 用法：调用 `validateSubject` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `validateSubject`, then continue the business flow using its result, exception, or side effect.
     */
    private void validateSubject(SnapshotRecord record, Subject subject) {
        if (!record.tenantId().equals(subject.tenantId())
                || !record.userId().equals(subject.userId())
                || !record.snapshot().sessionId().equals(subject.sessionId())) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
    }

    /**
     * 校验 Token 携带的三个授权版本与快照一致。
     * Validates all three token authorization versions against the snapshot.
     *
     * @param snapshot 当前授权快照 / current authorization snapshot
     * @param versions Token 授权版本 / token authorization versions
     * 用法：调用 `validateVersions` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `validateVersions`, then continue the business flow using its result, exception, or side effect.
     */
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

    /**
     * 从会话快照中读取指定应用上下文。
     * Finds the requested application context in a session snapshot.
     *
     * @param snapshot 会话授权快照 / session authorization snapshot
     * @param applicationCode 应用编码 / application code
     * @return 应用授权上下文 / application authorization context
     * 用法：调用 `application` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `application`, then continue the business flow using its result, exception, or side effect.
     */
    private AppAuthorizationContext application(
            SessionAuthorizationSnapshot snapshot,
            String applicationCode) {
        return snapshot.appContexts().stream()
                .filter(context -> context.applicationCode().equals(applicationCode))
                .findFirst()
                .orElseThrow(() -> new Rbac3RuleViolation("APPLICATION_BINDING_DENIED"));
    }

    /**
     * 为缺失的数据范围策略创建 Fail Closed 判定。
     * Creates a fail-closed decision for a missing data-scope policy.
     *
     * @param record 快照记录 / snapshot record
     * @param request 判定请求 / decision request
     * @param now 判定时间 / decision time
     * @return 拒绝数据范围 / denied data scope
     * 用法：调用 `missingDataScope` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `missingDataScope`, then continue the business flow using its result, exception, or side effect.
     */
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

    /**
     * 为缺失的字段策略创建 Fail Closed 判定。
     * Creates a fail-closed decision for a missing field policy.
     *
     * @param record 快照记录 / snapshot record
     * @param request 判定请求 / decision request
     * @param now 判定时间 / decision time
     * @return 拒绝字段策略 / denied field policy
     * 用法：调用 `missingFieldPolicy` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `missingFieldPolicy`, then continue the business flow using its result, exception, or side effect.
     */
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

    /**
     * 要求调用服务与目标租户精确绑定。
     * Requires the calling service to be exactly bound to the target tenant.
     *
     * @param caller 已认证调用服务 / authenticated calling service
     * @param tenantId 目标租户 / target tenant
     * 用法：调用 `requireServiceTenant` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireServiceTenant`, then continue the business flow using its result, exception, or side effect.
     */
    private void requireServiceTenant(
            ServiceIdentityPrincipal caller,
            String tenantId) {
        Objects.requireNonNull(caller, "caller");
        if (!caller.tenantId().equals(required(tenantId, "tenantId"))) {
            throw new Rbac3RuleViolation("SERVICE_IDENTITY_DENIED");
        }
    }

    /**
     * 要求调用服务与目标应用精确绑定。
     * Requires the calling service to be exactly bound to the target application.
     *
     * @param caller 已认证调用服务 / authenticated calling service
     * @param applicationCode 目标应用编码 / target application code
     * 用法：调用 `requireApplication` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireApplication`, then continue the business flow using its result, exception, or side effect.
     */
    private void requireApplication(
            ServiceIdentityPrincipal caller,
            String applicationCode) {
        if (!caller.sourceAppCode().equals(required(applicationCode, "applicationCode"))) {
            throw new Rbac3RuleViolation("APPLICATION_BINDING_DENIED");
        }
    }

    /**
     * 校验必填文本并移除首尾空白。
     * Validates required text and trims surrounding whitespace.
     *
     * @param value 待校验值 / value to validate
     * @param fieldName 字段名 / field name
     * @return 规范化文本 / normalized text
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     */
    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    /**
     * 会话授权快照读取端口。
     * Port for loading session authorization snapshots.
     * 语义与用法：将 `SnapshotSource` 作为 `AuthorizationDecisionService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SnapshotSource` as the responsibility boundary of `AuthorizationDecisionService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface SnapshotSource {

        /**
         * 按租户和会话读取授权快照记录。
         * Loads an authorization snapshot record by tenant and session.
         *
         * @param tenantId 租户标识 / tenant identifier
         * @param sessionId 会话标识 / session identifier
         * @return 授权快照记录 / authorization snapshot record
         * 用法：调用 `load` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `load`, then continue the business flow using its result, exception, or side effect.
         */
        SnapshotRecord load(String tenantId, String sessionId);
    }

    /**
     * 会话授权传播 Fence 校验端口。
     * Port for checking a session authorization propagation fence.
     * 语义与用法：将 `FenceVerifier` 作为 `AuthorizationDecisionService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `FenceVerifier` as the responsibility boundary of `AuthorizationDecisionService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface FenceVerifier {

        /**
         * 判断指定会话是否仍处于传播 Fence 中。
         * Determines whether the specified session is still fenced.
         *
         * @param tenantId 租户标识 / tenant identifier
         * @param sessionId 会话标识 / session identifier
         * @return 若存在 Fence 则为 {@code true} / {@code true} when fenced
         * 用法：调用 `isFenced` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `isFenced`, then continue the business flow using its result, exception, or side effect.
         */
        boolean isFenced(String tenantId, String sessionId);
    }

    /**
     * 关联租户、IdP 主体、RBAC 用户与不可变授权快照的记录。
     * Record associating a tenant, IdP subject, RBAC user, and immutable authorization snapshot.
     *
     * @param tenantId 租户标识 / tenant identifier
     * @param identitySub IdP 稳定主体标识 / stable IdP subject
     * @param userId RBAC 用户标识 / RBAC user identifier
     * @param snapshot 会话授权快照 / session authorization snapshot
     * 语义与用法：将 `SnapshotRecord` 作为 `AuthorizationDecisionService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SnapshotRecord` as the responsibility boundary of `AuthorizationDecisionService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public record SnapshotRecord(
            /**
             * 字段 `tenantId` 表示 `SnapshotRecord` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `SnapshotRecord` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `SnapshotRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `SnapshotRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `identitySub` 表示 `SnapshotRecord` 中与 `identity Sub` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `identitySub` stores the `identity Sub`-related state, dependency, configuration, or result of `SnapshotRecord` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `identitySub` 时应保持 `SnapshotRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `identitySub`, preserve `SnapshotRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            String identitySub,
            /**
             * 字段 `userId` 表示 `SnapshotRecord` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `SnapshotRecord` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userId` 时应保持 `SnapshotRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userId`, preserve `SnapshotRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            String userId,
            /**
             * 字段 `snapshot` 表示 `SnapshotRecord` 中与 `snapshot` 相关的状态、依赖、配置或结果（声明类型 `SessionAuthorizationSnapshot`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `snapshot` stores the `snapshot`-related state, dependency, configuration, or result of `SnapshotRecord` (declared type `SessionAuthorizationSnapshot`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `snapshot` 时应保持 `SnapshotRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `snapshot`, preserve `SnapshotRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            SessionAuthorizationSnapshot snapshot) {

        /**
         * 校验并规范化快照记录。
         * Validates and normalizes the snapshot record.
         * 用法：通过 `SnapshotRecord` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `SnapshotRecord`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param snapshot 输入参数 `snapshot`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public SnapshotRecord {
            tenantId = required(tenantId, "tenantId");
            identitySub = required(identitySub, "identitySub");
            userId = required(userId, "userId");
            snapshot = Objects.requireNonNull(snapshot, "snapshot");
        }

        /**
         * 使用同一值作为兼容的 IdP 主体和 RBAC 用户标识创建记录。
         * Creates a record using the same compatibility value for IdP subject and RBAC user ID.
         *
         * @param tenantId 租户标识 / tenant identifier
         * @param userId 用户标识，同时作为 IdP 主体 / user identifier, also used as IdP subject
         * @param snapshot 会话授权快照 / session authorization snapshot
         */
        public SnapshotRecord(
                String tenantId,
                String userId,
                SessionAuthorizationSnapshot snapshot) {
            this(tenantId, userId, userId, snapshot);
        }
    }

    /**
     * 类型化授权判定的用户主体定位信息。
     * User-subject locator for a typed authorization decision.
     *
     * @param tenantId 租户标识 / tenant identifier
     * @param userId RBAC 用户标识 / RBAC user identifier
     * @param sessionId 会话标识 / session identifier
     * 语义与用法：将 `Subject` 作为 `AuthorizationDecisionService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Subject` as the responsibility boundary of `AuthorizationDecisionService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public record Subject(/**
 * 字段 `tenantId` 表示 `Subject` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `Subject` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `Subject` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `Subject`'s lifecycle, immutability, and thread-safety constraints.
 */ String tenantId, /**
 * 字段 `userId` 表示 `Subject` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `Subject` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `userId` 时应保持 `Subject` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `userId`, preserve `Subject`'s lifecycle, immutability, and thread-safety constraints.
 */ String userId, /**
 * 字段 `sessionId` 表示 `Subject` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `Subject` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `Subject` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `Subject`'s lifecycle, immutability, and thread-safety constraints.
 */ String sessionId) {

        /**
         * 校验并规范化主体定位信息。
         * Validates and normalizes the subject locator.
         * 用法：通过 `Subject` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `Subject`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public Subject {
            tenantId = required(tenantId, "tenantId");
            userId = required(userId, "userId");
            sessionId = required(sessionId, "sessionId");
        }
    }

    /**
     * 类型化授权判定的目标应用资源。
     * Target application resource for a typed authorization decision.
     *
     * @param applicationCode 应用编码 / application code
     * @param resourceCode 资源编码 / resource code
     * 语义与用法：将 `Resource` 作为 `AuthorizationDecisionService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Resource` as the responsibility boundary of `AuthorizationDecisionService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public record Resource(/**
 * 字段 `applicationCode` 表示 `Resource` 中与 `application Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `applicationCode` stores the `application Code`-related state, dependency, configuration, or result of `Resource` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `applicationCode` 时应保持 `Resource` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `applicationCode`, preserve `Resource`'s lifecycle, immutability, and thread-safety constraints.
 */ String applicationCode, /**
 * 字段 `resourceCode` 表示 `Resource` 中与 `resource Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `resourceCode` stores the `resource Code`-related state, dependency, configuration, or result of `Resource` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `resourceCode` 时应保持 `Resource` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `resourceCode`, preserve `Resource`'s lifecycle, immutability, and thread-safety constraints.
 */ String resourceCode) {

        /**
         * 校验并规范化目标资源。
         * Validates and normalizes the target resource.
         * 用法：通过 `Resource` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `Resource`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         * @param applicationCode 输入参数 `applicationCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param resourceCode 输入参数 `resourceCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public Resource {
            applicationCode = required(applicationCode, "applicationCode");
            resourceCode = required(resourceCode, "resourceCode");
        }
    }

    /**
     * Token 携带的用户、会话和策略授权版本。
     * User, session, and policy authorization versions carried by a token.
     *
     * @param authVersion 用户授权版本 / user authorization version
     * @param sessionVersion 会话版本 / session version
     * @param policyVersion 策略版本 / policy version
     * 语义与用法：将 `TokenVersions` 作为 `AuthorizationDecisionService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `TokenVersions` as the responsibility boundary of `AuthorizationDecisionService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public record TokenVersions(/**
 * 字段 `authVersion` 表示 `TokenVersions` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `TokenVersions` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `TokenVersions` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `TokenVersions`'s lifecycle, immutability, and thread-safety constraints.
 */ long authVersion, /**
 * 字段 `sessionVersion` 表示 `TokenVersions` 中与 `session Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `sessionVersion` stores the `session Version`-related state, dependency, configuration, or result of `TokenVersions` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `sessionVersion` 时应保持 `TokenVersions` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `sessionVersion`, preserve `TokenVersions`'s lifecycle, immutability, and thread-safety constraints.
 */ long sessionVersion, /**
 * 字段 `policyVersion` 表示 `TokenVersions` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `TokenVersions` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `TokenVersions` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `TokenVersions`'s lifecycle, immutability, and thread-safety constraints.
 */ long policyVersion) {

        /**
         * 校验授权版本均为非负数。
         * Validates that all authorization versions are non-negative.
         * 用法：通过 `TokenVersions` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `TokenVersions`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         * @param authVersion 输入参数 `authVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionVersion 输入参数 `sessionVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param policyVersion 输入参数 `policyVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public TokenVersions {
            if (authVersion < 0 || sessionVersion < 0 || policyVersion < 0) {
                throw new IllegalArgumentException("token versions must not be negative");
            }
        }
    }

    /**
     * 一致快照上的类型化授权判定请求。
     * Typed authorization-decision request evaluated against a consistent snapshot.
     *
     * @param subject 用户主体定位信息 / user-subject locator
     * @param permissionCode 待校验权限编码 / permission code to check
     * @param resource 目标资源 / target resource
     * @param requestedDecisions 请求的判定类型 / requested decision types
     * @param tokenVersions Token 授权版本 / token authorization versions
     * 语义与用法：将 `DecisionRequest` 作为 `AuthorizationDecisionService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `DecisionRequest` as the responsibility boundary of `AuthorizationDecisionService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public record DecisionRequest(
            /**
             * 字段 `subject` 表示 `DecisionRequest` 中与 `subject` 相关的状态、依赖、配置或结果（声明类型 `Subject`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `subject` stores the `subject`-related state, dependency, configuration, or result of `DecisionRequest` (declared type `Subject`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `subject` 时应保持 `DecisionRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `subject`, preserve `DecisionRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            Subject subject,
            /**
             * 字段 `permissionCode` 表示 `DecisionRequest` 中与 `permission Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `permissionCode` stores the `permission Code`-related state, dependency, configuration, or result of `DecisionRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `permissionCode` 时应保持 `DecisionRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `permissionCode`, preserve `DecisionRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String permissionCode,
            /**
             * 字段 `resource` 表示 `DecisionRequest` 中与 `resource` 相关的状态、依赖、配置或结果（声明类型 `Resource`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `resource` stores the `resource`-related state, dependency, configuration, or result of `DecisionRequest` (declared type `Resource`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `resource` 时应保持 `DecisionRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `resource`, preserve `DecisionRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            Resource resource,
            /**
             * 字段 `requestedDecisions` 表示 `DecisionRequest` 中与 `requested Decisions` 相关的状态、依赖、配置或结果（声明类型 `Set&lt;DecisionType&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requestedDecisions` stores the `requested Decisions`-related state, dependency, configuration, or result of `DecisionRequest` (declared type `Set&lt;DecisionType&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requestedDecisions` 时应保持 `DecisionRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requestedDecisions`, preserve `DecisionRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            Set<DecisionType> requestedDecisions,
            /**
             * 字段 `tokenVersions` 表示 `DecisionRequest` 中与 `token Versions` 相关的状态、依赖、配置或结果（声明类型 `TokenVersions`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tokenVersions` stores the `token Versions`-related state, dependency, configuration, or result of `DecisionRequest` (declared type `TokenVersions`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tokenVersions` 时应保持 `DecisionRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tokenVersions`, preserve `DecisionRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            TokenVersions tokenVersions) {

        /**
         * 校验并固化类型化判定请求。
         * Validates and freezes the typed decision request.
         * 用法：通过 `DecisionRequest` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `DecisionRequest`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         * @param subject 输入参数 `subject`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param permissionCode 输入参数 `permissionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param resource 输入参数 `resource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param requestedDecisions 输入参数 `requestedDecisions`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param tokenVersions 输入参数 `tokenVersions`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
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

    /**
     * 函数、数据范围和字段策略的类型化判定组合。
     * Typed bundle of function, data-scope, and field-policy decisions.
     *
     * @param functionDecision 函数权限判定 / function-permission decision
     * @param dataScopeDecision 可选数据范围判定 / optional data-scope decision
     * @param fieldPolicyDecision 可选字段策略判定 / optional field-policy decision
     * @param snapshotChecksum 判定快照校验和 / decision snapshot checksum
     * 语义与用法：将 `DecisionBundle` 作为 `AuthorizationDecisionService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `DecisionBundle` as the responsibility boundary of `AuthorizationDecisionService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public record DecisionBundle(
            /**
             * 字段 `functionDecision` 表示 `DecisionBundle` 中与 `function Decision` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationDecision`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `functionDecision` stores the `function Decision`-related state, dependency, configuration, or result of `DecisionBundle` (declared type `AuthorizationDecision`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `functionDecision` 时应保持 `DecisionBundle` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `functionDecision`, preserve `DecisionBundle`'s lifecycle, immutability, and thread-safety constraints.
             */
            AuthorizationDecision functionDecision,
            /**
             * 字段 `dataScopeDecision` 表示 `DecisionBundle` 中与 `data Scope Decision` 相关的状态、依赖、配置或结果（声明类型 `DataScopeDecision`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `dataScopeDecision` stores the `data Scope Decision`-related state, dependency, configuration, or result of `DecisionBundle` (declared type `DataScopeDecision`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `dataScopeDecision` 时应保持 `DecisionBundle` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `dataScopeDecision`, preserve `DecisionBundle`'s lifecycle, immutability, and thread-safety constraints.
             */
            DataScopeDecision dataScopeDecision,
            /**
             * 字段 `fieldPolicyDecision` 表示 `DecisionBundle` 中与 `field Policy Decision` 相关的状态、依赖、配置或结果（声明类型 `FieldPolicyDecision`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `fieldPolicyDecision` stores the `field Policy Decision`-related state, dependency, configuration, or result of `DecisionBundle` (declared type `FieldPolicyDecision`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `fieldPolicyDecision` 时应保持 `DecisionBundle` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `fieldPolicyDecision`, preserve `DecisionBundle`'s lifecycle, immutability, and thread-safety constraints.
             */
            FieldPolicyDecision fieldPolicyDecision,
            /**
             * 字段 `snapshotChecksum` 表示 `DecisionBundle` 中与 `snapshot Checksum` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `snapshotChecksum` stores the `snapshot Checksum`-related state, dependency, configuration, or result of `DecisionBundle` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `snapshotChecksum` 时应保持 `DecisionBundle` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `snapshotChecksum`, preserve `DecisionBundle`'s lifecycle, immutability, and thread-safety constraints.
             */
            String snapshotChecksum) {
    }

    /**
     * 会话授权传播 Fence 校验结果。
     * Session authorization propagation-fence verification result.
     *
     * @param decision Fence 判定 / fence decision
     * @param reasonCode 稳定原因码 / stable reason code
     * @param sessionId 会话标识 / session identifier
     * @param verifiedAt 校验时间 / verification time
     * 语义与用法：将 `FenceVerification` 作为 `AuthorizationDecisionService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `FenceVerification` as the responsibility boundary of `AuthorizationDecisionService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public record FenceVerification(
            /**
             * 字段 `decision` 表示 `FenceVerification` 中与 `decision` 相关的状态、依赖、配置或结果（声明类型 `Decision`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `decision` stores the `decision`-related state, dependency, configuration, or result of `FenceVerification` (declared type `Decision`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `decision` 时应保持 `FenceVerification` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `decision`, preserve `FenceVerification`'s lifecycle, immutability, and thread-safety constraints.
             */
            Decision decision,
            /**
             * 字段 `reasonCode` 表示 `FenceVerification` 中与 `reason Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reasonCode` stores the `reason Code`-related state, dependency, configuration, or result of `FenceVerification` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reasonCode` 时应保持 `FenceVerification` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reasonCode`, preserve `FenceVerification`'s lifecycle, immutability, and thread-safety constraints.
             */
            String reasonCode,
            /**
             * 字段 `sessionId` 表示 `FenceVerification` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `FenceVerification` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `FenceVerification` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `FenceVerification`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sessionId,
            /**
             * 字段 `verifiedAt` 表示 `FenceVerification` 中与 `verified At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `verifiedAt` stores the `verified At`-related state, dependency, configuration, or result of `FenceVerification` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `verifiedAt` 时应保持 `FenceVerification` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `verifiedAt`, preserve `FenceVerification`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant verifiedAt) {
    }

    /**
     * 用户 Resource Server 入口判定请求。
     * User Resource Server entry-decision request.
     *
     * @param identitySub IdP 稳定用户主体标识 / stable IdP user subject
     * @param tenantId 租户标识 / tenant identifier
     * @param sessionId IdP 会话标识 / IdP session identifier
     * @param rbacApplicationCode 目标 RBAC3 应用编码 / target RBAC3 application code
     * @param entryPermissionCode 应用入口权限编码 / application entry permission code
     * 语义与用法：将 `ResourceAccessRequest` 作为 `AuthorizationDecisionService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ResourceAccessRequest` as the responsibility boundary of `AuthorizationDecisionService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public record ResourceAccessRequest(
            /**
             * 字段 `identitySub` 表示 `ResourceAccessRequest` 中与 `identity Sub` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `identitySub` stores the `identity Sub`-related state, dependency, configuration, or result of `ResourceAccessRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `identitySub` 时应保持 `ResourceAccessRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `identitySub`, preserve `ResourceAccessRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String identitySub,
            /**
             * 字段 `tenantId` 表示 `ResourceAccessRequest` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `ResourceAccessRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `ResourceAccessRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `ResourceAccessRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `sessionId` 表示 `ResourceAccessRequest` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `ResourceAccessRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `ResourceAccessRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `ResourceAccessRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sessionId,
            /**
             * 字段 `rbacApplicationCode` 表示 `ResourceAccessRequest` 中与 `rbac Application Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `rbacApplicationCode` stores the `rbac Application Code`-related state, dependency, configuration, or result of `ResourceAccessRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `rbacApplicationCode` 时应保持 `ResourceAccessRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `rbacApplicationCode`, preserve `ResourceAccessRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String rbacApplicationCode,
            /**
             * 字段 `entryPermissionCode` 表示 `ResourceAccessRequest` 中与 `entry Permission Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `entryPermissionCode` stores the `entry Permission Code`-related state, dependency, configuration, or result of `ResourceAccessRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `entryPermissionCode` 时应保持 `ResourceAccessRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `entryPermissionCode`, preserve `ResourceAccessRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String entryPermissionCode) {

        /**
         * 校验并规范化资源入口请求。
         * Validates and normalizes the resource-entry request.
         * 用法：通过 `ResourceAccessRequest` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `ResourceAccessRequest`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param rbacApplicationCode 输入参数 `rbacApplicationCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param entryPermissionCode 输入参数 `entryPermissionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public ResourceAccessRequest {
            identitySub = required(identitySub, "identitySub");
            tenantId = required(tenantId, "tenantId");
            sessionId = required(sessionId, "sessionId");
            rbacApplicationCode = required(rbacApplicationCode, "rbacApplicationCode");
            entryPermissionCode = required(entryPermissionCode, "entryPermissionCode");
        }
    }

    /**
     * 最小用户 Resource Server 入口判定结果。
     * Minimal user Resource Server entry-decision result.
     *
     * @param decision ALLOW 或 DENY 判定 / ALLOW or DENY decision
     * @param reasonCode 稳定原因码 / stable reason code
     * @param authVersion 用户授权版本；无快照时为空 / user authorization version, nullable without a snapshot
     * @param sessionVersion 会话版本；无快照时为空 / session version, nullable without a snapshot
     * @param policyVersion 策略版本；无快照时为空 / policy version, nullable without a snapshot
     * @param decidedAt 判定时间 / decision time
     * 语义与用法：将 `ResourceAccessDecision` 作为 `AuthorizationDecisionService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ResourceAccessDecision` as the responsibility boundary of `AuthorizationDecisionService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public record ResourceAccessDecision(
            /**
             * 字段 `decision` 表示 `ResourceAccessDecision` 中与 `decision` 相关的状态、依赖、配置或结果（声明类型 `Decision`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `decision` stores the `decision`-related state, dependency, configuration, or result of `ResourceAccessDecision` (declared type `Decision`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `decision` 时应保持 `ResourceAccessDecision` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `decision`, preserve `ResourceAccessDecision`'s lifecycle, immutability, and thread-safety constraints.
             */
            Decision decision,
            /**
             * 字段 `reasonCode` 表示 `ResourceAccessDecision` 中与 `reason Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reasonCode` stores the `reason Code`-related state, dependency, configuration, or result of `ResourceAccessDecision` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reasonCode` 时应保持 `ResourceAccessDecision` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reasonCode`, preserve `ResourceAccessDecision`'s lifecycle, immutability, and thread-safety constraints.
             */
            String reasonCode,
            /**
             * 字段 `authVersion` 表示 `ResourceAccessDecision` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `ResourceAccessDecision` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `ResourceAccessDecision` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `ResourceAccessDecision`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long authVersion,
            /**
             * 字段 `sessionVersion` 表示 `ResourceAccessDecision` 中与 `session Version` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionVersion` stores the `session Version`-related state, dependency, configuration, or result of `ResourceAccessDecision` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionVersion` 时应保持 `ResourceAccessDecision` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionVersion`, preserve `ResourceAccessDecision`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long sessionVersion,
            /**
             * 字段 `policyVersion` 表示 `ResourceAccessDecision` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `ResourceAccessDecision` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `ResourceAccessDecision` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `ResourceAccessDecision`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long policyVersion,
            /**
             * 字段 `decidedAt` 表示 `ResourceAccessDecision` 中与 `decided At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `decidedAt` stores the `decided At`-related state, dependency, configuration, or result of `ResourceAccessDecision` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `decidedAt` 时应保持 `ResourceAccessDecision` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `decidedAt`, preserve `ResourceAccessDecision`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant decidedAt) {

        /**
         * 校验最小资源入口判定。
         * Validates the minimal resource-entry decision.
         * 用法：通过 `ResourceAccessDecision` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `ResourceAccessDecision`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         * @param decision 输入参数 `decision`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param authVersion 输入参数 `authVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionVersion 输入参数 `sessionVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param policyVersion 输入参数 `policyVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param decidedAt 输入参数 `decidedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public ResourceAccessDecision {
            decision = Objects.requireNonNull(decision, "decision");
            reasonCode = required(reasonCode, "reasonCode");
            decidedAt = Objects.requireNonNull(decidedAt, "decidedAt");
            int versionCount = (authVersion == null ? 0 : 1)
                    + (sessionVersion == null ? 0 : 1)
                    + (policyVersion == null ? 0 : 1);
            if (versionCount != 0 && versionCount != 3) {
                throw new IllegalArgumentException(
                        "authorization versions must be all present or all absent");
            }
            if ((authVersion != null && authVersion < 0)
                    || (sessionVersion != null && sessionVersion < 0)
                    || (policyVersion != null && policyVersion < 0)) {
                throw new IllegalArgumentException(
                        "authorization versions must not be negative");
            }
        }
    }

    /**
     * 类型化授权判定维度。
     * Typed authorization-decision dimension.
     * 语义与用法：将 `DecisionType` 作为 `AuthorizationDecisionService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `DecisionType` as the responsibility boundary of `AuthorizationDecisionService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum DecisionType {
        /** 函数权限。 / Function permission.
         * 含义与用法：读取、传递或更新 `FUNCTION` 时应保持 `DecisionType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `FUNCTION`, preserve `DecisionType`'s lifecycle, immutability, and thread-safety constraints.
         */
        FUNCTION,
        /** 数据范围。 / Data scope.
         * 含义与用法：读取、传递或更新 `DATA_SCOPE` 时应保持 `DecisionType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `DATA_SCOPE`, preserve `DecisionType`'s lifecycle, immutability, and thread-safety constraints.
         */
        DATA_SCOPE,
        /** 字段策略。 / Field policy.
         * 含义与用法：读取、传递或更新 `FIELD` 时应保持 `DecisionType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `FIELD`, preserve `DecisionType`'s lifecycle, immutability, and thread-safety constraints.
         */
        FIELD,
        /** 参与约束。 / Participation constraint.
         * 含义与用法：读取、传递或更新 `PARTICIPATION` 时应保持 `DecisionType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `PARTICIPATION`, preserve `DecisionType`'s lifecycle, immutability, and thread-safety constraints.
         */
        PARTICIPATION,
        /** 授权传播 Fence。 / Authorization propagation fence.
         * 含义与用法：读取、传递或更新 `FENCE` 时应保持 `DecisionType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `FENCE`, preserve `DecisionType`'s lifecycle, immutability, and thread-safety constraints.
         */
        FENCE
    }
}
