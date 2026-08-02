package top.egon.cola.component.gateway.admin.mcp.interfaces;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.mcp.application.McpControlPlaneService;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpRemoteProviderStore;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/gateway/admin/mcp/remote")
@PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:read','CAP_*')")
public class McpRemoteProviderController {

    private final McpControlPlaneService service;

    public McpRemoteProviderController(McpControlPlaneService service) {
        this.service = service;
    }

    @GetMapping("/providers")
    public List<JdbcMcpRemoteProviderStore.RemoteProviderDraft> providers(
            @RequestParam String gatewayGroupId) {
        return service.providers(gatewayGroupId);
    }

    @PostMapping("/providers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult createProvider(
            @Valid @RequestBody ProviderRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.putProvider(
                null,
                request.mutation(),
                idempotencyKey,
                actor,
                audit()
        );
    }

    @PutMapping("/providers/{id}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult updateProvider(
            @PathVariable String id,
            @Valid @RequestBody ProviderRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.putProvider(
                id,
                request.mutation(),
                idempotencyKey,
                actor,
                audit()
        );
    }

    @DeleteMapping("/providers/{id}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult deleteProvider(
            @PathVariable String id,
            @Valid @RequestBody McpServerController.MutationRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.deleteProvider(
                id,
                control(request),
                idempotencyKey,
                actor,
                audit()
        );
    }

    @PostMapping("/providers/{id}/discover")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:test','CAP_*')")
    public List<JdbcMcpRemoteProviderStore.RemoteCapability> discover(
            @PathVariable String id) {
        return service.remoteCapabilities(id);
    }

    @GetMapping("/mounts")
    public List<JdbcMcpRemoteProviderStore.RemoteMountDraft> mounts(
            @RequestParam String gatewayGroupId) {
        return service.mounts(gatewayGroupId);
    }

    @PostMapping("/mounts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult createMount(
            @Valid @RequestBody MountRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.putMount(
                null,
                request.mutation(),
                idempotencyKey,
                actor,
                audit()
        );
    }

    @PutMapping("/mounts/{id}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult updateMount(
            @PathVariable String id,
            @Valid @RequestBody MountRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.putMount(
                id,
                request.mutation(),
                idempotencyKey,
                actor,
                audit()
        );
    }

    @DeleteMapping("/mounts/{id}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult deleteMount(
            @PathVariable String id,
            @Valid @RequestBody McpServerController.MutationRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.deleteMount(
                id,
                control(request),
                idempotencyKey,
                actor,
                audit()
        );
    }

    private McpControlPlaneService.MutationControl control(
            McpServerController.MutationRequest request) {
        return new McpControlPlaneService.MutationControl(
                request.gatewayGroupId(),
                request.expectedRevision(),
                request.expectedDraftRevision(),
                request.changeReason()
        );
    }

    private RequestAuditContext audit() {
        return RequestAuditContext.current();
    }

    public record ProviderRequest(
            @NotBlank String gatewayGroupId,
            @NotBlank String providerCode,
            @NotNull Map<String, Object> content,
            boolean enabled,
            @PositiveOrZero long expectedRevision,
            @PositiveOrZero long expectedDraftRevision,
            @NotBlank String changeReason
    ) {

        private McpControlPlaneService.RemoteProviderMutation mutation() {
            return new McpControlPlaneService.RemoteProviderMutation(
                    gatewayGroupId,
                    providerCode,
                    content,
                    enabled,
                    expectedRevision,
                    expectedDraftRevision,
                    changeReason
            );
        }
    }

    public record MountRequest(
            @NotBlank String gatewayGroupId,
            @NotBlank String serverId,
            @NotBlank String providerId,
            @NotBlank String namespace,
            @NotBlank String capabilityFingerprint,
            @NotNull Map<String, Object> content,
            boolean enabled,
            @PositiveOrZero long expectedRevision,
            @PositiveOrZero long expectedDraftRevision,
            @NotBlank String changeReason
    ) {

        private McpControlPlaneService.RemoteMountMutation mutation() {
            return new McpControlPlaneService.RemoteMountMutation(
                    gatewayGroupId,
                    serverId,
                    providerId,
                    namespace,
                    capabilityFingerprint,
                    content,
                    enabled,
                    expectedRevision,
                    expectedDraftRevision,
                    changeReason
            );
        }
    }
}
