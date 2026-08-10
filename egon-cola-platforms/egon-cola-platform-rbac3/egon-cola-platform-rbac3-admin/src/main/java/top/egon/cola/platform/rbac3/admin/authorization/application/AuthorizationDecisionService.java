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
 * 基于单个不可变会话授权快照提供远程授权判定门面。
 * Remote authorization-decision facade over one immutable session authorization snapshot.
 */
public final class AuthorizationDecisionService {

    /** 授权快照来源。 / Authorization snapshot source. */
    private final SnapshotSource snapshotSource;
    /** 会话传播 Fence 校验器。 / Session propagation-fence verifier. */
    private final FenceVerifier fenceVerifier;
    /** 产生可测试审计时间的时钟。 / Clock used for testable audit timestamps. */
    private final Clock clock;

    /**
     * 创建授权判定服务。
     * Creates the authorization-decision service.
     *
     * @param snapshotSource 授权快照来源 / authorization snapshot source
     * @param fenceVerifier 会话 Fence 校验器 / session-fence verifier
     * @param clock 审计时钟 / audit clock
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
            CurrentRbac3ServicePrincipal caller,
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
     */
    public ResourceAccessDecision decideResourceAccess(
            CurrentRbac3ServicePrincipal caller,
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
     * <p>平台级 IdP 服务使用 {@code *} 租户执行跨租户判定；普通服务只能访问自身租户。
     * A platform IdP service uses tenant {@code *} for cross-tenant decisions, while ordinary
     * services remain restricted to their own tenant.</p>
     *
     * @param caller 已认证调用服务 / authenticated calling service
     * @param tenantId 目标租户 / target tenant
     */
    private void requireResourceDecisionTenant(
            CurrentRbac3ServicePrincipal caller,
            String tenantId) {
        Objects.requireNonNull(caller, "caller");
        String targetTenant = required(tenantId, "tenantId");
        if (!(caller.tenantId().equals(targetTenant) || "*".equals(caller.tenantId()))) {
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
     */
    public SessionAuthorizationSnapshot snapshot(
            CurrentRbac3ServicePrincipal caller,
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
     */
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

    /**
     * 校验指定会话是否仍处于授权传播 Fence 中。
     * Verifies whether the specified session is still behind an authorization propagation fence.
     *
     * @param caller 已认证调用服务 / authenticated calling service
     * @param tenantId 租户标识 / tenant identifier
     * @param sessionId 会话标识 / session identifier
     * @return Fence 判定 / fence decision
     */
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

    /**
     * 要求会话不存在传播 Fence。
     * Requires the session to be free of a propagation fence.
     *
     * @param tenantId 租户标识 / tenant identifier
     * @param sessionId 会话标识 / session identifier
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
     */
    private void requireServiceTenant(
            CurrentRbac3ServicePrincipal caller,
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
     */
    private void requireApplication(
            CurrentRbac3ServicePrincipal caller,
            String applicationCode) {
        if (!caller.applicationCode().equals(required(applicationCode, "applicationCode"))) {
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
         */
        SnapshotRecord load(String tenantId, String sessionId);
    }

    /**
     * 会话授权传播 Fence 校验端口。
     * Port for checking a session authorization propagation fence.
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
     */
    public record SnapshotRecord(
            String tenantId,
            String identitySub,
            String userId,
            SessionAuthorizationSnapshot snapshot) {

        /**
         * 校验并规范化快照记录。
         * Validates and normalizes the snapshot record.
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
     */
    public record Subject(String tenantId, String userId, String sessionId) {

        /**
         * 校验并规范化主体定位信息。
         * Validates and normalizes the subject locator.
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
     */
    public record Resource(String applicationCode, String resourceCode) {

        /**
         * 校验并规范化目标资源。
         * Validates and normalizes the target resource.
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
     */
    public record TokenVersions(long authVersion, long sessionVersion, long policyVersion) {

        /**
         * 校验授权版本均为非负数。
         * Validates that all authorization versions are non-negative.
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
     */
    public record DecisionRequest(
            Subject subject,
            String permissionCode,
            Resource resource,
            Set<DecisionType> requestedDecisions,
            TokenVersions tokenVersions) {

        /**
         * 校验并固化类型化判定请求。
         * Validates and freezes the typed decision request.
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
     */
    public record DecisionBundle(
            AuthorizationDecision functionDecision,
            DataScopeDecision dataScopeDecision,
            FieldPolicyDecision fieldPolicyDecision,
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
     */
    public record FenceVerification(
            Decision decision,
            String reasonCode,
            String sessionId,
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
     */
    public record ResourceAccessRequest(
            String identitySub,
            String tenantId,
            String sessionId,
            String rbacApplicationCode,
            String entryPermissionCode) {

        /**
         * 校验并规范化资源入口请求。
         * Validates and normalizes the resource-entry request.
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
     */
    public record ResourceAccessDecision(
            Decision decision,
            String reasonCode,
            Long authVersion,
            Long sessionVersion,
            Long policyVersion,
            Instant decidedAt) {

        /**
         * 校验最小资源入口判定。
         * Validates the minimal resource-entry decision.
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
     */
    public enum DecisionType {
        /** 函数权限。 / Function permission. */
        FUNCTION,
        /** 数据范围。 / Data scope. */
        DATA_SCOPE,
        /** 字段策略。 / Field policy. */
        FIELD,
        /** 参与约束。 / Participation constraint. */
        PARTICIPATION,
        /** 授权传播 Fence。 / Authorization propagation fence. */
        FENCE
    }
}
