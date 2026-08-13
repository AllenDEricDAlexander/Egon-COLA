package top.egon.cola.component.rpc.exception;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import top.egon.cola.component.rpc.context.invocation.RpcFailureStage;

public class RpcStatusExceptionMapper {

    private static final Metadata.Key<String> ERROR_TYPE =
            Metadata.Key.of(
                    "x-egon-rpc-error-type",
                    Metadata.ASCII_STRING_MARSHALLER
            );

    public EgonRpcException map(StatusRuntimeException exception) {
        EgonRpcErrorCode code = switch (exception.getStatus().getCode()) {
            case DEADLINE_EXCEEDED ->
                    EgonRpcErrorCode.RPC_DEADLINE_EXCEEDED;
            case CANCELLED -> EgonRpcErrorCode.RPC_CANCELLED;
            case UNAVAILABLE -> unavailableCode(exception.getTrailers());
            case INVALID_ARGUMENT -> EgonRpcErrorCode.RPC_INVALID_REQUEST;
            case PERMISSION_DENIED -> EgonRpcErrorCode.RPC_PROVIDER_REJECTED;
            case UNIMPLEMENTED -> EgonRpcErrorCode.RPC_METHOD_NOT_FOUND;
            case NOT_FOUND -> notFoundCode(exception.getTrailers());
            default -> EgonRpcErrorCode.RPC_INTERNAL;
        };
        return new EgonRpcException(
                code,
                sanitizedMessage(exception.getStatus(), code),
                exception
        );
    }

    private EgonRpcErrorCode notFoundCode(Metadata trailers) {
        String type = trailers == null ? null : trailers.get(ERROR_TYPE);
        return "method".equalsIgnoreCase(type)
                ? EgonRpcErrorCode.RPC_METHOD_NOT_FOUND
                : EgonRpcErrorCode.RPC_SERVICE_NOT_FOUND;
    }

    private EgonRpcErrorCode unavailableCode(Metadata trailers) {
        return RpcFailureStage.from(trailers)
                .filter(stage -> stage == RpcFailureStage.PROVIDER)
                .isPresent()
                ? EgonRpcErrorCode.RPC_PROVIDER_UNAVAILABLE
                : EgonRpcErrorCode.RPC_GATEWAY_UNAVAILABLE;
    }

    private String sanitizedMessage(Status status, EgonRpcErrorCode code) {
        return switch (code) {
            case RPC_DEADLINE_EXCEEDED -> "RPC deadline exceeded";
            case RPC_CANCELLED -> "RPC call was cancelled";
            case RPC_GATEWAY_UNAVAILABLE -> "RPC Gateway is unavailable";
            case RPC_PROVIDER_UNAVAILABLE -> "RPC Provider is unavailable";
            case RPC_INVALID_REQUEST -> "RPC request is invalid";
            case RPC_PROVIDER_REJECTED -> "RPC Provider rejected the request";
            case RPC_METHOD_NOT_FOUND -> "RPC method was not found";
            case RPC_SERVICE_NOT_FOUND -> "RPC service was not found";
            default -> status.getCode() == Status.Code.INTERNAL
                    ? "RPC invocation failed"
                    : "RPC transport failed";
        };
    }
}
