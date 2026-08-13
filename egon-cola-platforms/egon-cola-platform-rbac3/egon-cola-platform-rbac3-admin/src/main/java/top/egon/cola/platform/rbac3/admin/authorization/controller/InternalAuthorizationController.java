package top.egon.cola.platform.rbac3.admin.authorization.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.idp.starter.security.RequiresServiceScope;
import top.egon.cola.platform.rbac3.admin.authorization.service.AuthorizationDecisionService;
import top.egon.cola.platform.rbac3.admin.snapshot.application.SystemAuthorizationSnapshotService;
import top.egon.cola.platform.rbac3.admin.tenant.domain.TenantContext;
import top.egon.cola.platform.rbac3.contract.authorization.SessionAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.shared.domain.vo.ApiEnvelopeVO;
import top.egon.cola.platform.rbac3.admin.authorization.domain.dto.AuthorizationFenceRequestDTO;
import top.egon.cola.platform.rbac3.admin.authorization.domain.dto.DecisionRequestDTO;
import top.egon.cola.platform.rbac3.admin.authorization.domain.dto.ResourceAccessDecisionRequestDTO;
import top.egon.cola.platform.rbac3.admin.authorization.domain.vo.DecisionBundleVO;
import top.egon.cola.platform.rbac3.admin.authorization.domain.vo.FenceVerificationVO;
import top.egon.cola.platform.rbac3.admin.authorization.domain.vo.ResourceAccessDecisionResponseVO;

