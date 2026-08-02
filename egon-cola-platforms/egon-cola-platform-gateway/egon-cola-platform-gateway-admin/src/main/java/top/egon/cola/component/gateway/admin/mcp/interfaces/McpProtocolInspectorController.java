package top.egon.cola.component.gateway.admin.mcp.interfaces;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.mcp.application.McpControlPlaneService;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/gateway/admin/mcp/servers")
@PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:test','CAP_*')")
public class McpProtocolInspectorController {

    private final McpControlPlaneService service;

    public McpProtocolInspectorController(McpControlPlaneService service) {
        this.service = service;
    }

    @PostMapping("/{serverId}/protocol-inspect")
    public Inspection inspect(
            @PathVariable String serverId,
            @Valid @RequestBody InspectRequest request) {
        var server = service.getServer(serverId);
        McpProtocolDialect dialect = McpProtocolDialect.valueOf(
                request.dialect()
        );
        if (!server.dialects().contains(dialect.name())) {
            throw new IllegalArgumentException(
                    "MCP protocol dialect is not enabled for this Server"
            );
        }
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("MCP-Protocol-Version", dialect.protocolVersion());
        if (dialect.releaseCandidate()) {
            headers.put("MCP-Method", request.method());
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jsonrpc", "2.0");
        body.put("id", "inspect-1");
        body.put("method", request.method());
        body.put("params", request.params());
        return new Inspection(
                "/mcp/" + server.serverCode(),
                Map.copyOf(headers),
                Map.copyOf(body),
                dialect.releaseCandidate()
        );
    }

    public record InspectRequest(
            @NotBlank String dialect,
            @NotBlank String method,
            @NotNull Map<String, Object> params
    ) {
    }

    public record Inspection(
            String path,
            Map<String, String> headers,
            Map<String, Object> body,
            boolean releaseCandidate
    ) {
    }
}
