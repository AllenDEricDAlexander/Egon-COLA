package top.egon.cola.component.gateway.engine.security;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import top.egon.cola.component.gateway.engine.http.GatewayHttpServer;
import top.egon.cola.component.gateway.engine.rpc.RpcGatewayServer;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * 中文说明：{@code GatewayTransportSecurityEndpoint} 是类型，位于当前 Gateway 模块的相关包中，负责网关传输安全Endpoint相关的职责与边界。
 * English summary: {@code GatewayTransportSecurityEndpoint} is a type in the current Gateway module; it owns the gateway transport security endpoint-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Endpoint(id = "gatewayTls")
public final class GatewayTransportSecurityEndpoint {

    /**
     * 中文说明：保存 http服务器 对应的状态、依赖或配置值；字段类型为 {@code GatewayHttpServer}，由 {@code GatewayTransportSecurityEndpoint} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by http server; its type is {@code GatewayHttpServer}, and {@code GatewayTransportSecurityEndpoint} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayTransportSecurityEndpoint} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTransportSecurityEndpoint}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayHttpServer httpServer;

    /**
     * 中文说明：保存 rpc服务器 对应的状态、依赖或配置值；字段类型为 {@code RpcGatewayServer}，由 {@code GatewayTransportSecurityEndpoint} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by rpc server; its type is {@code RpcGatewayServer}, and {@code GatewayTransportSecurityEndpoint} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayTransportSecurityEndpoint} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTransportSecurityEndpoint}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final RpcGatewayServer rpcServer;

    /**
     * 中文说明：创建 {@code GatewayTransportSecurityEndpoint} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayTransportSecurityEndpoint} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param httpServer 参数 http服务器；parameter http server。
     * @param rpcServer 参数 rpc服务器；parameter rpc server。
     */
    public GatewayTransportSecurityEndpoint(
            GatewayHttpServer httpServer,
            RpcGatewayServer rpcServer) {
        this.httpServer = Objects.requireNonNull(httpServer, "httpServer");
        this.rpcServer = Objects.requireNonNull(rpcServer, "rpcServer");
    }

    /**
     * 中文说明：执行 reload 操作；该方法是 {@code GatewayTransportSecurityEndpoint} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the reload operation; this method is the invocation entry point on {@code GatewayTransportSecurityEndpoint} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTransportSecurityEndpoint.reload(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 reload 的处理结果；returns the result of the operation.
     */
    @WriteOperation
    public Map<String, Object> reload() {
        httpServer.reloadTransportSecurity();
        rpcServer.reloadTransportSecurity();
        return Map.of(
                "reloaded", true,
                "reloadedAt", Instant.now().toString()
        );
    }
}
