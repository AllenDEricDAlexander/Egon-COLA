package top.egon.cola.component.gateway.engine.transport;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Reactive timeout operators with stable failure categories.
 * 补充说明 / Supplementary summary: {@code GatewayTransportTimeouts} 是类型，位于当前 Gateway 模块的相关包中，负责网关传输Timeouts相关的职责与边界。
 * English supplement: {@code GatewayTransportTimeouts} is a type in the current Gateway module; it owns the gateway transport timeouts-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayTransportTimeouts {

    /**
     * 中文说明：创建 {@code GatewayTransportTimeouts} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayTransportTimeouts} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     */
    private GatewayTransportTimeouts() {
    }

    /**
     * 中文说明：执行 connect 操作；该方法是 {@code GatewayTransportTimeouts} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the connect operation; this method is the invocation entry point on {@code GatewayTransportTimeouts} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTransportTimeouts.connect(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param timeout 参数 超时；parameter timeout。
     * @return 返回 connect 的处理结果；returns the result of the operation.
     */
    public static <T> Mono<T> connect(
            Mono<T> source,
            Duration timeout) {
        return monoTimeout(
                source,
                timeout,
                GatewayConnectTimeoutException::new
        );
    }

    /**
     * 中文说明：执行 响应Headers 操作；该方法是 {@code GatewayTransportTimeouts} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the response headers operation; this method is the invocation entry point on {@code GatewayTransportTimeouts} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTransportTimeouts.responseHeaders(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param timeout 参数 超时；parameter timeout。
     * @return 返回 响应Headers 的处理结果；returns the result of the operation.
     */
    public static <T> Mono<T> responseHeaders(
            Mono<T> source,
            Duration timeout) {
        return monoTimeout(
                source,
                timeout,
                GatewayResponseHeaderTimeoutException::new
        );
    }

    /**
     * 中文说明：执行 请求Idle 操作；该方法是 {@code GatewayTransportTimeouts} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the request idle operation; this method is the invocation entry point on {@code GatewayTransportTimeouts} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTransportTimeouts.requestIdle(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param timeout 参数 超时；parameter timeout。
     * @return 返回 请求Idle 的处理结果；returns the result of the operation.
     */
    public static <T> Flux<T> requestIdle(
            Flux<T> source,
            Duration timeout) {
        return fluxIdleTimeout(
                source,
                timeout,
                () -> new GatewayStreamIdleTimeoutException(
                        GatewayStreamDirection.REQUEST
                )
        );
    }

    /**
     * 中文说明：执行 响应Idle 操作；该方法是 {@code GatewayTransportTimeouts} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the response idle operation; this method is the invocation entry point on {@code GatewayTransportTimeouts} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTransportTimeouts.responseIdle(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param timeout 参数 超时；parameter timeout。
     * @return 返回 响应Idle 的处理结果；returns the result of the operation.
     */
    public static <T> Flux<T> responseIdle(
            Flux<T> source,
            Duration timeout) {
        return fluxIdleTimeout(
                source,
                timeout,
                () -> new GatewayStreamIdleTimeoutException(
                        GatewayStreamDirection.RESPONSE
                )
        );
    }

    /**
     * 中文说明：执行 total 操作；该方法是 {@code GatewayTransportTimeouts} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the total operation; this method is the invocation entry point on {@code GatewayTransportTimeouts} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTransportTimeouts.total(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param timeout 参数 超时；parameter timeout。
     * @return 返回 total 的处理结果；returns the result of the operation.
     */
    public static <T> Mono<T> total(
            Mono<T> source,
            Optional<Duration> timeout) {
        Objects.requireNonNull(timeout, "timeout");
        return timeout.map(value -> monoTimeout(
                        source,
                        value,
                        GatewayTotalTimeoutException::new
                ))
                .orElse(source);
    }

    /**
     * 中文说明：执行 total 操作；该方法是 {@code GatewayTransportTimeouts} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the total operation; this method is the invocation entry point on {@code GatewayTransportTimeouts} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTransportTimeouts.total(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param timeout 参数 超时；parameter timeout。
     * @return 返回 total 的处理结果；returns the result of the operation.
     */
    public static <T> Flux<T> total(
            Flux<T> source,
            Optional<Duration> timeout) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(timeout, "timeout");
        return timeout.map(value -> source.takeUntilOther(
                        Mono.delay(requirePositive(value))
                                .flatMap(ignored -> Mono.error(
                                        new GatewayTotalTimeoutException()
                                ))
                ))
                .orElse(source);
    }

    /**
     * 中文说明：执行 WebSocketIdle 操作；该方法是 {@code GatewayTransportTimeouts} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the websocket idle operation; this method is the invocation entry point on {@code GatewayTransportTimeouts} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTransportTimeouts.websocketIdle(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param timeout 参数 超时；parameter timeout。
     * @return 返回 WebSocketIdle 的处理结果；returns the result of the operation.
     */
    public static <T> Flux<T> websocketIdle(
            Flux<T> source,
            Duration timeout) {
        return fluxIdleTimeout(
                source,
                timeout,
                GatewayWebSocketIdleTimeoutException::new
        );
    }

    /**
     * 中文说明：执行 mono超时 操作；该方法是 {@code GatewayTransportTimeouts} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the mono timeout operation; this method is the invocation entry point on {@code GatewayTransportTimeouts} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTransportTimeouts.monoTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param timeout 参数 超时；parameter timeout。
     * @param failure 参数 failure；parameter failure。
     * @return 返回 mono超时 的处理结果；returns the result of the operation.
     */
    private static <T> Mono<T> monoTimeout(
            Mono<T> source,
            Duration timeout,
            Supplier<? extends RuntimeException> failure) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(failure, "failure");
        return source.timeout(
                requirePositive(timeout),
                Mono.defer(() -> Mono.error(failure.get()))
        );
    }

    /**
     * 中文说明：执行 fluxIdle超时 操作；该方法是 {@code GatewayTransportTimeouts} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the flux idle timeout operation; this method is the invocation entry point on {@code GatewayTransportTimeouts} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTransportTimeouts.fluxIdleTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param timeout 参数 超时；parameter timeout。
     * @param failure 参数 failure；parameter failure。
     * @return 返回 fluxIdle超时 的处理结果；returns the result of the operation.
     */
    private static <T> Flux<T> fluxIdleTimeout(
            Flux<T> source,
            Duration timeout,
            Supplier<? extends RuntimeException> failure) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(failure, "failure");
        Publisher<T> fallback = Flux.defer(() ->
                Flux.error(failure.get())
        );
        return source.timeout(requirePositive(timeout), fallback);
    }

    /**
     * 中文说明：执行 requirePositive 操作；该方法是 {@code GatewayTransportTimeouts} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the require positive operation; this method is the invocation entry point on {@code GatewayTransportTimeouts} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTransportTimeouts.requirePositive(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param timeout 参数 超时；parameter timeout。
     * @return 返回 requirePositive 的处理结果；returns the result of the operation.
     */
    private static Duration requirePositive(Duration timeout) {
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException(
                    "timeout must be positive"
            );
        }
        return timeout;
    }
}
