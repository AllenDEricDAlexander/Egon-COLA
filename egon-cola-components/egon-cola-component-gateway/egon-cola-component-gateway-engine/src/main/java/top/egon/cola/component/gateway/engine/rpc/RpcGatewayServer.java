package top.egon.cola.component.gateway.engine.rpc;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class RpcGatewayServer implements AutoCloseable {

    private final int configuredPort;

    private final int maxInboundMessageBytes;

    private final RpcGatewayHandlerRegistry registry;

    private Server server;

    public RpcGatewayServer(
            int configuredPort,
            int maxInboundMessageBytes,
            RpcGatewayHandlerRegistry registry) {
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
    }

    public synchronized void start() {
        if (server != null) {
            return;
        }
        try {
            server = NettyServerBuilder.forPort(configuredPort)
                    .maxInboundMessageSize(maxInboundMessageBytes)
                    .fallbackHandlerRegistry(registry)
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
}
