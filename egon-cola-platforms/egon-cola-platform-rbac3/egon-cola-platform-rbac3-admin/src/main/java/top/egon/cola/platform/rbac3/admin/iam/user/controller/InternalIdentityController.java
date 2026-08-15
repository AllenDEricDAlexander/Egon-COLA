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

/**
 * Trusted service endpoints for resolving direct RBAC user membership.
 */
@RestController
@RequestMapping("/internal/v1/identity")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "internal-identity",
        name = "统一身份内部成员接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/internal/v1")
public class InternalIdentityController {

    private final IdentityMembershipFacade memberships;

    public InternalIdentityController(IdentityMembershipFacade memberships) {
        this.memberships = memberships;
    }

    @GetMapping("/{identitySub}/tenants")
    @RequiresServiceScope("service:identity:resolve")
    @GatewayOperation(
            name = "rbac3-internal-identity-tenants-v1",
            summary = "查询全局身份可访问的租户",
            externalAccessible = false,
            tags = {"rbac3", "identity", "internal"})
    public ApiEnvelopeVO<List<TenantMembershipResponseVO>> tenants(
            @PathVariable("identitySub") String identitySub) {
        return ApiEnvelopeVO.success(memberships.tenants(identitySub).stream()
                .map(membership -> TenantMembershipResponseVO.from(identitySub, membership))
                .toList());
    }

    @PostMapping("/resolve")
    @RequiresServiceScope("service:identity:resolve")
    @GatewayOperation(
            name = "rbac3-internal-identity-resolve-v1",
            summary = "解析全局身份的租户成员关系",
            externalAccessible = false,
            tags = {"rbac3", "identity", "internal"})
    public ApiEnvelopeVO<TenantMembershipResponseVO> resolve(
            @Valid @RequestBody IdentityMembershipResolveRequestDTO request) {
        return memberships.resolve(request.identitySub(), request.tenantId())
                .map(value -> TenantMembershipResponseVO.from(request.identitySub(), value))
                .map(ApiEnvelopeVO::success)
                .orElseThrow(() -> new IdentityMembershipNotFoundException(
                        request.identitySub(), request.tenantId()));
    }
}
