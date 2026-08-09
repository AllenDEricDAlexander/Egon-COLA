package top.egon.cola.component.ddc.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.ddc.admin.service.registry.DdcServiceRegistryService;
import top.egon.cola.component.ddc.registry.model.DdcServiceLeaseRequest;
import top.egon.cola.component.ddc.registry.model.DdcServiceKind;
import top.egon.cola.component.ddc.registry.model.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.registry.model.DdcServiceKey;
import top.egon.cola.component.ddc.registry.model.DdcServiceQuery;
import top.egon.cola.component.ddc.registry.model.DdcServiceRegistration;
import top.egon.cola.component.ddc.registry.model.DdcServiceSnapshot;
import top.egon.cola.component.ddc.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.lease.DdcLeaseSession;

@RestController
@RequestMapping("/api/v1/ddc/openapi/registry")
public class DdcRegistryOpenApiController {

    private final DdcServiceRegistryService registryService;

    public DdcRegistryOpenApiController(DdcServiceRegistryService registryService) {
        this.registryService = registryService;
    }

    @PostMapping("/instances/register")
    public ResultRecord<DdcLeaseSession> register(
            @RequestBody DdcServiceRegistration registration) {
        return ResultRecord.success(registryService.register(registration));
    }

    @PostMapping("/instances/heartbeat")
    public ResultRecord<DdcLeaseOperationResult> heartbeat(
            @RequestBody DdcServiceLeaseRequest request) {
        return ResultRecord.success(registryService.heartbeat(request));
    }

    @PostMapping("/instances/deregister")
    public ResultRecord<DdcLeaseOperationResult> deregister(
            @RequestBody DdcServiceLeaseRequest request) {
        return ResultRecord.success(registryService.deregister(request));
    }

    @GetMapping("/instances")
    public ResultRecord<DdcServiceSnapshot> instances(
            @RequestParam("bizCode") String bizCode,
            @RequestParam("appCode") String appCode,
            @RequestParam("env") String env,
            @RequestParam("serviceKind") DdcServiceKind serviceKind,
            @RequestParam("serviceName") String serviceName,
            @RequestParam(value = "group", required = false) String group,
            @RequestParam(value = "version", required = false) String version,
            @RequestParam("protocol") String protocol) {
        return ResultRecord.success(registryService.getInstances(new DdcServiceKey(
                bizCode,
                env,
                appCode,
                serviceKind,
                serviceName,
                group,
                version,
                protocol
        )));
    }

    @GetMapping("/services")
    public ResultRecord<DdcServiceCatalogSnapshot> services(
            @RequestParam(value = "bizCode", required = false) String bizCode,
            @RequestParam(value = "appCode", required = false) String appCode,
            @RequestParam(value = "env", required = false) String env,
            @RequestParam(value = "serviceKind", required = false) DdcServiceKind serviceKind,
            @RequestParam(value = "protocol", required = false) String protocol,
            @RequestParam(value = "serviceName", required = false) String serviceName,
            @RequestParam(value = "group", required = false) String group,
            @RequestParam(value = "version", required = false) String version) {
        return ResultRecord.success(registryService.getServiceKeys(new DdcServiceQuery(
                bizCode,
                env,
                appCode,
                serviceKind,
                protocol,
                serviceName,
                group,
                version
        )));
    }
}
