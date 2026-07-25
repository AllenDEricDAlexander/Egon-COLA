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
                "rpc-driver-trace"
        );

        assertThat(response.providerId()).isEqualTo("rpc-provider-test");
        assertThat(response.message()).isEqualTo("through-gateway");
        assertThat(response.traceId()).isEqualTo("rpc-driver-trace");
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
