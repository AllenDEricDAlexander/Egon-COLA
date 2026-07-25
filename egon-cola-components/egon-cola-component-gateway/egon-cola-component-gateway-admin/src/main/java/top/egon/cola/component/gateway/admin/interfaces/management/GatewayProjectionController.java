package top.egon.cola.component.gateway.admin.interfaces.management;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.application.projection.GatewayProjectionService;

@RestController
@RequestMapping("/api/v1/gateway/admin")
public class GatewayProjectionController {

    private final GatewayProjectionService service;

    public GatewayProjectionController(GatewayProjectionService service) {
        this.service = service;
    }

    @GetMapping("/gateway-groups/{gatewayGroupId}/engine-nodes")
    public GatewayProjectionService.ProjectionEnvelope<?> engineNodes(
            @PathVariable String gatewayGroupId) {
        return service.engineNodes(gatewayGroupId);
    }

    @GetMapping("/providers/services")
    public GatewayProjectionService.ProjectionEnvelope<?> services(
            @RequestParam String env,
            @RequestParam String namespace,
            @RequestParam(required = false) String serviceKind,
            @RequestParam(required = false) String protocol,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String version) {
        return service.services(query(
                env,
                namespace,
                serviceKind,
                protocol,
                serviceName,
                group,
                version
        ));
    }

    @GetMapping("/providers/instances")
    public GatewayProjectionService.ProjectionEnvelope<?> instances(
            @RequestParam String env,
            @RequestParam String namespace,
            @RequestParam String protocol,
            @RequestParam String serviceName,
            @RequestParam(required = false) String serviceKind,
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String version) {
        return service.instances(query(
                env,
                namespace,
                serviceKind,
                protocol,
                serviceName,
                group,
                version
        ));
    }

    @GetMapping("/gateway-groups/{gatewayGroupId}/runtime-consistency")
    public GatewayProjectionService.RuntimeConsistency consistency(
            @PathVariable String gatewayGroupId) {
        return service.runtimeConsistency(gatewayGroupId);
    }

    private GatewayProjectionService.ProviderQuery query(
            String env,
            String namespace,
            String serviceKind,
            String protocol,
            String serviceName,
            String group,
            String version) {
        return new GatewayProjectionService.ProviderQuery(
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
