package top.egon.cola.component.gateway.mcp.prompt.service;

import org.reactivestreams.Publisher;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimePrompt;
import top.egon.cola.component.gateway.mcp.common.protocol.McpProtocolException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Strategy for rendering one reviewed local prompt descriptor.
 * 补充说明 / Supplementary summary: {@code McpPromptDriver} 是接口契约，位于当前 Gateway 模块的相关包中，负责MCP提示词驱动器相关的职责与边界。
 * English supplement: {@code McpPromptDriver} is an interface contract in the current Gateway module; it owns the mcp prompt driver-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public interface McpPromptDriver {

    /**
     * 中文说明：执行 sourceTypes 操作；该方法是 {@code McpPromptDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the source types operation; this method is the invocation entry point on {@code McpPromptDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpPromptDriver.sourceTypes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 sourceTypes 的处理结果；returns the result of the operation.
     */
    Set<String> sourceTypes();

    /**
     * 中文说明：执行 render 操作；该方法是 {@code McpPromptDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the render operation; this method is the invocation entry point on {@code McpPromptDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpPromptDriver.render(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param prompt 参数 提示词；parameter prompt。
     * @param arguments 参数 arguments；parameter arguments。
     * @param attributes 参数 attributes；parameter attributes。
     * @return 返回 render 的处理结果；returns the result of the operation.
     */
    Publisher<Result> render(
            McpRuntimePrompt prompt,
            Map<String, String> arguments,
            Map<String, Object> attributes
    );

    /**
     * 中文说明：{@code Message} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责消息相关的职责与边界。
     * English summary: {@code Message} is an immutable data carrier in the current Gateway module; it owns the message-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param role 参数 角色；parameter role。
     * @param text 参数 text；parameter text。
     */
    record Message(
    /**
     * 中文说明：保存 角色 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpPromptDriver.Message} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by role; its type is {@code String}, and {@code McpPromptDriver.Message} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpPromptDriver.Message} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpPromptDriver.Message}; do not couple callers to its representation when the owning type exposes an API.
     */
    String role,
    /**
     * 中文说明：保存 text 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpPromptDriver.Message} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by text; its type is {@code String}, and {@code McpPromptDriver.Message} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpPromptDriver.Message} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpPromptDriver.Message}; do not couple callers to its representation when the owning type exposes an API.
     */
    String text) {

        /**
         * 中文说明：创建 {@code McpPromptDriver.Message} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code McpPromptDriver.Message} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param role 参数 角色；parameter role。
         * @param text 参数 text；parameter text。
         */
        public Message {
            role = required(role, "role");
            if (!Set.of("user", "assistant").contains(role)) {
                throw invalid("MCP prompt role is invalid");
            }
            text = Objects.requireNonNull(text, "text");
            if (text.length() > 512 * 1024) {
                throw invalid("MCP prompt message is too large");
            }
        }
    }

    /**
     * 中文说明：{@code Result} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Result相关的职责与边界。
     * English summary: {@code Result} is an immutable data carrier in the current Gateway module; it owns the result-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param description 参数 description；parameter description。
     * @param messages 参数 messages；parameter messages。
     */
    record Result(
    /**
     * 中文说明：保存 description 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpPromptDriver.Result} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by description; its type is {@code String}, and {@code McpPromptDriver.Result} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpPromptDriver.Result} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpPromptDriver.Result}; do not couple callers to its representation when the owning type exposes an API.
     */
    String description,
    /**
     * 中文说明：保存 messages 对应的状态、依赖或配置值；字段类型为 {@code List<Message>}，由 {@code McpPromptDriver.Result} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by messages; its type is {@code List<Message>}, and {@code McpPromptDriver.Result} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpPromptDriver.Result} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpPromptDriver.Result}; do not couple callers to its representation when the owning type exposes an API.
     */
    List<Message> messages) {

        /**
         * 中文说明：创建 {@code McpPromptDriver.Result} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code McpPromptDriver.Result} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param description 参数 description；parameter description。
         * @param messages 参数 messages；parameter messages。
         */
        public Result {
            description = description == null ? "" : description.trim();
            messages = List.copyOf(Objects.requireNonNull(
                    messages,
                    "messages"
            ));
            if (messages.isEmpty() || messages.size() > 64) {
                throw invalid("MCP prompt messages are invalid");
            }
        }
    }

    /**
     * 中文说明：执行 invalid 操作；该方法是 {@code McpPromptDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the invalid operation; this method is the invocation entry point on {@code McpPromptDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpPromptDriver.invalid(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param message 参数 消息；parameter message。
     * @return 返回 invalid 的处理结果；returns the result of the operation.
     */
    static McpProtocolException invalid(String message) {
        return new McpProtocolException(
                McpErrorCode.MCP_INVALID_PARAMS,
                message
        );
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code McpPromptDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code McpPromptDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpPromptDriver.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw invalid("MCP prompt " + field + " is required");
        }
        return value.trim();
    }
}
