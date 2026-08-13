package top.egon.cola.platform.rbac3.admin.auth.service;

import top.egon.cola.platform.rbac3.contract.auth.LoginRequest;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.auth.repository.IdentityRepository;
import top.egon.cola.platform.rbac3.admin.auth.repository.SessionStrengthRepository;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.StepUpIdentityVO;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.StepUpResultVO;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.AuthenticatedIdentityVO;

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
     * 字段 `identitySource` 表示 `StepUpFacade` 中与 `identity Source` 相关的状态、依赖、配置或结果（声明类型 `IdentityRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `identitySource` stores the `identity Source`-related state, dependency, configuration, or result of `StepUpFacade` (declared type `IdentityRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `identitySource` 时应保持 `StepUpFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `identitySource`, preserve `StepUpFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final IdentityRepository identitySource;
    /**
     * 字段 `sessionStore` 表示 `StepUpFacade` 中与 `session Store` 相关的状态、依赖、配置或结果（声明类型 `SessionStrengthRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sessionStore` stores the `session Store`-related state, dependency, configuration, or result of `StepUpFacade` (declared type `SessionStrengthRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sessionStore` 时应保持 `StepUpFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sessionStore`, preserve `StepUpFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final SessionStrengthRepository sessionStore;

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
            IdentityRepository identitySource,
            SessionStrengthRepository sessionStore) {
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
    public StepUpResultVO stepUp(
            String tenantId,
            String userId,
            String sessionId,
            String method,
            String credential,
            Instant now) {
        if (!"PASSWORD".equals(normalizeMethod(method))) {
            throw new Rbac3RuleViolation("STEP_UP_METHOD_UNSUPPORTED");
        }
        StepUpIdentityVO identity = identitySource.load(tenantId, userId);
        AuthenticatedIdentityVO authenticated =
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
