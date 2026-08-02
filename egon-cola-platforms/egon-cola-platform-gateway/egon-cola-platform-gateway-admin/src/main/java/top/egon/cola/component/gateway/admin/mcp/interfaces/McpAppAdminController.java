package top.egon.cola.component.gateway.admin.mcp.interfaces;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.mcp.application.McpControlPlaneService;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpArtifactMetadataStore;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/gateway/admin/mcp/apps")
@PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:read','CAP_*')")
public class McpAppAdminController {

    private final McpControlPlaneService service;

    public McpAppAdminController(McpControlPlaneService service) {
        this.service = service;
    }

    @GetMapping("/artifacts")
    public List<JdbcMcpArtifactMetadataStore.ArtifactMetadata> list(
            @RequestParam String gatewayGroupId) {
        return service.artifacts(gatewayGroupId);
    }

    @PostMapping("/artifacts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult register(
            @Valid @RequestBody ArtifactRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.registerArtifact(
                request.mutation(),
                idempotencyKey,
                actor,
                RequestAuditContext.current()
        );
    }

    @GetMapping("/artifacts/{id}")
    public JdbcMcpArtifactMetadataStore.ArtifactMetadata get(
            @PathVariable String id) {
        return service.artifact(id);
    }

    @DeleteMapping("/artifacts/{id}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult revoke(
            @PathVariable String id,
            @Valid @RequestBody McpServerController.MutationRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.revokeArtifact(
                id,
                new McpControlPlaneService.MutationControl(
                        request.gatewayGroupId(),
                        request.expectedRevision(),
                        request.expectedDraftRevision(),
                        request.changeReason()
                ),
                idempotencyKey,
                actor,
                RequestAuditContext.current()
        );
    }

    public record ArtifactRequest(
            @NotBlank String gatewayGroupId,
            @NotBlank String appCode,
            @NotBlank String version,
            @NotBlank String displayName,
            @NotBlank String resourceUri,
            @NotBlank String artifactReference,
            @NotBlank String sha256,
            @PositiveOrZero long sizeBytes,
            @NotBlank String mimeType,
            @NotBlank String contentSecurityPolicy,
            @NotEmpty Set<String> permissions,
            Set<String> allowedOrigins,
            @PositiveOrZero long expectedRevision,
            @PositiveOrZero long expectedDraftRevision,
            @NotBlank String changeReason
    ) {

        private McpControlPlaneService.ArtifactMutation mutation() {
            return new McpControlPlaneService.ArtifactMutation(
                    gatewayGroupId,
                    appCode,
                    version,
                    displayName,
                    resourceUri,
                    artifactReference,
                    sha256,
                    sizeBytes,
                    mimeType,
                    contentSecurityPolicy,
                    permissions,
                    allowedOrigins == null ? Set.of() : allowedOrigins,
                    expectedRevision,
                    expectedDraftRevision,
                    changeReason
            );
        }
    }
}
