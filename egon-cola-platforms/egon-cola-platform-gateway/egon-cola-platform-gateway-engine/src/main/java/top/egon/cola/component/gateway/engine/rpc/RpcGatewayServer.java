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

/**
 * 中文说明：{@code RpcGatewayServer} 是类型，位于当前 Gateway 模块的相关包中，负责Rpc网关服务器相关的职责与边界。
 * English summary: {@code RpcGatewayServer} is a type in the current Gateway module; it owns the rpc gateway server-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class RpcGatewayServer implements AutoCloseable {

    /**
     * 中文说明：保存 configuredPort 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code RpcGatewayServer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by configured port; its type is {@code int}, and {@code RpcGatewayServer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcGatewayServer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayServer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final int configuredPort;

    /**
     * 中文说明：保存 maxInbound消息Bytes 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code RpcGatewayServer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by max inbound message bytes; its type is {@code int}, and {@code RpcGatewayServer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcGatewayServer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayServer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final int maxInboundMessageBytes;

    /**
     * 中文说明：保存 注册表 对应的状态、依赖或配置值；字段类型为 {@code RpcGatewayHandlerRegistry}，由 {@code RpcGatewayServer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by registry; its type is {@code RpcGatewayHandlerRegistry}, and {@code RpcGatewayServer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcGatewayServer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayServer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final RpcGatewayHandlerRegistry registry;

    /**
     * 中文说明：保存 传输安全 对应的状态、依赖或配置值；字段类型为 {@code GatewayTransportSecurity}，由 {@code RpcGatewayServer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by transport security; its type is {@code GatewayTransportSecurity}, and {@code RpcGatewayServer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcGatewayServer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayServer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayTransportSecurity transportSecurity;

    /**
     * 中文说明：保存 服务器 对应的状态、依赖或配置值；字段类型为 {@code Server}，由 {@code RpcGatewayServer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by server; its type is {@code Server}, and {@code RpcGatewayServer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcGatewayServer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayServer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private Server server;

    /**
     * 中文说明：创建 {@code RpcGatewayServer} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code RpcGatewayServer} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param configuredPort 参数 configuredPort；parameter configured port。
     * @param maxInboundMessageBytes 参数 maxInbound消息Bytes；parameter max inbound message bytes。
     * @param registry 参数 注册表；parameter registry。
     */
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

    /**
     * 中文说明：创建 {@code RpcGatewayServer} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code RpcGatewayServer} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param configuredPort 参数 configuredPort；parameter configured port。
     * @param maxInboundMessageBytes 参数 maxInbound消息Bytes；parameter max inbound message bytes。
     * @param registry 参数 注册表；parameter registry。
     * @param transportSecurity 参数 传输安全；parameter transport security。
     */
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

    /**
     * 中文说明：执行 start 操作；该方法是 {@code RpcGatewayServer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the start operation; this method is the invocation entry point on {@code RpcGatewayServer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayServer.start(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
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

    /**
     * 中文说明：执行 port 操作；该方法是 {@code RpcGatewayServer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the port operation; this method is the invocation entry point on {@code RpcGatewayServer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayServer.port(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 port 的处理结果；returns the result of the operation.
     */
    public synchronized int port() {
        return server == null ? -1 : server.getPort();
    }

    /**
     * 中文说明：执行 reload传输安全 操作；该方法是 {@code RpcGatewayServer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the reload transport security operation; this method is the invocation entry point on {@code RpcGatewayServer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayServer.reloadTransportSecurity(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    public synchronized void reloadTransportSecurity() {
        boolean running = server != null;
        close();
        if (running) {
            start();
        }
    }

    /**
     * 中文说明：执行 close 操作；该方法是 {@code RpcGatewayServer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the close operation; this method is the invocation entry point on {@code RpcGatewayServer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayServer.close(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
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

    /**
     * 中文说明：执行 服务器SslContext 操作；该方法是 {@code RpcGatewayServer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the server ssl context operation; this method is the invocation entry point on {@code RpcGatewayServer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayServer.serverSslContext(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 服务器SslContext 的处理结果；returns the result of the operation.
     */
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
