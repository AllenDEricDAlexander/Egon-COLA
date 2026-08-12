package top.egon.cola.component.gateway.engine.http.proxy;

import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.engine.http.GatewayOutboundHttpResponse;

/**
 * 中文说明：{@code GatewayHttpProxyStrategy} 是接口契约，位于当前 Gateway 模块的相关包中，负责网关Http代理Strategy相关的职责与边界。
 * English summary: {@code GatewayHttpProxyStrategy} is an interface contract in the current Gateway module; it owns the gateway http proxy strategy-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@FunctionalInterface
public interface GatewayHttpProxyStrategy {

    /**
     * 中文说明：执行 代理 操作；该方法是 {@code GatewayHttpProxyStrategy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the proxy operation; this method is the invocation entry point on {@code GatewayHttpProxyStrategy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpProxyStrategy.proxy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param context 参数 context；parameter context。
     * @return 返回 代理 的处理结果；returns the result of the operation.
     */
    Mono<GatewayOutboundHttpResponse> proxy(GatewayHttpProxyContext context);
}
