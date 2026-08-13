package top.egon.cola.platform.rbac3.admin.auth.application;

import top.egon.cola.platform.rbac3.contract.auth.LoginRequest;

import java.time.Instant;

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
    AuthenticatedIdentity authenticate(LoginRequest request, Instant now);

    /**
     * 类型 `AuthenticatedIdentity` 位于 `IdentityAuthenticatorStrategy` 内，是记录类型，用于承载 `Authenticated Identity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AuthenticatedIdentity` is a record inside `IdentityAuthenticatorStrategy` and carries the responsibility, state, or contract for `Authenticated Identity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AuthenticatedIdentity` 作为 `IdentityAuthenticatorStrategy` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AuthenticatedIdentity` as the responsibility boundary of `IdentityAuthenticatorStrategy`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param userId 记录组件 `userId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `userId` carries constructor data whose meaning is defined by the record contract.
     * @param authenticationMethod 记录组件 `authenticationMethod` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authenticationMethod` carries constructor data whose meaning is defined by the record contract.
     * @param authenticationStrength 记录组件 `authenticationStrength` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authenticationStrength` carries constructor data whose meaning is defined by the record contract.
     */
    record AuthenticatedIdentity(
            /**
             * 字段 `tenantId` 表示 `AuthenticatedIdentity` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `AuthenticatedIdentity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `AuthenticatedIdentity` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `AuthenticatedIdentity`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `userId` 表示 `AuthenticatedIdentity` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `AuthenticatedIdentity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userId` 时应保持 `AuthenticatedIdentity` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userId`, preserve `AuthenticatedIdentity`'s lifecycle, immutability, and thread-safety constraints.
             */
            String userId,
            /**
             * 字段 `authenticationMethod` 表示 `AuthenticatedIdentity` 中与 `authentication Method` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authenticationMethod` stores the `authentication Method`-related state, dependency, configuration, or result of `AuthenticatedIdentity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authenticationMethod` 时应保持 `AuthenticatedIdentity` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authenticationMethod`, preserve `AuthenticatedIdentity`'s lifecycle, immutability, and thread-safety constraints.
             */
            String authenticationMethod,
            /**
             * 字段 `authenticationStrength` 表示 `AuthenticatedIdentity` 中与 `authentication Strength` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authenticationStrength` stores the `authentication Strength`-related state, dependency, configuration, or result of `AuthenticatedIdentity` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authenticationStrength` 时应保持 `AuthenticatedIdentity` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authenticationStrength`, preserve `AuthenticatedIdentity`'s lifecycle, immutability, and thread-safety constraints.
             */
            int authenticationStrength
    ) {
    }
}
