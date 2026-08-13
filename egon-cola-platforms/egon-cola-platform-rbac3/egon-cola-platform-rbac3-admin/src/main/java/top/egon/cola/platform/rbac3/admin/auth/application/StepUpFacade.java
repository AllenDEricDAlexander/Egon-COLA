package top.egon.cola.platform.rbac3.admin.auth.application;

import top.egon.cola.platform.rbac3.contract.auth.LoginRequest;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/**
 * 类型 `StepUpFacade` 位于当前包内，是类型，用于承载 `Step Up Facade` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `StepUpFacade` is a type in its package and carries the responsibility, state, or contract for `Step Up Facade`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * Re-authenticates the current identity and strengthens only its current session.
 */
public final class StepUpFacade {

    /**
     * 字段 `authenticator` 表示 `StepUpFacade` 中与 `authenticator` 相关的状态、依赖、配置或结果（声明类型 `IdentityAuthenticatorStrategy`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `authenticator` stores the `authenticator`-related state, dependency, configuration, or result of `StepUpFacade` (declared type `IdentityAuthenticatorStrategy`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `authenticator` 时应保持 `StepUpFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `authenticator`, preserve `StepUpFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final IdentityAuthenticatorStrategy authenticator;
    /**
     * 字段 `identitySource` 表示 `StepUpFacade` 中与 `identity Source` 相关的状态、依赖、配置或结果（声明类型 `IdentitySource`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `identitySource` stores the `identity Source`-related state, dependency, configuration, or result of `StepUpFacade` (declared type `IdentitySource`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `identitySource` 时应保持 `StepUpFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `identitySource`, preserve `StepUpFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final IdentitySource identitySource;
    /**
     * 字段 `sessionStore` 表示 `StepUpFacade` 中与 `session Store` 相关的状态、依赖、配置或结果（声明类型 `SessionStrengthStore`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sessionStore` stores the `session Store`-related state, dependency, configuration, or result of `StepUpFacade` (declared type `SessionStrengthStore`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sessionStore` 时应保持 `StepUpFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sessionStore`, preserve `StepUpFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final SessionStrengthStore sessionStore;

    /**
     * 构造器 `StepUpFacade` 用于创建并初始化 `StepUpFacade` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `StepUpFacade` creates and initializes `StepUpFacade`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `StepUpFacade` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `StepUpFacade`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param authenticator 输入参数 `authenticator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param identitySource 输入参数 `identitySource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionStore 输入参数 `sessionStore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public StepUpFacade(
            IdentityAuthenticatorStrategy authenticator,
            IdentitySource identitySource,
            SessionStrengthStore sessionStore) {
        this.authenticator = Objects.requireNonNull(authenticator, "authenticator");
        this.identitySource = Objects.requireNonNull(identitySource, "identitySource");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
    }

    /**
     * 方法 `stepUp` 按照 `StepUpFacade` 的职责处理输入，完成 `step Up` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `stepUp` processes its inputs according to `StepUpFacade`'s responsibility, performs the `step Up` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `stepUp` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `stepUp`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param method 输入参数 `method`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param credential 输入参数 `credential`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public StepUpResult stepUp(
            String tenantId,
            String userId,
            String sessionId,
            String method,
            String credential,
            Instant now) {
        if (!"PASSWORD".equals(normalizeMethod(method))) {
            throw new Rbac3RuleViolation("STEP_UP_METHOD_UNSUPPORTED");
        }
        Identity identity = identitySource.load(tenantId, userId);
        IdentityAuthenticatorStrategy.AuthenticatedIdentity authenticated =
                authenticator.authenticate(new LoginRequest(
                        identity.tenantCode(), identity.username(), credential,
                        new LoginRequest.Device("step-up:" + sessionId, "Current session")),
                        now);
        if (!tenantId.equals(authenticated.tenantId())
                || !userId.equals(authenticated.userId())) {
            throw new Rbac3RuleViolation("AUTHENTICATION_FAILED");
        }
        return sessionStore.strengthen(tenantId, userId, sessionId, now);
    }

    /**
     * 方法 `normalizeMethod` 按照 `StepUpFacade` 的职责处理输入，完成 `normalize Method` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `normalizeMethod` processes its inputs according to `StepUpFacade`'s responsibility, performs the `normalize Method` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `normalizeMethod` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `normalizeMethod`, then continue the business flow using its result, exception, or side effect.
     *
     * @param method 输入参数 `method`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String normalizeMethod(String method) {
        return method == null ? "" : method.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 类型 `IdentitySource` 位于 `StepUpFacade` 内，是接口，用于承载 `Identity Source` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `IdentitySource` is an interface inside `StepUpFacade` and carries the responsibility, state, or contract for `Identity Source`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `IdentitySource` 作为 `StepUpFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `IdentitySource` as the responsibility boundary of `StepUpFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface IdentitySource {

        /**
         * 方法 `load` 按照 `IdentitySource` 的职责处理输入，完成 `load` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `load` processes its inputs according to `IdentitySource`'s responsibility, performs the `load` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `load` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `load`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        Identity load(String tenantId, String userId);
    }

    /**
     * 类型 `SessionStrengthStore` 位于 `StepUpFacade` 内，是接口，用于承载 `Session Strength Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SessionStrengthStore` is an interface inside `StepUpFacade` and carries the responsibility, state, or contract for `Session Strength Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SessionStrengthStore` 作为 `StepUpFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SessionStrengthStore` as the responsibility boundary of `StepUpFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface SessionStrengthStore {

        /**
         * 方法 `strengthen` 按照 `SessionStrengthStore` 的职责处理输入，完成 `strengthen` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `strengthen` processes its inputs according to `SessionStrengthStore`'s responsibility, performs the `strengthen` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `strengthen` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `strengthen`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        StepUpResult strengthen(
                String tenantId, String userId, String sessionId, Instant now);
    }

    /**
     * 类型 `Identity` 位于 `StepUpFacade` 内，是记录类型，用于承载 `Identity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Identity` is a record inside `StepUpFacade` and carries the responsibility, state, or contract for `Identity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Identity` 作为 `StepUpFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Identity` as the responsibility boundary of `StepUpFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantCode 记录组件 `tenantCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantCode` carries constructor data whose meaning is defined by the record contract.
     * @param username 记录组件 `username` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `username` carries constructor data whose meaning is defined by the record contract.
     */
    public record Identity(/**
 * 字段 `tenantCode` 表示 `Identity` 中与 `tenant Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `tenantCode` stores the `tenant Code`-related state, dependency, configuration, or result of `Identity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `tenantCode` 时应保持 `Identity` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `tenantCode`, preserve `Identity`'s lifecycle, immutability, and thread-safety constraints.
 */ String tenantCode, /**
 * 字段 `username` 表示 `Identity` 中与 `username` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `username` stores the `username`-related state, dependency, configuration, or result of `Identity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `username` 时应保持 `Identity` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `username`, preserve `Identity`'s lifecycle, immutability, and thread-safety constraints.
 */ String username) {

        /**
         * 构造器 `Identity` 用于创建并初始化 `Identity` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `Identity` creates and initializes `Identity`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `Identity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `Identity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tenantCode 输入参数 `tenantCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param username 输入参数 `username`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public Identity {
            tenantCode = required(tenantCode, "tenantCode");
            username = required(username, "username");
        }
    }

    /**
     * 类型 `StepUpResult` 位于 `StepUpFacade` 内，是记录类型，用于承载 `Step Up Result` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `StepUpResult` is a record inside `StepUpFacade` and carries the responsibility, state, or contract for `Step Up Result`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `StepUpResult` 作为 `StepUpFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `StepUpResult` as the responsibility boundary of `StepUpFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param sessionId 记录组件 `sessionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionId` carries constructor data whose meaning is defined by the record contract.
     * @param authStrength 记录组件 `authStrength` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authStrength` carries constructor data whose meaning is defined by the record contract.
     * @param strongAuthenticatedAt 记录组件 `strongAuthenticatedAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `strongAuthenticatedAt` carries constructor data whose meaning is defined by the record contract.
     */
    public record StepUpResult(
            /**
             * 字段 `sessionId` 表示 `StepUpResult` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `StepUpResult` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `StepUpResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `StepUpResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sessionId,
            /**
             * 字段 `authStrength` 表示 `StepUpResult` 中与 `auth Strength` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authStrength` stores the `auth Strength`-related state, dependency, configuration, or result of `StepUpResult` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authStrength` 时应保持 `StepUpResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authStrength`, preserve `StepUpResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            String authStrength,
            /**
             * 字段 `strongAuthenticatedAt` 表示 `StepUpResult` 中与 `strong Authenticated At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `strongAuthenticatedAt` stores the `strong Authenticated At`-related state, dependency, configuration, or result of `StepUpResult` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `strongAuthenticatedAt` 时应保持 `StepUpResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `strongAuthenticatedAt`, preserve `StepUpResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant strongAuthenticatedAt
    ) {
    }

    /**
     * 方法 `required` 按照 `StepUpFacade` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `StepUpFacade`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param field 输入参数 `field`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
