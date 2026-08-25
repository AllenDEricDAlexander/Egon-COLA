package top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.domain.dto.ReplaceRoleResourcesRequestDTO;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.domain.vo.RoleResourceGrantMutationVO;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.domain.vo.RoleResourceGrantTreeVO;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.service.RoleResourceGrantService;
import top.egon.cola.platform.rbac3.admin.config.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.config.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.shared.domain.DatabaseClock;

/** Resource-first role authorization endpoints; permission characters stay server-side. */
@RestController
@RequestMapping("/api/rbac3/v1/iam/roles/{roleId}/resources")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "role-resource-grant",
        name = "角色资源授权接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public class RoleResourceGrantController {

    private final RoleResourceGrantService service;
    private final DatabaseClock clock;

    public RoleResourceGrantController(
            RoleResourceGrantService service,
            DatabaseClock clock) {
        this.service = service;
        this.clock = clock;
    }

    @GetMapping
    @RequiresRbac3Permission(permission = "system:role-resource:read")
    @GatewayOperation(
            name = "rbac3-role-resource-tree-v1",
            summary = "查询角色资源树",
            externalAccessible = true,
            tags = {"rbac3", "role", "resource"})
    public ResultRecord<RoleResourceGrantTreeVO> tree(@PathVariable String roleId) {
        CurrentRbac3Principal principal = CurrentRbac3Principal.requireCurrent();
        return ResultRecord.success(service.tree(
                principal.tenantId(), roleId, clock.transactionNow()));
    }

    @PutMapping
    @RequiresRbac3Permission(permission = "system:role-resource:manage")
    @GatewayOperation(
            name = "rbac3-role-resource-replace-v1",
            summary = "原子替换角色资源授权",
            externalAccessible = true,
            tags = {"rbac3", "role", "resource"})
    public ResultRecord<RoleResourceGrantMutationVO> replace(
            @PathVariable String roleId,
            @RequestBody ReplaceRoleResourcesRequestDTO request) {
        CurrentRbac3Principal principal = CurrentRbac3Principal.requireCurrent();
        return ResultRecord.success(service.replace(
                principal.tenantId(), roleId, request, principal.userId(),
                clock.transactionNow()));
    }
}
