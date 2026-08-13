package top.egon.cola.platform.rbac3.starter.client;

import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;

/**
 * 类型 `Rbac3AuthorizationClient` 位于当前包内，是接口，用于承载 `Rbac3 Authorization Client` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3AuthorizationClient` is an interface in its package and carries the responsibility, state, or contract for `Rbac3 Authorization Client`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * Retrieves one current-system authorization snapshot using service identity.
 */
@FunctionalInterface
public interface Rbac3AuthorizationClient {

    /**
     * 方法 `fetch` 按照 `Rbac3AuthorizationClient` 的职责处理输入，完成 `fetch` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `fetch` processes its inputs according to `Rbac3AuthorizationClient`'s responsibility, performs the `fetch` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `fetch` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `fetch`, then continue the business flow using its result, exception, or side effect.
     *
     * @param systemCode 输入参数 `systemCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     * @throws InterruptedException 当输入违反契约或依赖不可用时抛出；thrown when the contract is violated or a dependency is unavailable.
     */
    SystemAuthorizationSnapshot fetch(
            String systemCode,
            IdentityPrincipal principal) throws InterruptedException;

    /**
     * 类型 `AuthorizationUnavailableException` 位于 `Rbac3AuthorizationClient` 内，是类型，用于承载 `Authorization Unavailable Exception` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AuthorizationUnavailableException` is a type inside `Rbac3AuthorizationClient` and carries the responsibility, state, or contract for `Authorization Unavailable Exception`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AuthorizationUnavailableException` 作为 `Rbac3AuthorizationClient` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AuthorizationUnavailableException` as the responsibility boundary of `Rbac3AuthorizationClient`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    final class AuthorizationUnavailableException extends RuntimeException {

        /**
         * 构造器 `AuthorizationUnavailableException` 用于创建并初始化 `AuthorizationUnavailableException` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `AuthorizationUnavailableException` creates and initializes `AuthorizationUnavailableException`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `AuthorizationUnavailableException` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `AuthorizationUnavailableException`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public AuthorizationUnavailableException(String reasonCode) {
            super(reasonCode);
        }

        /**
         * 构造器 `AuthorizationUnavailableException` 用于创建并初始化 `AuthorizationUnavailableException` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `AuthorizationUnavailableException` creates and initializes `AuthorizationUnavailableException`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `AuthorizationUnavailableException` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `AuthorizationUnavailableException`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param cause 输入参数 `cause`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public AuthorizationUnavailableException(String reasonCode, Throwable cause) {
            super(reasonCode, cause);
        }
    }

    /**
     * 类型 `AuthorizationDeniedException` 位于 `Rbac3AuthorizationClient` 内，是类型，用于承载 `Authorization Denied Exception` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AuthorizationDeniedException` is a type inside `Rbac3AuthorizationClient` and carries the responsibility, state, or contract for `Authorization Denied Exception`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AuthorizationDeniedException` 作为 `Rbac3AuthorizationClient` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AuthorizationDeniedException` as the responsibility boundary of `Rbac3AuthorizationClient`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    final class AuthorizationDeniedException extends RuntimeException {

        /**
         * 构造器 `AuthorizationDeniedException` 用于创建并初始化 `AuthorizationDeniedException` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `AuthorizationDeniedException` creates and initializes `AuthorizationDeniedException`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `AuthorizationDeniedException` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `AuthorizationDeniedException`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public AuthorizationDeniedException(String reasonCode) {
            super(reasonCode);
        }
    }
}
