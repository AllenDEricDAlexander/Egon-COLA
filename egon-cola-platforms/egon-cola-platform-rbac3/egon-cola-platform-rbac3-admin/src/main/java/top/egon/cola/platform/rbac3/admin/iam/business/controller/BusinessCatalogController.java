package top.egon.cola.platform.rbac3.admin.iam.business.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.config.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.iam.business.service.ApplicationCatalogEntry;
import top.egon.cola.platform.rbac3.admin.iam.business.service.BusinessCatalogEntry;
import top.egon.cola.platform.rbac3.admin.iam.business.service.BusinessCatalogService;
import top.egon.cola.platform.rbac3.admin.shared.domain.vo.ApiEnvelopeVO;

import java.util.List;

/** Read-only DDC Business/Application catalog endpoints. */
@RestController
@RequestMapping("/api/rbac3/v1/iam/catalog")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "business-catalog",
        name = "业务域与应用目录接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public final class BusinessCatalogController {

    private final BusinessCatalogService service;

    public BusinessCatalogController(BusinessCatalogService service) {
        this.service = service;
    }

    @GetMapping("/businesses")
    @RequiresRbac3Permission(permission = "system:business:read")
    @GatewayOperation(
            name = "rbac3-business-catalog-list-v1",
            summary = "查询 DDC 业务域目录",
            externalAccessible = true,
            tags = {"rbac3", "business"})
    public ApiEnvelopeVO<List<BusinessCatalogEntry>> businesses(
            @RequestParam(name = "keyword", required = false) String keyword) {
        return ApiEnvelopeVO.success(service.businesses(keyword));
    }

    @GetMapping("/businesses/{ddcBusinessId}/applications")
    @RequiresRbac3Permission(permission = "system:application:read")
    @GatewayOperation(
            name = "rbac3-business-catalog-applications-v1",
            summary = "查询 DDC 业务域下的应用目录",
            externalAccessible = true,
            tags = {"rbac3", "business", "application"})
    public ApiEnvelopeVO<List<ApplicationCatalogEntry>> applications(
            @PathVariable String ddcBusinessId,
            @RequestParam(name = "keyword", required = false) String keyword) {
        return ApiEnvelopeVO.success(service.applications(ddcBusinessId, keyword));
    }
}
