package top.egon.cola.component.gateway.engine.rpc;

import java.util.Map;
import java.util.Optional;

/**
 * 中文说明：{@code RpcMethodIndex} 是类型，位于当前 Gateway 模块的相关包中，负责Rpc方法索引相关的职责与边界。
 * English summary: {@code RpcMethodIndex} is a type in the current Gateway module; it owns the rpc method index-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class RpcMethodIndex {

    /**
     * 中文说明：保存 routes 对应的状态、依赖或配置值；字段类型为 {@code Map<String, RuntimeRpcRoute>}，由 {@code RpcMethodIndex} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by routes; its type is {@code Map<String, RuntimeRpcRoute>}, and {@code RpcMethodIndex} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcMethodIndex} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcMethodIndex}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Map<String, RuntimeRpcRoute> routes;

    /**
     * 中文说明：创建 {@code RpcMethodIndex} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code RpcMethodIndex} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param routes 参数 routes；parameter routes。
     */
    RpcMethodIndex(Map<String, RuntimeRpcRoute> routes) {
        this.routes = Map.copyOf(routes);
    }

    /**
     * 中文说明：执行 empty 操作；该方法是 {@code RpcMethodIndex} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the empty operation; this method is the invocation entry point on {@code RpcMethodIndex} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcMethodIndex.empty(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 empty 的处理结果；returns the result of the operation.
     */
    public static RpcMethodIndex empty() {
        return new RpcMethodIndex(Map.of());
    }

    /**
     * 中文说明：执行 find 操作；该方法是 {@code RpcMethodIndex} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find operation; this method is the invocation entry point on {@code RpcMethodIndex} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcMethodIndex.find(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param fullMethodName 参数 full方法Name；parameter full method name。
     * @return 返回 find 的处理结果；returns the result of the operation.
     */
    public Optional<RuntimeRpcRoute> find(String fullMethodName) {
        return Optional.ofNullable(routes.get(fullMethodName));
    }

    /**
     * 中文说明：执行 routes 操作；该方法是 {@code RpcMethodIndex} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the routes operation; this method is the invocation entry point on {@code RpcMethodIndex} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcMethodIndex.routes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 routes 的处理结果；returns the result of the operation.
     */
    public Map<String, RuntimeRpcRoute> routes() {
        return routes;
    }
}
