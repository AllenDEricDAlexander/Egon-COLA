package top.egon.cola.component.gateway.mcp.tool;

import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTool;
import top.egon.cola.component.gateway.mcp.rule.CompiledMcpRules;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 中文说明：{@code McpToolCatalog} 是类型，位于当前 Gateway 模块的相关包中，负责MCP工具目录相关的职责与边界。
 * English summary: {@code McpToolCatalog} is a type in the current Gateway module; it owns the mcp tool catalog-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpToolCatalog {

    /**
     * 中文说明：保存 rules 对应的状态、依赖或配置值；字段类型为 {@code Supplier<CompiledMcpRules>}，由 {@code McpToolCatalog} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by rules; its type is {@code Supplier<CompiledMcpRules>}, and {@code McpToolCatalog} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpToolCatalog} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolCatalog}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Supplier<CompiledMcpRules> rules;

    /**
     * 中文说明：创建 {@code McpToolCatalog} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpToolCatalog} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param rules 参数 rules；parameter rules。
     */
    public McpToolCatalog(Supplier<CompiledMcpRules> rules) {
        this.rules = Objects.requireNonNull(rules, "rules");
    }

    /**
     * 中文说明：执行 localTools 操作；该方法是 {@code McpToolCatalog} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the local tools operation; this method is the invocation entry point on {@code McpToolCatalog} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolCatalog.localTools(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serverCode 参数 服务器Code；parameter server code。
     * @return 返回 localTools 的处理结果；returns the result of the operation.
     */
    public List<McpRuntimeTool> localTools(String serverCode) {
        return tools(serverCode).stream()
                .filter(tool -> tool.operationId() != null)
                .toList();
    }

    /**
     * 中文说明：执行 tools 操作；该方法是 {@code McpToolCatalog} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the tools operation; this method is the invocation entry point on {@code McpToolCatalog} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolCatalog.tools(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serverCode 参数 服务器Code；parameter server code。
     * @return 返回 tools 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 local工具 操作；该方法是 {@code McpToolCatalog} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the local tool operation; this method is the invocation entry point on {@code McpToolCatalog} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolCatalog.localTool(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param name 参数 name；parameter name。
     * @return 返回 local工具 的处理结果；returns the result of the operation.
     */
    public Optional<McpRuntimeTool> localTool(
            String serverCode,
            String name) {
        return tool(serverCode, name)
                .filter(McpRuntimeTool::enabled)
                .filter(tool -> tool.operationId() != null);
    }

    /**
     * 中文说明：执行 工具 操作；该方法是 {@code McpToolCatalog} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the tool operation; this method is the invocation entry point on {@code McpToolCatalog} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolCatalog.tool(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param name 参数 name；parameter name。
     * @return 返回 工具 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 active 操作；该方法是 {@code McpToolCatalog} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the active operation; this method is the invocation entry point on {@code McpToolCatalog} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolCatalog.active(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 active 的处理结果；returns the result of the operation.
     */
    private CompiledMcpRules active() {
        CompiledMcpRules value = rules.get();
        return value == null ? CompiledMcpRules.empty() : value;
    }
}
