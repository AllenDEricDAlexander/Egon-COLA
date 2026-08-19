package top.egon.cola.component.gateway.mcp.prompt.service;

import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimePrompt;
import top.egon.cola.component.gateway.mcp.prompt.domain.StrictPromptTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 中文说明：{@code StaticPromptDriver} 是驱动器，位于当前 Gateway 模块的相关包中，负责Static提示词驱动器相关的职责与边界。
 * English summary: {@code StaticPromptDriver} is a static prompt driver driver in the current Gateway module; it owns the static prompt driver-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class StaticPromptDriver implements McpPromptDriver {

    /**
     * 中文说明：表示 SOURCETYPES 这一固定值；它属于 {@code StaticPromptDriver} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value source types; it is a state, type, or protocol value of {@code StaticPromptDriver} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code StaticPromptDriver} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code StaticPromptDriver}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Set<String> SOURCE_TYPES = Set.of(
            "LOCAL_TEMPLATE",
            "STATIC_TEMPLATE",
            "STRICT_TEMPLATE"
    );

    /**
     * 中文说明：保存 模板 对应的状态、依赖或配置值；字段类型为 {@code StrictPromptTemplate}，由 {@code StaticPromptDriver} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by template; its type is {@code StrictPromptTemplate}, and {@code StaticPromptDriver} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code StaticPromptDriver} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code StaticPromptDriver}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final StrictPromptTemplate template;

    /**
     * 中文说明：创建 {@code StaticPromptDriver} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code StaticPromptDriver} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param template 参数 模板；parameter template。
     */
    public StaticPromptDriver(StrictPromptTemplate template) {
        this.template = Objects.requireNonNull(template, "template");
    }

    /**
     * 中文说明：执行 sourceTypes 操作；该方法是 {@code StaticPromptDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the source types operation; this method is the invocation entry point on {@code StaticPromptDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code StaticPromptDriver.sourceTypes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 sourceTypes 的处理结果；returns the result of the operation.
     */
    @Override
    public Set<String> sourceTypes() {
        return SOURCE_TYPES;
    }

    /**
     * 中文说明：执行 render 操作；该方法是 {@code StaticPromptDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the render operation; this method is the invocation entry point on {@code StaticPromptDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code StaticPromptDriver.render(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param prompt 参数 提示词；parameter prompt。
     * @param arguments 参数 arguments；parameter arguments。
     * @param attributes 参数 attributes；parameter attributes。
     * @return 返回 render 的处理结果；returns the result of the operation.
     */
    @Override
    public Mono<Result> render(
            McpRuntimePrompt prompt,
            Map<String, String> arguments,
            Map<String, Object> attributes) {
        if (prompt.template() == null) {
            throw McpPromptDriver.invalid(
                    "MCP prompt template is not configured"
            );
        }
        String rendered = template.render(
                prompt.template(),
                prompt.arguments(),
                arguments
        );
        return Mono.just(new Result(
                prompt.description(),
                List.of(new Message("user", rendered))
        ));
    }
}
