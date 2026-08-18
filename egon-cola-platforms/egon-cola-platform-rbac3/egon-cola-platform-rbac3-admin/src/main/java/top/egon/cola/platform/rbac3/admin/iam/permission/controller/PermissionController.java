package top.egon.cola.platform.rbac3.admin.iam.permission.controller;

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
import top.egon.cola.platform.rbac3.admin.config.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.config.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.iam.permission.domain.dto.ChangePermissionStatusRequestDTO;
import top.egon.cola.platform.rbac3.admin.iam.permission.domain.dto.CreatePermissionRequestDTO;
import top.egon.cola.platform.rbac3.admin.iam.permission.domain.vo.PermissionCatalogVO;
import top.egon.cola.platform.rbac3.admin.iam.permission.service.PermissionCatalogService;
import top.egon.cola.platform.rbac3.admin.shared.domain.vo.ApiEnvelopeVO;

import java.util.List;

/** Global permission catalog CRUD and ACTIVE selector endpoints. */
@RestController
@RequestMapping("/api/rbac3/v1/iam/permissions")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public final class PermissionController {

    private final PermissionCatalogService service;

    public PermissionController(PermissionCatalogService service) {
        this.service = service;
    }

    @GetMapping
    @RequiresRbac3Permission(permission = "system:permission:read")
    @GatewayOperation(name = "rbac3-permission-list-v1", summary = "查询全局权限字符",
            externalAccessible = true, tags = {"rbac3", "permission"})
    public ApiEnvelopeVO<List<PermissionCatalogVO>> list(
            @RequestParam String applicationId,
            @RequestParam(defaultValue = "false") boolean assignable) {
        return ApiEnvelopeVO.success(service.list(applicationId, assignable));
    }

    @GetMapping("/{id}")
    @RequiresRbac3Permission(permission = "system:permission:read")
    public ApiEnvelopeVO<PermissionCatalogVO> find(@PathVariable String id) {
        return ApiEnvelopeVO.success(service.find(id));
    }

    @PostMapping
    @RequiresRbac3Permission(permission = "system:permission:manage")
    public ApiEnvelopeVO<PermissionCatalogVO> create(
            @Valid @RequestBody CreatePermissionRequestDTO command
            ) {
        return ApiEnvelopeVO.success(service.create(
                command, CurrentRbac3Principal.requireCurrent().userId()));
    }

    @PutMapping("/{id}/status")
    @RequiresRbac3Permission(permission = "system:permission:manage")
    public ApiEnvelopeVO<PermissionCatalogVO> changeStatus(
            @PathVariable String id,
            @Valid @RequestBody ChangePermissionStatusRequestDTO command
            ) {
        return ApiEnvelopeVO.success(service.changeStatus(
                id, command, CurrentRbac3Principal.requireCurrent().userId()));
    }
}
