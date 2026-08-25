package top.egon.cola.platform.rbac3.admin.authorization.permission.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.authorization.permission.domain.dto.UpdateResourcePermissionMappingRequestDTO;
import top.egon.cola.platform.rbac3.admin.authorization.permission.domain.vo.ResourcePermissionMappingVO;
import top.egon.cola.platform.rbac3.admin.authorization.permission.service.ResourcePermissionMappingService;
import top.egon.cola.platform.rbac3.admin.config.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.config.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.shared.domain.DatabaseClock;

/** Dedicated administrator endpoint for the sole actual resource-permission mapping write path. */
@RestController
@RequestMapping("/api/rbac3/v1/iam/resources/{resourceId}/permission-mapping")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public class ResourcePermissionMappingController {

    private final ResourcePermissionMappingService service;
    private final DatabaseClock clock;

    public ResourcePermissionMappingController(
            ResourcePermissionMappingService service,
            DatabaseClock clock) {
        this.service = service;
        this.clock = clock;
    }

    @GetMapping
    @RequiresRbac3Permission(permission = "system:resource-permission:read")
    @GatewayOperation(name = "rbac3-resource-permission-mapping-get-v1",
            summary = "查询资源实际权限映射", externalAccessible = true,
            tags = {"rbac3", "resource", "permission"})
    public ResultRecord<ResourcePermissionMappingVO> get(@PathVariable String resourceId) {
        return ResultRecord.success(service.get(resourceId, clock.transactionNow()));
    }

    @PutMapping
    @RequiresRbac3Permission(permission = "system:resource-permission:manage")
    @GatewayOperation(name = "rbac3-resource-permission-mapping-update-v1",
            summary = "确认资源实际权限映射", externalAccessible = true,
            tags = {"rbac3", "resource", "permission"})
    public ResultRecord<ResourcePermissionMappingVO> update(
            @PathVariable String resourceId,
            @Valid @RequestBody UpdateResourcePermissionMappingRequestDTO request) {
        CurrentRbac3Principal principal = CurrentRbac3Principal.requireCurrent();
        return ResultRecord.success(service.update(
                resourceId, request, principal.userId(), clock.transactionNow()));
    }
}
