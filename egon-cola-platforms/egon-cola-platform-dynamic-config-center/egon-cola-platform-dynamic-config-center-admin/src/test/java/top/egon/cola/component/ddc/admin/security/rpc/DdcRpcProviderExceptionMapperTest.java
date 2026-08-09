package top.egon.cola.component.ddc.admin.security.rpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.TransientDataAccessResourceException;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.error.DdcClientTransportException;
import top.egon.cola.component.ddc.error.DdcErrorStatus;
import top.egon.cola.component.ddc.error.management.DdcManagementErrorCode;
import top.egon.cola.component.rpc.ddc.mapping.DdcRpcStatusExceptionMapper;

import static org.assertj.core.api.Assertions.assertThat;

class DdcRpcProviderExceptionMapperTest {

    private final DdcRpcProviderExceptionMapper mapper =
            new DdcRpcProviderExceptionMapper();

    @Test
    void mapsStableDdcCategoriesWithTypedDetails() {
        assertMapped(
                new DdcAdminException(DdcErrorStatus.INVALID_REQUEST),
                Status.Code.INVALID_ARGUMENT,
                DdcErrorStatus.INVALID_REQUEST.getStatus(),
                false
        );
        assertMapped(
                new DdcAdminException(DdcManagementErrorCode.CONFIG_NOT_FOUND),
                Status.Code.NOT_FOUND,
                DdcManagementErrorCode.CONFIG_NOT_FOUND.getStatus(),
                false
        );
        assertMapped(
                new DdcAdminException(DdcErrorStatus.PUBLISH_IN_PROGRESS),
                Status.Code.FAILED_PRECONDITION,
                DdcErrorStatus.PUBLISH_IN_PROGRESS.getStatus(),
                false
        );
        assertMapped(
                new DdcClientTransportException("redis-secret", true),
                Status.Code.UNAVAILABLE,
                "DDC_CLIENT_TRANSPORT_ERROR",
                true
        );
        assertMapped(
                new TransientDataAccessResourceException("database-secret"),
                Status.Code.UNAVAILABLE,
                "DDC_SERVICE_UNAVAILABLE",
                true
        );
    }

    @Test
    void sanitizesMappedDescriptionsAndLeavesUnknownFailuresToCoreFallback() {
        StatusRuntimeException invalid = mapper.map(
                new IllegalArgumentException("config-secret"))
                .orElseThrow();

        assertThat(invalid.getStatus().getCode())
                .isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(invalid.getStatus().getDescription())
                .doesNotContain("config-secret");
        assertThat(invalid.getTrailers()
                .get(DdcRpcStatusExceptionMapper.ERROR_DETAIL)
                .getMessage()).doesNotContain("config-secret");
        assertThat(mapper.map(new IllegalStateException("server-secret")))
                .isEmpty();
    }

    private void assertMapped(
            RuntimeException failure,
            Status.Code status,
            String code,
            boolean retryable) {
        StatusRuntimeException mapped = mapper.map(failure).orElseThrow();
        var detail = mapped.getTrailers()
                .get(DdcRpcStatusExceptionMapper.ERROR_DETAIL);
        assertThat(mapped.getStatus().getCode()).isEqualTo(status);
        assertThat(detail.getCode()).isEqualTo(code);
        assertThat(detail.getRetryable()).isEqualTo(retryable);
        assertThat(mapped.getStatus().getDescription())
                .doesNotContain(
                        "redis-secret",
                        "database-secret",
                        "config-secret",
                        "server-secret"
                );
    }
}
