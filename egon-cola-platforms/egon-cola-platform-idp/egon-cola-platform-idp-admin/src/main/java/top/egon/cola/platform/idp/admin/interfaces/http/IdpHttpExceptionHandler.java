package top.egon.cola.platform.idp.admin.interfaces.http;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class IdpHttpExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(
            IdpHttpExceptionHandler.class
    );

    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErrorResponse> invalidRequest(Exception exception) {
        return response(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "request is invalid"
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> forbidden(
            AccessDeniedException exception
    ) {
        return response(
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                "request is not authorized"
        );
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> notFound(
            NoSuchElementException exception
    ) {
        return response(
                HttpStatus.NOT_FOUND,
                "NOT_FOUND",
                "requested resource was not found"
        );
    }

    @ExceptionHandler({
            IllegalStateException.class,
            OptimisticLockingFailureException.class
    })
    public ResponseEntity<ErrorResponse> conflict(Exception exception) {
        return response(
                HttpStatus.CONFLICT,
                "CONFLICT",
                "request conflicts with current state"
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> internalError(Exception exception) {
        LOG.error("Unhandled IdP request failure", exception);
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "request could not be completed"
        );
    }

    private ResponseEntity<ErrorResponse> response(
            HttpStatus status,
            String code,
            String message
    ) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                code,
                message,
                Instant.now()
        ));
    }

    public record ErrorResponse(
            String code,
            String message,
            Instant timestamp
    ) {
    }
}
