package top.egon.cola.archetype.source.web.adapter.handler;

import top.egon.cola.component.common.core.exception.ExceptionLogger;
import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContext;
import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContextHolder;
import top.egon.cola.archetype.source.web.common.exception.OrganizationApplicationException;
import top.egon.cola.archetype.source.web.common.enums.OrganizationFailureType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService.RuntimeUnavailableException;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.DefaultAuthorizationService.AuthorizationDeniedException;
import top.egon.cola.platform.tianquan.jianshen.starter.web.Rbac3AuthorizationExceptionHandler;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpSecurityObservationFilter;
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
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class OrganizationGlobalExceptionHandler {
    @Qualifier("rbac3AuthorizationExceptionHandler")
    private final Rbac3AuthorizationExceptionHandler securityErrors;

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAuthorization(AuthorizationDeniedException failure) {
        return confirmedDenial() ? securityErrors.denied(failure) : auditUnavailable();
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleScopeOrFields(AccessDeniedException failure) {
        return confirmedDenial() ? securityErrors.fieldDenied(
                new org.springframework.security.authorization.AuthorizationDeniedException(failure.getMessage())) : auditUnavailable();
    }

    @ExceptionHandler(RuntimeUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleSecurityUnavailable(RuntimeUnavailableException failure) {
        return securityErrors.unavailable(failure);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException failure) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)
                || !IdpSecurityObservationFilter.beforeAuthenticationFailure(attributes.getRequest())) {
            return auditUnavailable();
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header("Cache-Control", "no-store")
                .header("WWW-Authenticate", "Bearer").body(Map.of(
                        "code", "AUTHENTICATION_REQUIRED", "message", "Tianquan-Shoubing authentication failed"));
    }

    private static boolean confirmedDenial() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) { return false; }
        var marker = IdpSecurityObservationFilter.authorizationObservationContext(attributes.getRequest());
        return marker != null && marker.isActive() && IdpSecurityObservationFilter.beforeAuthorizationDenied(attributes.getRequest())
                && marker.isConfirmed();
    }

    private static ResponseEntity<Map<String, Object>> auditUnavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).header("Cache-Control", "no-store").body(Map.of(
                "code", "OAUTH_AUDIT_DELIVERY_UNAVAILABLE", "message", "Tianquan-Jianshen authorization runtime unavailable"));
    }

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
