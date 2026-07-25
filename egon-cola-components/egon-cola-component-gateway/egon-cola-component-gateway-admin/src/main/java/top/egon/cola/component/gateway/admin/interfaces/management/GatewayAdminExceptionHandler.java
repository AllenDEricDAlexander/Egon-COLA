package top.egon.cola.component.gateway.admin.interfaces.management;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.egon.cola.component.gateway.admin.application.GatewayAdminNotFoundException;
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
