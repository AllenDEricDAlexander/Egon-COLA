package top.egon.cola.platform.rbac3.starter.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationService;
import top.egon.cola.platform.rbac3.starter.authorization.DefaultAuthorizationService.AuthorizationDeniedException;

import java.util.Map;

/**
 * 类型 `Rbac3AuthorizationExceptionHandler` 位于当前包内，是类型，用于承载 `Rbac3 Authorization Exception Handler` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3AuthorizationExceptionHandler` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Authorization Exception Handler`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `Rbac3AuthorizationExceptionHandler` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `Rbac3AuthorizationExceptionHandler` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@RestControllerAdvice
public final class Rbac3AuthorizationExceptionHandler {

    /**
     * 方法 `denied` 按照 `Rbac3AuthorizationExceptionHandler` 的职责处理输入，完成 `denied` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `denied` processes its inputs according to `Rbac3AuthorizationExceptionHandler`'s responsibility, performs the `denied` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `denied` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `denied`, then continue the business flow using its result, exception, or side effect.
     *
     * @param exception 输入参数 `exception`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<Map<String, Object>> denied(
            AuthorizationDeniedException exception
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "code", exception.reasonCode(),
                "message", "RBAC3 authorization denied"));
    }

    /**
     * 方法 `unavailable` 按照 `Rbac3AuthorizationExceptionHandler` 的职责处理输入，完成 `unavailable` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `unavailable` processes its inputs according to `Rbac3AuthorizationExceptionHandler`'s responsibility, performs the `unavailable` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `unavailable` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `unavailable`, then continue the business flow using its result, exception, or side effect.
     *
     * @param exception 输入参数 `exception`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @ExceptionHandler(AuthorizationService.RuntimeUnavailableException.class)
    public ResponseEntity<Map<String, Object>> unavailable(
            AuthorizationService.RuntimeUnavailableException exception
    ) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "code", exception.reasonCode(),
                "message", "RBAC3 authorization runtime unavailable"));
    }
}
