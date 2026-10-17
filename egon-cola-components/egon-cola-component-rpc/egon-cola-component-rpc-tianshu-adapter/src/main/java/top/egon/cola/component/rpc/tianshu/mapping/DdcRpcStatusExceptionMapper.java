package top.egon.cola.component.rpc.tianshu.mapping;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.ProtoUtils;
import top.egon.cola.component.tianshu.error.DdcClientTransportException;
import top.egon.cola.component.tianshu.error.DdcErrorStatus;
import top.egon.cola.component.tianshu.error.DdcException;
import top.egon.cola.component.tianshu.error.management.DdcManagementClientException;
import top.egon.cola.component.tianshu.error.management.DdcManagementErrorCode;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.DdcRpcErrorDetail;
import top.egon.cola.component.rpc.tianshu.security.DdcRpcOperation;
import top.egon.cola.component.rpc.provider.server.RpcProviderExceptionMapper;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/**
 * 在 Tianshu 业务异常与携带类型化 Trailer 的 gRPC 状态之间双向转换。
 * / Converts bidirectionally between Tianshu business failures and gRPC statuses
 * carrying a typed trailer.
 */
public final class DdcRpcStatusExceptionMapper
        implements RpcProviderExceptionMapper {

    public static final Metadata.Key<DdcRpcErrorDetail> ERROR_DETAIL =
            Metadata.Key.of(
                    "x-egon-tianshu-error-bin",
                    ProtoUtils.metadataMarshaller(
                            DdcRpcErrorDetail.getDefaultInstance())
            );

    /**
     * Admin AI 业务状态串到稳定业务码的映射，用于在客户端恢复类型化异常。
     * / Admin AI business status strings mapped to stable business codes for typed client-side restoration.
     */
    private static final Map<String, Integer> AI_STATUS_CODES = Map.ofEntries(
            Map.entry("TIANSHU_AI_VALIDATION_ERROR", 56301),
            Map.entry("TIANSHU_AI_NOT_FOUND", 56302),
            Map.entry("TIANSHU_AI_REVISION_CONFLICT", 56303),
            Map.entry("TIANSHU_AI_DUPLICATE_RESOURCE_KEY", 56304),
            Map.entry("TIANSHU_AI_IDEMPOTENCY_CONFLICT", 56305),
            Map.entry("TIANSHU_AI_NOT_PUBLISHED", 56306),
            Map.entry("TIANSHU_AI_SCOPE_FORBIDDEN", 56307),
            Map.entry("TIANSHU_AI_KIND_MISMATCH", 56308),
            Map.entry("TIANSHU_AI_CONTENT_INTEGRITY", 56309),
            Map.entry("TIANSHU_AI_REQUEST_TOO_LARGE", 56312),
            Map.entry("TIANSHU_AI_VERSION_COUNTER_EXHAUSTED", 56314),
            Map.entry("TIANSHU_AI_LIFECYCLE_CONFLICT", 56316),
            Map.entry("TIANSHU_AI_FEEDBACK_INVALID", 56320),
            Map.entry("TIANSHU_AI_FEEDBACK_SKEW", 56321),
            Map.entry("TIANSHU_AI_OPTIMIZATION_DISABLED", 56322),
            Map.entry("TIANSHU_AI_RUN_ACTIVE", 56323),
            Map.entry("TIANSHU_AI_POLICY_STATE", 56324),
            Map.entry("TIANSHU_AI_RUN_NOT_FOUND", 56325));

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

    /** 将已知 Tianshu 异常转换为 gRPC 状态。 / Converts a known Tianshu failure to gRPC status. */
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
     * 客户端从原始或被通用 RPC 异常包装的状态中恢复 Tianshu 端口异常。
     * / Restores a Tianshu Port exception from a raw or general-RPC-wrapped status.
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
                    "Tianshu RPC invocation failed",
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
        Integer aiCode = AI_STATUS_CODES.get(detail.getCode());
        if (aiCode != null) {
            return new DdcException(aiCode, detail.getCode(), detail.getMessage());
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
                    "TIANSHU_CLIENT_TRANSPORT_ERROR",
                    "Tianshu transport failed",
                    value.retryable()
            );
        }
        throw new IllegalArgumentException("Unsupported Tianshu failure", throwable);
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
            case INVALID_ARGUMENT -> "Tianshu RPC request is invalid";
            case NOT_FOUND -> "Tianshu resource was not found";
            case FAILED_PRECONDITION -> "Tianshu RPC precondition failed";
            case UNAUTHENTICATED -> "Tianshu RPC authentication failed";
            case PERMISSION_DENIED -> "Tianshu RPC permission denied";
            case UNAVAILABLE -> "Tianshu RPC service is unavailable";
            case DEADLINE_EXCEEDED -> "Tianshu RPC deadline exceeded";
            default -> "Tianshu RPC invocation failed";
        };
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "Tianshu operation failed" : value;
    }

    private record Failure(
            Status status,
            String code,
            String message,
            boolean retryable) {
    }
}