/**
 * 暴露给平台内部受信服务的 RBAC3 授权快照与判定接口。
 * RBAC3 authorization snapshot and decision endpoints exposed to trusted platform services.
 * 语义与用法：将 `InternalAuthorizationController` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `InternalAuthorizationController` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@RestController
@RequestMapping("/internal/v1/authorization")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "internal-authorization",
        name = "内部授权决策接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/internal/v1")
public class InternalAuthorizationController {

    /** 类型化授权判定服务。 / Typed authorization-decision service.
     * 含义与用法：读取、传递或更新 `service` 时应保持 `InternalAuthorizationController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `service`, preserve `InternalAuthorizationController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AuthorizationDecisionService service;
    /** 系统级授权快照服务。 / System-level authorization snapshot service.
     * 含义与用法：读取、传递或更新 `systemSnapshots` 时应保持 `InternalAuthorizationController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `systemSnapshots`, preserve `InternalAuthorizationController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final SystemAuthorizationSnapshotService systemSnapshots;

    /**
     * 创建内部授权控制器。
     * Creates the internal authorization controller.
     *
     * @param service 类型化授权判定服务 / typed authorization-decision service
     * @param systemSnapshots 系统级授权快照服务 / system-level authorization snapshot service
     * 用法：通过 `InternalAuthorizationController` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `InternalAuthorizationController`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    public InternalAuthorizationController(
            AuthorizationDecisionService service,
            SystemAuthorizationSnapshotService systemSnapshots) {
        this.service = Objects.requireNonNull(service, "service");
        this.systemSnapshots = Objects.requireNonNull(systemSnapshots, "systemSnapshots");
    }

    /**
     * 按 IdP 主体和目标系统读取跨租户系统授权上下文。
     * Loads a cross-tenant system authorization context by IdP subject and target system.
     * OAuth 来源应用编码与 RBAC3 系统编码属于不同命名空间；服务权限由 IdP Scope 与精确租户共同约束。
     * OAuth source-app and RBAC3 system codes use separate namespaces; IdP scope and the exact
     * tenant jointly constrain service access.
     *
     * @param tenantId 目标租户 / target tenant
     * @param sessionId IdP 会话标识 / IdP session identifier
     * @param systemCode 目标系统编码 / target system code
     * @param identitySub IdP 稳定主体标识 / stable IdP subject
     * @param principal 已认证调用服务 / authenticated calling service
     * @return 系统授权快照 / system authorization snapshot
     */
    @GetMapping("/contexts/{tenantId}/{sessionId}")
    @RequiresServiceScope("service:authorization:snapshot")
    @GatewayOperation(name = "rbac3-internal-system-snapshot-v1",
            summary = "按IdP身份和系统读取会话授权上下文",
            externalAccessible = false, tags = {"rbac3", "internal", "authorization"})
    public ApiEnvelopeVO<SystemAuthorizationSnapshot> systemSnapshot(
            @PathVariable("tenantId") String tenantId,
            @PathVariable("sessionId") String sessionId,
            @RequestParam("systemCode") String systemCode,
            @RequestParam("identitySub") String identitySub,
            @AuthenticationPrincipal ServiceIdentityPrincipal principal) {
        if (!principal.tenantId().equals(tenantId)) {
            throw new Rbac3RuleViolation("SERVICE_IDENTITY_DENIED");
        }
        return ApiEnvelopeVO.success(systemSnapshots.snapshot(
                tenantId, sessionId, systemCode, identitySub));
    }

    /**
     * 冷加载当前租户中调用服务绑定应用的会话授权快照。
     * Cold-loads the session authorization snapshot for the caller-bound application in the current tenant.
     *
     * @param sessionId 会话标识 / session identifier
     * @param principal 已认证调用服务 / authenticated calling service
     * @return 应用受限会话快照 / application-bound session snapshot
     * 用法：调用 `snapshot` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `snapshot`, then continue the business flow using its result, exception, or side effect.
     */
    @GetMapping("/sessions/{sessionId}/snapshot")
    @RequiresServiceScope("service:authorization:snapshot")
    @GatewayOperation(name = "rbac3-internal-session-snapshot-v1",
            summary = "按服务绑定应用冷加载会话授权快照",
            externalAccessible = false, tags = {"rbac3", "internal", "authorization"})
    public ApiEnvelopeVO<SessionAuthorizationSnapshot> snapshot(
            @PathVariable String sessionId,
            @AuthenticationPrincipal ServiceIdentityPrincipal principal) {
        return ApiEnvelopeVO.success(service.snapshot(principal, tenantId(), sessionId));
    }

    /**
     * 在一致会话快照上执行类型化授权判定。
     * Executes a typed authorization decision against a consistent session snapshot.
     *
     * @param request 类型化授权请求 / typed authorization request
     * @param principal 已认证调用服务 / authenticated calling service
     * @return 函数、数据和字段判定组合 / function, data, and field decision bundle
     * 用法：调用 `decide` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `decide`, then continue the business flow using its result, exception, or side effect.
     */
    @PostMapping("/decisions")
    @RequiresServiceScope("service:authorization:decide")
    @GatewayOperation(name = "rbac3-internal-authorization-decision-v1",
            summary = "使用一致会话快照执行类型化授权决策",
            externalAccessible = false, tags = {"rbac3", "internal", "authorization"})
    public ApiEnvelopeVO<DecisionBundleVO> decide(
            @Valid @RequestBody DecisionRequestDTO request,
            @AuthenticationPrincipal ServiceIdentityPrincipal principal) {
        return ApiEnvelopeVO.success(service.decide(principal, request));
    }

    /**
     * 判定 IdP 用户是否具备目标 Resource Server 应用的入口权限。
     * Decides whether an IdP user has the entry permission for a target Resource Server application.
     *
     * @param request 最小资源入口请求 / minimal resource-entry request
     * @param principal 已认证的 IdP 调用服务 / authenticated IdP calling service
     * @return 仅含判定、原因和授权版本的响应 / response containing only decision, reason, and versions
     * 用法：调用 `decideResourceAccess` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `decideResourceAccess`, then continue the business flow using its result, exception, or side effect.
     */
    @PostMapping("/resource-access-decisions")
    @RequiresServiceScope("service:authorization:decide")
    @GatewayOperation(name = "rbac3-internal-resource-access-decision-v1",
            summary = "判定用户是否具备Resource Server应用入口权限",
            externalAccessible = false, tags = {"rbac3", "internal", "authorization"})
    public ApiEnvelopeVO<ResourceAccessDecisionResponseVO> decideResourceAccess(
            @Valid @RequestBody ResourceAccessDecisionRequestDTO request,
            @AuthenticationPrincipal ServiceIdentityPrincipal principal) {
        systemSnapshots.snapshot(
                request.tid(),
                request.sid(),
                request.rbacApplicationCode(),
                request.identitySub()
        );
        return ApiEnvelopeVO.success(ResourceAccessDecisionResponseVO.from(
                service.decideResourceAccess(principal, request.toCommand())));
    }

    /**
     * 校验当前租户会话是否仍处于授权传播 Fence 中。
     * Verifies whether a current-tenant session is still behind an authorization propagation fence.
     *
     * @param request Fence 校验请求 / fence-verification request
     * @param principal 已认证调用服务 / authenticated calling service
     * @return Fence 校验结果 / fence-verification result
     * 用法：调用 `verifyFence` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `verifyFence`, then continue the business flow using its result, exception, or side effect.
     */
    @PostMapping("/fences/verify")
    @RequiresServiceScope("service:authorization:fence")
    @GatewayOperation(name = "rbac3-internal-authorization-fence-verify-v1",
            summary = "校验会话授权传播 Fence",
            externalAccessible = false, tags = {"rbac3", "internal", "authorization"})
    public ApiEnvelopeVO<FenceVerificationVO> verifyFence(
            @Valid @RequestBody AuthorizationFenceRequestDTO request,
            @AuthenticationPrincipal ServiceIdentityPrincipal principal) {
        return ApiEnvelopeVO.success(service.verifyFence(
                principal, tenantId(), request.sessionId()));
    }

    /**
     * 获取当前请求上下文的有效租户标识。
     * Returns the effective tenant identifier from the current request context.
     *
     * @return 有效租户标识 / effective tenant identifier
     * 用法：调用 `tenantId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `tenantId`, then continue the business flow using its result, exception, or side effect.
     */
    private static String tenantId() {
        return TenantContext.requireCurrent().effectiveTenantId();
    }

    }
