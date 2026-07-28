package top.egon.cola.component.ddc.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.pojo.ResultRecord;
import top.egon.cola.component.ddc.admin.security.DdcServicePrincipal;
import top.egon.cola.component.ddc.admin.service.DdcManagementFacade;
import top.egon.cola.component.ddc.management.model.DdcManagementConfig;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigClientInstance;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigDeleteRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigUpsertRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementInstanceQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishResult;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishTask;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceCatalog;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceSnapshot;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ddc/openapi/management")
public class DdcManagementOpenApiController {

    private final DdcManagementFacade facade;

    public DdcManagementOpenApiController(DdcManagementFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/configs/{appCode}/{env}/{namespace}/{configKey}")
    public ResultRecord<DdcManagementConfig> config(
            @PathVariable("appCode") String appCode,
            @PathVariable("env") String env,
            @PathVariable("namespace") String namespace,
            @PathVariable("configKey") String configKey
    ) {
        return ResultRecord.success(facade.findConfig(new DdcManagementConfigQuery(
                appCode,
                env,
                namespace,
                configKey
        )));
    }

    @PutMapping("/configs/{appCode}/{env}/{namespace}/{configKey}")
    public ResultRecord<DdcManagementConfig> upsert(
            @PathVariable("appCode") String appCode,
            @PathVariable("env") String env,
            @PathVariable("namespace") String namespace,
            @PathVariable("configKey") String configKey,
            @RequestBody DdcManagementConfigUpsertRequest request,
            HttpServletRequest servletRequest
    ) {
        return ResultRecord.success(facade.upsert(new DdcManagementConfigUpsertRequest(
                appCode,
                env,
                namespace,
                configKey,
                request.configValue(),
                request.valueType(),
                request.description(),
                request.expectedVersion(),
                trustedOperator(servletRequest, request.operator())
        )));
    }

    @DeleteMapping("/configs/{appCode}/{env}/{namespace}/{configKey}")
    public ResultRecord<Void> delete(
            @PathVariable("appCode") String appCode,
            @PathVariable("env") String env,
            @PathVariable("namespace") String namespace,
            @PathVariable("configKey") String configKey,
            @RequestBody DdcManagementConfigDeleteRequest request,
            HttpServletRequest servletRequest
    ) {
        facade.delete(new DdcManagementConfigDeleteRequest(
                appCode,
                env,
                namespace,
                configKey,
                request.expectedVersion(),
                trustedOperator(servletRequest, request.operator()),
                request.reason()
        ));
        return ResultRecord.success(null);
    }

    @PostMapping("/configs/{appCode}/{env}/{namespace}/{configKey}/publish")
    public ResultRecord<DdcManagementPublishResult> publish(
            @PathVariable("appCode") String appCode,
            @PathVariable("env") String env,
            @PathVariable("namespace") String namespace,
            @PathVariable("configKey") String configKey,
            @RequestBody DdcManagementPublishRequest request,
            HttpServletRequest servletRequest
    ) {
        return ResultRecord.success(facade.publish(new DdcManagementPublishRequest(
                appCode,
                env,
                namespace,
                configKey,
                request.configValue(),
                request.expectedVersion(),
                request.changeId(),
                request.timeoutMs(),
                trustedOperator(servletRequest, request.operator())
        )));
    }

    @GetMapping("/publish-tasks/{changeId}")
    public ResultRecord<DdcManagementPublishTask> task(
            @PathVariable("changeId") String changeId
    ) {
        return ResultRecord.success(facade.getPublishTask(changeId));
    }

    @PostMapping("/publish-tasks/{changeId}/retry")
    public ResultRecord<DdcManagementPublishResult> retry(
            @PathVariable("changeId") String changeId
    ) {
        return ResultRecord.success(facade.retry(changeId));
    }

    @GetMapping("/instances")
    public ResultRecord<List<DdcManagementConfigClientInstance>> configClients(
            @RequestParam("appCode") String appCode,
            @RequestParam("env") String env,
            @RequestParam("namespace") String namespace
    ) {
        return ResultRecord.success(facade.getConfigClients(
                new DdcManagementInstanceQuery(appCode, env, namespace)
        ));
    }

    @GetMapping("/registry/services")
    public ResultRecord<DdcManagementServiceCatalog> services(
            @RequestParam("env") String env,
            @RequestParam("namespace") String namespace,
            @RequestParam("serviceKind") String serviceKind,
            @RequestParam("protocol") String protocol,
            @RequestParam(value = "serviceName", required = false) String serviceName,
            @RequestParam(value = "group", required = false) String group,
            @RequestParam(value = "version", required = false) String version
    ) {
        return ResultRecord.success(facade.getServiceKeys(new DdcManagementServiceQuery(
                env,
                namespace,
                serviceKind,
                protocol,
                serviceName,
                group,
                version
        )));
    }

    @GetMapping("/registry/instances")
    public ResultRecord<DdcManagementServiceSnapshot> serviceInstances(
            @RequestParam("env") String env,
            @RequestParam("namespace") String namespace,
            @RequestParam("serviceKind") String serviceKind,
            @RequestParam("protocol") String protocol,
            @RequestParam("serviceName") String serviceName,
            @RequestParam(value = "group", required = false) String group,
            @RequestParam(value = "version", required = false) String version
    ) {
        return ResultRecord.success(facade.getInstances(new DdcManagementServiceQuery(
                env,
                namespace,
                serviceKind,
                protocol,
                serviceName,
                group,
                version
        )));
    }

    private String trustedOperator(
            HttpServletRequest request,
            String requestedOperator) {
        Object principal = request.getAttribute(
                DdcServicePrincipal.REQUEST_ATTRIBUTE
        );
        if (principal instanceof DdcServicePrincipal servicePrincipal) {
            return servicePrincipal.auditOperator(requestedOperator);
        }
        return requestedOperator;
    }
}
