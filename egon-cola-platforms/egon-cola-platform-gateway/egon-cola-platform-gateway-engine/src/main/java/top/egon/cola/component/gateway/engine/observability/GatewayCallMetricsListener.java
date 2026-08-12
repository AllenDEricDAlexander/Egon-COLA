package top.egon.cola.component.gateway.engine.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import top.egon.cola.component.gateway.contract.observability.GatewayCallEventV1;

import java.time.Duration;
import java.util.Objects;

/**
 * 中文说明：{@code GatewayCallMetricsListener} 是监听器，位于当前 Gateway 模块的相关包中，负责网关调用Metrics监听器相关的职责与边界。
 * English summary: {@code GatewayCallMetricsListener} is a gateway call metrics listener listener in the current Gateway module; it owns the gateway call metrics listener-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayCallMetricsListener
        implements GatewayCallCompletionListener {

    /**
     * 中文说明：保存 注册表 对应的状态、依赖或配置值；字段类型为 {@code MeterRegistry}，由 {@code GatewayCallMetricsListener} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by registry; its type is {@code MeterRegistry}, and {@code GatewayCallMetricsListener} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallMetricsListener} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallMetricsListener}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final MeterRegistry registry;

    /**
     * 中文说明：创建 {@code GatewayCallMetricsListener} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayCallMetricsListener} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param registry 参数 注册表；parameter registry。
     */
    public GatewayCallMetricsListener(MeterRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    /**
     * 中文说明：执行 onComplete 操作；该方法是 {@code GatewayCallMetricsListener} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the on complete operation; this method is the invocation entry point on {@code GatewayCallMetricsListener} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallMetricsListener.onComplete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param event 参数 事件；parameter event。
     */
    @Override
    public void onComplete(GatewayCallEventV1 event) {
        String protocol = bounded(event.request().protocol());
        String zone = bounded(event.request().accessZone());
        String result = bounded(event.result().category());
        Counter.builder("gateway.calls")
                .tag("protocol", protocol)
                .tag("zone", zone)
                .tag("result", result)
                .register(registry)
                .increment();
        Timer.builder("gateway.call.duration")
                .tag("protocol", protocol)
                .tag("zone", zone)
                .tag("result", result)
                .register(registry)
                .record(Duration.ofMillis(event.result().durationMs()));
    }

    /**
     * 中文说明：执行 bounded 操作；该方法是 {@code GatewayCallMetricsListener} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the bounded operation; this method is the invocation entry point on {@code GatewayCallMetricsListener} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallMetricsListener.bounded(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 bounded 的处理结果；returns the result of the operation.
     */
    private static String bounded(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.length() > 48 ? "other" : value;
    }
}
