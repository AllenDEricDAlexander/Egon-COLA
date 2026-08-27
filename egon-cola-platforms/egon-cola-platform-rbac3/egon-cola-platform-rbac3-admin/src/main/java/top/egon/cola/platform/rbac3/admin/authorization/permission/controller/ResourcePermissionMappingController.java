package top.egon.cola.platform.rbac3.admin.authorization.permission.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.gateway.openapi.annotation.EgonGatewayPolicy;import io.swagger.v3.oas.annotations.Operation;
import top.egon.cola.platform.rbac3.admin.authorization.permission.domain.dto.UpdateResourcePermissionMappingRequestDTO;
import top.egon.cola.platform.rbac3.admin.authorization.permission.domain.vo.ResourcePermissionMappingVO;
import top.egon.cola.platform.rbac3.admin.authorization.permission.service.ResourcePermissionMappingService;
import top.egon.cola.platform.rbac3.admin.shared.domain.DatabaseClock;
import top.egon.cola.platform.rbac3.starter.security.CurrentRbac3User;
import top.egon.cola.platform.rbac3.starter.security.Rbac3UserDetails;
import top.egon.cola.platform.rbac3.starter.security.RequiresPermission;

/** Dedicated administrator endpoint for the sole actual resource-permission mapping write path. */
@RestController
@RequestMapping("/api/rbac3/v1/iam/resources/{resourceId}/permission-mapping")
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
    @RequiresPermission(value = "system:resource-permission:read")
    @Operation(
            operationId = "rbac3-resource-permission-mapping-get-v1",
            summary = "查询资源实际权限映射",
            tags = {"rbac3", "resource", "permission"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<ResourcePermissionMappingVO> get(@PathVariable String resourceId) {
        return ResultRecord.success(service.get(resourceId, clock.transactionNow()));
    }

    @PutMapping
    @RequiresPermission(value = "system:resource-permission:manage")
    @Operation(
            operationId = "rbac3-resource-permission-mapping-update-v1",
            summary = "确认资源实际权限映射",
            tags = {"rbac3", "resource", "permission"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<ResourcePermissionMappingVO> update(
            @PathVariable String resourceId,
            @Valid @RequestBody UpdateResourcePermissionMappingRequestDTO request) {
        Rbac3UserDetails principal = new CurrentRbac3User().require();
        return ResultRecord.success(service.update(
                resourceId, request, principal.rbac3UserId(), clock.transactionNow()));
    }
}
