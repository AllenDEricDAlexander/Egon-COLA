package top.egon.cola.platform.rbac3.admin.interfaces.http;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.egon.cola.platform.rbac3.contract.error.Rbac3ErrorCode;
import top.egon.cola.platform.rbac3.contract.error.Rbac3ErrorResponse;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class Rbac3ApiExceptionHandler {

    @ExceptionHandler(Rbac3RuleViolation.class)
    public ResponseEntity<Rbac3ErrorResponse> handleRuleViolation(
            Rbac3RuleViolation error,
            HttpServletRequest request
    ) {
        Rbac3ErrorCode code = toCode(error.reasonCode());
        return response(code, "Request rejected by authorization policy", request);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public ResponseEntity<Rbac3ErrorResponse> handleInvalidRequest(
            Exception error,
            HttpServletRequest request
    ) {
        return response(Rbac3ErrorCode.REQUEST_INVALID,
                "Request payload is invalid", request);
    }

    private ResponseEntity<Rbac3ErrorResponse> response(
            Rbac3ErrorCode code,
            String message,
            HttpServletRequest request
    ) {
        String requestId = headerOrGenerated(request, "X-Request-Id");
        String traceId = headerOrGenerated(request, "X-Trace-Id");
        Rbac3ErrorResponse body = new Rbac3ErrorResponse(
                new Rbac3ErrorResponse.Error(
                        code, message, code.retryable(), List.of()),
                new Rbac3ErrorResponse.Meta(requestId, traceId, Instant.now())
        );
        return ResponseEntity.status(code.httpStatus()).body(body);
    }

    private Rbac3ErrorCode toCode(String reasonCode) {
        try {
            return Rbac3ErrorCode.valueOf(reasonCode);
        } catch (IllegalArgumentException ignored) {
            return Rbac3ErrorCode.REQUEST_INVALID;
        }
    }

    private String headerOrGenerated(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value == null || value.isBlank() ? UUID.randomUUID().toString() : value.trim();
    }
}
