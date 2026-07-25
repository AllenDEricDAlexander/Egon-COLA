package top.egon.cola.component.gateway.admin.interfaces.management;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.application.credential.GatewayCredentialService;
import top.egon.cola.component.gateway.admin.domain.AdminActor;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/gateway/admin/applications/{applicationId}/credentials")
@PreAuthorize("hasAnyAuthority('CAP_gateway:credentials:write','CAP_*')")
public class GatewayCredentialController {

    private final GatewayCredentialService service;

    public GatewayCredentialController(GatewayCredentialService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GatewayCredentialService.IssuedCredential create(
            @PathVariable String applicationId,
            AdminActor actor) {
        return service.create(applicationId, actor, audit());
    }

    @PostMapping("/{keyId}/rotate")
    @ResponseStatus(HttpStatus.CREATED)
    public GatewayCredentialService.IssuedCredential rotate(
            @PathVariable String applicationId,
            @PathVariable String keyId,
            @Valid @RequestBody RotateRequest request,
            AdminActor actor) {
        return service.rotate(
                applicationId,
                keyId,
                Duration.ofMinutes(request.overlapMinutes()),
                actor,
                audit()
        );
    }

    @PostMapping("/{keyId}/revoke")
    public GatewayCredentialService.CredentialView revoke(
            @PathVariable String applicationId,
            @PathVariable String keyId,
            AdminActor actor) {
        return service.revoke(
                applicationId,
                keyId,
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

    public record RotateRequest(
            @Min(0) @Max(1440) long overlapMinutes
    ) {
    }
}
