package top.egon.cola.component.gateway.engine.rpc;

import io.grpc.HandlerRegistry;
import io.grpc.MethodDescriptor;
import io.grpc.ServerMethodDefinition;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class RpcGatewayHandlerRegistry extends HandlerRegistry {

    private final AtomicReference<RpcMethodIndex> active =
            new AtomicReference<>(new RpcMethodIndex(java.util.Map.of()));

    private final RpcGatewayForwarder forwarder;

    public RpcGatewayHandlerRegistry(RpcGatewayForwarder forwarder) {
        this.forwarder = Objects.requireNonNull(forwarder, "forwarder");
    }

    public void activate(RpcMethodIndex index) {
        active.set(Objects.requireNonNull(index, "index"));
    }

    public RpcMethodIndex activeIndex() {
        return active.get();
    }

    @Override
    public ServerMethodDefinition<?, ?> lookupMethod(
            String methodName,
            String authority) {
        RuntimeRpcRoute route = active.get().find(methodName).orElse(null);
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

    @Override
    public List<io.grpc.ServerServiceDefinition> getServices() {
        return List.of();
    }
}
