package top.egon.cola.component.gateway.admin.interfaces.management;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.application.release.GatewayReleaseService;
import top.egon.cola.component.gateway.admin.domain.AdminActor;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/gateway/admin")
@PreAuthorize("hasAnyAuthority('CAP_gateway:read','CAP_*')")
public class GatewayReleaseController {

    private final GatewayReleaseService service;

    public GatewayReleaseController(GatewayReleaseService service) {
        this.service = service;
    }

    @PostMapping("/gateway-groups/{gatewayGroupId}/releases")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:releases:write','CAP_*')")
    public GatewayReleaseService.ReleaseView create(
            @PathVariable String gatewayGroupId,
            @Valid @RequestBody CreateRequest request,
            AdminActor actor) {
        return service.create(
                gatewayGroupId,
                new GatewayReleaseService.CreateRelease(
                        request.expectedDraftRevision(),
                        request.changeReason()
                ),
                actor,
                audit()
        );
    }

    @GetMapping("/releases/{releaseId}")
    public GatewayReleaseService.ReleaseView get(
            @PathVariable String releaseId) {
        return service.get(releaseId);
    }

    @GetMapping("/releases/{releaseId}/diff")
    public Map<String, Object> diff(@PathVariable String releaseId) {
        return service.diff(releaseId);
    }

    @PostMapping("/releases/{releaseId}/retry")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:releases:write','CAP_*')")
    public GatewayReleaseService.ReleaseView retry(
            @PathVariable String releaseId,
            AdminActor actor) {
        return service.retry(
                releaseId,
                actor,
                audit()
        );
    }

    @PostMapping("/gateway-groups/{gatewayGroupId}/rollback")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:releases:write','CAP_*')")
    public GatewayReleaseService.ReleaseView rollback(
            @PathVariable String gatewayGroupId,
            @Valid @RequestBody RollbackRequest request,
            AdminActor actor) {
        return service.rollback(
                gatewayGroupId,
                new GatewayReleaseService.RollbackRelease(
                        request.sourceReleaseId(),
                        request.expectedDraftRevision(),
                        request.changeReason()
                ),
                actor,
                audit()
        );
    }

    @GetMapping("/gateway-groups/{gatewayGroupId}/releases")
    public List<GatewayReleaseService.ReleaseView> history(
            @PathVariable String gatewayGroupId) {
        return service.history(gatewayGroupId);
    }

    private RequestAuditContext audit() {
        return new RequestAuditContext(
                UuidV7.simpleString(),
                UuidV7.simpleString()
        );
    }

    public record CreateRequest(
            @PositiveOrZero long expectedDraftRevision,
            @NotBlank String changeReason
    ) {
    }

    public record RollbackRequest(
            @NotBlank String sourceReleaseId,
            @PositiveOrZero long expectedDraftRevision,
            @NotBlank String changeReason
    ) {
    }
}
