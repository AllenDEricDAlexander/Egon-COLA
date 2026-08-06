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
import top.egon.cola.component.gateway.admin.mcp.application.McpToolAdminService;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/gateway/admin/mcp")
@PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:read','CAP_*')")
public class McpToolAdminController {

    private final McpToolAdminService service;

    public McpToolAdminController(McpToolAdminService service) {
        this.service = service;
    }

    @GetMapping("/groups/{groupId}/managed-tools")
    public List<McpToolAdminService.ManagedToolView> managedTools(
            @PathVariable String groupId,
            @RequestParam(required = false) String serverId) {
        return service.managedTools(groupId, serverId);
    }

    @PutMapping("/managed-tools/{toolId}/override")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult putOverride(
            @PathVariable String toolId,
            @Valid @RequestBody ManagedToolOverrideRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.putOverride(
                toolId,
                request.mutation(),
                idempotencyKey,
                actor,
                RequestAuditContext.current()
        );
    }

    @DeleteMapping("/managed-tools/{toolId}/override")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult deleteOverride(
            @PathVariable String toolId,
            @Valid @RequestBody MutationRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.deleteOverride(
                toolId,
                request.control(),
                idempotencyKey,
                actor,
                RequestAuditContext.current()
        );
    }

    @GetMapping("/remote-tools")
    public List<McpToolAdminService.RemoteToolView> remoteTools(
            @RequestParam String gatewayGroupId,
            @RequestParam(required = false) String serverId) {
        return service.remoteTools(gatewayGroupId, serverId);
    }

    @PostMapping("/remote-tools")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult createRemoteTool(
            @Valid @RequestBody RemoteToolRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.putRemoteTool(
                null,
                request.mutation(),
                idempotencyKey,
                actor,
                RequestAuditContext.current()
        );
    }

    @PutMapping("/remote-tools/{toolId}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult updateRemoteTool(
            @PathVariable String toolId,
            @Valid @RequestBody RemoteToolRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.putRemoteTool(
                toolId,
                request.mutation(),
                idempotencyKey,
                actor,
                RequestAuditContext.current()
        );
    }

    @DeleteMapping("/remote-tools/{toolId}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult deleteRemoteTool(
            @PathVariable String toolId,
            @Valid @RequestBody MutationRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.deleteRemoteTool(
                toolId,
                request.control(),
                idempotencyKey,
                actor,
                RequestAuditContext.current()
        );
    }

    public record ManagedToolOverrideRequest(
            @NotBlank String gatewayGroupId,
            Boolean enabled,
            String serverId,
            Set<String> additionalPermissions,
            String minimumRiskLevel,
            @PositiveOrZero long expectedRevision,
            @PositiveOrZero long expectedDraftRevision,
            @NotBlank String changeReason
    ) {

        private McpToolAdminService.ManagedToolOverrideMutation mutation() {
            return new McpToolAdminService.ManagedToolOverrideMutation(
                    gatewayGroupId,
                    enabled,
                    serverId,
                    additionalPermissions,
                    minimumRiskLevel,
                    expectedRevision,
                    expectedDraftRevision,
                    changeReason
            );
        }
    }

    public record RemoteToolRequest(
            @NotBlank String gatewayGroupId,
            @NotBlank String serverId,
            @NotBlank String name,
            String description,
            @NotBlank String remoteMountId,
            Object inputSchema,
            Object outputSchema,
            Map<String, String> annotations,
            Set<String> requiredPermissions,
            String riskLevel,
            boolean idempotent,
            @NotNull Boolean enabled,
            @PositiveOrZero long expectedRevision,
            @PositiveOrZero long expectedDraftRevision,
            @NotBlank String changeReason
    ) {

        private McpToolAdminService.RemoteToolMutation mutation() {
            return new McpToolAdminService.RemoteToolMutation(
                    gatewayGroupId,
                    serverId,
                    name,
                    description,
                    remoteMountId,
                    inputSchema,
                    outputSchema,
                    annotations,
                    requiredPermissions,
                    riskLevel,
                    idempotent,
                    enabled,
                    expectedRevision,
                    expectedDraftRevision,
                    changeReason
            );
        }
    }

    public record MutationRequest(
            @NotBlank String gatewayGroupId,
            @PositiveOrZero long expectedRevision,
            @PositiveOrZero long expectedDraftRevision,
            @NotBlank String changeReason
    ) {

        private McpToolAdminService.MutationControl control() {
            return new McpToolAdminService.MutationControl(
                    gatewayGroupId,
                    expectedRevision,
                    expectedDraftRevision,
                    changeReason
            );
        }
    }
}
