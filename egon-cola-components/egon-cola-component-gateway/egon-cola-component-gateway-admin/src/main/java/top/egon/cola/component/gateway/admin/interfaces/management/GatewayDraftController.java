package top.egon.cola.component.gateway.admin.interfaces.management;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.application.routing.GatewayDraftService;
import top.egon.cola.component.gateway.admin.domain.AdminActor;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/gateway/admin/gateway-groups/{gatewayGroupId}/draft")
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
    public GatewayDraftService.MutationResult putRoute(
            @PathVariable String gatewayGroupId,
            @PathVariable String routeId,
            @Valid @RequestBody RouteRequest request,
            @RequestHeader(value = "X-Admin-Actor-Id",
                    defaultValue = "local-admin") String actorId) {
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
                actor(actorId),
                audit()
        );
    }

    @DeleteMapping("/routes/{routeId}")
    public GatewayDraftService.MutationResult deleteRoute(
            @PathVariable String gatewayGroupId,
            @PathVariable String routeId,
            @Valid @RequestBody MutationRequest request,
            @RequestHeader(value = "X-Admin-Actor-Id",
                    defaultValue = "local-admin") String actorId) {
        return service.deleteRoute(
                gatewayGroupId,
                routeId,
                request.control(),
                actor(actorId),
                audit()
        );
    }

    @PutMapping("/policies/{policyId}")
    public GatewayDraftService.MutationResult putPolicy(
            @PathVariable String gatewayGroupId,
            @PathVariable String policyId,
            @Valid @RequestBody PolicyRequest request,
            @RequestHeader(value = "X-Admin-Actor-Id",
                    defaultValue = "local-admin") String actorId) {
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
                actor(actorId),
                audit()
        );
    }

    @DeleteMapping("/policies/{policyId}")
    public GatewayDraftService.MutationResult deletePolicy(
            @PathVariable String gatewayGroupId,
            @PathVariable String policyId,
            @Valid @RequestBody MutationRequest request,
            @RequestHeader(value = "X-Admin-Actor-Id",
                    defaultValue = "local-admin") String actorId) {
        return service.deletePolicy(
                gatewayGroupId,
                policyId,
                request.control(),
                actor(actorId),
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

    private AdminActor actor(String actorId) {
        return new AdminActor(
                actorId,
                AdminActor.ActorType.USER,
                Set.of("*"),
                Set.of("GATEWAY_ADMIN")
        );
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
