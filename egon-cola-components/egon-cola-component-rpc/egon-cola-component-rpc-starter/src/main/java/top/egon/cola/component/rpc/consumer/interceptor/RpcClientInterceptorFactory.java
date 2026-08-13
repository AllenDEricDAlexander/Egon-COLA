package top.egon.cola.component.rpc.consumer.interceptor;

import io.grpc.ClientInterceptor;

/**
 * 根据完整请求创建客户端拦截器的中立扩展点。
 *
 * <p>Neutral extension point that creates a client interceptor from the full
 * request context.
 */
@FunctionalInterface
public interface RpcClientInterceptorFactory {

    /**
     * 为当前请求创建拦截器。
     *
     * <p>Creates an interceptor for the current request.
     *
     * @param invocation 当前调用 / current invocation
     * @return 客户端拦截器 / client interceptor
     */
    ClientInterceptor create(RpcClientInvocation invocation);
}
