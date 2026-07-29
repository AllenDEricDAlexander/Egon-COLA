package top.egon.cola.component.gateway.admin.interfaces.management;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.application.GatewayGroupService;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.domain.AdminActor;

import java.util.List;

@RestController
@RequestMapping("/api/v1/gateway/admin/gateway-groups")
@PreAuthorize("hasAnyAuthority('CAP_gateway:read','CAP_*')")
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
    @PreAuthorize("hasAnyAuthority('CAP_gateway:groups:write','CAP_*')")
    public GatewayGroupService.GatewayGroupView create(
            @Valid @RequestBody CreateRequest request,
            AdminActor actor,
            @RequestHeader(value = "X-Request-Id",
                    required = false) String requestId) {
        return service.create(
                new GatewayGroupService.CreateGatewayGroup(
                        request.gatewayGroupCode(),
                        request.displayName(),
                        request.env(),
                        request.namespace(),
                        request.description()
                ),
                actor,
                audit(requestId)
        );
    }

    @GetMapping("/{id}")
    public GatewayGroupService.GatewayGroupView get(
            @PathVariable String id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:groups:write','CAP_*')")
    public GatewayGroupService.GatewayGroupView update(
            @PathVariable String id,
            @Valid @RequestBody UpdateRequest request,
            AdminActor actor,
            @RequestHeader(value = "X-Request-Id",
                    required = false) String requestId) {
        return service.update(
                id,
                new GatewayGroupService.UpdateGatewayGroup(
                        request.displayName(),
                        request.description(),
                        request.expectedRevision()
                ),
                actor,
                audit(requestId)
        );
    }

    @PostMapping("/{id}/enable")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:groups:write','CAP_*')")
    public GatewayGroupService.GatewayGroupView enable(
            @PathVariable String id,
            AdminActor actor) {
        return service.setEnabled(
                id,
                true,
                actor,
                audit(null)
        );
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:groups:write','CAP_*')")
    public GatewayGroupService.GatewayGroupView disable(
            @PathVariable String id,
            AdminActor actor) {
        return service.setEnabled(
                id,
                false,
                actor,
                audit(null)
        );
    }

    private RequestAuditContext audit(String requestId) {
        return RequestAuditContext.current(requestId);
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
