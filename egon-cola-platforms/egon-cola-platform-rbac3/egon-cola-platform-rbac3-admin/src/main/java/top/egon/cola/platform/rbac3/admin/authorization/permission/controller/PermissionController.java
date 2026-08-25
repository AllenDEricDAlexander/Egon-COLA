package top.egon.cola.platform.rbac3.admin.authorization.permission.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.starter.security.CurrentRbac3User;
import top.egon.cola.platform.rbac3.starter.security.Rbac3UserDetails;
import top.egon.cola.platform.rbac3.starter.security.RequiresPermission;
import top.egon.cola.platform.rbac3.admin.authorization.permission.domain.dto.ChangePermissionStatusRequestDTO;
import top.egon.cola.platform.rbac3.admin.authorization.permission.domain.dto.CreatePermissionRequestDTO;
import top.egon.cola.platform.rbac3.admin.authorization.permission.domain.vo.PermissionCatalogVO;
import top.egon.cola.platform.rbac3.admin.authorization.permission.service.PermissionCatalogService;
import top.egon.cola.component.common.core.pojo.ResultRecord;

import java.util.List;

/** Global permission catalog CRUD and ACTIVE selector endpoints. */
@RestController
@RequestMapping("/api/rbac3/v1/iam/permissions")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public class PermissionController {

    private final PermissionCatalogService service;

    public PermissionController(PermissionCatalogService service) {
        this.service = service;
    }

    @GetMapping
    @RequiresPermission(value = "system:permission:read")
    @GatewayOperation(name = "rbac3-permission-list-v1", summary = "查询全局权限字符",
            externalAccessible = true, tags = {"rbac3", "permission"})
    public ResultRecord<List<PermissionCatalogVO>> list(
            @RequestParam String applicationId,
            @RequestParam(defaultValue = "false") boolean assignable) {
        return ResultRecord.success(service.list(applicationId, assignable));
    }

    @GetMapping("/{id}")
    @RequiresPermission(value = "system:permission:read")
    public ResultRecord<PermissionCatalogVO> find(@PathVariable String id) {
        return ResultRecord.success(service.find(id));
    }

    @PostMapping
    @RequiresPermission(value = "system:permission:manage")
    public ResultRecord<PermissionCatalogVO> create(
            @Valid @RequestBody CreatePermissionRequestDTO command
            ) {
        return ResultRecord.success(service.create(
                command, new CurrentRbac3User().require().rbac3UserId()));
    }

    @PutMapping("/{id}/status")
    @RequiresPermission(value = "system:permission:manage")
    public ResultRecord<PermissionCatalogVO> changeStatus(
            @PathVariable String id,
            @Valid @RequestBody ChangePermissionStatusRequestDTO command
            ) {
        return ResultRecord.success(service.changeStatus(
                id, command, new CurrentRbac3User().require().rbac3UserId()));
    }
}
