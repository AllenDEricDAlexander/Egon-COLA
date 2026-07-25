package top.egon.cola.component.gateway.engine.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.gateway.engine.http.GatewayHttpServer;
import top.egon.cola.component.gateway.engine.rpc.RpcGatewayServer;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "egon.cola.component.gateway.engine.tls-reload",
        name = "enabled",
        havingValue = "true"
)
public class GatewayTransportSecurityEndpointConfiguration {

    @Bean
    GatewayTransportSecurityEndpoint gatewayTransportSecurityEndpoint(
            GatewayHttpServer httpServer,
            RpcGatewayServer rpcServer) {
        return new GatewayTransportSecurityEndpoint(httpServer, rpcServer);
    }
}
