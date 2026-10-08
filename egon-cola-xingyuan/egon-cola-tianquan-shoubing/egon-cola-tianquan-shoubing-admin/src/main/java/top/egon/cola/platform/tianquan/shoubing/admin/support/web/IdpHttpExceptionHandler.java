package top.egon.cola.platform.tianquan.shoubing.admin.support.web;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.NoSuchElementException;

@RestControllerAdvice
@Component("idpHttpExceptionHandler")
@Slf4j
public class IdpHttpExceptionHandler {

    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErrorResponse> invalidRequest(Exception exception) {
        log.debug("Rejected invalid Tianquan-Shoubing request", exception);
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

    @ExceptionHandler(OAuth2AuthenticationException.class)
    public ResponseEntity<ErrorResponse> temporarilyUnavailable(OAuth2AuthenticationException exception) {
        if (exception.getError() != null
                && "temporarily_unavailable".equals(exception.getError().getErrorCode())) {
            return response(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "TEMPORARILY_UNAVAILABLE",
                    "request temporarily unavailable");
        }
        return internalError(exception);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> internalError(Exception exception) {
        log.error("Unhandled Tianquan-Shoubing request failure", exception);
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
