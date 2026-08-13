package top.egon.cola.platform.rbac3.admin.auth.service;

import top.egon.cola.platform.rbac3.contract.auth.LoginRequest;

import java.time.Instant;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.AuthenticatedIdentityVO;

/**
 * 类型 `IdentityAuthenticatorStrategy` 位于当前包内，是接口，用于承载 `Identity Authenticator Strategy` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `IdentityAuthenticatorStrategy` is an interface in its package and carries the responsibility, state, or contract for `Identity Authenticator Strategy`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Verifies an identity credential without deriving any authorization data.
 */
public interface IdentityAuthenticatorStrategy {

    /**
     * 方法 `authenticate` 按照 `IdentityAuthenticatorStrategy` 的职责处理输入，完成 `authenticate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `authenticate` processes its inputs according to `IdentityAuthenticatorStrategy`'s responsibility, performs the `authenticate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `authenticate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `authenticate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    AuthenticatedIdentityVO authenticate(LoginRequest request, Instant now);

    }
