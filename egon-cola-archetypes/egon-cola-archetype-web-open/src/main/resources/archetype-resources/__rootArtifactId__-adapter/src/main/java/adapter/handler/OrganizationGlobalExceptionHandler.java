package ${package}.adapter.handler;

import ${package}.application.context.OrganizationRequestContext;
import ${package}.application.context.OrganizationRequestContextHolder;
import ${package}.application.exceptions.OrganizationApplicationException;
import ${package}.application.exceptions.OrganizationFailureType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@RestControllerAdvice(name = "organizationGlobalExceptionHandler")
public class OrganizationGlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(OrganizationGlobalExceptionHandler.class);

    @ExceptionHandler(OrganizationApplicationException.class)
    public ResponseEntity<OrganizationErrorResponse> handleApplication(OrganizationApplicationException failure) {
        return response(status(failure.failureType()), failure.code(), failure.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<OrganizationErrorResponse> handleBodyValidation(MethodArgumentNotValidException failure) {
        Map<String, List<String>> fields = failure.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.groupingBy(error -> error.getField(), LinkedHashMap::new,
                Collectors.mapping(error -> error.getDefaultMessage() == null ? "invalid" : error.getDefaultMessage(),
                    Collectors.toList())));
        return response(HttpStatus.BAD_REQUEST, "ORG_VALIDATION_ERROR", "validation failed", fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<OrganizationErrorResponse> handleConstraintValidation(ConstraintViolationException failure) {
        Map<String, List<String>> fields = failure.getConstraintViolations().stream()
            .collect(Collectors.groupingBy(this::fieldName, LinkedHashMap::new,
                Collectors.mapping(ConstraintViolation::getMessage, Collectors.toList())));
        return response(HttpStatus.BAD_REQUEST, "ORG_VALIDATION_ERROR", "validation failed", fields);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<OrganizationErrorResponse> handleUnexpected(Exception failure) {
        log.error("unexpected organization request failure traceId={}", traceId(), failure);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "ORG_INTERNAL_ERROR", "internal error", Map.of());
    }

    private ResponseEntity<OrganizationErrorResponse> response(
            HttpStatus status, String code, String message, Map<String, List<String>> fieldErrors) {
        return ResponseEntity.status(status).body(new OrganizationErrorResponse(
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
