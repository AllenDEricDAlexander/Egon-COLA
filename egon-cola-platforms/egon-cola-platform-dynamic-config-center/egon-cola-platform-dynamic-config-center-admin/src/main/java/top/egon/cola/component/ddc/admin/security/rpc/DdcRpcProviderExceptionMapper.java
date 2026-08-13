package top.egon.cola.component.ddc.admin.security.rpc;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.redisson.client.RedisException;
import org.springframework.dao.TransientDataAccessException;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.error.DdcClientTransportException;
import top.egon.cola.component.ddc.error.DdcErrorStatus;
import top.egon.cola.component.ddc.error.management.DdcManagementErrorCode;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcRpcErrorDetail;
import top.egon.cola.component.rpc.ddc.mapping.DdcRpcStatusExceptionMapper;
import top.egon.cola.component.rpc.provider.server.RpcProviderExceptionMapper;

import java.util.Arrays;
import java.util.Optional;

/**
 * 将 Admin 领域失败映射为稳定且脱敏的 DDC RPC 状态。
 * / Maps Admin domain failures to stable, sanitized DDC RPC statuses.
 */
public final class DdcRpcProviderExceptionMapper
        implements RpcProviderExceptionMapper {

    private final DdcRpcStatusExceptionMapper delegate =
            new DdcRpcStatusExceptionMapper();

    @Override
    public Optional<StatusRuntimeException> map(Throwable throwable) {
        if (throwable instanceof DdcAdminException admin) {
            return Optional.of(status(
                    grpcStatus(admin.getStatus()),
                    admin.getStatus(),
                    message(admin.getStatus()),
                    admin.isRetryable()
            ));
        }
        if (throwable instanceof IllegalArgumentException) {
            return Optional.of(status(
                    Status.INVALID_ARGUMENT,
                    DdcErrorStatus.INVALID_REQUEST.getStatus(),
                    DdcErrorStatus.INVALID_REQUEST.getMessage(),
                    false
            ));
        }
        if (throwable instanceof DdcClientTransportException transport) {
            return Optional.of(status(
                    transport.retryable() ? Status.UNAVAILABLE : Status.INTERNAL,
                    "DDC_CLIENT_TRANSPORT_ERROR",
                    "DDC transport failed",
                    transport.retryable()
            ));
        }
        if (throwable instanceof TransientDataAccessException
                || throwable instanceof RedisException) {
            return Optional.of(status(
                    Status.UNAVAILABLE,
                    "DDC_SERVICE_UNAVAILABLE",
                    "DDC service is temporarily unavailable",
                    true
            ));
        }
        return delegate.map(throwable);
    }

    /** 构造携带类型化 DDC detail 的脱敏状态。 / Builds a sanitized status with typed DDC detail. */
    public static StatusRuntimeException status(
            Status status,
            String code,
            String message,
            boolean retryable) {
        Metadata trailers = new Metadata();
        trailers.put(
                DdcRpcStatusExceptionMapper.ERROR_DETAIL,
                DdcRpcErrorDetail.newBuilder()
                        .setCode(code)
                        .setMessage(message)
                        .setRetryable(retryable)
                        .build()
        );
        return status.withDescription(message).asRuntimeException(trailers);
    }

    private Status grpcStatus(String code) {
        if (code == null) {
            return Status.INTERNAL;
        }
        if (code.contains("SIGNATURE")) {
            return Status.UNAUTHENTICATED;
        }
        if (code.contains("SCOPE_DISABLED")) {
            return Status.PERMISSION_DENIED;
        }
        if (code.contains("NOT_FOUND")) {
            return Status.NOT_FOUND;
        }
        if (code.contains("INVALID_REQUEST")) {
            return Status.INVALID_ARGUMENT;
        }
        if (code.contains("MISMATCH")
                || code.contains("CONFLICT")
                || code.contains("IN_PROGRESS")
                || code.contains("NO_LIVE_INSTANCE")
                || code.contains("TARGET_LEASE_EXPIRED")
                || code.contains("IN_USE")) {
            return Status.FAILED_PRECONDITION;
        }
        return Status.INTERNAL;
    }

    private String message(String code) {
        return Arrays.stream(DdcErrorStatus.values())
                .filter(value -> value.getStatus().equals(code))
                .map(DdcErrorStatus::getMessage)
                .findFirst()
                .or(() -> Arrays.stream(DdcManagementErrorCode.values())
                        .filter(value -> value.getStatus().equals(code))
                        .map(DdcManagementErrorCode::getMessage)
                        .findFirst())
                .orElse("DDC operation failed");
    }
}
