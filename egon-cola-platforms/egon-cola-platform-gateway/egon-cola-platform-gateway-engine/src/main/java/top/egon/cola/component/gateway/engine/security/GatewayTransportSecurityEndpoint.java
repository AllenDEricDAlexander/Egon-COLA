package top.egon.cola.component.gateway.engine.security;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import top.egon.cola.component.gateway.engine.http.GatewayHttpServer;
import top.egon.cola.component.gateway.engine.rpc.RpcGatewayServer;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Endpoint(id = "gatewayTls")
public final class GatewayTransportSecurityEndpoint {

    private final GatewayHttpServer httpServer;

    private final RpcGatewayServer rpcServer;

    public GatewayTransportSecurityEndpoint(
            GatewayHttpServer httpServer,
            RpcGatewayServer rpcServer) {
        this.httpServer = Objects.requireNonNull(httpServer, "httpServer");
        this.rpcServer = Objects.requireNonNull(rpcServer, "rpcServer");
    }

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
