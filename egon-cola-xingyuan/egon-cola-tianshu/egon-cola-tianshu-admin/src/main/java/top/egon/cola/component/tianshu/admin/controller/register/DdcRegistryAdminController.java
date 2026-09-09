package top.egon.cola.component.tianshu.admin.controller.register;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.tianshu.admin.service.management.DdcManagementFacade;
import top.egon.cola.component.tianshu.admin.service.management.DdcRegistryAdminPageService;
import top.egon.cola.component.tianshu.admin.support.DdcAdminPageSupport;
import top.egon.cola.component.tianshu.model.management.DdcManagementServiceCatalog;
import top.egon.cola.component.tianshu.model.management.DdcManagementServiceInstance;
import top.egon.cola.component.tianshu.model.management.DdcManagementServiceKey;
import top.egon.cola.component.tianshu.model.management.DdcManagementServiceQuery;
import top.egon.cola.component.tianshu.model.management.DdcManagementServiceSnapshot;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;

@RestController
@RequestMapping("/api/v1/tianshu/registry")
@Tag(name = "tianshu-admin-tianshu-registry-admin-controller", description = "DdcRegistryAdminController 管理接口组")
@EgonApiCatalog(
        businessDomainCode = "xingyuan",
        businessDomainName = "平台治理域",
        entityDomainCode = "tianshu-admin",
        entityDomainName = "Dynamic Config Center 管理实体域",
        interfaceGroupCode = "tianshu"
)
public class DdcRegistryAdminController {

    private final DdcManagementFacade facade;

    private final DdcRegistryAdminPageService pageService;

    public DdcRegistryAdminController(
            DdcManagementFacade facade,
            DdcRegistryAdminPageService pageService
    ) {
        this.facade = facade;
        this.pageService = pageService;
    }

    @Operation(operationId = "tianshu.ddcRegistryAdminController.services")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/services")
    public ResultRecord<DdcManagementServiceCatalog> services(
            @RequestParam(value = "bizCode", required = false) String bizCode,
            @RequestParam(value = "namespaceCode", required = false)
            String namespaceCode,
            @RequestParam(value = "env", required = false) String env,
            @RequestParam(value = "appCode", required = false) String appCode,
            @RequestParam(value = "serviceKind", required = false)
            String serviceKind,
            @RequestParam(value = "protocol", required = false) String protocol,
            @RequestParam(value = "serviceName", required = false)
            String serviceName,
            @RequestParam(value = "group", required = false) String group,
            @RequestParam(value = "version", required = false) String version
    ) {
        return ResultRecord.success(facade.getServiceKeys(query(
                bizCode,
                namespaceCode,
                env,
                appCode,
                serviceKind,
                protocol,
                serviceName,
                group,
                version
        )));
    }

    @Operation(operationId = "tianshu.ddcRegistryAdminController.pageServices")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/services/page")
    public PageResultRecord<DdcManagementServiceKey> pageServices(
            @RequestParam(value = "bizCode", required = false) String bizCode,
            @RequestParam(value = "namespaceCode", required = false)
            String namespaceCode,
            @RequestParam(value = "env", required = false) String env,
            @RequestParam(value = "appCode", required = false) String appCode,
            @RequestParam(value = "serviceKind", required = false)
            String serviceKind,
            @RequestParam(value = "protocol", required = false) String protocol,
            @RequestParam(value = "serviceName", required = false)
            String serviceName,
            @RequestParam(value = "group", required = false) String group,
            @RequestParam(value = "version", required = false) String version,
            PageQuery pageQuery
    ) {
        return DdcAdminPageSupport.result(pageService.pageServices(
                query(
                        bizCode, namespaceCode, env, appCode, serviceKind,
                        protocol, serviceName, group, version
                ),
                pageQuery
        ));
    }

    @Operation(operationId = "tianshu.ddcRegistryAdminController.instances")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/instances")
    public ResultRecord<DdcManagementServiceSnapshot> instances(
            @RequestParam("bizCode") String bizCode,
            @RequestParam("env") String env,
            @RequestParam("appCode") String appCode,
            @RequestParam("serviceKind") String serviceKind,
            @RequestParam("protocol") String protocol,
            @RequestParam("serviceName") String serviceName,
            @RequestParam(value = "group", required = false) String group,
            @RequestParam(value = "version", required = false) String version
    ) {
        return ResultRecord.success(facade.getInstances(query(
                bizCode,
                null,
                env,
                appCode,
                serviceKind,
                protocol,
                serviceName,
                group,
                version
        )));
    }

    @Operation(operationId = "tianshu.ddcRegistryAdminController.pageInstances")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/instances/page")
    public PageResultRecord<DdcManagementServiceInstance> pageInstances(
            @RequestParam("bizCode") String bizCode,
            @RequestParam("env") String env,
            @RequestParam("appCode") String appCode,
            @RequestParam("serviceKind") String serviceKind,
            @RequestParam("protocol") String protocol,
            @RequestParam("serviceName") String serviceName,
            @RequestParam(value = "group", required = false) String group,
            @RequestParam(value = "version", required = false) String version,
            PageQuery pageQuery
    ) {
        return DdcAdminPageSupport.result(pageService.pageInstances(
                query(
                        bizCode, null, env, appCode, serviceKind,
                        protocol, serviceName, group, version
                ),
                pageQuery
        ));
    }

    private DdcManagementServiceQuery query(
            String bizCode,
            String namespaceCode,
            String env,
            String appCode,
            String serviceKind,
            String protocol,
            String serviceName,
            String group,
            String version
    ) {
        return new DdcManagementServiceQuery(
                bizCode,
                namespaceCode,
                env,
                appCode,
                serviceKind,
                protocol,
                serviceName,
                group,
                version
        );
    }
}
