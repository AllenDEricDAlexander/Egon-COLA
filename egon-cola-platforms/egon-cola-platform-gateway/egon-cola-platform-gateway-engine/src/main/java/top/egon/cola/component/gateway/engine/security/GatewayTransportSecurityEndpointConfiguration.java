package top.egon.cola.component.gateway.engine.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.gateway.engine.http.GatewayHttpServer;
import top.egon.cola.component.gateway.engine.rpc.RpcGatewayServer;

/**
 * 中文说明：{@code GatewayTransportSecurityEndpointConfiguration} 是配置类，位于当前 Gateway 模块的相关包中，负责网关传输安全Endpoint配置相关的职责与边界。
 * English summary: {@code GatewayTransportSecurityEndpointConfiguration} is a gateway transport security endpoint configuration configuration in the current Gateway module; it owns the gateway transport security endpoint configuration-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "egon.cola.component.gateway.engine.tls-reload",
        name = "enabled",
        havingValue = "true"
)
public class GatewayTransportSecurityEndpointConfiguration {

    /**
     * 中文说明：执行 网关传输安全Endpoint 操作；该方法是 {@code GatewayTransportSecurityEndpointConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway transport security endpoint operation; this method is the invocation entry point on {@code GatewayTransportSecurityEndpointConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTransportSecurityEndpointConfiguration.gatewayTransportSecurityEndpoint(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param httpServer 参数 http服务器；parameter http server。
     * @param rpcServer 参数 rpc服务器；parameter rpc server。
     * @return 返回 网关传输安全Endpoint 的处理结果；returns the result of the operation.
     */
    @Bean
    GatewayTransportSecurityEndpoint gatewayTransportSecurityEndpoint(
            GatewayHttpServer httpServer,
            RpcGatewayServer rpcServer) {
        return new GatewayTransportSecurityEndpoint(httpServer, rpcServer);
    }
}
