package top.egon.cola.component.rpc.provider;

import io.grpc.Server;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import top.egon.cola.component.rpc.config.RpcTransportSecurity;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;

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
                         List<ServerInterceptor> interceptors) {
        NettyServerBuilder builder = NettyServerBuilder.forAddress(
                new InetSocketAddress(bindAddress, port)
        );
        if (transportSecurity.enabled()) {
            builder.sslContext(transportSecurity.serverContext());
        }
        services.stream()
                .map(service -> ServerInterceptors.interceptForward(
                        service,
                        interceptors
                ))
                .forEach(builder::addService);
        return builder.build();
    }
}
