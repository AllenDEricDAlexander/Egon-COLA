package top.egon.cola.component.gateway.engine.balance;

import top.egon.cola.component.gateway.core.provider.ProviderInstance;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 中文说明：{@code ProviderSelectionHandle} 是类型，位于当前 Gateway 模块的相关包中，负责提供方SelectionHandle相关的职责与边界。
 * English summary: {@code ProviderSelectionHandle} is a type in the current Gateway module; it owns the provider selection handle-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class ProviderSelectionHandle implements AutoCloseable {

    /**
     * 中文说明：保存 instance 对应的状态、依赖或配置值；字段类型为 {@code ProviderInstance}，由 {@code ProviderSelectionHandle} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by instance; its type is {@code ProviderInstance}, and {@code ProviderSelectionHandle} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderSelectionHandle} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderSelectionHandle}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ProviderInstance instance;

    /**
     * 中文说明：保存 发布 对应的状态、依赖或配置值；字段类型为 {@code Runnable}，由 {@code ProviderSelectionHandle} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by release; its type is {@code Runnable}, and {@code ProviderSelectionHandle} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderSelectionHandle} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderSelectionHandle}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Runnable release;

    /**
     * 中文说明：保存 closed 对应的状态、依赖或配置值；字段类型为 {@code AtomicBoolean}，由 {@code ProviderSelectionHandle} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by closed; its type is {@code AtomicBoolean}, and {@code ProviderSelectionHandle} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderSelectionHandle} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderSelectionHandle}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * 中文说明：创建 {@code ProviderSelectionHandle} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code ProviderSelectionHandle} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param instance 参数 instance；parameter instance。
     * @param release 参数 发布；parameter release。
     */
    public ProviderSelectionHandle(
            ProviderInstance instance,
            Runnable release) {
        this.instance = Objects.requireNonNull(instance, "instance");
        this.release = Objects.requireNonNull(release, "release");
    }

    /**
     * 中文说明：执行 instance 操作；该方法是 {@code ProviderSelectionHandle} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the instance operation; this method is the invocation entry point on {@code ProviderSelectionHandle} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderSelectionHandle.instance(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 instance 的处理结果；returns the result of the operation.
     */
    public ProviderInstance instance() {
        return instance;
    }

    /**
     * 中文说明：执行 close 操作；该方法是 {@code ProviderSelectionHandle} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the close operation; this method is the invocation entry point on {@code ProviderSelectionHandle} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderSelectionHandle.close(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            release.run();
        }
    }
}
