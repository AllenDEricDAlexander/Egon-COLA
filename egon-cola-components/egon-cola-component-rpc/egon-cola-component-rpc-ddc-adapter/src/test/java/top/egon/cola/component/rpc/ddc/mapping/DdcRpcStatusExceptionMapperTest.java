package top.egon.cola.component.rpc.ddc.mapping;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.error.DdcClientTransportException;
import top.egon.cola.component.ddc.error.DdcErrorStatus;
import top.egon.cola.component.ddc.error.DdcException;
import top.egon.cola.component.ddc.error.management.DdcManagementClientException;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcRpcErrorDetail;
import top.egon.cola.component.rpc.ddc.security.DdcRpcOperation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcRpcStatusExceptionMapperTest {

    private final DdcRpcStatusExceptionMapper mapper =
            new DdcRpcStatusExceptionMapper();

    @Test
    void restoresRuntimeAndManagementBusinessErrorsFromTypedTrailers() {
        StatusRuntimeException runtimeStatus = mapper.toStatus(
                new DdcException(DdcErrorStatus.LEASE_MISMATCH));

        RuntimeException runtime = mapper.restore(
                runtimeStatus,
                DdcRpcOperation.SDK_HEARTBEAT
        );

        assertThat(runtime).isInstanceOf(DdcException.class);
        assertThat(((DdcException) runtime).getCode())
                .isEqualTo(DdcErrorStatus.LEASE_MISMATCH.getCode());

        StatusRuntimeException managementStatus = mapper.toStatus(
                new DdcManagementClientException(
                        56004,
                        "DDC_CONFIG_NOT_FOUND",
                        "config not found"
                ));

        RuntimeException management = mapper.restore(
                managementStatus,
                DdcRpcOperation.MANAGEMENT_CONFIG_READ
        );

        assertThat(management).isInstanceOf(DdcManagementClientException.class);
        assertThat(((DdcManagementClientException) management).code())
                .isEqualTo(56004);
    }

    @Test
    void mapsApprovedGrpcStatusesWithoutExposingRawServerText() {
        assertTransport(Status.INVALID_ARGUMENT, false);
        assertTransport(Status.NOT_FOUND, false);
        assertTransport(Status.FAILED_PRECONDITION, false);
        assertTransport(Status.UNAUTHENTICATED, false);
        assertTransport(Status.PERMISSION_DENIED, false);
        assertTransport(Status.UNAVAILABLE, true);
        assertTransport(Status.INTERNAL, false);
    }

    @Test
    void malformedTypedDetailFallsBackToSanitizedTransportFailure() {
        Metadata trailers = new Metadata();
        Metadata.Key<byte[]> raw = Metadata.Key.of(
                "x-egon-ddc-error-bin",
                Metadata.BINARY_BYTE_MARSHALLER
        );
        trailers.put(raw, new byte[]{1, 2, 3});
        StatusRuntimeException status = Status.INVALID_ARGUMENT
                .withDescription("secret raw server text")
                .asRuntimeException(trailers);

        assertThatThrownBy(() -> {
            throw mapper.restore(status, DdcRpcOperation.CONFIG_PULL);
        }).isInstanceOf(DdcClientTransportException.class)
                .hasMessageNotContaining("secret raw server text");
    }

    @Test
    void exposesThePublishedBinaryTrailerKey() {
        Metadata trailers = new Metadata();
        trailers.put(
                DdcRpcStatusExceptionMapper.ERROR_DETAIL,
                DdcRpcErrorDetail.newBuilder()
                        .setCode("DDC_INVALID_REQUEST")
                        .setMessage("invalid")
                        .build()
        );

        assertThat(trailers.get(DdcRpcStatusExceptionMapper.ERROR_DETAIL)
                .getCode()).isEqualTo("DDC_INVALID_REQUEST");
    }

    private void assertTransport(Status status, boolean retryable) {
        StatusRuntimeException failure = status
                .withDescription("secret raw server text")
                .asRuntimeException();

        RuntimeException restored = mapper.restore(
                failure,
                DdcRpcOperation.CONFIG_PULL
        );

        assertThat(restored).isInstanceOf(DdcClientTransportException.class);
        DdcClientTransportException transport =
                (DdcClientTransportException) restored;
        assertThat(transport.retryable()).isEqualTo(retryable);
        assertThat(transport.getMessage())
                .doesNotContain("secret raw server text");
    }
}
