package top.egon.cola.platform.rbac3.admin.interfaces.http;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.participation.application.ParticipationFacade;
import top.egon.cola.platform.rbac3.admin.security.CurrentRbac3ServicePrincipal;
import top.egon.cola.platform.rbac3.admin.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.tenant.TenantContext;
import top.egon.cola.platform.rbac3.contract.participation.BusinessParticipationCommand;

@RestController
@RequestMapping("/api/rbac3/v1/internal/business-participations")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "business-participation",
        name = "业务参与事实接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public class ParticipationController {

    private final ParticipationFacade facade;

    public ParticipationController(ParticipationFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    @RequiresRbac3Permission(permission = "service:participation:write")
    @GatewayOperation(name = "rbac3-business-participation-record-v1",
            summary = "幂等追加业务对象参与事实",
            externalAccessible = false, tags = {"rbac3", "internal", "participation"})
    public ApiEnvelope<ParticipationFacade.RecordResult> record(
            @Valid @RequestBody BusinessParticipationCommand command,
            @AuthenticationPrincipal CurrentRbac3ServicePrincipal principal) {
        return ApiEnvelope.success(facade.record(principal, tenantId(), command));
    }

    @GetMapping("/conflicts")
    @RequiresRbac3Permission(permission = "service:participation:read")
    @GatewayOperation(name = "rbac3-business-participation-conflicts-v1",
            summary = "查询同一业务对象的职责冲突证据",
            externalAccessible = false, tags = {"rbac3", "internal", "participation"})
    public ApiEnvelope<ParticipationFacade.ConflictDecision> conflicts(
            @RequestParam @NotBlank String applicationCode,
            @RequestParam @NotBlank String businessResource,
            @RequestParam @NotBlank String businessId,
            @RequestParam @NotBlank String actorUserId,
            @RequestParam @NotBlank String actionCode,
            @AuthenticationPrincipal CurrentRbac3ServicePrincipal principal) {
        return ApiEnvelope.success(facade.conflicts(
                principal, tenantId(), new ParticipationFacade.ConflictQuery(
                        applicationCode, businessResource, businessId,
                        actorUserId, actionCode)));
    }

    private static String tenantId() {
        return TenantContext.requireCurrent().effectiveTenantId();
    }
}
