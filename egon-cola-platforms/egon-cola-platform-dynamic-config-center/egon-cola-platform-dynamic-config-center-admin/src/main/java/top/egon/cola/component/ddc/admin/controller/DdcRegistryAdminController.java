package top.egon.cola.component.ddc.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.ddc.admin.service.DdcManagementFacade;
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
            @RequestParam("env") String env,
            @RequestParam("namespace") String namespace,
            @RequestParam("serviceKind") String serviceKind,
            @RequestParam("protocol") String protocol,
            @RequestParam(value = "serviceName", required = false)
            String serviceName,
            @RequestParam(value = "group", required = false) String group,
            @RequestParam(value = "version", required = false) String version
    ) {
        return ResultRecord.success(facade.getServiceKeys(query(
                env,
                namespace,
                serviceKind,
                protocol,
                serviceName,
                group,
                version
        )));
    }

    @GetMapping("/instances")
    public ResultRecord<DdcManagementServiceSnapshot> instances(
            @RequestParam("env") String env,
            @RequestParam("namespace") String namespace,
            @RequestParam("serviceKind") String serviceKind,
            @RequestParam("protocol") String protocol,
            @RequestParam("serviceName") String serviceName,
            @RequestParam(value = "group", required = false) String group,
            @RequestParam(value = "version", required = false) String version
    ) {
        return ResultRecord.success(facade.getInstances(query(
                env,
                namespace,
                serviceKind,
                protocol,
                serviceName,
                group,
                version
        )));
    }

    private DdcManagementServiceQuery query(
            String env,
            String namespace,
            String serviceKind,
            String protocol,
            String serviceName,
            String group,
            String version
    ) {
        return new DdcManagementServiceQuery(
                env,
                namespace,
                serviceKind,
                protocol,
                serviceName,
                group,
                version
        );
    }
}
