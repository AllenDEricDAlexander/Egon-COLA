package top.egon.cola.component.rpc.provider.server;

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

    private static final int DEFAULT_MAX_INBOUND_MESSAGE_SIZE = 4 * 1024 * 1024;

    private final RpcTransportSecurity transportSecurity;

    private final int maxInboundMessageSize;

    public RpcProviderServerFactory() {
        this(
                RpcTransportSecurity.developmentPlaintextConfig(),
                DEFAULT_MAX_INBOUND_MESSAGE_SIZE
        );
    }

    public RpcProviderServerFactory(
            RpcTransportSecurity transportSecurity) {
        this(transportSecurity, DEFAULT_MAX_INBOUND_MESSAGE_SIZE);
    }

    public RpcProviderServerFactory(
            RpcTransportSecurity transportSecurity,
            int maxInboundMessageSize) {
        this.transportSecurity = transportSecurity;
        if (maxInboundMessageSize < 1024) {
            throw new IllegalArgumentException(
                    "maxInboundMessageSize must be at least 1024"
            );
        }
        this.maxInboundMessageSize = maxInboundMessageSize;
    }

    public Server create(String bindAddress,
                         int port,
                         Collection<ServerServiceDefinition> services,
                         List<ServerInterceptor> interceptors) {
        NettyServerBuilder builder = NettyServerBuilder.forAddress(
                new InetSocketAddress(bindAddress, port)
        ).maxInboundMessageSize(maxInboundMessageSize);
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
