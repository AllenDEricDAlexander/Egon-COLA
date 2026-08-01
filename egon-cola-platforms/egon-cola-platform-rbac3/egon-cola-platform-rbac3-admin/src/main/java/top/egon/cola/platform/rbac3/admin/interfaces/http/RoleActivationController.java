package top.egon.cola.platform.rbac3.admin.interfaces.http;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.activation.application.RoleActivationCandidateService;
import top.egon.cola.platform.rbac3.admin.activation.application.RoleActivationFacade;
import top.egon.cola.platform.rbac3.admin.application.port.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.tenant.TenantContext;
import top.egon.cola.platform.rbac3.contract.activation.ActiveRoleSetView;
import top.egon.cola.platform.rbac3.contract.activation.ReplaceActiveRolesRequest;
import top.egon.cola.platform.rbac3.contract.activation.ReplaceActiveRolesResult;
import top.egon.cola.platform.rbac3.contract.activation.RoleActivationCandidateView;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@RestController
@RequestMapping("/api/rbac3/v1/auth")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "role-activation",
        name = "当前会话角色激活接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public class RoleActivationController {

    private final RoleActivationCandidateService candidateService;
    private final RoleActivationFacade facade;
    private final DatabaseClock databaseClock;

    public RoleActivationController(
            RoleActivationCandidateService candidateService,
            RoleActivationFacade facade,
            DatabaseClock databaseClock
    ) {
        this.candidateService = candidateService;
        this.facade = facade;
        this.databaseClock = databaseClock;
    }

    @GetMapping("/role-activation-candidates")
    @RequiresRbac3Permission(permission = "system:role-activation:read")
    @GatewayOperation(
            name = "rbac3-role-activation-candidates-v1",
            summary = "查询当前会话可激活的规范根角色",
            externalAccessible = true,
            tags = {"rbac3", "role-activation"})
    public ApiEnvelope<RoleActivationCandidateView> candidates(
            @AuthenticationPrincipal CurrentRbac3Principal principal
    ) {
        return ApiEnvelope.success(candidateService.candidates(
                tenantId(), principal.userId(), databaseClock.transactionNow()));
    }

    @GetMapping("/role-activations")
    @RequiresRbac3Permission(permission = "system:role-activation:read")
    @GatewayOperation(
            name = "rbac3-role-activation-current-v1",
            summary = "查询当前会话已激活的规范根角色",
            externalAccessible = true,
            tags = {"rbac3", "role-activation"})
    public ApiEnvelope<ActiveRoleSetView> current(
            @AuthenticationPrincipal CurrentRbac3Principal principal
    ) {
        return ApiEnvelope.success(facade.current(
                tenantId(), principal.userId(), principal.sessionId()));
    }

    @PutMapping("/role-activations")
    @RequiresRbac3Permission(permission = "system:role-activation:use")
    @GatewayOperation(
            name = "rbac3-role-activation-replace-v1",
            summary = "原子替换当前会话激活角色集合",
            externalAccessible = true,
            tags = {"rbac3", "role-activation"})
    public ApiEnvelope<ReplaceActiveRolesResult> replace(
            @Valid @RequestBody ReplaceActiveRolesRequest request,
            @AuthenticationPrincipal CurrentRbac3Principal principal
    ) {
        String commandId = activationCommandId(principal.sessionId(), request);
        return ApiEnvelope.success(facade.replace(new RoleActivationFacade.ReplaceCommand(
                tenantId(), principal.userId(), principal.sessionId(), request.roleIds(),
                request.expectedSessionVersion(), principal.userId(), commandId)));
    }

    private static String tenantId() {
        return TenantContext.requireCurrent().effectiveTenantId();
    }

    private static String activationCommandId(
            String sessionId,
            ReplaceActiveRolesRequest request) {
        String canonicalRoles = request.roleIds().stream()
                .sorted()
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        String canonical = sessionId + '|' + request.expectedSessionVersion()
                + '|' + canonicalRoles;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return "role-activation:" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
