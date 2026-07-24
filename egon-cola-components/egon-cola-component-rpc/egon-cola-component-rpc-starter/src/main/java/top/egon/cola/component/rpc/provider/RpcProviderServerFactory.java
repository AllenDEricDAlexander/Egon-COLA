package top.egon.cola.component.rpc.provider;

import io.grpc.Server;
import io.grpc.ServerInterceptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;

import java.net.InetSocketAddress;
import java.util.Collection;

public class RpcProviderServerFactory {

    public Server create(String bindAddress,
                         int port,
                         Collection<ServerServiceDefinition> services,
                         ServerInterceptor interceptor) {
        NettyServerBuilder builder = NettyServerBuilder.forAddress(
                new InetSocketAddress(bindAddress, port)
        ).intercept(interceptor);
        services.forEach(builder::addService);
        return builder.build();
    }
}
