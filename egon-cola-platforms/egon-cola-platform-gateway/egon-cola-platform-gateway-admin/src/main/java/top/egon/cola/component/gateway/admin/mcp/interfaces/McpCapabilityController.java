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
import top.egon.cola.component.gateway.admin.mcp.application.McpValidationService;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpCapabilityDraftStore;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/gateway/admin/mcp")
@PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:read','CAP_*')")
public class McpCapabilityController {

    private static final String CAPABILITY_COLLECTION =
            "{plural:resources|resource-templates|prompts|"
                    + "task-policies|app-bindings}";

    private final McpControlPlaneService service;

    public McpCapabilityController(McpControlPlaneService service) {
        this.service = service;
    }

    @GetMapping("/servers/{serverId}/" + CAPABILITY_COLLECTION)
    public List<JdbcMcpCapabilityDraftStore.CapabilityDraft> list(
            @PathVariable String serverId,
            @PathVariable String plural,
            @RequestParam String gatewayGroupId) {
        return service.capabilities(
                gatewayGroupId,
                serverId,
                kind(plural)
        );
    }

    @PostMapping("/servers/{serverId}/" + CAPABILITY_COLLECTION)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult create(
            @PathVariable String serverId,
            @PathVariable String plural,
            @Valid @RequestBody CapabilityRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.putCapability(
                null,
                kind(plural),
                request.mutation(serverId),
                idempotencyKey,
                actor,
                audit()
        );
    }

    @PutMapping("/" + CAPABILITY_COLLECTION + "/{id}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult update(
            @PathVariable String plural,
            @PathVariable String id,
            @Valid @RequestBody CapabilityRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.putCapability(
                id,
                kind(plural),
                request.mutation(request.serverId()),
                idempotencyKey,
                actor,
                audit()
        );
    }

    @DeleteMapping("/" + CAPABILITY_COLLECTION + "/{id}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult delete(
            @PathVariable String plural,
            @PathVariable String id,
            @Valid @RequestBody McpServerController.MutationRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.deleteCapability(
                id,
                kind(plural),
                new McpControlPlaneService.MutationControl(
                        request.gatewayGroupId(),
                        request.expectedRevision(),
                        request.expectedDraftRevision(),
                        request.changeReason()
                ),
                idempotencyKey,
                actor,
                audit()
        );
    }

    @PostMapping("/" + CAPABILITY_COLLECTION + "/{id}/validate")
    public McpValidationService.ValidationReport validate(
            @PathVariable String plural,
            @PathVariable String id,
            @RequestParam String gatewayGroupId) {
        kind(plural);
        return service.validate(gatewayGroupId);
    }

    private JdbcMcpCapabilityDraftStore.CapabilityKind kind(String value) {
        String normalized = value.toUpperCase(Locale.ROOT)
                .replace('-', '_');
        return switch (normalized) {
            case "RESOURCES" ->
                    JdbcMcpCapabilityDraftStore.CapabilityKind.RESOURCE;
            case "RESOURCE_TEMPLATES" ->
                    JdbcMcpCapabilityDraftStore.CapabilityKind
                            .RESOURCE_TEMPLATE;
            case "PROMPTS" ->
                    JdbcMcpCapabilityDraftStore.CapabilityKind.PROMPT;
            case "TASK_POLICIES" ->
                    JdbcMcpCapabilityDraftStore.CapabilityKind.TASK_POLICY;
            case "APP_BINDINGS" ->
                    JdbcMcpCapabilityDraftStore.CapabilityKind.APP_BINDING;
            default -> throw new IllegalArgumentException(
                    "unsupported MCP capability collection: " + value
            );
        };
    }

    private RequestAuditContext audit() {
        return RequestAuditContext.current();
    }

    public record CapabilityRequest(
            @NotBlank String gatewayGroupId,
            String serverId,
            @NotBlank String name,
            @NotNull Map<String, Object> content,
            boolean enabled,
            @PositiveOrZero long expectedRevision,
            @PositiveOrZero long expectedDraftRevision,
            @NotBlank String changeReason
    ) {

        private McpControlPlaneService.CapabilityMutation mutation(
                String resolvedServerId) {
            return new McpControlPlaneService.CapabilityMutation(
                    gatewayGroupId,
                    resolvedServerId,
                    name,
                    content,
                    enabled,
                    expectedRevision,
                    expectedDraftRevision,
                    changeReason
            );
        }
    }
}
