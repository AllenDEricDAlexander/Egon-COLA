package top.egon.cola.component.gateway.admin.mcp.interfaces;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
import top.egon.cola.component.gateway.admin.mcp.application.McpValidationService;

import java.util.List;
import java.util.Set;

@Validated
@RestController
@RequestMapping("/api/v1/gateway/admin/mcp/servers")
@PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:read','CAP_*')")
public class McpServerController {

    private final McpControlPlaneService service;

    public McpServerController(McpControlPlaneService service) {
        this.service = service;
    }

    @GetMapping
    public List<McpControlPlaneService.ServerView> list(
            @RequestParam String gatewayGroupId) {
        return service.listServers(gatewayGroupId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult create(
            @Valid @RequestBody ServerRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.createServer(
                request.mutation(),
                idempotencyKey,
                actor,
                audit()
        );
    }

    @GetMapping("/{id}")
    public McpControlPlaneService.ServerView get(@PathVariable String id) {
        return service.getServer(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult update(
            @PathVariable String id,
            @Valid @RequestBody ServerRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.updateServer(
                id,
                request.mutation(),
                idempotencyKey,
                actor,
                audit()
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult delete(
            @PathVariable String id,
            @Valid @RequestBody MutationRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.deleteServer(
                id,
                request.control(),
                idempotencyKey,
                actor,
                audit()
        );
    }

    @PostMapping("/{id}/validate")
    public McpValidationService.ValidationReport validate(
            @PathVariable String id) {
        return service.validate(service.getServer(id).gatewayGroupId());
    }

    @GetMapping("/{id}/capability-preview")
    public McpControlPlaneService.Preview preview(@PathVariable String id) {
        return service.preview(service.getServer(id).gatewayGroupId());
    }

    private RequestAuditContext audit() {
        return RequestAuditContext.current();
    }

    public record ServerRequest(
            @NotBlank String gatewayGroupId,
            @NotBlank String serverCode,
            @NotBlank String displayName,
            String description,
            String instructions,
            @NotEmpty Set<String> dialects,
            @NotBlank String oauthAudience,
            @PositiveOrZero long listCacheTtlSeconds,
            Boolean enabled,
            @PositiveOrZero long expectedRevision,
            @PositiveOrZero long expectedDraftRevision,
            @NotBlank String changeReason
    ) {

        private McpControlPlaneService.ServerMutation mutation() {
            return new McpControlPlaneService.ServerMutation(
                    gatewayGroupId,
                    serverCode,
                    displayName,
                    description,
                    instructions,
                    dialects,
                    oauthAudience,
                    listCacheTtlSeconds,
                    enabled == null || enabled,
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

        private McpControlPlaneService.MutationControl control() {
            return new McpControlPlaneService.MutationControl(
                    gatewayGroupId,
                    expectedRevision,
                    expectedDraftRevision,
                    changeReason
            );
        }
    }
}
