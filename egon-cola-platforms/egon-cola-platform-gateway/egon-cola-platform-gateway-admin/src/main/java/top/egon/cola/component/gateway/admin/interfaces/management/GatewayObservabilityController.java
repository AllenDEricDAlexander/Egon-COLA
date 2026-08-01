package top.egon.cola.component.gateway.admin.interfaces.management;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.application.observability.GatewayObservabilityQueryService;
import top.egon.cola.component.gateway.admin.application.observability.GatewayObservabilityStore;

@RestController
@RequestMapping("/api/v1/gateway/admin")
@PreAuthorize("hasAnyAuthority('CAP_gateway:read','CAP_*')")
public class GatewayObservabilityController {

    private final GatewayObservabilityQueryService service;

    public GatewayObservabilityController(
            GatewayObservabilityQueryService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    public GatewayObservabilityStore.DashboardSummary dashboard(
            @RequestParam String bizCode,
            @RequestParam String appCode,
            @RequestParam String env,
            @RequestParam String namespace) {
        return service.dashboard(bizCode, appCode, env, namespace);
    }

    @GetMapping("/observability/traces")
    public GatewayObservabilityStore.Page<
            GatewayObservabilityStore.TraceSummary> traces(
            @RequestParam String env,
            @RequestParam String namespace,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) String protocol,
            @RequestParam(required = false) String statusCategory,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.traces(new GatewayObservabilityStore.TraceQuery(
                env,
                namespace,
                traceId,
                protocol,
                statusCategory,
                page,
                size
        ));
    }

    @GetMapping("/audit")
    public GatewayObservabilityStore.Page<
            GatewayObservabilityStore.AuditSummary> audits(
            @RequestParam String env,
            @RequestParam String namespace,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) Boolean successful,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.audits(new GatewayObservabilityStore.AuditQuery(
                env,
                namespace,
                actorId,
                resourceId,
                traceId,
                successful,
                page,
                size
        ));
    }
}
