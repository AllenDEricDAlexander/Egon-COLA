package top.egon.cola.component.gateway.admin.interfaces.management;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.egon.cola.component.gateway.admin.application.GatewayAdminNotFoundException;
import top.egon.cola.component.gateway.admin.application.GatewayAdminIdempotencyConflictException;
import top.egon.cola.component.gateway.admin.domain.GatewayAdminRevisionConflictException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GatewayAdminExceptionHandler {

    @ExceptionHandler(GatewayAdminRevisionConflictException.class)
    public ResponseEntity<ErrorResponse> revision(
            GatewayAdminRevisionConflictException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponse(
                        "GATEWAY_ADMIN_REVISION_CONFLICT",
                        "draft or resource revision is stale",
                        error.currentRevision(),
                        List.of(),
                        Instant.now()
                )
        );
    }

    @ExceptionHandler(GatewayAdminIdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> idempotency(
            GatewayAdminIdempotencyConflictException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponse(
                        "GATEWAY_ADMIN_IDEMPOTENCY_CONFLICT",
                        error.getMessage(),
                        null,
                        List.of(),
                        Instant.now()
                )
        );
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> optimisticLock(
            ObjectOptimisticLockingFailureException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponse(
                        "GATEWAY_ADMIN_REVISION_CONFLICT",
                        "resource was modified concurrently",
                        null,
                        List.of(),
                        Instant.now()
                )
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> uniqueConflict(
            DataIntegrityViolationException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponse(
                        "GATEWAY_ADMIN_RESOURCE_CONFLICT",
                        "resource violates a uniqueness or reference constraint",
                        null,
                        List.of(),
                        Instant.now()
                )
        );
    }

    @ExceptionHandler(GatewayAdminNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(
            GatewayAdminNotFoundException error) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponse(
                        "GATEWAY_ADMIN_NOT_FOUND",
                        error.getMessage(),
                        null,
                        List.of(),
                        Instant.now()
                )
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(
            MethodArgumentNotValidException error) {
        List<FieldError> fields = error.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(item -> new FieldError(
                        item.getField(),
                        "INVALID",
                        item.getDefaultMessage()
                ))
                .toList();
        return ResponseEntity.unprocessableEntity().body(
                new ErrorResponse(
                        "GATEWAY_ADMIN_VALIDATION_FAILED",
                        "request validation failed",
                        null,
                        fields,
                        Instant.now()
                )
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> invalid(
            IllegalArgumentException error) {
        return ResponseEntity.unprocessableEntity().body(
                new ErrorResponse(
                        "GATEWAY_ADMIN_VALIDATION_FAILED",
                        error.getMessage(),
                        null,
                        List.of(new FieldError(
                                "$",
                                "INVALID",
                                error.getMessage()
                        )),
                        Instant.now()
                )
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> invalidState(
            IllegalStateException error) {
        boolean unavailable = error.getMessage() != null
                && (error.getMessage().contains("DDC")
                || error.getMessage().contains("PROTECTOR"));
        String code = unavailable
                ? "GATEWAY_ADMIN_DDC_UNAVAILABLE"
                : errorCode(error.getMessage());
        return ResponseEntity.status(
                unavailable
                        ? HttpStatus.SERVICE_UNAVAILABLE
                        : HttpStatus.CONFLICT
        ).body(new ErrorResponse(
                code,
                error.getMessage(),
                null,
                List.of(),
                Instant.now()
        ));
    }

    private String errorCode(String message) {
        if (message != null
                && message.startsWith("GATEWAY_ADMIN_")) {
            int separator = message.indexOf(':');
            return separator < 0
                    ? message
                    : message.substring(0, separator);
        }
        return "GATEWAY_ADMIN_RESOURCE_CONFLICT";
    }

    public record ErrorResponse(
            String code,
            String message,
            Long currentRevision,
            List<FieldError> errors,
            Instant timestamp
    ) {
    }

    public record FieldError(
            String path,
            String code,
            String message
    ) {
    }
}
