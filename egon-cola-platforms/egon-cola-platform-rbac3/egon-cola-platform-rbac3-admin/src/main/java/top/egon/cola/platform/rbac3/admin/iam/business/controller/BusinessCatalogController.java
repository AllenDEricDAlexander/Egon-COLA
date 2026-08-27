package top.egon.cola.platform.rbac3.admin.iam.business.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import top.egon.cola.component.gateway.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.gateway.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.platform.rbac3.admin.iam.business.service.ApplicationCatalogEntry;
import top.egon.cola.platform.rbac3.admin.iam.business.service.BusinessCatalogEntry;
import top.egon.cola.platform.rbac3.admin.iam.business.service.BusinessCatalogService;
import top.egon.cola.platform.rbac3.starter.security.RequiresPermission;

import java.util.List;

/** Read-only DDC Business/Application catalog endpoints. */
@RestController
@RequestMapping("/api/rbac3/v1/iam/catalog")
@Tag(name = "business-catalog", description = "业务域与应用目录接口组")
@EgonApiCatalog(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        interfaceGroupCode = "business-catalog"
)
public class BusinessCatalogController {

    private final BusinessCatalogService service;

    public BusinessCatalogController(BusinessCatalogService service) {
        this.service = service;
    }

    @GetMapping("/businesses")
    @RequiresPermission(value = "system:business:read")
    @Operation(
            operationId = "rbac3-business-catalog-list-v1",
            summary = "查询 DDC 业务域目录",
            tags = {"rbac3", "business"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<List<BusinessCatalogEntry>> businesses(
            @RequestParam(name = "keyword", required = false) String keyword) {
        return ResultRecord.success(service.businesses(keyword));
    }

    @GetMapping("/businesses/{ddcBusinessId}/applications")
    @RequiresPermission(value = "system:application:read")
    @Operation(
            operationId = "rbac3-business-catalog-applications-v1",
            summary = "查询 DDC 业务域下的应用目录",
            tags = {"rbac3", "business", "application"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<List<ApplicationCatalogEntry>> applications(
            @PathVariable String ddcBusinessId,
            @RequestParam(name = "keyword", required = false) String keyword) {
        return ResultRecord.success(service.applications(ddcBusinessId, keyword));
    }
}
