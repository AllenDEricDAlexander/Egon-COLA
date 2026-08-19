package top.egon.cola.component.gateway.engine.http.proxy.service;

import top.egon.cola.component.gateway.engine.http.service.GatewayOutboundHttpResponse;

import top.egon.cola.component.gateway.engine.http.proxy.domain.GatewayHttpProxyContext;

import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.engine.http.service.GatewayBodySizeLimiter;
import top.egon.cola.component.gateway.engine.http.domain.HttpUpstreamRequest;
import top.egon.cola.component.gateway.engine.http.common.logging.GatewayBodyLogDirection;

/**
 * 中文说明：{@code AggregatedHttpProxyStrategy} 是策略实现，位于当前 Gateway 模块的相关包中，负责AggregatedHttp代理Strategy相关的职责与边界。
 * English summary: {@code AggregatedHttpProxyStrategy} is a aggregated http proxy strategy strategy in the current Gateway module; it owns the aggregated http proxy strategy-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class AggregatedHttpProxyStrategy
        implements GatewayHttpProxyStrategy {

    /**
     * 中文说明：保存 limiter 对应的状态、依赖或配置值；字段类型为 {@code GatewayBodySizeLimiter}，由 {@code AggregatedHttpProxyStrategy} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by limiter; its type is {@code GatewayBodySizeLimiter}, and {@code AggregatedHttpProxyStrategy} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code AggregatedHttpProxyStrategy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code AggregatedHttpProxyStrategy}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayBodySizeLimiter limiter =
            new GatewayBodySizeLimiter();

    /**
     * 中文说明：保存 响应Semantics 对应的状态、依赖或配置值；字段类型为 {@code GatewayHttpResponseSemantics}，由 {@code AggregatedHttpProxyStrategy} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by response semantics; its type is {@code GatewayHttpResponseSemantics}, and {@code AggregatedHttpProxyStrategy} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code AggregatedHttpProxyStrategy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code AggregatedHttpProxyStrategy}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayHttpResponseSemantics responseSemantics =
            new GatewayHttpResponseSemantics(limiter);

    /**
     * 中文说明：执行 代理 操作；该方法是 {@code AggregatedHttpProxyStrategy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the proxy operation; this method is the invocation entry point on {@code AggregatedHttpProxyStrategy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code AggregatedHttpProxyStrategy.proxy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param context 参数 context；parameter context。
     * @return 返回 代理 的处理结果；returns the result of the operation.
     */
    @Override
    public Mono<GatewayOutboundHttpResponse> proxy(
            GatewayHttpProxyContext context) {
        long limit = context.policy().maxRequestBodyBytes();
        limiter.validateRequestHeaders(context.headers(), limit);
        return limiter.aggregateRequest(context.body(), limit)
                .flatMap(body -> context.adapter().invoke(
                        new HttpUpstreamRequest(
                                context.provider(),
                                context.method(),
                                context.pathAndQuery(),
                                context.headers(),
                                context.observeBody(
                                        Flux.defer(() -> Flux.just(
                                                DefaultDataBufferFactory
                                                        .sharedInstance
                                                        .wrap(body)
                                        )),
                                        GatewayBodyLogDirection.REQUEST,
                                        context.headers()
                                ),
                                context.policy().connectTimeout(),
                                context.policy().responseHeaderTimeout(),
                                context.policy().streamIdleTimeout(),
                                context.policy().totalTimeout(),
                                true
                        )
                ))
                .map(response -> responseSemantics.apply(response, context))
                .map(response -> response.withHeadersAndBody(
                        response.headers(),
                        context.observeBody(
                                response.body(),
                                GatewayBodyLogDirection.RESPONSE,
                                response.headers()
                        )
                ));
    }
}
