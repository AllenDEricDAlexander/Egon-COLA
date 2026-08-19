package top.egon.cola.component.gateway.engine.rpc.service;

import top.egon.cola.component.gateway.engine.rpc.service.RpcMethodIndex;
import top.egon.cola.component.gateway.engine.rpc.domain.RuntimeRpcRoute;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 中文说明：{@code RpcMethodIndexCompiler} 是编译器，位于当前 Gateway 模块的相关包中，负责Rpc方法索引Compiler相关的职责与边界。
 * English summary: {@code RpcMethodIndexCompiler} is a rpc method index compiler compiler in the current Gateway module; it owns the rpc method index compiler-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class RpcMethodIndexCompiler {

    /**
     * 中文说明：执行 compile 操作；该方法是 {@code RpcMethodIndexCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the compile operation; this method is the invocation entry point on {@code RpcMethodIndexCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcMethodIndexCompiler.compile(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param routes 参数 routes；parameter routes。
     * @return 返回 compile 的处理结果；returns the result of the operation.
     */
    public RpcMethodIndex compile(List<RuntimeRpcRoute> routes) {
        Map<String, RuntimeRpcRoute> index = new LinkedHashMap<>();
        for (RuntimeRpcRoute route : Objects.requireNonNull(routes, "routes")) {
            RuntimeRpcRoute previous = index.putIfAbsent(
                    route.fullMethodName(),
                    route
            );
            if (previous != null) {
                throw new IllegalArgumentException(
                        "duplicate RPC route for " + route.fullMethodName()
                );
            }
        }
        return new RpcMethodIndex(index);
    }
}
