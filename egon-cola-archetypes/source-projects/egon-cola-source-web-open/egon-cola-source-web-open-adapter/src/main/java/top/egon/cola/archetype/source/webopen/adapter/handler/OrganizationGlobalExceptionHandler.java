package top.egon.cola.archetype.source.webopen.adapter.handler;

import top.egon.cola.component.common.core.exception.ExceptionLogger;
import top.egon.cola.archetype.source.webopen.application.context.OrganizationRequestContext;
import top.egon.cola.archetype.source.webopen.application.context.OrganizationRequestContextHolder;
import top.egon.cola.archetype.source.webopen.common.exception.OrganizationApplicationException;
import top.egon.cola.archetype.source.webopen.common.enums.OrganizationFailureType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice(name = "organizationGlobalExceptionHandler")
public class OrganizationGlobalExceptionHandler {

    @ExceptionHandler(OrganizationApplicationException.class)
    public ResponseEntity<OrganizationErrorResult> handleApplication(OrganizationApplicationException failure) {
        ExceptionLogger.log(log, failure);
        return response(status(failure.failureType()), failure.code(), failure.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<OrganizationErrorResult> handleBodyValidation(MethodArgumentNotValidException failure) {
        Map<String, List<String>> fields = failure.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.groupingBy(error -> error.getField(), LinkedHashMap::new,
                Collectors.mapping(error -> error.getDefaultMessage() == null ? "invalid" : error.getDefaultMessage(),
                    Collectors.toList())));
        return response(HttpStatus.BAD_REQUEST, "ORG_VALIDATION_ERROR", "validation failed", fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<OrganizationErrorResult> handleConstraintValidation(ConstraintViolationException failure) {
        Map<String, List<String>> fields = failure.getConstraintViolations().stream()
            .collect(Collectors.groupingBy(this::fieldName, LinkedHashMap::new,
                Collectors.mapping(ConstraintViolation::getMessage, Collectors.toList())));
        return response(HttpStatus.BAD_REQUEST, "ORG_VALIDATION_ERROR", "validation failed", fields);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<OrganizationErrorResult> handleUnexpected(Exception failure) {
        ExceptionLogger.log(log, failure);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "ORG_INTERNAL_ERROR", "internal error", Map.of());
    }

    private ResponseEntity<OrganizationErrorResult> response(
            HttpStatus status, String code, String message, Map<String, List<String>> fieldErrors) {
        return ResponseEntity.status(status).body(new OrganizationErrorResult(
            code, message, traceId(), Instant.now(), fieldErrors));
    }

    private static HttpStatus status(OrganizationFailureType type) {
        return switch (type) {
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case DOMAIN_REJECTED -> HttpStatus.UNPROCESSABLE_ENTITY;
            case DEPENDENCY_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case INTERNAL -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private String fieldName(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int separator = path.lastIndexOf('.');
        return separator < 0 ? path : path.substring(separator + 1);
    }

    private static String traceId() {
        return OrganizationRequestContextHolder.current().map(OrganizationRequestContext::traceId)
            .orElseGet(() -> {
                String trace = MDC.get("traceId");
                return trace == null ? "unknown" : trace;
            });
    }
}
