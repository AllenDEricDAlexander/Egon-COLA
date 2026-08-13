package top.egon.cola.platform.rbac3.admin.authorization.service;

import top.egon.cola.platform.rbac3.admin.config.security.CurrentRbac3Principal;
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
import top.egon.cola.platform.rbac3.admin.authorization.repository.AuthorizationSnapshotRepository;
import top.egon.cola.platform.rbac3.admin.authorization.repository.FenceVerifier;
import top.egon.cola.platform.rbac3.admin.authorization.domain.vo.SnapshotRecordVO;
import top.egon.cola.platform.rbac3.admin.authorization.domain.vo.AuthorizationDecisionSubjectVO;
import top.egon.cola.platform.rbac3.admin.authorization.domain.vo.AuthorizationDecisionResourceVO;
import top.egon.cola.platform.rbac3.admin.authorization.domain.vo.TokenVersionsVO;
import top.egon.cola.platform.rbac3.admin.authorization.domain.dto.DecisionRequestDTO;
import top.egon.cola.platform.rbac3.admin.authorization.domain.vo.DecisionBundleVO;
import top.egon.cola.platform.rbac3.admin.authorization.domain.vo.FenceVerificationVO;
import top.egon.cola.platform.rbac3.admin.authorization.domain.dto.ResourceAccessRequestDTO;
import top.egon.cola.platform.rbac3.admin.authorization.domain.vo.ResourceAccessDecisionVO;
import top.egon.cola.platform.rbac3.admin.authorization.domain.enums.AuthorizationDecisionDecisionTypeEnum;

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
    private final AuthorizationSnapshotRepository snapshotSource;
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
            AuthorizationSnapshotRepository snapshotSource,
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
    public DecisionBundleVO decide(
            ServiceIdentityPrincipal caller,
            DecisionRequestDTO request) {
        requireServiceTenant(caller, request.subject().tenantId());
        requireApplication(caller, request.resource().applicationCode());
        requireUnfenced(request.subject().tenantId(), request.subject().sessionId());
        SnapshotRecordVO snapshot = load(request.subject());
        return evaluateConsistentSnapshot(snapshot, request, Set.of(), Set.of());
    }

    /**
     * 判定 IdP 用户是否具备进入目标 AuthorizationDecisionResourceVO Server 应用的入口权限。
     * Decides whether an IdP user has the entry permission for a target AuthorizationDecisionResourceVO Server application.
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
    public ResourceAccessDecisionVO decideResourceAccess(
            ServiceIdentityPrincipal caller,
            ResourceAccessRequestDTO request) {
        Objects.requireNonNull(request, "request");
        requireResourceDecisionTenant(caller, request.tenantId());
        if (fenceVerifier.isFenced(request.tenantId(), request.sessionId())) {
            return resourceAccessDeny("AUTH_PROPAGATION_PENDING", null);
        }
        SnapshotRecordVO record;
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
    private ResourceAccessDecisionVO resourceAccessDeny(
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
    private ResourceAccessDecisionVO resourceAccessDecision(
            Decision decision,
            String reasonCode,
            SessionAuthorizationSnapshot snapshot) {
        return new ResourceAccessDecisionVO(
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
        SnapshotRecordVO record = snapshotSource.load(tenantId, required(sessionId, "sessionId"));
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
    public FenceVerificationVO verifyFence(
            ServiceIdentityPrincipal caller,
            String tenantId,
            String sessionId) {
        requireServiceTenant(caller, tenantId);
        boundSnapshot(caller, tenantId, sessionId);
        boolean fenced = fenceVerifier.isFenced(tenantId, sessionId);
        return new FenceVerificationVO(
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
    public SnapshotRecordVO consistentSnapshot(
            CurrentRbac3Principal caller,
            DecisionRequestDTO request) {
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
    public DecisionBundleVO evaluateConsistentSnapshot(
            SnapshotRecordVO record,
            DecisionRequestDTO request,
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
            return new DecisionBundleVO(function, null, null, record.snapshot().checksum());
        }
        DataScopeDecision dataScope = request.requestedDecisions().contains(
                AuthorizationDecisionDecisionTypeEnum.DATA_SCOPE)
                ? application.dataScopes().getOrDefault(
                        request.permissionCode(), missingDataScope(record, request, now))
                : null;
        String fieldKey = request.permissionCode() + ':'
                + request.resource().applicationCode() + ':'
                + request.resource().resourceCode();
        FieldPolicyDecision field = request.requestedDecisions().contains(AuthorizationDecisionDecisionTypeEnum.FIELD)
                ? application.fieldPolicies().getOrDefault(
                        fieldKey, missingFieldPolicy(record, request, now))
                : null;
        return new DecisionBundleVO(function, dataScope, field, record.snapshot().checksum());
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
    private SnapshotRecordVO load(AuthorizationDecisionSubjectVO subject) {
        SnapshotRecordVO record = snapshotSource.load(subject.tenantId(), subject.sessionId());
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
    private void validateSubject(SnapshotRecordVO record, AuthorizationDecisionSubjectVO subject) {
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
            TokenVersionsVO versions) {
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
            SnapshotRecordVO record,
            DecisionRequestDTO request,
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
            SnapshotRecordVO record,
            DecisionRequestDTO request,
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












    }
