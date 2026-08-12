package top.egon.cola.component.gateway.engine.http.proxy;

import top.egon.cola.component.gateway.contract.rule.GatewayRequestBodyMode;

import java.util.Objects;

/**
 * 中文说明：{@code GatewayHttpProxyStrategySelector} 是类型，位于当前 Gateway 模块的相关包中，负责网关Http代理StrategySelector相关的职责与边界。
 * English summary: {@code GatewayHttpProxyStrategySelector} is a type in the current Gateway module; it owns the gateway http proxy strategy selector-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayHttpProxyStrategySelector {

    /**
     * 中文说明：保存 aggregated 对应的状态、依赖或配置值；字段类型为 {@code GatewayHttpProxyStrategy}，由 {@code GatewayHttpProxyStrategySelector} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by aggregated; its type is {@code GatewayHttpProxyStrategy}, and {@code GatewayHttpProxyStrategySelector} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayHttpProxyStrategySelector} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpProxyStrategySelector}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayHttpProxyStrategy aggregated;

    /**
     * 中文说明：保存 streaming 对应的状态、依赖或配置值；字段类型为 {@code GatewayHttpProxyStrategy}，由 {@code GatewayHttpProxyStrategySelector} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by streaming; its type is {@code GatewayHttpProxyStrategy}, and {@code GatewayHttpProxyStrategySelector} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayHttpProxyStrategySelector} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpProxyStrategySelector}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayHttpProxyStrategy streaming;

    /**
     * 中文说明：创建 {@code GatewayHttpProxyStrategySelector} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayHttpProxyStrategySelector} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param aggregated 参数 aggregated；parameter aggregated。
     * @param streaming 参数 streaming；parameter streaming。
     */
    public GatewayHttpProxyStrategySelector(
            GatewayHttpProxyStrategy aggregated,
            GatewayHttpProxyStrategy streaming) {
        this.aggregated = Objects.requireNonNull(aggregated, "aggregated");
        this.streaming = Objects.requireNonNull(streaming, "streaming");
    }

    /**
     * 中文说明：执行 select 操作；该方法是 {@code GatewayHttpProxyStrategySelector} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the select operation; this method is the invocation entry point on {@code GatewayHttpProxyStrategySelector} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpProxyStrategySelector.select(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param mode 参数 mode；parameter mode。
     * @return 返回 select 的处理结果；returns the result of the operation.
     */
    public GatewayHttpProxyStrategy select(GatewayRequestBodyMode mode) {
        return switch (Objects.requireNonNull(mode, "mode")) {
            case AGGREGATED -> aggregated;
            case STREAMING -> streaming;
        };
    }
}
