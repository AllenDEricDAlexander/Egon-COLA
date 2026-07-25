package top.egon.cola.component.gateway.admin.interfaces.management;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.admin.application.GatewayGroupService;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.domain.AdminActor;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/gateway/admin/gateway-groups")
public class GatewayGroupController {

    private final GatewayGroupService service;

    public GatewayGroupController(GatewayGroupService service) {
        this.service = service;
    }

    @GetMapping
    public List<GatewayGroupService.GatewayGroupView> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GatewayGroupService.GatewayGroupView create(
            @Valid @RequestBody CreateRequest request,
            @RequestHeader(value = "X-Admin-Actor-Id",
                    defaultValue = "local-admin") String actorId,
            @RequestHeader(value = "X-Request-Id",
                    required = false) String requestId,
            @RequestHeader(value = "X-Trace-Id",
                    required = false) String traceId) {
        return service.create(
                new GatewayGroupService.CreateGatewayGroup(
                        request.gatewayGroupCode(),
                        request.displayName(),
                        request.env(),
                        request.namespace(),
                        request.description()
                ),
                actor(actorId),
                audit(requestId, traceId)
        );
    }

    @GetMapping("/{id}")
    public GatewayGroupService.GatewayGroupView get(
            @PathVariable String id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public GatewayGroupService.GatewayGroupView update(
            @PathVariable String id,
            @Valid @RequestBody UpdateRequest request,
            @RequestHeader(value = "X-Admin-Actor-Id",
                    defaultValue = "local-admin") String actorId,
            @RequestHeader(value = "X-Request-Id",
                    required = false) String requestId,
            @RequestHeader(value = "X-Trace-Id",
                    required = false) String traceId) {
        return service.update(
                id,
                new GatewayGroupService.UpdateGatewayGroup(
                        request.displayName(),
                        request.description(),
                        request.expectedRevision()
                ),
                actor(actorId),
                audit(requestId, traceId)
        );
    }

    @PostMapping("/{id}/enable")
    public GatewayGroupService.GatewayGroupView enable(
            @PathVariable String id,
            @RequestHeader(value = "X-Admin-Actor-Id",
                    defaultValue = "local-admin") String actorId) {
        return service.setEnabled(
                id,
                true,
                actor(actorId),
                audit(null, null)
        );
    }

    @PostMapping("/{id}/disable")
    public GatewayGroupService.GatewayGroupView disable(
            @PathVariable String id,
            @RequestHeader(value = "X-Admin-Actor-Id",
                    defaultValue = "local-admin") String actorId) {
        return service.setEnabled(
                id,
                false,
                actor(actorId),
                audit(null, null)
        );
    }

    private AdminActor actor(String actorId) {
        return new AdminActor(
                actorId,
                AdminActor.ActorType.USER,
                Set.of("*"),
                Set.of("GATEWAY_ADMIN")
        );
    }

    private RequestAuditContext audit(
            String requestId,
            String traceId) {
        return new RequestAuditContext(
                valueOrGenerated(requestId),
                valueOrGenerated(traceId)
        );
    }

    private String valueOrGenerated(String value) {
        return value == null || value.isBlank()
                ? UuidV7.simpleString()
                : value;
    }

    public record CreateRequest(
            @NotBlank String gatewayGroupCode,
            @NotBlank String displayName,
            @NotBlank String env,
            @NotBlank String namespace,
            String description
    ) {
    }

    public record UpdateRequest(
            @NotBlank String displayName,
            String description,
            @PositiveOrZero long expectedRevision
    ) {
    }
}
