package top.egon.cola.component.gateway.admin.mcp.interfaces;

import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.mcp.application.McpControlPlaneService;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpTaskStore;

import java.util.List;

@RestController
@RequestMapping("/api/v1/gateway/admin/mcp/tasks")
@PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:runtime:read','CAP_*')")
public class McpTaskAdminController {

    private final McpControlPlaneService service;

    public McpTaskAdminController(McpControlPlaneService service) {
        this.service = service;
    }

    @GetMapping
    public List<JdbcMcpTaskStore.TaskRecord> list(
            @RequestParam String tenantId,
            @RequestParam(required = false) String clientId) {
        return service.tasks(tenantId, clientId);
    }

    @GetMapping("/{id}")
    public JdbcMcpTaskStore.TaskRecord get(@PathVariable String id) {
        return service.task(id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public CancelResult cancel(
            @PathVariable String id,
            @Valid @RequestBody CancelRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return new CancelResult(service.cancelTask(
                id,
                request.expectedRevision(),
                idempotencyKey,
                actor,
                RequestAuditContext.current()
        ));
    }

    public record CancelRequest(@PositiveOrZero long expectedRevision) {
    }

    public record CancelResult(boolean cancelled) {
    }
}
