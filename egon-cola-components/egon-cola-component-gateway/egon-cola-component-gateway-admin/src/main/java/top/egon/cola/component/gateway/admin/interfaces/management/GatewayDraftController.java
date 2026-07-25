package top.egon.cola.component.gateway.admin.interfaces.management;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.application.routing.GatewayDraftService;
import top.egon.cola.component.gateway.admin.domain.AdminActor;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/gateway/admin/gateway-groups/{gatewayGroupId}/draft")
@PreAuthorize("hasAnyAuthority('CAP_gateway:read','CAP_*')")
public class GatewayDraftController {

    private final GatewayDraftService service;

    public GatewayDraftController(GatewayDraftService service) {
        this.service = service;
    }

    @GetMapping
    public GatewayDraftService.DraftView get(
            @PathVariable String gatewayGroupId) {
        return service.get(gatewayGroupId);
    }

    @PutMapping("/routes/{routeId}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:drafts:write','CAP_*')")
    public GatewayDraftService.MutationResult putRoute(
            @PathVariable String gatewayGroupId,
            @PathVariable String routeId,
            @Valid @RequestBody RouteRequest request,
            AdminActor actor) {
        return service.putRoute(
                gatewayGroupId,
                routeId,
                new GatewayDraftService.RouteMutation(
                        request.operationId(),
                        request.content(),
                        request.enabled(),
                        request.expectedRevision(),
                        request.idempotencyKey(),
                        request.changeReason()
                ),
                actor,
                audit()
        );
    }

    @DeleteMapping("/routes/{routeId}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:drafts:write','CAP_*')")
    public GatewayDraftService.MutationResult deleteRoute(
            @PathVariable String gatewayGroupId,
            @PathVariable String routeId,
            @Valid @RequestBody MutationRequest request,
            AdminActor actor) {
        return service.deleteRoute(
                gatewayGroupId,
                routeId,
                request.control(),
                actor,
                audit()
        );
    }

    @PutMapping("/policies/{policyId}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:drafts:write','CAP_*')")
    public GatewayDraftService.MutationResult putPolicy(
            @PathVariable String gatewayGroupId,
            @PathVariable String policyId,
            @Valid @RequestBody PolicyRequest request,
            AdminActor actor) {
        return service.putPolicy(
                gatewayGroupId,
                policyId,
                new GatewayDraftService.PolicyMutation(
                        request.policyType(),
                        request.policyScope(),
                        request.content(),
                        request.enabled(),
                        request.expectedRevision(),
                        request.idempotencyKey(),
                        request.changeReason()
                ),
                actor,
                audit()
        );
    }

    @DeleteMapping("/policies/{policyId}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:drafts:write','CAP_*')")
    public GatewayDraftService.MutationResult deletePolicy(
            @PathVariable String gatewayGroupId,
            @PathVariable String policyId,
            @Valid @RequestBody MutationRequest request,
            AdminActor actor) {
        return service.deletePolicy(
                gatewayGroupId,
                policyId,
                request.control(),
                actor,
                audit()
        );
    }

    @PostMapping("/validate")
    public GatewayDraftService.ValidationReport validate(
            @PathVariable String gatewayGroupId) {
        return service.validate(gatewayGroupId);
    }

    @GetMapping("/diff")
    public GatewayDraftService.DraftDiff diff(
            @PathVariable String gatewayGroupId) {
        return service.diff(gatewayGroupId);
    }

    private RequestAuditContext audit() {
        return new RequestAuditContext(
                UuidV7.simpleString(),
                UuidV7.simpleString()
        );
    }

    public record RouteRequest(
            @NotBlank String operationId,
            @NotNull Map<String, Object> content,
            boolean enabled,
            @PositiveOrZero long expectedRevision,
            @NotBlank String idempotencyKey,
            @NotBlank String changeReason
    ) {
    }

    public record PolicyRequest(
            @NotBlank String policyType,
            @NotBlank String policyScope,
            @NotNull Map<String, Object> content,
            boolean enabled,
            @PositiveOrZero long expectedRevision,
            @NotBlank String idempotencyKey,
            @NotBlank String changeReason
    ) {
    }

    public record MutationRequest(
            @PositiveOrZero long expectedRevision,
            @NotBlank String idempotencyKey,
            @NotBlank String changeReason
    ) {

        private GatewayDraftService.MutationControl control() {
            return new GatewayDraftService.MutationControl(
                    expectedRevision,
                    idempotencyKey,
                    changeReason
            );
        }
    }
}
