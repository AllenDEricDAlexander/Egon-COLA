package top.egon.cola.component.gateway.engine.common.transport.service;

import top.egon.cola.component.gateway.engine.common.transport.domain.GatewayStreamDirection;

import java.util.Objects;

/**
 * 中文说明：{@code GatewayStreamIdleTimeoutException} 是异常类型，位于当前 Gateway 模块的相关包中，负责网关StreamIdle超时Exception相关的职责与边界。
 * English summary: {@code GatewayStreamIdleTimeoutException} is a gateway stream idle timeout exception exception in the current Gateway module; it owns the gateway stream idle timeout exception-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayStreamIdleTimeoutException
        extends GatewayTransportTimeoutException {

    /**
     * 中文说明：保存 direction 对应的状态、依赖或配置值；字段类型为 {@code GatewayStreamDirection}，由 {@code GatewayStreamIdleTimeoutException} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by direction; its type is {@code GatewayStreamDirection}, and {@code GatewayStreamIdleTimeoutException} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayStreamIdleTimeoutException} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayStreamIdleTimeoutException}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayStreamDirection direction;

    /**
     * 中文说明：创建 {@code GatewayStreamIdleTimeoutException} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayStreamIdleTimeoutException} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param direction 参数 direction；parameter direction。
     */
    GatewayStreamIdleTimeoutException(GatewayStreamDirection direction) {
        super(
                "GATEWAY_" + direction + "_STREAM_IDLE_TIMEOUT",
                direction.name().toLowerCase() + " stream timed out"
        );
        this.direction = Objects.requireNonNull(direction, "direction");
    }

    /**
     * 中文说明：执行 direction 操作；该方法是 {@code GatewayStreamIdleTimeoutException} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the direction operation; this method is the invocation entry point on {@code GatewayStreamIdleTimeoutException} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayStreamIdleTimeoutException.direction(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 direction 的处理结果；returns the result of the operation.
     */
    public GatewayStreamDirection direction() {
        return direction;
    }
}
