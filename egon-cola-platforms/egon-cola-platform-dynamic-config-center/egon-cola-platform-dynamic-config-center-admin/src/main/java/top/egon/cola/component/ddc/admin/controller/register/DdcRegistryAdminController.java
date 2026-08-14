package top.egon.cola.component.ddc.admin.controller.register;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.ddc.admin.service.management.DdcManagementFacade;
import top.egon.cola.component.ddc.admin.service.management.DdcRegistryAdminPageService;
import top.egon.cola.component.ddc.admin.support.DdcAdminPageSupport;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceCatalog;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceInstance;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceKey;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceSnapshot;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;

@RestController
@RequestMapping("/api/v1/ddc/registry")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "ddc-admin",
        entityDomainName = "Dynamic Config Center 管理实体域",
        code = "ddc-admin-ddc-registry-admin-controller",
        name = "DdcRegistryAdminController 管理接口组")
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

    @GatewayOperation(externalAccessible = true)
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

    @GatewayOperation(externalAccessible = true)
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

    @GatewayOperation(externalAccessible = true)
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

    @GatewayOperation(externalAccessible = true)
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
