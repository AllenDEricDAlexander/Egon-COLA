package top.egon.cola.component.gateway.test.rpc.consumer;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.gateway.test.rpc.contract.proto.EchoResponse;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayRpcDriverControllerTest {

    @Test
    void exposesStableJsonViewAndScopesFrontendTraceToRpcCall() {
        GatewayRpcDriverController controller =
                new GatewayRpcDriverController(new RecordingClient());

        GatewayRpcDriverController.EchoView response = controller.echo(
                "through-gateway",
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
                "rpc-driver-request"
        );

        assertThat(response.providerId()).isEqualTo("rpc-provider-test");
        assertThat(response.message()).isEqualTo("through-gateway");
        assertThat(response.traceId())
                .isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(TraceContext.getTraceId()).isNull();
    }

    private static final class RecordingClient
            extends GatewayRpcTestClient {

        @Override
        public EchoResponse echo(String message) {
            return EchoResponse.newBuilder()
                    .setProviderId("rpc-provider-test")
                    .setMessage(message)
                    .setInvocationId("invocation-1")
                    .setTraceId(TraceContext.getTraceId())
                    .build();
        }
    }
}
