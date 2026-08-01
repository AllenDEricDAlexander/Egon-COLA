package top.egon.cola.platform.rbac3.starter.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationService;
import top.egon.cola.platform.rbac3.starter.authorization.DefaultAuthorizationService.AuthorizationDeniedException;

import java.util.Map;

@RestControllerAdvice
public final class Rbac3AuthorizationExceptionHandler {

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<Map<String, Object>> denied(
            AuthorizationDeniedException exception
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "code", exception.reasonCode(),
                "message", "RBAC3 authorization denied"));
    }

    @ExceptionHandler(AuthorizationService.RuntimeUnavailableException.class)
    public ResponseEntity<Map<String, Object>> unavailable(
            AuthorizationService.RuntimeUnavailableException exception
    ) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "code", exception.reasonCode(),
                "message", "RBAC3 authorization runtime unavailable"));
    }
}
