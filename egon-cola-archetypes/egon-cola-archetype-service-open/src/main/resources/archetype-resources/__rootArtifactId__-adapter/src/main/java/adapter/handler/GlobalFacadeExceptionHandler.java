#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.handler;

import ${package}.application.exceptions.ApplicationException;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.rpc.RpcContext;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.id.generator.LongIdGenerator;

@Component
@RequiredArgsConstructor
public class GlobalFacadeExceptionHandler {

    private static final Metadata.Key<String> ERROR_CODE = Metadata.Key.of(
            "x-egon-error-code", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> TRACE_ID = Metadata.Key.of(
            "x-egon-trace-id", Metadata.ASCII_STRING_MARSHALLER);

    private final LongIdGenerator idGenerator;

    public StatusRuntimeException toStatus(RuntimeException failure) {
        Objects.requireNonNull(failure, "failure");
        if (failure instanceof StatusRuntimeException statusFailure) {
            return statusFailure;
        }
        ApplicationException applicationFailure = failure instanceof ApplicationException typed
                ? typed : null;
        String errorCode = applicationFailure == null
                ? (failure instanceof IllegalArgumentException ? "VALIDATION_FAILED" : "INTERNAL_ERROR")
                : applicationFailure.code().name();
        Status status = statusFor(applicationFailure, failure);
        Metadata trailers = new Metadata();
        trailers.put(ERROR_CODE, errorCode);
        trailers.put(TRACE_ID, traceId());
        String description = applicationFailure == null
                ? (failure instanceof IllegalArgumentException ? failure.getMessage() : "service request failed")
                : applicationFailure.getMessage();
        return status.withDescription(description == null ? "service request failed" : description)
                .asRuntimeException(trailers);
    }

    private static Status statusFor(ApplicationException applicationFailure, RuntimeException failure) {
        if (failure instanceof IllegalArgumentException) {
            return Status.INVALID_ARGUMENT;
        }
        if (applicationFailure == null) {
            return Status.INTERNAL;
        }
        return switch (applicationFailure.code()) {
            case COURSE_NOT_FOUND, EXAM_NOT_FOUND, EXAM_PAPER_NOT_FOUND, SCORE_NOT_FOUND -> Status.NOT_FOUND;
            case COURSE_CODE_DUPLICATED -> Status.ALREADY_EXISTS;
            case BUSINESS_REJECTED -> Status.FAILED_PRECONDITION;
            case INFRASTRUCTURE_FAILURE -> Status.UNAVAILABLE;
            case VALIDATION_FAILED -> Status.INVALID_ARGUMENT;
        };
    }

    private String traceId() {
        String supplied = RpcContext.getServerAttachment().getAttachment("x-egon-trace-id");
        if (supplied != null && !supplied.isBlank()) {
            return supplied;
        }
        return Long.toString(idGenerator.nextLongId());
    }
}
