package top.egon.cola.component.gateway.engine.rpc;

import io.grpc.HandlerRegistry;
import io.grpc.MethodDescriptor;
import io.grpc.ServerMethodDefinition;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 中文说明：{@code RpcGatewayHandlerRegistry} 是类型，位于当前 Gateway 模块的相关包中，负责Rpc网关处理器注册表相关的职责与边界。
 * English summary: {@code RpcGatewayHandlerRegistry} is a type in the current Gateway module; it owns the rpc gateway handler registry-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class RpcGatewayHandlerRegistry extends HandlerRegistry {

    /**
     * 中文说明：保存 active 对应的状态、依赖或配置值；字段类型为 {@code AtomicReference<RpcMethodIndex>}，由 {@code RpcGatewayHandlerRegistry} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by active; its type is {@code AtomicReference<RpcMethodIndex>}, and {@code RpcGatewayHandlerRegistry} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcGatewayHandlerRegistry} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayHandlerRegistry}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicReference<RpcMethodIndex> active =
            new AtomicReference<>(RpcMethodIndex.empty());

    /**
     * 中文说明：保存 转发器 对应的状态、依赖或配置值；字段类型为 {@code RpcGatewayForwarder}，由 {@code RpcGatewayHandlerRegistry} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by forwarder; its type is {@code RpcGatewayForwarder}, and {@code RpcGatewayHandlerRegistry} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcGatewayHandlerRegistry} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayHandlerRegistry}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final RpcGatewayForwarder forwarder;

    /**
     * 中文说明：保存 索引Supplier 对应的状态、依赖或配置值；字段类型为 {@code Supplier<RpcMethodIndex>}，由 {@code RpcGatewayHandlerRegistry} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by index supplier; its type is {@code Supplier<RpcMethodIndex>}, and {@code RpcGatewayHandlerRegistry} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcGatewayHandlerRegistry} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayHandlerRegistry}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Supplier<RpcMethodIndex> indexSupplier;

    /**
     * 中文说明：创建 {@code RpcGatewayHandlerRegistry} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code RpcGatewayHandlerRegistry} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param forwarder 参数 转发器；parameter forwarder。
     */
    public RpcGatewayHandlerRegistry(RpcGatewayForwarder forwarder) {
        this(forwarder, null);
    }

    /**
     * 中文说明：创建 {@code RpcGatewayHandlerRegistry} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code RpcGatewayHandlerRegistry} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param forwarder 参数 转发器；parameter forwarder。
     * @param indexSupplier 参数 索引Supplier；parameter index supplier。
     */
    public RpcGatewayHandlerRegistry(
            RpcGatewayForwarder forwarder,
            Supplier<RpcMethodIndex> indexSupplier) {
        this.forwarder = Objects.requireNonNull(forwarder, "forwarder");
        this.indexSupplier = indexSupplier;
    }

    /**
     * 中文说明：执行 activate 操作；该方法是 {@code RpcGatewayHandlerRegistry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the activate operation; this method is the invocation entry point on {@code RpcGatewayHandlerRegistry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayHandlerRegistry.activate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param index 参数 索引；parameter index。
     */
    public void activate(RpcMethodIndex index) {
        active.set(Objects.requireNonNull(index, "index"));
    }

    /**
     * 中文说明：执行 active索引 操作；该方法是 {@code RpcGatewayHandlerRegistry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the active index operation; this method is the invocation entry point on {@code RpcGatewayHandlerRegistry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayHandlerRegistry.activeIndex(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 active索引 的处理结果；returns the result of the operation.
     */
    public RpcMethodIndex activeIndex() {
        return current();
    }

    /**
     * 中文说明：执行 lookup方法 操作；该方法是 {@code RpcGatewayHandlerRegistry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the lookup method operation; this method is the invocation entry point on {@code RpcGatewayHandlerRegistry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayHandlerRegistry.lookupMethod(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param methodName 参数 方法Name；parameter method name。
     * @param authority 参数 authority；parameter authority。
     * @return 返回 lookup方法 的处理结果；returns the result of the operation.
     */
    @Override
    public ServerMethodDefinition<?, ?> lookupMethod(
            String methodName,
            String authority) {
        RuntimeRpcRoute route = current().find(methodName).orElse(null);
        if (route == null) {
            return null;
        }
        MethodDescriptor<byte[], byte[]> descriptor =
                RawByteMarshaller.INSTANCE.descriptor(methodName);
        return ServerMethodDefinition.create(
                descriptor,
                forwarder.handler(route)
        );
    }

    /**
     * 中文说明：执行 getServices 操作；该方法是 {@code RpcGatewayHandlerRegistry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get services operation; this method is the invocation entry point on {@code RpcGatewayHandlerRegistry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayHandlerRegistry.getServices(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getServices 的处理结果；returns the result of the operation.
     */
    @Override
    public List<io.grpc.ServerServiceDefinition> getServices() {
        return List.of();
    }

    /**
     * 中文说明：执行 current 操作；该方法是 {@code RpcGatewayHandlerRegistry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the current operation; this method is the invocation entry point on {@code RpcGatewayHandlerRegistry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayHandlerRegistry.current(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 current 的处理结果；returns the result of the operation.
     */
    private RpcMethodIndex current() {
        if (indexSupplier == null) {
            return active.get();
        }
        RpcMethodIndex supplied = indexSupplier.get();
        return supplied == null ? active.get() : supplied;
    }
}
