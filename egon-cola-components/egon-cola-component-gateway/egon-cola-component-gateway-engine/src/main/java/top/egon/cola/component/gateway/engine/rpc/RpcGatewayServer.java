package top.egon.cola.component.gateway.engine.rpc;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import top.egon.cola.component.gateway.engine.security.GatewayTransportSecurity;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class RpcGatewayServer implements AutoCloseable {

    private final int configuredPort;

    private final int maxInboundMessageBytes;

    private final RpcGatewayHandlerRegistry registry;

    private final GatewayTransportSecurity transportSecurity;

    private Server server;

    public RpcGatewayServer(
            int configuredPort,
            int maxInboundMessageBytes,
            RpcGatewayHandlerRegistry registry) {
        this(
                configuredPort,
                maxInboundMessageBytes,
                registry,
                GatewayTransportSecurity.developmentPlaintextConfig()
        );
    }

    public RpcGatewayServer(
            int configuredPort,
            int maxInboundMessageBytes,
            RpcGatewayHandlerRegistry registry,
            GatewayTransportSecurity transportSecurity) {
        if (configuredPort < 0 || configuredPort > 65535) {
            throw new IllegalArgumentException("invalid RPC listener port");
        }
        if (maxInboundMessageBytes < 1024) {
            throw new IllegalArgumentException(
                    "maxInboundMessageBytes must be at least 1024"
            );
        }
        this.configuredPort = configuredPort;
        this.maxInboundMessageBytes = maxInboundMessageBytes;
        this.registry = Objects.requireNonNull(registry, "registry");
        this.transportSecurity = Objects.requireNonNull(
                transportSecurity,
                "transportSecurity"
        );
        if (transportSecurity.enabled()
                && !transportSecurity.clientCertificateRequired()) {
            throw new IllegalArgumentException(
                    "RPC Gateway TLS must require a client certificate"
            );
        }
    }

    public synchronized void start() {
        if (server != null) {
            return;
        }
        try {
            NettyServerBuilder builder =
                    NettyServerBuilder.forPort(configuredPort)
                    .maxInboundMessageSize(maxInboundMessageBytes)
                    .fallbackHandlerRegistry(registry);
            if (transportSecurity.enabled()) {
                builder.sslContext(serverSslContext());
            }
            server = builder
                    .build()
                    .start();
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "failed to start RPC gateway listener",
                    failure
            );
        }
    }

    public synchronized int port() {
        return server == null ? -1 : server.getPort();
    }

    public synchronized void reloadTransportSecurity() {
        boolean running = server != null;
        close();
        if (running) {
            start();
        }
    }

    @Override
    public synchronized void close() {
        if (server == null) {
            return;
        }
        server.shutdown();
        try {
            if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                server.shutdownNow();
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            server.shutdownNow();
        } finally {
            server = null;
        }
    }

    private SslContext serverSslContext() {
        try {
            return GrpcSslContexts.forServer(
                            transportSecurity.certificateChainFile().toFile(),
                            transportSecurity.privateKeyFile().toFile()
                    )
                    .trustManager(
                            transportSecurity
                                    .trustCertificateCollectionFile()
                                    .toFile()
                    )
                    .clientAuth(ClientAuth.REQUIRE)
                    .build();
        } catch (SSLException failure) {
            throw new IllegalStateException(
                    "failed to configure RPC Gateway mTLS",
                    failure
            );
        }
    }
}
