package top.egon.cola.platform.rbac3.admin.interfaces.http;

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
import top.egon.cola.platform.rbac3.admin.authorization.application.AuthorizationDecisionService;
import top.egon.cola.platform.rbac3.admin.snapshot.application.SystemAuthorizationSnapshotService;
import top.egon.cola.platform.rbac3.admin.security.CurrentRbac3ServicePrincipal;
import top.egon.cola.platform.rbac3.admin.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.tenant.TenantContext;
import top.egon.cola.platform.rbac3.contract.authorization.SessionAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

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

    private final AuthorizationDecisionService service;
    private final SystemAuthorizationSnapshotService systemSnapshots;

    public InternalAuthorizationController(
            AuthorizationDecisionService service,
            SystemAuthorizationSnapshotService systemSnapshots) {
        this.service = service;
        this.systemSnapshots = systemSnapshots;
    }

    @GetMapping("/contexts/{tenantId}/{sessionId}")
    @RequiresRbac3Permission(permission = "service:authorization:snapshot")
    @GatewayOperation(name = "rbac3-internal-system-snapshot-v1",
            summary = "按IdP身份和系统读取会话授权上下文",
            externalAccessible = false, tags = {"rbac3", "internal", "authorization"})
    public ApiEnvelope<SystemAuthorizationSnapshot> systemSnapshot(
            @PathVariable String tenantId,
            @PathVariable String sessionId,
            @RequestParam String systemCode,
            @RequestParam String identitySub,
            @AuthenticationPrincipal CurrentRbac3ServicePrincipal principal) {
        if (!principal.tenantId().equals(tenantId)
                || !principal.applicationCode().equals(systemCode)) {
            throw new Rbac3RuleViolation("SERVICE_IDENTITY_DENIED");
        }
        return ApiEnvelope.success(systemSnapshots.snapshot(
                tenantId, sessionId, systemCode, identitySub));
    }

    @GetMapping("/sessions/{sessionId}/snapshot")
    @RequiresRbac3Permission(permission = "service:authorization:snapshot")
    @GatewayOperation(name = "rbac3-internal-session-snapshot-v1",
            summary = "按服务绑定应用冷加载会话授权快照",
            externalAccessible = false, tags = {"rbac3", "internal", "authorization"})
    public ApiEnvelope<SessionAuthorizationSnapshot> snapshot(
            @PathVariable String sessionId,
            @AuthenticationPrincipal CurrentRbac3ServicePrincipal principal) {
        return ApiEnvelope.success(service.snapshot(principal, tenantId(), sessionId));
    }

    @PostMapping("/decisions")
    @RequiresRbac3Permission(permission = "service:authorization:decide")
    @GatewayOperation(name = "rbac3-internal-authorization-decision-v1",
            summary = "使用一致会话快照执行类型化授权决策",
            externalAccessible = false, tags = {"rbac3", "internal", "authorization"})
    public ApiEnvelope<AuthorizationDecisionService.DecisionBundle> decide(
            @Valid @RequestBody AuthorizationDecisionService.DecisionRequest request,
            @AuthenticationPrincipal CurrentRbac3ServicePrincipal principal) {
        return ApiEnvelope.success(service.decide(principal, request));
    }

    @PostMapping("/fences/verify")
    @RequiresRbac3Permission(permission = "service:authorization:fence")
    @GatewayOperation(name = "rbac3-internal-authorization-fence-verify-v1",
            summary = "校验会话授权传播 Fence",
            externalAccessible = false, tags = {"rbac3", "internal", "authorization"})
    public ApiEnvelope<AuthorizationDecisionService.FenceVerification> verifyFence(
            @Valid @RequestBody FenceRequest request,
            @AuthenticationPrincipal CurrentRbac3ServicePrincipal principal) {
        return ApiEnvelope.success(service.verifyFence(
                principal, tenantId(), request.sessionId()));
    }

    private static String tenantId() {
        return TenantContext.requireCurrent().effectiveTenantId();
    }

    public record FenceRequest(@NotBlank String sessionId) {
    }
}
