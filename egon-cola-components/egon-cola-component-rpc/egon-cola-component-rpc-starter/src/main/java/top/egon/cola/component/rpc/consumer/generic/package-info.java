/**
 * Restricted raw-Protobuf unary invocation APIs.
 *
 * <p>Generic calls use the canonical gRPC {@code Service/Method} identity and
 * reuse the fixed-mode discovery, load-balancing, channel, interceptor and
 * invocation-executor path. This package deliberately does not expose JSON,
 * Map/Object[] serialization, arbitrary transport metadata, endpoint
 * addresses, streaming or mode fallback. Dynamic target state is bounded and
 * owned by {@link top.egon.cola.component.rpc.consumer.generic.RpcGenericTargetCache}.
 */
package top.egon.cola.component.rpc.consumer.generic;
