package top.egon.cola.component.rpc.ddc.mapping;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.ProtoUtils;
import top.egon.cola.component.ddc.error.DdcClientTransportException;
import top.egon.cola.component.ddc.error.DdcErrorStatus;
import top.egon.cola.component.ddc.error.DdcException;
import top.egon.cola.component.ddc.error.management.DdcManagementClientException;
import top.egon.cola.component.ddc.error.management.DdcManagementErrorCode;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcRpcErrorDetail;
import top.egon.cola.component.rpc.ddc.security.DdcRpcOperation;
import top.egon.cola.component.rpc.provider.RpcProviderExceptionMapper;

import java.util.Arrays;
import java.util.Optional;

/**
 * 在 DDC 业务异常与携带类型化 Trailer 的 gRPC 状态之间双向转换。
 * / Converts bidirectionally between DDC business failures and gRPC statuses
 * carrying a typed trailer.
 */
public final class DdcRpcStatusExceptionMapper
        implements RpcProviderExceptionMapper {

    public static final Metadata.Key<DdcRpcErrorDetail> ERROR_DETAIL =
            Metadata.Key.of(
                    "x-egon-ddc-error-bin",
                    ProtoUtils.metadataMarshaller(
                            DdcRpcErrorDetail.getDefaultInstance())
            );

    /** 服务端异常链映射入口。 / Server-side failure-chain mapping entry point. */
    @Override
    public Optional<StatusRuntimeException> map(Throwable throwable) {
        if (throwable instanceof DdcException
                || throwable instanceof DdcManagementClientException
                || throwable instanceof DdcClientTransportException) {
            return Optional.of(toStatus(throwable));
        }
        return Optional.empty();
    }

    /** 将已知 DDC 异常转换为 gRPC 状态。 / Converts a known DDC failure to gRPC status. */
    public StatusRuntimeException toStatus(Throwable throwable) {
        Failure failure = failure(throwable);
        Metadata trailers = new Metadata();
        trailers.put(
                ERROR_DETAIL,
                DdcRpcErrorDetail.newBuilder()
                        .setCode(failure.code())
                        .setMessage(failure.message())
                        .setRetryable(failure.retryable())
                        .build()
        );
        return failure.status().asRuntimeException(trailers);
    }

    /**
     * 客户端从原始或被通用 RPC 异常包装的状态中恢复 DDC 端口异常。
     * / Restores a DDC Port exception from a raw or general-RPC-wrapped status.
     */
    public RuntimeException restore(
            RuntimeException throwable,
            DdcRpcOperation operation) {
        if (throwable == null || operation == null) {
            throw new IllegalArgumentException(
                    "throwable and operation are required");
        }
        StatusRuntimeException status = findStatus(throwable);
        if (status == null) {
            return new DdcClientTransportException(
                    "DDC RPC invocation failed",
                    false,
                    throwable
            );
        }
        DdcRpcErrorDetail detail = detail(status.getTrailers());
        if (detail != null) {
            RuntimeException restored = restoreBusiness(
                    detail,
                    operation,
                    status
            );
            if (restored != null) {
                return restored;
            }
        }
        boolean retryable = switch (status.getStatus().getCode()) {
            case UNAVAILABLE, DEADLINE_EXCEEDED -> true;
            default -> false;
        };
        return new DdcClientTransportException(
                sanitizedMessage(status.getStatus().getCode()),
                retryable,
                status
        );
    }

    private RuntimeException restoreBusiness(
            DdcRpcErrorDetail detail,
            DdcRpcOperation operation,
            StatusRuntimeException cause) {
        Optional<DdcErrorStatus> runtime = Arrays.stream(DdcErrorStatus.values())
                .filter(value -> value.getStatus().equals(detail.getCode()))
                .findFirst();
        Optional<DdcManagementErrorCode> management = Arrays.stream(
                        DdcManagementErrorCode.values())
                .filter(value -> value.getStatus().equals(detail.getCode()))
                .findFirst();
        if (operation.management()) {
            if (management.isPresent()) {
                DdcManagementErrorCode value = management.orElseThrow();
                return new DdcManagementClientException(
                        value.getCode(), value.getStatus(), detail.getMessage());
            }
            if (runtime.isPresent()) {
                DdcErrorStatus value = runtime.orElseThrow();
                return new DdcManagementClientException(
                        value.getCode(), value.getStatus(), detail.getMessage());
            }
            return null;
        }
        if (runtime.isPresent()) {
            DdcErrorStatus value = runtime.orElseThrow();
            return new DdcException(
                    value.getCode(),
                    value.getStatus(),
                    detail.getMessage()
            );
        }
        return null;
    }

    private DdcRpcErrorDetail detail(Metadata trailers) {
        if (trailers == null) {
            return null;
        }
        try {
            DdcRpcErrorDetail detail = trailers.get(ERROR_DETAIL);
            if (detail == null || detail.getCode().isBlank()) {
                return null;
            }
            return detail;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private StatusRuntimeException findStatus(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof StatusRuntimeException status) {
                return status;
            }
            current = current.getCause();
        }
        return null;
    }

    private Failure failure(Throwable throwable) {
        if (throwable instanceof DdcException value) {
            return new Failure(
                    grpcStatus(value.getStatus()),
                    value.getStatus(),
                    safe(value.getMessage()),
                    value.isRetryable()
            );
        }
        if (throwable instanceof DdcManagementClientException value) {
            return new Failure(
                    grpcStatus(value.status()),
                    value.status(),
                    safe(value.getMessage()),
                    false
            );
        }
        if (throwable instanceof DdcClientTransportException value) {
            return new Failure(
                    value.retryable() ? Status.UNAVAILABLE : Status.INTERNAL,
                    "DDC_CLIENT_TRANSPORT_ERROR",
                    "DDC transport failed",
                    value.retryable()
            );
        }
        throw new IllegalArgumentException("Unsupported DDC failure", throwable);
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
                || code.contains("IN_USE")) {
            return Status.FAILED_PRECONDITION;
        }
        return Status.INTERNAL;
    }

    private String sanitizedMessage(Status.Code code) {
        return switch (code) {
            case INVALID_ARGUMENT -> "DDC RPC request is invalid";
            case NOT_FOUND -> "DDC resource was not found";
            case FAILED_PRECONDITION -> "DDC RPC precondition failed";
            case UNAUTHENTICATED -> "DDC RPC authentication failed";
            case PERMISSION_DENIED -> "DDC RPC permission denied";
            case UNAVAILABLE -> "DDC RPC service is unavailable";
            case DEADLINE_EXCEEDED -> "DDC RPC deadline exceeded";
            default -> "DDC RPC invocation failed";
        };
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "DDC operation failed" : value;
    }

    private record Failure(
            Status status,
            String code,
            String message,
            boolean retryable) {
    }
}
