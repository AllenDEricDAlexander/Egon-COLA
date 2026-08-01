package top.egon.cola.platform.rbac3.admin.interfaces.http;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.runtime.application.ControlPlaneRuntimeStatusPort;
import top.egon.cola.platform.rbac3.admin.runtime.application.RuntimeQueryService;
import top.egon.cola.platform.rbac3.admin.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.tenant.TenantContext;

@RestController
@RequestMapping("/api/rbac3/v1/runtime")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "authorization-runtime",
        name = "授权运行状态接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public class RuntimeController {

    private final RuntimeQueryService service;

    public RuntimeController(RuntimeQueryService service) {
        this.service = service;
    }

    @GetMapping("/status")
    @RequiresRbac3Permission(permission = "system:authorization-runtime:read")
    @GatewayOperation(name = "rbac3-runtime-status-v1", summary = "查询授权运行状态",
            externalAccessible = true, tags = {"rbac3", "runtime"})
    public ApiEnvelope<ControlPlaneRuntimeStatusPort.RuntimeStatus> status() {
        return ApiEnvelope.success(service.status());
    }

    @GetMapping("/mutations")
    @RequiresRbac3Permission(permission = "system:authorization-runtime:read")
    @GatewayOperation(name = "rbac3-runtime-mutations-v1",
            summary = "游标查询授权 Mutation Journal",
            externalAccessible = true, tags = {"rbac3", "runtime"})
    public ApiEnvelope<RuntimeQueryService.MutationPage> mutations(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit) {
        return ApiEnvelope.success(service.mutations(
                tenantId(), status, cursor, limit));
    }

    @PostMapping("/mutations/{mutationId}/retry")
    @RequiresRbac3Permission(permission = "system:authorization-runtime:operate")
    @GatewayOperation(name = "rbac3-runtime-mutation-retry-v1",
            summary = "按 Mutation ID 触发幂等受控恢复",
            externalAccessible = true, tags = {"rbac3", "runtime"})
    public ApiEnvelope<RuntimeQueryService.RetryResult> retry(
            @PathVariable String mutationId,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelope.success(service.retry(
                tenantId(), mutationId, principal.userId()));
    }

    @GetMapping("/gateway-ddc-status")
    @RequiresRbac3Permission(permission = "system:authorization-runtime:read")
    @GatewayOperation(name = "rbac3-runtime-gateway-ddc-status-v1",
            summary = "分别查询 Definition、DDC Lease 和 Gateway Release",
            externalAccessible = true, tags = {"rbac3", "runtime", "gateway", "ddc"})
    public ApiEnvelope<ControlPlaneRuntimeStatusPort.RuntimeStatus> gatewayDdcStatus() {
        return ApiEnvelope.success(service.gatewayDdcStatus());
    }

    private static String tenantId() {
        return TenantContext.requireCurrent().effectiveTenantId();
    }
}
