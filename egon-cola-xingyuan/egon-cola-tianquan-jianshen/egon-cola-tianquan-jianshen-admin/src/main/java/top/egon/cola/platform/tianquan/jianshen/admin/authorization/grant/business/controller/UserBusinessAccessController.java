package top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.business.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.business.domain.command.ReplaceUserBusinessAccessesCommand;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.business.domain.vo.UserBusinessAccessVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.business.service.UserBusinessAccessFacade;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.business.domain.vo.UserApplicationAccessVO;
import top.egon.cola.platform.tianquan.jianshen.admin.shared.tenant.domain.TenantContext;
import top.egon.cola.platform.tianquan.jianshen.starter.security.CurrentRbac3User;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3UserDetails;
import top.egon.cola.platform.tianquan.jianshen.starter.security.RequiresPermission;

import java.util.List;

/** User Business grant and derived Application access endpoints. */
@RestController
@RequestMapping("/api/rbac3/v1/iam")
@Tag(name = "user-business-access", description = "用户业务域授权接口组")
@EgonApiCatalog(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        interfaceGroupCode = "iam"
)
public class UserBusinessAccessController {

    private final UserBusinessAccessFacade facade;

    public UserBusinessAccessController(UserBusinessAccessFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/users/{userId}/business-accesses")
    @RequiresPermission(value = "system:user-business-access:read")
    @Operation(
            operationId = "rbac3-user-business-access-list-v1",
            summary = "查询用户业务域授权",
            tags = {"rbac3", "user", "business"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<List<UserBusinessAccessVO>> accesses(
            @PathVariable Long userId) {
        return ResultRecord.success(facade.accesses(tenantId(), userId));
    }

    @PutMapping("/users/{userId}/business-accesses")
    @RequiresPermission(value = "system:user-business-access:manage")
    @Operation(
            operationId = "rbac3-user-business-access-replace-v1",
            summary = "替换用户人工业务域授权",
            tags = {"rbac3", "user", "business"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<List<UserBusinessAccessVO>> replace(
            @PathVariable Long userId,
            @Valid @RequestBody ReplaceUserBusinessAccessesCommand command
) {
        return ResultRecord.success(facade.replace(
                tenantId(), userId, new CurrentRbac3User().require().rbac3UserId(), command));
    }

    @GetMapping("/users/{userId}/application-accesses")
    @RequiresPermission(value = "system:user-application-access:read")
    @Operation(
            operationId = "rbac3-user-application-access-list-v1",
            summary = "查询用户派生应用访问范围",
            tags = {"rbac3", "user", "application"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<List<UserApplicationAccessVO>> applicationAccesses(
            @PathVariable Long userId) {
        return ResultRecord.success(facade.applicationAccesses(tenantId(), userId));
    }

    private static Long tenantId() {
        return Long.valueOf(TenantContext.requireCurrent().effectiveTenantId());
    }
}
