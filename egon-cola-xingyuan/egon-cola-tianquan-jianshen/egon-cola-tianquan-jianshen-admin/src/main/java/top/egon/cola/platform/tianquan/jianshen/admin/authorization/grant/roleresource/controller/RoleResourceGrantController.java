package top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.dto.ReplaceRoleResourcesRequestDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.vo.RoleResourceGrantMutationVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.vo.RoleResourceGrantTreeVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.service.RoleResourceGrantService;
import top.egon.cola.platform.tianquan.jianshen.admin.shared.domain.DatabaseClock;
import top.egon.cola.platform.tianquan.jianshen.starter.security.CurrentRbac3User;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3UserDetails;
import top.egon.cola.platform.tianquan.jianshen.starter.security.RequiresPermission;

/** Resource-first role authorization endpoints; permission characters stay server-side. */
@RestController
@RequestMapping("/api/tianquan-jianshen/v1/iam/roles/{roleId}/resources")
@Tag(name = "role-resource-grant", description = "角色资源授权接口组")
@EgonApiCatalog(
        businessDomainCode = "xingyuan",
        businessDomainName = "平台治理域",
        entityDomainCode = "tianquan-jianshen",
        entityDomainName = "RBAC3权限实体域",
        interfaceGroupCode = "iam"
)
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
    @RequiresPermission(value = "system:role-resource:read")
    @Operation(
            operationId = "tianquan-jianshen-role-resource-tree-v1",
            summary = "查询角色资源树",
            tags = {"tianquan-jianshen", "role", "resource"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<RoleResourceGrantTreeVO> tree(
            @Parameter(description = "当前租户中待查询的角色 ID", required = true)
            @PathVariable String roleId) {
        Rbac3UserDetails principal = new CurrentRbac3User().require();
        return ResultRecord.success(service.tree(
                principal.tenantId(), roleId, clock.transactionNow()));
    }

    @PutMapping
    @RequiresPermission(value = "system:role-resource:manage")
    @Operation(
            operationId = "tianquan-jianshen-role-resource-replace-v1",
            summary = "原子替换角色资源授权",
            tags = {"tianquan-jianshen", "role", "resource"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<RoleResourceGrantMutationVO> replace(
            @Parameter(description = "当前租户中待配置授权的角色 ID", required = true)
            @PathVariable String roleId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "完整替换角色的直接资源授权，不修改角色继承关系", required = true)
            @RequestBody ReplaceRoleResourcesRequestDTO request) {
        Rbac3UserDetails principal = new CurrentRbac3User().require();
        return ResultRecord.success(service.replace(
                principal.tenantId(), roleId, request, principal.rbac3UserId(),
                clock.transactionNow()));
    }
}
