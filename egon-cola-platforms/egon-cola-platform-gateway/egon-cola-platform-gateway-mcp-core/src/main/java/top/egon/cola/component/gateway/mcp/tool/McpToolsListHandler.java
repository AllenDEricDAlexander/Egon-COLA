package top.egon.cola.component.gateway.mcp.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTool;
import top.egon.cola.component.gateway.mcp.server.McpMethodHandler;
import top.egon.cola.component.gateway.mcp.server.McpRequestContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 中文说明：{@code McpToolsListHandler} 是处理器，位于当前 Gateway 模块的相关包中，负责MCPToolsList处理器相关的职责与边界。
 * English summary: {@code McpToolsListHandler} is a mcp tools list handler handler in the current Gateway module; it owns the mcp tools list handler-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpToolsListHandler implements McpMethodHandler {

    /**
     * 中文说明：保存 目录 对应的状态、依赖或配置值；字段类型为 {@code McpToolCatalog}，由 {@code McpToolsListHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by catalog; its type is {@code McpToolCatalog}, and {@code McpToolsListHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpToolsListHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolsListHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpToolCatalog catalog;

    /**
     * 中文说明：保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code McpToolsListHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code McpToolsListHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpToolsListHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolsListHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：创建 {@code McpToolsListHandler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpToolsListHandler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param catalog 参数 目录；parameter catalog。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     */
    public McpToolsListHandler(
            McpToolCatalog catalog,
            ObjectMapper objectMapper) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
    }

    /**
     * 中文说明：执行 方法 操作；该方法是 {@code McpToolsListHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the method operation; this method is the invocation entry point on {@code McpToolsListHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolsListHandler.method(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 方法 的处理结果；returns the result of the operation.
     */
    @Override
    public String method() {
        return "tools/list";
    }

    /**
     * 中文说明：执行 handle 操作；该方法是 {@code McpToolsListHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the handle operation; this method is the invocation entry point on {@code McpToolsListHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolsListHandler.handle(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param context 参数 context；parameter context。
     * @return 返回 handle 的处理结果；returns the result of the operation.
     */
    @Override
    public Publisher<McpJsonRpcResponse> handle(
            McpJsonRpcRequest request,
            McpRequestContext context) {
        List<Map<String, Object>> tools = catalog.tools(
                context.server().serverCode()
        ).stream().map(this::describe).toList();
        return Mono.just(McpJsonRpcResponse.success(
                request.id(),
                Map.of("tools", tools)
        ));
    }

    /**
     * 中文说明：执行 describe 操作；该方法是 {@code McpToolsListHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the describe operation; this method is the invocation entry point on {@code McpToolsListHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolsListHandler.describe(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param tool 参数 工具；parameter tool。
     * @return 返回 describe 的处理结果；returns the result of the operation.
     */
    private Map<String, Object> describe(McpRuntimeTool tool) {
        LinkedHashMap<String, Object> value = new LinkedHashMap<>();
        value.put("name", tool.name());
        value.put("description", tool.description() == null
                ? ""
                : tool.description());
        value.put("inputSchema", schema(tool.inputSchema()));
        value.put("annotations", tool.annotations());
        return Map.copyOf(value);
    }

    /**
     * 中文说明：执行 模式 操作；该方法是 {@code McpToolsListHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the schema operation; this method is the invocation entry point on {@code McpToolsListHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolsListHandler.schema(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param json 参数 json；parameter json。
     * @return 返回 模式 的处理结果；returns the result of the operation.
     */
    private Map<String, Object> schema(String json) {
        if (json == null) {
            return Map.of("type", "object");
        }
        try {
            return objectMapper.readValue(
                    json,
                    new TypeReference<LinkedHashMap<String, Object>>() {
                    }
            );
        } catch (Exception failure) {
            throw new IllegalStateException(
                    "MCP tool input schema is invalid",
                    failure
            );
        }
    }
}
