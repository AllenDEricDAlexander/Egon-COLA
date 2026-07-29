package top.egon.cola.component.ddc.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.ddc.admin.service.DdcServiceRegistryService;
import top.egon.cola.component.ddc.model.dto.DdcServiceLeaseRequest;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;

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
            @RequestParam("env") String env,
            @RequestParam("namespace") String namespace,
            @RequestParam("serviceKind") DdcServiceKind serviceKind,
            @RequestParam("serviceName") String serviceName,
            @RequestParam(value = "group", required = false) String group,
            @RequestParam(value = "version", required = false) String version,
            @RequestParam("protocol") String protocol) {
        return ResultRecord.success(registryService.getInstances(new DdcServiceKey(
                env,
                namespace,
                serviceKind,
                serviceName,
                group,
                version,
                protocol
        )));
    }

    @GetMapping("/services")
    public ResultRecord<DdcServiceCatalogSnapshot> services(
            @RequestParam("env") String env,
            @RequestParam("namespace") String namespace,
            @RequestParam("serviceKind") DdcServiceKind serviceKind,
            @RequestParam("protocol") String protocol,
            @RequestParam(value = "serviceName", required = false) String serviceName,
            @RequestParam(value = "group", required = false) String group,
            @RequestParam(value = "version", required = false) String version) {
        return ResultRecord.success(registryService.getServiceKeys(new DdcServiceQuery(
                env,
                namespace,
                serviceKind,
                protocol,
                serviceName,
                group,
                version
        )));
    }
}
