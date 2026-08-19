package top.egon.cola.component.gateway.engine.http.service;

import top.egon.cola.component.gateway.engine.http.domain.GatewayHttpFlushMode;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Streaming HTTP response plus an idempotent action for abandoning resources
 * before, during, or after body subscription.
 *
 * <p>The body producer owns buffers that have not been emitted. It must create
 * pooled elements lazily or release them when its subscription is cancelled.
 * The downstream consumer owns every emitted buffer until it transfers or
 * releases that buffer. A publisher of pre-created pooled buffers, such as a
 * plain {@code Flux.just}, cannot satisfy this cancellation contract.</p>
 * 补充说明 / Supplementary summary: {@code GatewayOutboundHttpResponse} 是类型，位于当前 Gateway 模块的相关包中，负责网关OutboundHttp响应相关的职责与边界。
 * English supplement: {@code GatewayOutboundHttpResponse} is a type in the current Gateway module; it owns the gateway outbound http response-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayOutboundHttpResponse {

    /**
     * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayOutboundHttpResponse} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code int}, and {@code GatewayOutboundHttpResponse} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayOutboundHttpResponse} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayOutboundHttpResponse}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final int status;

    /**
     * 中文说明：保存 headers 对应的状态、依赖或配置值；字段类型为 {@code Map<String, List<String>>}，由 {@code GatewayOutboundHttpResponse} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by headers; its type is {@code Map<String, List<String>>}, and {@code GatewayOutboundHttpResponse} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayOutboundHttpResponse} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayOutboundHttpResponse}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Map<String, List<String>> headers;

    /**
     * 中文说明：保存 body 对应的状态、依赖或配置值；字段类型为 {@code Flux<DataBuffer>}，由 {@code GatewayOutboundHttpResponse} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by body; its type is {@code Flux<DataBuffer>}, and {@code GatewayOutboundHttpResponse} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayOutboundHttpResponse} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayOutboundHttpResponse}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Flux<DataBuffer> body;

    /**
     * 中文说明：保存 flushMode 对应的状态、依赖或配置值；字段类型为 {@code GatewayHttpFlushMode}，由 {@code GatewayOutboundHttpResponse} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by flush mode; its type is {@code GatewayHttpFlushMode}, and {@code GatewayOutboundHttpResponse} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayOutboundHttpResponse} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayOutboundHttpResponse}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayHttpFlushMode flushMode;

    /**
     * 中文说明：保存 abandonAction 对应的状态、依赖或配置值；字段类型为 {@code Runnable}，由 {@code GatewayOutboundHttpResponse} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by abandon action; its type is {@code Runnable}, and {@code GatewayOutboundHttpResponse} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayOutboundHttpResponse} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayOutboundHttpResponse}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Runnable abandonAction;

    /**
     * 中文说明：保存 abandoned 对应的状态、依赖或配置值；字段类型为 {@code AtomicBoolean}，由 {@code GatewayOutboundHttpResponse} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by abandoned; its type is {@code AtomicBoolean}, and {@code GatewayOutboundHttpResponse} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayOutboundHttpResponse} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayOutboundHttpResponse}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicBoolean abandoned = new AtomicBoolean();

    /**
     * 中文说明：创建 {@code GatewayOutboundHttpResponse} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayOutboundHttpResponse} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param status 参数 status；parameter status。
     * @param headers 参数 headers；parameter headers。
     * @param body 参数 body；parameter body。
     */
    public GatewayOutboundHttpResponse(
            int status,
            Map<String, List<String>> headers,
            Flux<DataBuffer> body) {
        this(status, headers, body, GatewayHttpFlushMode.STANDARD);
    }

    /**
     * 中文说明：创建 {@code GatewayOutboundHttpResponse} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayOutboundHttpResponse} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param status 参数 status；parameter status。
     * @param headers 参数 headers；parameter headers。
     * @param body 参数 body；parameter body。
     * @param flushMode 参数 flushMode；parameter flush mode。
     */
    public GatewayOutboundHttpResponse(
            int status,
            Map<String, List<String>> headers,
            Flux<DataBuffer> body,
            GatewayHttpFlushMode flushMode) {
        this(status, headers, body, flushMode, () -> {
        });
    }

    /**
     * 中文说明：创建 {@code GatewayOutboundHttpResponse} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayOutboundHttpResponse} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param status 参数 status；parameter status。
     * @param headers 参数 headers；parameter headers。
     * @param body 参数 body；parameter body。
     * @param flushMode 参数 flushMode；parameter flush mode。
     * @param abandonAction 参数 abandonAction；parameter abandon action。
     */
    GatewayOutboundHttpResponse(
            int status,
            Map<String, List<String>> headers,
            Flux<DataBuffer> body,
            GatewayHttpFlushMode flushMode,
            Runnable abandonAction) {
        if (status < 100 || status > 599) {
            throw new IllegalArgumentException("invalid HTTP status");
        }
        this.status = status;
        this.headers = Map.copyOf(
                Objects.requireNonNull(headers, "headers")
        );
        this.body = Objects.requireNonNull(body, "body");
        this.flushMode = Objects.requireNonNull(flushMode, "flushMode");
        this.abandonAction = Objects.requireNonNull(
                abandonAction,
                "abandonAction"
        );
    }

    /**
     * 中文说明：执行 status 操作；该方法是 {@code GatewayOutboundHttpResponse} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the status operation; this method is the invocation entry point on {@code GatewayOutboundHttpResponse} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOutboundHttpResponse.status(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 status 的处理结果；returns the result of the operation.
     */
    public int status() {
        return status;
    }

    /**
     * 中文说明：执行 headers 操作；该方法是 {@code GatewayOutboundHttpResponse} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the headers operation; this method is the invocation entry point on {@code GatewayOutboundHttpResponse} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOutboundHttpResponse.headers(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 headers 的处理结果；returns the result of the operation.
     */
    public Map<String, List<String>> headers() {
        return headers;
    }

    /**
     * 中文说明：执行 body 操作；该方法是 {@code GatewayOutboundHttpResponse} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the body operation; this method is the invocation entry point on {@code GatewayOutboundHttpResponse} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOutboundHttpResponse.body(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 body 的处理结果；returns the result of the operation.
     */
    public Flux<DataBuffer> body() {
        return body;
    }

    /**
     * 中文说明：执行 flushMode 操作；该方法是 {@code GatewayOutboundHttpResponse} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the flush mode operation; this method is the invocation entry point on {@code GatewayOutboundHttpResponse} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOutboundHttpResponse.flushMode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 flushMode 的处理结果；returns the result of the operation.
     */
    public GatewayHttpFlushMode flushMode() {
        return flushMode;
    }

    /**
     * 中文说明：执行 withBody 操作；该方法是 {@code GatewayOutboundHttpResponse} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the with body operation; this method is the invocation entry point on {@code GatewayOutboundHttpResponse} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOutboundHttpResponse.withBody(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param replacement 参数 replacement；parameter replacement。
     * @return 返回 withBody 的处理结果；returns the result of the operation.
     */
    GatewayOutboundHttpResponse withBody(Flux<DataBuffer> replacement) {
        return withHeadersAndBody(headers, replacement);
    }

    /**
     * 中文说明：执行 withHeadersAndBody 操作；该方法是 {@code GatewayOutboundHttpResponse} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the with headers and body operation; this method is the invocation entry point on {@code GatewayOutboundHttpResponse} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOutboundHttpResponse.withHeadersAndBody(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param replacementHeaders 参数 replacementHeaders；parameter replacement headers。
     * @param replacementBody 参数 replacementBody；parameter replacement body。
     * @return 返回 withHeadersAndBody 的处理结果；returns the result of the operation.
     */
    public GatewayOutboundHttpResponse withHeadersAndBody(
            Map<String, List<String>> replacementHeaders,
            Flux<DataBuffer> replacementBody) {
        return new GatewayOutboundHttpResponse(
                status,
                replacementHeaders,
                replacementBody,
                flushMode,
                this::abandon
        );
    }

    /**
     * 中文说明：执行 withFlushMode 操作；该方法是 {@code GatewayOutboundHttpResponse} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the with flush mode operation; this method is the invocation entry point on {@code GatewayOutboundHttpResponse} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOutboundHttpResponse.withFlushMode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param replacement 参数 replacement；parameter replacement。
     * @return 返回 withFlushMode 的处理结果；returns the result of the operation.
     */
    public GatewayOutboundHttpResponse withFlushMode(
            GatewayHttpFlushMode replacement) {
        return new GatewayOutboundHttpResponse(
                status,
                headers,
                body,
                replacement,
                this::abandon
        );
    }

    /**
     * 中文说明：执行 onAbandon 操作；该方法是 {@code GatewayOutboundHttpResponse} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the on abandon operation; this method is the invocation entry point on {@code GatewayOutboundHttpResponse} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOutboundHttpResponse.onAbandon(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param action 参数 action；parameter action。
     * @return 返回 onAbandon 的处理结果；returns the result of the operation.
     */
    GatewayOutboundHttpResponse onAbandon(Runnable action) {
        Objects.requireNonNull(action, "action");
        return new GatewayOutboundHttpResponse(
                status,
                headers,
                body,
                flushMode,
                () -> {
                    try {
                        abandon();
                    } finally {
                        action.run();
                    }
                }
        );
    }

    /**
     * 中文说明：执行 abandon 操作；该方法是 {@code GatewayOutboundHttpResponse} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the abandon operation; this method is the invocation entry point on {@code GatewayOutboundHttpResponse} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOutboundHttpResponse.abandon(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    void abandon() {
        if (abandoned.compareAndSet(false, true)) {
            abandonAction.run();
        }
    }

    /**
     * 中文说明：执行 text 操作；该方法是 {@code GatewayOutboundHttpResponse} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the text operation; this method is the invocation entry point on {@code GatewayOutboundHttpResponse} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOutboundHttpResponse.text(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param status 参数 status；parameter status。
     * @param content 参数 content；parameter content。
     * @return 返回 text 的处理结果；returns the result of the operation.
     */
    public static GatewayOutboundHttpResponse text(
            int status,
            String content) {
        return new GatewayOutboundHttpResponse(
                status,
                Map.of("content-type", List.of("text/plain; charset=UTF-8")),
                Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(
                        content.getBytes(
                                java.nio.charset.StandardCharsets.UTF_8
                        )
                ))
        );
    }
}
