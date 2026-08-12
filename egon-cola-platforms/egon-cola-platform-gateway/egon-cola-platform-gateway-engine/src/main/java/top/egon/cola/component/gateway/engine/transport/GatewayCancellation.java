package top.egon.cola.component.gateway.engine.transport;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import reactor.core.Disposable;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Idempotent owner for subscriptions and buffers abandoned by cancellation.
 * 补充说明 / Supplementary summary: {@code GatewayCancellation} 是类型，位于当前 Gateway 模块的相关包中，负责网关Cancellation相关的职责与边界。
 * English supplement: {@code GatewayCancellation} is a type in the current Gateway module; it owns the gateway cancellation-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayCancellation implements AutoCloseable {

    /**
     * 中文说明：保存 cancelled 对应的状态、依赖或配置值；字段类型为 {@code AtomicBoolean}，由 {@code GatewayCancellation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by cancelled; its type is {@code AtomicBoolean}, and {@code GatewayCancellation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCancellation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCancellation}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicBoolean cancelled = new AtomicBoolean();

    /**
     * 中文说明：保存 resources 对应的状态、依赖或配置值；字段类型为 {@code ConcurrentLinkedQueue<Disposable>}，由 {@code GatewayCancellation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by resources; its type is {@code ConcurrentLinkedQueue<Disposable>}, and {@code GatewayCancellation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCancellation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCancellation}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ConcurrentLinkedQueue<Disposable> resources =
            new ConcurrentLinkedQueue<>();

    /**
     * 中文说明：保存 buffers 对应的状态、依赖或配置值；字段类型为 {@code Set<DataBuffer>}，由 {@code GatewayCancellation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by buffers; its type is {@code Set<DataBuffer>}, and {@code GatewayCancellation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCancellation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCancellation}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Set<DataBuffer> buffers = ConcurrentHashMap.newKeySet();

    /**
     * 中文说明：执行 register 操作；该方法是 {@code GatewayCancellation} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the register operation; this method is the invocation entry point on {@code GatewayCancellation} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCancellation.register(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param resource 参数 资源；parameter resource。
     */
    public void register(Disposable resource) {
        Objects.requireNonNull(resource, "resource");
        resources.add(resource);
        if (cancelled.get() && resources.remove(resource)) {
            resource.dispose();
        }
    }

    /**
     * 中文说明：执行 own 操作；该方法是 {@code GatewayCancellation} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the own operation; this method is the invocation entry point on {@code GatewayCancellation} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCancellation.own(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param buffer 参数 缓冲区；parameter buffer。
     * @return 返回 own 的处理结果；returns the result of the operation.
     */
    public boolean own(DataBuffer buffer) {
        Objects.requireNonNull(buffer, "buffer");
        if (cancelled.get()) {
            DataBufferUtils.release(buffer);
            return false;
        }
        buffers.add(buffer);
        if (cancelled.get() && buffers.remove(buffer)) {
            DataBufferUtils.release(buffer);
            return false;
        }
        return true;
    }

    /**
     * 中文说明：执行 transfer 操作；该方法是 {@code GatewayCancellation} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the transfer operation; this method is the invocation entry point on {@code GatewayCancellation} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCancellation.transfer(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param buffer 参数 缓冲区；parameter buffer。
     * @return 返回 transfer 的处理结果；returns the result of the operation.
     */
    public boolean transfer(DataBuffer buffer) {
        return buffer != null && buffers.remove(buffer);
    }

    /**
     * 中文说明：执行 cancel 操作；该方法是 {@code GatewayCancellation} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the cancel operation; this method is the invocation entry point on {@code GatewayCancellation} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCancellation.cancel(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 cancel 的处理结果；returns the result of the operation.
     */
    public boolean cancel() {
        if (!cancelled.compareAndSet(false, true)) {
            return false;
        }
        Disposable resource;
        while ((resource = resources.poll()) != null) {
            resource.dispose();
        }
        buffers.forEach(DataBufferUtils::release);
        buffers.clear();
        return true;
    }

    /**
     * 中文说明：执行 cancelled 操作；该方法是 {@code GatewayCancellation} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the cancelled operation; this method is the invocation entry point on {@code GatewayCancellation} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCancellation.cancelled(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 cancelled 的处理结果；returns the result of the operation.
     */
    public boolean cancelled() {
        return cancelled.get();
    }

    /**
     * 中文说明：执行 close 操作；该方法是 {@code GatewayCancellation} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the close operation; this method is the invocation entry point on {@code GatewayCancellation} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCancellation.close(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    @Override
    public void close() {
        cancel();
    }
}
