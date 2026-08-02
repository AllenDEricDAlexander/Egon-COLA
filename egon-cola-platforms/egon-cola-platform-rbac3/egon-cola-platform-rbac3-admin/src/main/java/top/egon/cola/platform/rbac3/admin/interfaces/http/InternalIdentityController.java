package top.egon.cola.platform.rbac3.admin.interfaces.http;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.application.port.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.identity.application.IdentityMappingFacade;
import top.egon.cola.platform.rbac3.admin.security.RequiresRbac3ServicePermission;

import java.util.List;

/** Trusted service endpoints used by the IdP to resolve tenant membership. */
@RestController
@RequestMapping("/internal/v1/identity")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "internal-identity",
        name = "统一身份内部映射接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/internal/v1")
public class InternalIdentityController {

    private final IdentityMappingFacade facade;
    private final DatabaseClock databaseClock;

    public InternalIdentityController(
            IdentityMappingFacade facade, DatabaseClock databaseClock) {
        this.facade = facade;
        this.databaseClock = databaseClock;
    }

    @GetMapping("/{identitySub}/tenants")
    @RequiresRbac3ServicePermission(permission = "service:identity:resolve")
    @GatewayOperation(
            name = "rbac3-internal-identity-tenants-v1",
            summary = "查询全局身份可访问的租户",
            externalAccessible = false,
            tags = {"rbac3", "identity", "internal"})
    public ApiEnvelope<List<IdentityMappingFacade.TenantMembership>> tenants(
            @PathVariable String identitySub,
            @RequestParam String clientId) {
        return ApiEnvelope.success(facade.tenants(identitySub, clientId));
    }

    @PostMapping("/resolve")
    @RequiresRbac3ServicePermission(permission = "service:identity:resolve")
    @GatewayOperation(
            name = "rbac3-internal-identity-resolve-v1",
            summary = "解析全局身份的租户成员关系",
            externalAccessible = false,
            tags = {"rbac3", "identity", "internal"})
    public ApiEnvelope<IdentityMappingFacade.ResolvedMembership> resolve(
            @Valid @RequestBody ResolveRequest request) {
        return facade.resolve(
                        request.identitySub(), request.tenantId(), request.clientId())
                .map(ApiEnvelope::success)
                .orElseThrow(() -> new IdentityMembershipNotFoundException(
                        request.identitySub(), request.tenantId()));
    }

    @PostMapping("/bindings")
    @RequiresRbac3ServicePermission(permission = "service:identity:bind")
    @GatewayOperation(
            name = "rbac3-internal-identity-bind-v1",
            summary = "绑定全局身份与租户用户",
            externalAccessible = false,
            tags = {"rbac3", "identity", "internal"})
    public ApiEnvelope<IdentityMappingFacade.Mapping> bind(
            @Valid @RequestBody BindRequest request) {
        return ApiEnvelope.success(facade.bind(
                request.tenantId(), request.identitySub(), request.rbac3UserId(),
                request.actorId(), databaseClock.transactionNow()));
    }

    public record ResolveRequest(
            @NotBlank String identitySub,
            @NotBlank String tenantId,
            @NotBlank String clientId
    ) {
    }

    public record BindRequest(
            @NotBlank String tenantId,
            @NotBlank String identitySub,
            @NotBlank String rbac3UserId,
            @NotBlank String actorId
    ) {
    }

    public static final class IdentityMembershipNotFoundException
            extends IllegalStateException {

        IdentityMembershipNotFoundException(String identitySub, String tenantId) {
            super("active identity membership not found: identitySub="
                    + identitySub + ", tenantId=" + tenantId);
        }
    }
}
