package top.egon.cola.component.gateway.mcp.tool;

import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTool;
import top.egon.cola.component.gateway.mcp.rule.CompiledMcpRules;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public final class McpToolCatalog {

    private final Supplier<CompiledMcpRules> rules;

    public McpToolCatalog(Supplier<CompiledMcpRules> rules) {
        this.rules = Objects.requireNonNull(rules, "rules");
    }

    public List<McpRuntimeTool> localTools(String serverCode) {
        return tools(serverCode).stream()
                .filter(tool -> tool.operationId() != null)
                .toList();
    }

    public List<McpRuntimeTool> tools(String serverCode) {
        CompiledMcpRules current = active();
        return current.toolsByQualifiedName().values().stream()
                .filter(McpRuntimeTool::enabled)
                .filter(tool -> tool.serverCode().equals(serverCode))
                .filter(tool -> tool.operationId() != null
                        || tool.remoteMountId() != null)
                .filter(tool -> current.remoteAvailable(
                        tool.remoteMountId(),
                        "TOOL"
                ))
                .sorted(java.util.Comparator.comparing(McpRuntimeTool::name))
                .toList();
    }

    public Optional<McpRuntimeTool> localTool(
            String serverCode,
            String name) {
        return tool(serverCode, name)
                .filter(McpRuntimeTool::enabled)
                .filter(tool -> tool.operationId() != null);
    }

    public Optional<McpRuntimeTool> tool(String serverCode, String name) {
        CompiledMcpRules current = active();
        return current.tool(serverCode, name)
                .filter(McpRuntimeTool::enabled)
                .filter(tool -> tool.operationId() != null
                        || tool.remoteMountId() != null)
                .filter(tool -> current.remoteAvailable(
                        tool.remoteMountId(),
                        "TOOL"
                ));
    }

    private CompiledMcpRules active() {
        CompiledMcpRules value = rules.get();
        return value == null ? CompiledMcpRules.empty() : value;
    }
}
