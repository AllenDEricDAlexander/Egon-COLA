package top.egon.cola.component.rpc.provider;

import io.grpc.Server;
import io.grpc.ServerInterceptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import top.egon.cola.component.rpc.config.RpcTransportSecurity;

import java.net.InetSocketAddress;
import java.util.Collection;

public class RpcProviderServerFactory {

    private final RpcTransportSecurity transportSecurity;

    public RpcProviderServerFactory() {
        this(RpcTransportSecurity.developmentPlaintextConfig());
    }

    public RpcProviderServerFactory(
            RpcTransportSecurity transportSecurity) {
        this.transportSecurity = transportSecurity;
    }

    public Server create(String bindAddress,
                         int port,
                         Collection<ServerServiceDefinition> services,
                         ServerInterceptor interceptor) {
        NettyServerBuilder builder = NettyServerBuilder.forAddress(
                new InetSocketAddress(bindAddress, port)
        ).intercept(interceptor);
        if (transportSecurity.enabled()) {
            builder.sslContext(transportSecurity.serverContext());
        }
        services.forEach(builder::addService);
        return builder.build();
    }
}
