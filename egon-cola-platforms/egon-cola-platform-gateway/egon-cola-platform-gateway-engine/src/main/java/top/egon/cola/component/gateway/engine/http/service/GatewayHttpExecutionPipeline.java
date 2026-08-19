package top.egon.cola.component.gateway.engine.http.service;

import top.egon.cola.component.gateway.engine.http.service.GatewayOutboundHttpResponse;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.core.execution.DefaultGatewayExecutor;
import top.egon.cola.component.gateway.core.execution.GatewayExecutor;
import top.egon.cola.component.gateway.core.exchange.GatewayExchange;
import top.egon.cola.component.gateway.core.exchange.GatewayResponse;
import top.egon.cola.component.gateway.core.filter.DefaultGatewayFilterChain;
import top.egon.cola.component.gateway.core.filter.GatewayFilter;
import top.egon.cola.component.gateway.core.filter.GatewayFilterChain;
import top.egon.cola.component.gateway.core.filter.GatewayFilterStage;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayWebSocketHandshakeResult;

import java.util.List;

/**
 * 中文说明：{@code GatewayHttpExecutionPipeline} 是类型，位于当前 Gateway 模块的相关包中，负责网关HttpExecutionPipeline相关的职责与边界。
 * English summary: {@code GatewayHttpExecutionPipeline} is a type in the current Gateway module; it owns the gateway http execution pipeline-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayHttpExecutionPipeline {

    /**
     * 中文说明：保存 executor 对应的状态、依赖或配置值；字段类型为 {@code GatewayExecutor}，由 {@code GatewayHttpExecutionPipeline} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by executor; its type is {@code GatewayExecutor}, and {@code GatewayHttpExecutionPipeline} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayHttpExecutionPipeline} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpExecutionPipeline}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayExecutor executor;

    /**
     * 中文说明：创建 {@code GatewayHttpExecutionPipeline} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayHttpExecutionPipeline} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     */
    public GatewayHttpExecutionPipeline() {
        executor = new DefaultGatewayExecutor(
                new DefaultGatewayFilterChain(List.of(
                        filter(
                                "gateway-http-cors",
                                GatewayFilterStage.CORS,
                                (exchange, chain) -> exchange.cors(chain)
                        ),
                        filter(
                                "gateway-http-security",
                                GatewayFilterStage.AUTHENTICATION,
                                (exchange, chain) ->
                                        exchange.security(chain)
                        ),
                        filter(
                                "gateway-http-governance",
                                GatewayFilterStage.RATE_CONCURRENCY,
                                (exchange, chain) ->
                                        exchange.governance(chain)
                        ),
                        filter(
                                "gateway-http-invocation",
                                GatewayFilterStage.INVOCATION,
                                (exchange, chain) -> exchange.invoke()
                        )
                )),
                (exchange, failure) -> stage(exchange).fail(failure)
        );
    }

    /**
     * 中文说明：执行 execute 操作；该方法是 {@code GatewayHttpExecutionPipeline} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the execute operation; this method is the invocation entry point on {@code GatewayHttpExecutionPipeline} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpExecutionPipeline.execute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param exchange 参数 exchange；parameter exchange。
     * @return 返回 execute 的处理结果；returns the result of the operation.
     */
    public Mono<GatewayOutboundHttpResponse> execute(
            AbstractGatewayHttpStageExchange exchange) {
        return Mono.from(executor.execute(exchange))
                .map(ignored -> exchange.outbound());
    }

    /**
     * 中文说明：执行 executeWebSocket 操作；该方法是 {@code GatewayHttpExecutionPipeline} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the execute web socket operation; this method is the invocation entry point on {@code GatewayHttpExecutionPipeline} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpExecutionPipeline.executeWebSocket(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param exchange 参数 exchange；parameter exchange。
     * @return 返回 executeWebSocket 的处理结果；returns the result of the operation.
     */
    public Mono<GatewayWebSocketHandshakeResult> executeWebSocket(
            AbstractGatewayHttpStageExchange exchange) {
        return Mono.from(executor.execute(exchange))
                .map(ignored -> exchange.webSocketResult());
    }

    /**
     * 中文说明：执行 过滤器 操作；该方法是 {@code GatewayHttpExecutionPipeline} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the filter operation; this method is the invocation entry point on {@code GatewayHttpExecutionPipeline} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpExecutionPipeline.filter(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param stage 参数 stage；parameter stage。
     * @param invocation 参数 invocation；parameter invocation。
     * @return 返回 过滤器 的处理结果；returns the result of the operation.
     */
    private GatewayFilter filter(
            String id,
            GatewayFilterStage stage,
            StageInvocation invocation) {
        return new GatewayFilter() {
            /**
             * 中文说明：执行 id 操作；该方法是 {@code GatewayHttpExecutionPipeline} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the id operation; this method is the invocation entry point on {@code GatewayHttpExecutionPipeline} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpExecutionPipeline.id(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             * @return 返回 id 的处理结果；returns the result of the operation.
             */
            @Override
            public String id() {
                return id;
            }

            /**
             * 中文说明：执行 stage 操作；该方法是 {@code GatewayHttpExecutionPipeline} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the stage operation; this method is the invocation entry point on {@code GatewayHttpExecutionPipeline} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpExecutionPipeline.stage(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             * @return 返回 stage 的处理结果；returns the result of the operation.
             */
            @Override
            public GatewayFilterStage stage() {
                return stage;
            }

            /**
             * 中文说明：执行 order 操作；该方法是 {@code GatewayHttpExecutionPipeline} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the order operation; this method is the invocation entry point on {@code GatewayHttpExecutionPipeline} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpExecutionPipeline.order(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             * @return 返回 order 的处理结果；returns the result of the operation.
             */
            @Override
            public int order() {
                return 0;
            }

            /**
             * 中文说明：执行 过滤器 操作；该方法是 {@code GatewayHttpExecutionPipeline} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the filter operation; this method is the invocation entry point on {@code GatewayHttpExecutionPipeline} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpExecutionPipeline.filter(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             * @param exchange 参数 exchange；parameter exchange。
             * @param chain 参数 chain；parameter chain。
             * @return 返回 过滤器 的处理结果；returns the result of the operation.
             */
            @Override
            public Publisher<GatewayResponse> filter(
                    GatewayExchange exchange,
                    GatewayFilterChain chain) {
                return invocation.invoke(
                        GatewayHttpExecutionPipeline.stage(exchange),
                        chain
                );
            }
        };
    }

    /**
     * 中文说明：执行 stage 操作；该方法是 {@code GatewayHttpExecutionPipeline} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the stage operation; this method is the invocation entry point on {@code GatewayHttpExecutionPipeline} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpExecutionPipeline.stage(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param exchange 参数 exchange；parameter exchange。
     * @return 返回 stage 的处理结果；returns the result of the operation.
     */
    private static AbstractGatewayHttpStageExchange stage(
            GatewayExchange exchange) {
        if (exchange instanceof AbstractGatewayHttpStageExchange staged) {
            return staged;
        }
        throw new IllegalArgumentException(
                "gateway HTTP pipeline requires a staged HTTP exchange"
        );
    }

    /**
     * 中文说明：{@code StageInvocation} 是接口契约，位于当前 Gateway 模块的相关包中，负责StageInvocation相关的职责与边界。
     * English summary: {@code StageInvocation} is an interface contract in the current Gateway module; it owns the stage invocation-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    @FunctionalInterface
    private interface StageInvocation {

        /**
         * 中文说明：执行 invoke 操作；该方法是 {@code GatewayHttpExecutionPipeline.StageInvocation} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the invoke operation; this method is the invocation entry point on {@code GatewayHttpExecutionPipeline.StageInvocation} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpExecutionPipeline.StageInvocation.invoke(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param exchange 参数 exchange；parameter exchange。
         * @param chain 参数 chain；parameter chain。
         * @return 返回 invoke 的处理结果；returns the result of the operation.
         */
        Publisher<GatewayResponse> invoke(
                AbstractGatewayHttpStageExchange exchange,
                GatewayFilterChain chain);
    }
}
