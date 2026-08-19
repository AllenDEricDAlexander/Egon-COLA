package top.egon.cola.component.gateway.engine.http.adapter;

import top.egon.cola.component.gateway.engine.http.service.GatewayOutboundHttpResponse;

import top.egon.cola.component.gateway.engine.http.domain.HttpUpstreamRequest;

import reactor.core.publisher.Mono;

/**
 * 中文说明：{@code HttpUpstreamAdapter} 是接口契约，位于当前 Gateway 模块的相关包中，负责HttpUpstreamAdapter相关的职责与边界。
 * English summary: {@code HttpUpstreamAdapter} is an interface contract in the current Gateway module; it owns the http upstream adapter-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@FunctionalInterface
public interface HttpUpstreamAdapter {

    /**
     * 中文说明：执行 invoke 操作；该方法是 {@code HttpUpstreamAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the invoke operation; this method is the invocation entry point on {@code HttpUpstreamAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code HttpUpstreamAdapter.invoke(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 invoke 的处理结果；returns the result of the operation.
     */
    Mono<GatewayOutboundHttpResponse> invoke(HttpUpstreamRequest request);
}
