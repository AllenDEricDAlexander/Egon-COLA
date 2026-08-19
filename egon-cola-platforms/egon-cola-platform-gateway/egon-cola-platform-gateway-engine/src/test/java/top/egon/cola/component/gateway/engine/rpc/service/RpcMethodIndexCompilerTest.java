package top.egon.cola.component.gateway.engine.rpc.service;

import top.egon.cola.component.gateway.engine.rpc.service.RpcMethodIndex;
import top.egon.cola.component.gateway.engine.rpc.domain.RuntimeRpcRoute;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.route.GatewayResponseMode;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RpcMethodIndexCompilerTest {

    @Test
    void compilesUniqueUnaryMethodNamesAndRejectsDuplicates() {
        RuntimeRpcRoute route = route("route-a");
        RpcMethodIndex index = new RpcMethodIndexCompiler().compile(
                List.of(route)
        );

        assertEquals(
                "route-a",
                index.find("test.Echo/Call").orElseThrow().routeId()
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new RpcMethodIndexCompiler().compile(
                        List.of(route, route("route-b"))
                )
        );
    }

    private RuntimeRpcRoute route(String id) {
        return new RuntimeRpcRoute(
                id,
                "operation",
                "test.Echo/Call",
                new ProviderServiceKey(
                        "test-biz",
                        "test-app",
                        "test",
                        "default",
                        ProviderProtocolType.RPC,
                        "test.Echo",
                        "default",
                        "v1",
                        "grpc"
                ),
                "test.Request",
                "test.Response",
                "sha",
                Set.of(),
                GatewayResponseMode.TRANSPARENT,
                false,
                Duration.ofSeconds(3)
        );
    }
}
