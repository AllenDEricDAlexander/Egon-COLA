package top.egon.cola.platform.rbac3.admin.shared.controller;

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
import top.egon.cola.platform.rbac3.admin.auth.domain.exception.AuthenticationFailedException;

/**
 * 类型 `Rbac3ApiExceptionHandler` 位于当前包内，是类型，用于承载 `Rbac3 Api Exception Handler` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3ApiExceptionHandler` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Api Exception Handler`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `Rbac3ApiExceptionHandler` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `Rbac3ApiExceptionHandler` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@RestControllerAdvice
public class Rbac3ApiExceptionHandler {

    /**
     * 方法 `handleAuthenticationFailure` 按照 `Rbac3ApiExceptionHandler` 的职责处理输入，完成 `handle Authentication Failure` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `handleAuthenticationFailure` processes its inputs according to `Rbac3ApiExceptionHandler`'s responsibility, performs the `handle Authentication Failure` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `handleAuthenticationFailure` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `handleAuthenticationFailure`, then continue the business flow using its result, exception, or side effect.
     *
     * @param error 输入参数 `error`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<Rbac3ErrorResponse> handleAuthenticationFailure(
            AuthenticationFailedException error,
            HttpServletRequest request) {
        return response(
                Rbac3ErrorCode.AUTHENTICATION_FAILED,
                "Authentication failed",
                request);
    }

    /**
     * 方法 `handleRuleViolation` 按照 `Rbac3ApiExceptionHandler` 的职责处理输入，完成 `handle Rule Violation` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `handleRuleViolation` processes its inputs according to `Rbac3ApiExceptionHandler`'s responsibility, performs the `handle Rule Violation` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `handleRuleViolation` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `handleRuleViolation`, then continue the business flow using its result, exception, or side effect.
     *
     * @param error 输入参数 `error`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @ExceptionHandler(Rbac3RuleViolation.class)
    public ResponseEntity<Rbac3ErrorResponse> handleRuleViolation(
            Rbac3RuleViolation error,
            HttpServletRequest request
    ) {
        Rbac3ErrorCode code = toCode(error.reasonCode());
        return response(code, "Request rejected by authorization policy", request);
    }

    /**
     * 方法 `handleInvalidRequest` 按照 `Rbac3ApiExceptionHandler` 的职责处理输入，完成 `handle Invalid Request` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `handleInvalidRequest` processes its inputs according to `Rbac3ApiExceptionHandler`'s responsibility, performs the `handle Invalid Request` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `handleInvalidRequest` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `handleInvalidRequest`, then continue the business flow using its result, exception, or side effect.
     *
     * @param error 输入参数 `error`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @ExceptionHandler({MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public ResponseEntity<Rbac3ErrorResponse> handleInvalidRequest(
            Exception error,
            HttpServletRequest request
    ) {
        return response(Rbac3ErrorCode.REQUEST_INVALID,
                "Request payload is invalid", request);
    }

    /**
     * 方法 `response` 按照 `Rbac3ApiExceptionHandler` 的职责处理输入，完成 `response` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `response` processes its inputs according to `Rbac3ApiExceptionHandler`'s responsibility, performs the `response` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `response` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `response`, then continue the business flow using its result, exception, or side effect.
     *
     * @param code 输入参数 `code`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param message 输入参数 `message`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `toCode` 按照 `Rbac3ApiExceptionHandler` 的职责处理输入，完成 `to Code` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `toCode` processes its inputs according to `Rbac3ApiExceptionHandler`'s responsibility, performs the `to Code` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `toCode` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `toCode`, then continue the business flow using its result, exception, or side effect.
     *
     * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private Rbac3ErrorCode toCode(String reasonCode) {
        try {
            return Rbac3ErrorCode.valueOf(reasonCode);
        } catch (IllegalArgumentException ignored) {
            return Rbac3ErrorCode.REQUEST_INVALID;
        }
    }

    /**
     * 方法 `headerOrGenerated` 按照 `Rbac3ApiExceptionHandler` 的职责处理输入，完成 `header Or Generated` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `headerOrGenerated` processes its inputs according to `Rbac3ApiExceptionHandler`'s responsibility, performs the `header Or Generated` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `headerOrGenerated` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `headerOrGenerated`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String headerOrGenerated(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value == null || value.isBlank() ? UUID.randomUUID().toString() : value.trim();
    }
}
