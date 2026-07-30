package top.egon.cola.component.gateway.engine.rpc;

import io.grpc.HandlerRegistry;
import io.grpc.MethodDescriptor;
import io.grpc.ServerMethodDefinition;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class RpcGatewayHandlerRegistry extends HandlerRegistry {

    private final AtomicReference<RpcMethodIndex> active =
            new AtomicReference<>(RpcMethodIndex.empty());

    private final RpcGatewayForwarder forwarder;

    private final Supplier<RpcMethodIndex> indexSupplier;

    public RpcGatewayHandlerRegistry(RpcGatewayForwarder forwarder) {
        this(forwarder, null);
    }

    public RpcGatewayHandlerRegistry(
            RpcGatewayForwarder forwarder,
            Supplier<RpcMethodIndex> indexSupplier) {
        this.forwarder = Objects.requireNonNull(forwarder, "forwarder");
        this.indexSupplier = indexSupplier;
    }

    public void activate(RpcMethodIndex index) {
        active.set(Objects.requireNonNull(index, "index"));
    }

    public RpcMethodIndex activeIndex() {
        return current();
    }

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

    @Override
    public List<io.grpc.ServerServiceDefinition> getServices() {
        return List.of();
    }

    private RpcMethodIndex current() {
        if (indexSupplier == null) {
            return active.get();
        }
        RpcMethodIndex supplied = indexSupplier.get();
        return supplied == null ? active.get() : supplied;
    }
}
