package top.egon.cola.platform.rbac3.admin.iam.user.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.idp.starter.security.RequiresServiceScope;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.dto.IdentityMembershipResolveRequestDTO;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.exception.IdentityMembershipNotFoundException;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.vo.TenantMembershipResponseVO;
import top.egon.cola.platform.rbac3.admin.iam.user.service.IdentityMembershipFacade;
import top.egon.cola.platform.rbac3.admin.shared.domain.vo.ApiEnvelopeVO;

import java.util.List;

/** Internal RBAC membership resolution under the IAM URI family. */
@RestController
@RequestMapping("/internal/v1/iam/users")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "internal-iam-user",
        name = "IAM用户内部成员接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/internal/v1")
public class InternalUserController {

    private final IdentityMembershipFacade memberships;

    public InternalUserController(IdentityMembershipFacade memberships) {
        this.memberships = memberships;
    }

    @GetMapping("/{identitySub}/tenants")
    @RequiresServiceScope("service:identity:resolve")
    @GatewayOperation(name = "rbac3-internal-iam-user-tenants-v1", summary = "查询身份可访问租户",
            externalAccessible = false, tags = {"rbac3", "iam", "internal"})
    public ApiEnvelopeVO<List<TenantMembershipResponseVO>> tenants(
            @PathVariable String identitySub) {
        return ApiEnvelopeVO.success(memberships.tenants(identitySub).stream()
                .map(membership -> TenantMembershipResponseVO.from(identitySub, membership))
                .toList());
    }

    @PostMapping("/resolve")
    @RequiresServiceScope("service:identity:resolve")
    @GatewayOperation(name = "rbac3-internal-iam-user-resolve-v1", summary = "解析身份租户成员关系",
            externalAccessible = false, tags = {"rbac3", "iam", "internal"})
    public ApiEnvelopeVO<TenantMembershipResponseVO> resolve(
            @Valid @RequestBody IdentityMembershipResolveRequestDTO request) {
        return memberships.resolve(request.identitySub(), request.tenantId())
                .map(value -> TenantMembershipResponseVO.from(request.identitySub(), value))
                .map(ApiEnvelopeVO::success)
                .orElseThrow(() -> new IdentityMembershipNotFoundException(
                        request.identitySub(), request.tenantId()));
    }
}
