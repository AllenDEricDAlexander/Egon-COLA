package top.egon.cola.component.rpc.exception;

import io.grpc.Metadata;
import io.grpc.Status;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.context.invocation.RpcFailureStage;
import top.egon.cola.component.rpc.context.invocation.RpcMetadataKeys;

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
        assertCode(Status.UNIMPLEMENTED, EgonRpcErrorCode.RPC_METHOD_NOT_FOUND);
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
        Metadata providerTrailers = new Metadata();
        RpcFailureStage.PROVIDER.put(providerTrailers);
        Metadata gatewayTrailers = new Metadata();
        RpcFailureStage.GATEWAY.put(gatewayTrailers);

        assertThat(mapper.map(
                Status.UNAVAILABLE.asRuntimeException(providerTrailers)
        ).getCode()).isEqualTo(EgonRpcErrorCode.RPC_PROVIDER_UNAVAILABLE);
        assertThat(mapper.map(
                Status.UNAVAILABLE.asRuntimeException(gatewayTrailers)
        ).getCode()).isEqualTo(EgonRpcErrorCode.RPC_GATEWAY_UNAVAILABLE);
    }

    @Test
    void shouldMapProviderRateLimitToDedicatedCode() {
        Metadata trailers = new Metadata();
        RpcFailureStage.PROVIDER.put(trailers);
        trailers.put(RpcMetadataKeys.ERROR_TYPE, "rate-limit");

        assertThat(mapper.map(Status.UNAVAILABLE.asRuntimeException(trailers)).getCode())
                .isEqualTo(EgonRpcErrorCode.RPC_RATE_LIMITED);
    }

    private void assertCode(Status status, EgonRpcErrorCode expected) {
        assertThat(mapper.map(status.asRuntimeException()).getCode())
                .isEqualTo(expected);
    }
}
