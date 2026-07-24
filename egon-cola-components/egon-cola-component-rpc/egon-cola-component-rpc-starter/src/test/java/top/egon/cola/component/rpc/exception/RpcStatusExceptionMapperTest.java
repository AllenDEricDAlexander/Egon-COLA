package top.egon.cola.component.rpc.exception;

import io.grpc.Metadata;
import io.grpc.Status;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.context.RpcMetadataKeys;

import static org.assertj.core.api.Assertions.assertThat;

class RpcStatusExceptionMapperTest {

    private final RpcStatusExceptionMapper mapper =
            new RpcStatusExceptionMapper();

    @Test
    void shouldMapStablePublicErrorCodes() {
        assertCode(Status.DEADLINE_EXCEEDED, EgonRpcErrorCode.RPC_DEADLINE_EXCEEDED);
        assertCode(Status.CANCELLED, EgonRpcErrorCode.RPC_CANCELLED);
        assertCode(Status.UNAVAILABLE, EgonRpcErrorCode.RPC_GATEWAY_UNAVAILABLE);
        assertCode(Status.INVALID_ARGUMENT, EgonRpcErrorCode.RPC_INVALID_REQUEST);
        assertCode(Status.INTERNAL, EgonRpcErrorCode.RPC_INTERNAL);
    }

    @Test
    void shouldClassifyNotFoundUsingFrameworkTrailer() {
        Metadata trailers = new Metadata();
        trailers.put(
                Metadata.Key.of(
                        "x-egon-rpc-error-type",
                        Metadata.ASCII_STRING_MARSHALLER
                ),
                "method"
        );

        EgonRpcException exception = mapper.map(
                Status.NOT_FOUND.asRuntimeException(trailers)
        );

        assertThat(exception.getCode())
                .isEqualTo(EgonRpcErrorCode.RPC_METHOD_NOT_FOUND);
    }

    @Test
    void shouldDistinguishProviderFromGatewayUnavailable() {
        Metadata trailers = new Metadata();
        trailers.put(RpcMetadataKeys.FAILURE_STAGE, "provider");

        assertThat(mapper.map(
                Status.UNAVAILABLE.asRuntimeException(trailers)
        ).getCode()).isEqualTo(EgonRpcErrorCode.RPC_PROVIDER_UNAVAILABLE);
        assertThat(mapper.map(
                Status.UNAVAILABLE.asRuntimeException()
        ).getCode()).isEqualTo(EgonRpcErrorCode.RPC_GATEWAY_UNAVAILABLE);
    }

    private void assertCode(Status status, EgonRpcErrorCode expected) {
        assertThat(mapper.map(status.asRuntimeException()).getCode())
                .isEqualTo(expected);
    }
}
