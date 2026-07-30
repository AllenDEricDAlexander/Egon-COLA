package top.egon.cola.platform.rbac3.admin.interfaces.http;

import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.application.port.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.resource.application.ApplicationResourceFacade;
import top.egon.cola.platform.rbac3.admin.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.tenant.TenantContext;

import java.util.List;

@RestController
@RequestMapping("/api/rbac3/v1")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "application-resource",
        name = "应用与资源接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public class ApplicationResourceController {

    private final ApplicationResourceFacade facade;
    private final DatabaseClock databaseClock;

    public ApplicationResourceController(
            ApplicationResourceFacade facade,
            DatabaseClock databaseClock) {
        this.facade = facade;
        this.databaseClock = databaseClock;
    }

    @GetMapping("/applications")
    @RequiresRbac3Permission(permission = "system:application:read")
    @GatewayOperation(
            name = "rbac3-application-list-v1",
            summary = "查询租户应用",
            externalAccessible = true,
            tags = {"rbac3", "application"})
    public ApiEnvelope<List<ApplicationResourceFacade.ApplicationView>> applications() {
        return ApiEnvelope.success(facade.applications(tenantId()));
    }

    @GetMapping("/applications/{applicationId}/resources")
    @RequiresRbac3Permission(permission = "system:resource:read")
    @GatewayOperation(
            name = "rbac3-application-resource-list-v1",
            summary = "查询应用资源",
            externalAccessible = true,
            tags = {"rbac3", "resource"})
    public ApiEnvelope<List<ApplicationResourceFacade.ResourceView>> resources(
            @PathVariable String applicationId) {
        return ApiEnvelope.success(facade.resources(tenantId(), applicationId));
    }

    @PostMapping("/resources/{resourceId}/archive")
    @RequiresRbac3Permission(permission = "system:resource:archive")
    @GatewayOperation(
            name = "rbac3-resource-archive-v1",
            summary = "归档已失效资源",
            externalAccessible = true,
            tags = {"rbac3", "resource"})
    public ApiEnvelope<ApplicationResourceFacade.ArchiveResult> archive(
            @PathVariable String resourceId,
            @Valid @RequestBody ArchiveResourceRequest request,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelope.success(facade.archive(
                tenantId(),
                resourceId,
                request.expectedVersion(),
                principal.userId(),
                databaseClock.transactionNow()));
    }

    private static String tenantId() {
        return TenantContext.requireCurrent().effectiveTenantId();
    }

    public record ArchiveResourceRequest(@PositiveOrZero long expectedVersion) {
    }
}
