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
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.ddc.admin.security.openapi.DdcServicePrincipal;
import top.egon.cola.component.ddc.admin.service.DdcManagementFacade;
import top.egon.cola.component.ddc.admin.service.DdcNamespaceEnvAppBindingService;
import top.egon.cola.component.ddc.management.model.DdcManagementConfig;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigClientInstance;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigDeleteRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigUpsertRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementInstanceQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishResult;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishTask;
import top.egon.cola.component.ddc.management.model.DdcManagementScopeBinding;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceCatalog;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceSnapshot;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ddc/openapi/management")
public class DdcManagementOpenApiController {

    private final DdcManagementFacade facade;

    private final DdcNamespaceEnvAppBindingService bindingService;

    public DdcManagementOpenApiController(
            DdcManagementFacade facade,
            DdcNamespaceEnvAppBindingService bindingService) {
        this.facade = facade;
        this.bindingService = bindingService;
    }

    @GetMapping("/configs/{bizCode}/{env}/{appCode}/{configKey}")
    public ResultRecord<DdcManagementConfig> config(
            @PathVariable("bizCode") String bizCode,
            @PathVariable("env") String env,
            @PathVariable("appCode") String appCode,
            @PathVariable("configKey") String configKey
    ) {
        return ResultRecord.success(facade.findConfig(new DdcManagementConfigQuery(
                bizCode,
                env,
                appCode,
                configKey
        )));
    }

    @PutMapping("/configs/{bizCode}/{env}/{appCode}/{configKey}")
    public ResultRecord<DdcManagementConfig> upsert(
            @PathVariable("bizCode") String bizCode,
            @PathVariable("env") String env,
            @PathVariable("appCode") String appCode,
            @PathVariable("configKey") String configKey,
            @RequestBody DdcManagementConfigUpsertRequest request,
            HttpServletRequest servletRequest
    ) {
        return ResultRecord.success(facade.upsert(new DdcManagementConfigUpsertRequest(
                bizCode,
                env,
                appCode,
                configKey,
                request.configValue(),
                request.valueType(),
                request.description(),
                request.expectedVersion(),
                trustedOperator(servletRequest, request.operator())
        )));
    }

    @DeleteMapping("/configs/{bizCode}/{env}/{appCode}/{configKey}")
    public ResultRecord<Void> delete(
            @PathVariable("bizCode") String bizCode,
            @PathVariable("env") String env,
            @PathVariable("appCode") String appCode,
            @PathVariable("configKey") String configKey,
            @RequestBody DdcManagementConfigDeleteRequest request,
            HttpServletRequest servletRequest
    ) {
        facade.delete(new DdcManagementConfigDeleteRequest(
                bizCode,
                env,
                appCode,
                configKey,
                request.expectedVersion(),
                trustedOperator(servletRequest, request.operator()),
                request.reason()
        ));
        return ResultRecord.success(null);
    }

    @PostMapping("/configs/{bizCode}/{env}/{appCode}/{configKey}/publish")
    public ResultRecord<DdcManagementPublishResult> publish(
            @PathVariable("bizCode") String bizCode,
            @PathVariable("env") String env,
            @PathVariable("appCode") String appCode,
            @PathVariable("configKey") String configKey,
            @RequestBody DdcManagementPublishRequest request,
            HttpServletRequest servletRequest
    ) {
        return ResultRecord.success(facade.publish(new DdcManagementPublishRequest(
                bizCode,
                env,
                appCode,
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
            @RequestParam("bizCode") String bizCode,
            @RequestParam("env") String env,
            @RequestParam("appCode") String appCode
    ) {
        return ResultRecord.success(facade.getConfigClients(
                new DdcManagementInstanceQuery(bizCode, env, appCode)
        ));
    }

    @GetMapping("/scope-bindings")
    public ResultRecord<List<DdcManagementScopeBinding>> scopeBindings(
            @RequestParam(value = "bizCode", required = false) String bizCode,
            @RequestParam(value = "namespaceCode", required = false)
            String namespaceCode,
            @RequestParam(value = "env", required = false) String env,
            @RequestParam(value = "appCode", required = false) String appCode
    ) {
        return ResultRecord.success(bindingService.list(
                        bizCode,
                        namespaceCode,
                        env,
                        appCode
                ).stream()
                .map(value -> new DdcManagementScopeBinding(
                        value.id(),
                        value.bizCode(),
                        value.namespaceCode(),
                        value.env(),
                        value.appId(),
                        value.appCode(),
                        value.appName(),
                        value.enabled()
                ))
                .toList());
    }

    @GetMapping("/registry/services")
    public ResultRecord<DdcManagementServiceCatalog> services(
            @RequestParam(value = "bizCode", required = false) String bizCode,
            @RequestParam(value = "namespaceCode", required = false)
            String namespaceCode,
            @RequestParam(value = "env", required = false) String env,
            @RequestParam(value = "appCode", required = false) String appCode,
            @RequestParam(value = "serviceKind", required = false)
            String serviceKind,
            @RequestParam(value = "protocol", required = false) String protocol,
            @RequestParam(value = "serviceName", required = false) String serviceName,
            @RequestParam(value = "group", required = false) String group,
            @RequestParam(value = "version", required = false) String version
    ) {
        return ResultRecord.success(facade.getServiceKeys(new DdcManagementServiceQuery(
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

    @GetMapping("/registry/instances")
    public ResultRecord<DdcManagementServiceSnapshot> serviceInstances(
            @RequestParam("bizCode") String bizCode,
            @RequestParam("env") String env,
            @RequestParam("appCode") String appCode,
            @RequestParam("serviceKind") String serviceKind,
            @RequestParam("protocol") String protocol,
            @RequestParam("serviceName") String serviceName,
            @RequestParam(value = "group", required = false) String group,
            @RequestParam(value = "version", required = false) String version
    ) {
        return ResultRecord.success(facade.getInstances(new DdcManagementServiceQuery(
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
