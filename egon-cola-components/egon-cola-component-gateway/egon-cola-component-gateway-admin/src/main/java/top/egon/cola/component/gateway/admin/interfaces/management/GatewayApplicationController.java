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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.admin.application.GatewayApplicationService;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.domain.AdminActor;

import java.util.List;

@RestController
@RequestMapping("/api/v1/gateway/admin/applications")
@PreAuthorize("hasAnyAuthority('CAP_gateway:read','CAP_*')")
public class GatewayApplicationController {

    private final GatewayApplicationService service;

    public GatewayApplicationController(GatewayApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public List<GatewayApplicationService.GatewayApplicationView> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CAP_gateway:applications:write','CAP_*')")
    public GatewayApplicationService.GatewayApplicationView create(
            @Valid @RequestBody CreateRequest request,
            AdminActor actor) {
        return service.create(
                new GatewayApplicationService.CreateGatewayApplication(
                        request.applicationCode(),
                        request.displayName(),
                        request.env(),
                        request.namespace(),
                        request.description()
                ),
                actor,
                audit()
        );
    }

    @GetMapping("/{id}")
    public GatewayApplicationService.GatewayApplicationView get(
            @PathVariable String id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:applications:write','CAP_*')")
    public GatewayApplicationService.GatewayApplicationView update(
            @PathVariable String id,
            @Valid @RequestBody UpdateRequest request,
            AdminActor actor) {
        return service.update(
                id,
                new GatewayApplicationService.UpdateGatewayApplication(
                        request.displayName(),
                        request.description(),
                        request.expectedRevision()
                ),
                actor,
                audit()
        );
    }

    private RequestAuditContext audit() {
        return new RequestAuditContext(
                UuidV7.simpleString(),
                UuidV7.simpleString()
        );
    }

    public record CreateRequest(
            @NotBlank String applicationCode,
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
