package top.egon.cola.component.ddc.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.ddc.admin.service.management.DdcManagementFacade;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceCatalog;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceSnapshot;

@RestController
@RequestMapping("/api/v1/ddc/registry")
public class DdcRegistryAdminController {

    private final DdcManagementFacade facade;

    public DdcRegistryAdminController(DdcManagementFacade facade) {
        this.facade = facade;
    }

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
