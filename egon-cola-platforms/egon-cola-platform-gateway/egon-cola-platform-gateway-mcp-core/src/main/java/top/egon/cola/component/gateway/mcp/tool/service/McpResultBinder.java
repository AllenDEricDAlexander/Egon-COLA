package top.egon.cola.component.gateway.mcp.tool.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.egon.cola.component.gateway.core.operation.GatewayInvocationResult;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 中文说明：{@code McpResultBinder} 是类型，位于当前 Gateway 模块的相关包中，负责MCPResultBinder相关的职责与边界。
 * English summary: {@code McpResultBinder} is a type in the current Gateway module; it owns the mcp result binder-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpResultBinder {

    /**
     * 中文说明：保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code McpResultBinder} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code McpResultBinder} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpResultBinder} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpResultBinder}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：创建 {@code McpResultBinder} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpResultBinder} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param objectMapper 参数 object映射器；parameter object mapper。
     */
    public McpResultBinder(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
    }

    /**
     * 中文说明：执行 bind 操作；该方法是 {@code McpResultBinder} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the bind operation; this method is the invocation entry point on {@code McpResultBinder} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpResultBinder.bind(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param result 参数 result；parameter result。
     * @return 返回 bind 的处理结果；returns the result of the operation.
     */
    public Map<String, Object> bind(GatewayInvocationResult result) {
        String text = new String(result.body(), StandardCharsets.UTF_8);
        Object decoded = decode(text);
        Object structured = decoded;
        if (structured == null) {
            structured = Map.of();
        }
        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        response.put("content", List.of(Map.of(
                "type", "text",
                "text", text
        )));
        response.put("structuredContent", structured);
        response.put("isError", result.statusCode() >= 400);
        return Map.copyOf(response);
    }

    /**
     * 中文说明：执行 decode 操作；该方法是 {@code McpResultBinder} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the decode operation; this method is the invocation entry point on {@code McpResultBinder} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpResultBinder.decode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param text 参数 text；parameter text。
     * @return 返回 decode 的处理结果；returns the result of the operation.
     */
    private Object decode(String text) {
        try {
            return objectMapper.readValue(
                    text,
                    new TypeReference<Object>() {
                    }
            );
        } catch (Exception ignored) {
            return Map.of("text", text);
        }
    }

}
