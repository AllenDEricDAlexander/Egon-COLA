package top.egon.cola.archetype.source.serviceopen.adapter.handler;

import top.egon.cola.archetype.source.serviceopen.common.exception.ApplicationException;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.validation.ConstraintViolationException;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.rpc.RpcContext;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;

/** Normalizes use-case rejections into the gRPC status plus the string code carried on trailers. */
@Component("globalFacadeExceptionHandler")
@Slf4j
public class GlobalFacadeExceptionHandler {

    private static final Metadata.Key<String> ERROR_CODE = Metadata.Key.of(
            "x-egon-error-code", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> TRACE_ID = Metadata.Key.of(
            "x-egon-trace-id", Metadata.ASCII_STRING_MARSHALLER);

    public StatusRuntimeException toStatus(RuntimeException failure) {
        Objects.requireNonNull(failure, "failure");
        if (failure instanceof StatusRuntimeException statusFailure) {
            return statusFailure;
        }
        ApplicationException applicationFailure = failure instanceof ApplicationException typed
                ? typed : null;
        boolean rejectedInput = isValidationFailure(failure);
        String errorCode = applicationFailure == null
                ? (rejectedInput ? "VALIDATION_FAILED" : "INTERNAL_ERROR")
                : applicationFailure.getStatus();
        Status status = statusFor(applicationFailure, failure);
        Metadata trailers = new Metadata();
        trailers.put(ERROR_CODE, errorCode);
        trailers.put(TRACE_ID, traceId());
        String description = applicationFailure == null
                ? (rejectedInput ? failure.getMessage() : "service request failed")
                : applicationFailure.getMessage();
        if (applicationFailure == null && !rejectedInput) {
            log.warn("unexpected open facade failure masked as {}", errorCode, failure);
        }
        return status.withDescription(description == null ? "service request failed" : description)
                .asRuntimeException(trailers);
    }

    private static Status statusFor(ApplicationException applicationFailure, RuntimeException failure) {
        if (isValidationFailure(failure)) {
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

    /** Jakarta bean validation and the handwritten relation hooks are the same wire rejection. */
    private static boolean isValidationFailure(RuntimeException failure) {
        return failure instanceof IllegalArgumentException || failure instanceof ConstraintViolationException;
    }

    private String traceId() {
        String supplied = RpcContext.getServerAttachment().getAttachment("x-egon-trace-id");
        if (supplied != null && !supplied.isBlank()) {
            return supplied;
        }
        return Long.toString(SnowflakeIdGenerator.nextLongId());
    }
}
