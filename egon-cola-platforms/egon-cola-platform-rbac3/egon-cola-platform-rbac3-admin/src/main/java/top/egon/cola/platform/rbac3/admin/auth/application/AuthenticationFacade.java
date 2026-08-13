package top.egon.cola.platform.rbac3.admin.auth.application;

import top.egon.cola.platform.rbac3.admin.session.application.SessionFacade;
import top.egon.cola.platform.rbac3.contract.auth.LoginRequest;
import top.egon.cola.platform.rbac3.contract.auth.LoginResult;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 类型 `AuthenticationFacade` 位于当前包内，是类型，用于承载 `Authentication Facade` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `AuthenticationFacade` is a type in its package and carries the responsibility, state, or contract for `Authentication Facade`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Orchestrates authentication and empty-role session creation.
 */
public final class AuthenticationFacade {

    /**
     * 字段 `CANDIDATES_URL` 表示 `AuthenticationFacade` 中与 `CANDIDATES URL` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `CANDIDATES_URL` stores the `CANDIDATES URL`-related state, dependency, configuration, or result of `AuthenticationFacade` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `CANDIDATES_URL` 时应保持 `AuthenticationFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `CANDIDATES_URL`, preserve `AuthenticationFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final String CANDIDATES_URL =
            "/api/rbac3/v1/auth/role-activation-candidates";

    /**
     * 字段 `authenticator` 表示 `AuthenticationFacade` 中与 `authenticator` 相关的状态、依赖、配置或结果（声明类型 `IdentityAuthenticatorStrategy`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `authenticator` stores the `authenticator`-related state, dependency, configuration, or result of `AuthenticationFacade` (declared type `IdentityAuthenticatorStrategy`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `authenticator` 时应保持 `AuthenticationFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `authenticator`, preserve `AuthenticationFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final IdentityAuthenticatorStrategy authenticator;
    /**
     * 字段 `loginStateSource` 表示 `AuthenticationFacade` 中与 `login State Source` 相关的状态、依赖、配置或结果（声明类型 `LoginStateSource`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `loginStateSource` stores the `login State Source`-related state, dependency, configuration, or result of `AuthenticationFacade` (declared type `LoginStateSource`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `loginStateSource` 时应保持 `AuthenticationFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `loginStateSource`, preserve `AuthenticationFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final LoginStateSource loginStateSource;
    /**
     * 字段 `sessionFacade` 表示 `AuthenticationFacade` 中与 `session Facade` 相关的状态、依赖、配置或结果（声明类型 `SessionFacade`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sessionFacade` stores the `session Facade`-related state, dependency, configuration, or result of `AuthenticationFacade` (declared type `SessionFacade`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sessionFacade` 时应保持 `AuthenticationFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sessionFacade`, preserve `AuthenticationFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final SessionFacade sessionFacade;
    /**
     * 字段 `jwtTokenService` 表示 `AuthenticationFacade` 中与 `jwt Token Service` 相关的状态、依赖、配置或结果（声明类型 `JwtTokenService`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `jwtTokenService` stores the `jwt Token Service`-related state, dependency, configuration, or result of `AuthenticationFacade` (declared type `JwtTokenService`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `jwtTokenService` 时应保持 `AuthenticationFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `jwtTokenService`, preserve `AuthenticationFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final JwtTokenService jwtTokenService;
    /**
     * 字段 `runtimePublisher` 表示 `AuthenticationFacade` 中与 `runtime Publisher` 相关的状态、依赖、配置或结果（声明类型 `LoginRuntimePublisher`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `runtimePublisher` stores the `runtime Publisher`-related state, dependency, configuration, or result of `AuthenticationFacade` (declared type `LoginRuntimePublisher`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `runtimePublisher` 时应保持 `AuthenticationFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `runtimePublisher`, preserve `AuthenticationFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final LoginRuntimePublisher runtimePublisher;
    /**
     * 字段 `auditRecorder` 表示 `AuthenticationFacade` 中与 `audit Recorder` 相关的状态、依赖、配置或结果（声明类型 `LoginAuditRecorder`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `auditRecorder` stores the `audit Recorder`-related state, dependency, configuration, or result of `AuthenticationFacade` (declared type `LoginAuditRecorder`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `auditRecorder` 时应保持 `AuthenticationFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `auditRecorder`, preserve `AuthenticationFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final LoginAuditRecorder auditRecorder;

    /**
     * 构造器 `AuthenticationFacade` 用于创建并初始化 `AuthenticationFacade` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `AuthenticationFacade` creates and initializes `AuthenticationFacade`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `AuthenticationFacade` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `AuthenticationFacade`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param authenticator 输入参数 `authenticator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param loginStateSource 输入参数 `loginStateSource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionFacade 输入参数 `sessionFacade`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param jwtTokenService 输入参数 `jwtTokenService`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param runtimePublisher 输入参数 `runtimePublisher`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public AuthenticationFacade(
            IdentityAuthenticatorStrategy authenticator,
            LoginStateSource loginStateSource,
            SessionFacade sessionFacade,
            JwtTokenService jwtTokenService,
            LoginRuntimePublisher runtimePublisher) {
        this(authenticator, loginStateSource, sessionFacade, jwtTokenService,
                runtimePublisher, audit -> {
                });
    }

    /**
     * 构造器 `AuthenticationFacade` 用于创建并初始化 `AuthenticationFacade` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `AuthenticationFacade` creates and initializes `AuthenticationFacade`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `AuthenticationFacade` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `AuthenticationFacade`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param authenticator 输入参数 `authenticator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param loginStateSource 输入参数 `loginStateSource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionFacade 输入参数 `sessionFacade`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param jwtTokenService 输入参数 `jwtTokenService`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param runtimePublisher 输入参数 `runtimePublisher`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param auditRecorder 输入参数 `auditRecorder`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public AuthenticationFacade(
            IdentityAuthenticatorStrategy authenticator,
            LoginStateSource loginStateSource,
            SessionFacade sessionFacade,
            JwtTokenService jwtTokenService,
            LoginRuntimePublisher runtimePublisher,
            LoginAuditRecorder auditRecorder) {
        this.authenticator = Objects.requireNonNull(authenticator, "authenticator");
        this.loginStateSource = Objects.requireNonNull(loginStateSource, "loginStateSource");
        this.sessionFacade = Objects.requireNonNull(sessionFacade, "sessionFacade");
        this.jwtTokenService = Objects.requireNonNull(jwtTokenService, "jwtTokenService");
        this.runtimePublisher = Objects.requireNonNull(runtimePublisher, "runtimePublisher");
        this.auditRecorder = Objects.requireNonNull(auditRecorder, "auditRecorder");
    }

    /**
     * 方法 `login` 按照 `AuthenticationFacade` 的职责处理输入，完成 `login` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `login` processes its inputs according to `AuthenticationFacade`'s responsibility, performs the `login` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `login` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `login`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public LoginResult login(LoginRequest request, Instant now) {
        IdentityAuthenticatorStrategy.AuthenticatedIdentity identity =
                authenticator.authenticate(request, now);
        LoginState state = loginStateSource.load(
                identity.tenantId(), identity.userId(), now);
        SessionFacade.IssuedSession issued = sessionFacade.create(
                state.tenantId(),
                identity.userId(),
                state.authVersion(),
                state.policyVersion(),
                request.device().deviceId(),
                now);
        SessionFacade.SessionRecord session = issued.session();
        JwtTokenService.IssuedAccessToken accessToken = jwtTokenService.issue(
                new JwtTokenService.AccessTokenSubject(
                        state.tenantId(),
                        identity.userId(),
                        session.sessionId(),
                        session.authVersion(),
                        session.sessionVersion(),
                        session.policyVersion()),
                now);
        try {
            runtimePublisher.publish(session, now);
            auditRecorder.record(new LoginAudit(
                    state.tenantId(), identity.userId(), session.sessionId(),
                    identity.authenticationMethod(), identity.authenticationStrength(),
                    session.sessionVersion(), now));
        } catch (RuntimeException publicationFailure) {
            try {
                sessionFacade.logout(
                        state.tenantId(), identity.userId(), session.sessionId(), now);
            } catch (RuntimeException compensationFailure) {
                publicationFailure.addSuppressed(compensationFailure);
            }
            throw publicationFailure;
        }
        return new LoginResult(
                "Bearer",
                accessToken.token(),
                Duration.between(now, accessToken.expiresAt()).toSeconds(),
                issued.refreshToken(),
                Duration.between(now, issued.refreshExpiresAt()).toSeconds(),
                session.sessionId(),
                true,
                state.activationCandidateCount(),
                CANDIDATES_URL,
                false);
    }

    /**
     * 类型 `LoginStateSource` 位于 `AuthenticationFacade` 内，是接口，用于承载 `Login State Source` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `LoginStateSource` is an interface inside `AuthenticationFacade` and carries the responsibility, state, or contract for `Login State Source`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `LoginStateSource` 作为 `AuthenticationFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `LoginStateSource` as the responsibility boundary of `AuthenticationFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface LoginStateSource {

        /**
         * 方法 `load` 按照 `LoginStateSource` 的职责处理输入，完成 `load` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `load` processes its inputs according to `LoginStateSource`'s responsibility, performs the `load` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `load` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `load`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantCode 输入参数 `tenantCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        LoginState load(String tenantCode, String userId, Instant now);
    }

    /**
     * 类型 `LoginRuntimePublisher` 位于 `AuthenticationFacade` 内，是接口，用于承载 `Login Runtime Publisher` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `LoginRuntimePublisher` is an interface inside `AuthenticationFacade` and carries the responsibility, state, or contract for `Login Runtime Publisher`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `LoginRuntimePublisher` 作为 `AuthenticationFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `LoginRuntimePublisher` as the responsibility boundary of `AuthenticationFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface LoginRuntimePublisher {

        /**
         * 方法 `publish` 按照 `LoginRuntimePublisher` 的职责处理输入，完成 `publish` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `publish` processes its inputs according to `LoginRuntimePublisher`'s responsibility, performs the `publish` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `publish` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `publish`, then continue the business flow using its result, exception, or side effect.
         *
         * @param session 输入参数 `session`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param generatedAt 输入参数 `generatedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void publish(SessionFacade.SessionRecord session, Instant generatedAt);
    }

    /**
     * 类型 `LoginAuditRecorder` 位于 `AuthenticationFacade` 内，是接口，用于承载 `Login Audit Recorder` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `LoginAuditRecorder` is an interface inside `AuthenticationFacade` and carries the responsibility, state, or contract for `Login Audit Recorder`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `LoginAuditRecorder` 作为 `AuthenticationFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `LoginAuditRecorder` as the responsibility boundary of `AuthenticationFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface LoginAuditRecorder {

        /**
         * 方法 `record` 按照 `LoginAuditRecorder` 的职责处理输入，完成 `record` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `record` processes its inputs according to `LoginAuditRecorder`'s responsibility, performs the `record` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `record` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `record`, then continue the business flow using its result, exception, or side effect.
         *
         * @param audit 输入参数 `audit`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void record(LoginAudit audit);
    }

    /**
     * 类型 `LoginAudit` 位于 `AuthenticationFacade` 内，是记录类型，用于承载 `Login Audit` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `LoginAudit` is a record inside `AuthenticationFacade` and carries the responsibility, state, or contract for `Login Audit`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `LoginAudit` 作为 `AuthenticationFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `LoginAudit` as the responsibility boundary of `AuthenticationFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param userId 记录组件 `userId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `userId` carries constructor data whose meaning is defined by the record contract.
     * @param sessionId 记录组件 `sessionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionId` carries constructor data whose meaning is defined by the record contract.
     * @param authenticationMethod 记录组件 `authenticationMethod` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authenticationMethod` carries constructor data whose meaning is defined by the record contract.
     * @param authenticationStrength 记录组件 `authenticationStrength` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authenticationStrength` carries constructor data whose meaning is defined by the record contract.
     * @param sessionVersion 记录组件 `sessionVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionVersion` carries constructor data whose meaning is defined by the record contract.
     * @param occurredAt 记录组件 `occurredAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `occurredAt` carries constructor data whose meaning is defined by the record contract.
     */
    public record LoginAudit(
            /**
             * 字段 `tenantId` 表示 `LoginAudit` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `LoginAudit` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `LoginAudit` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `LoginAudit`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `userId` 表示 `LoginAudit` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `LoginAudit` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userId` 时应保持 `LoginAudit` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userId`, preserve `LoginAudit`'s lifecycle, immutability, and thread-safety constraints.
             */
            String userId,
            /**
             * 字段 `sessionId` 表示 `LoginAudit` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `LoginAudit` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `LoginAudit` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `LoginAudit`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sessionId,
            /**
             * 字段 `authenticationMethod` 表示 `LoginAudit` 中与 `authentication Method` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authenticationMethod` stores the `authentication Method`-related state, dependency, configuration, or result of `LoginAudit` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authenticationMethod` 时应保持 `LoginAudit` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authenticationMethod`, preserve `LoginAudit`'s lifecycle, immutability, and thread-safety constraints.
             */
            String authenticationMethod,
            /**
             * 字段 `authenticationStrength` 表示 `LoginAudit` 中与 `authentication Strength` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authenticationStrength` stores the `authentication Strength`-related state, dependency, configuration, or result of `LoginAudit` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authenticationStrength` 时应保持 `LoginAudit` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authenticationStrength`, preserve `LoginAudit`'s lifecycle, immutability, and thread-safety constraints.
             */
            int authenticationStrength,
            /**
             * 字段 `sessionVersion` 表示 `LoginAudit` 中与 `session Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionVersion` stores the `session Version`-related state, dependency, configuration, or result of `LoginAudit` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionVersion` 时应保持 `LoginAudit` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionVersion`, preserve `LoginAudit`'s lifecycle, immutability, and thread-safety constraints.
             */
            long sessionVersion,
            /**
             * 字段 `occurredAt` 表示 `LoginAudit` 中与 `occurred At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `occurredAt` stores the `occurred At`-related state, dependency, configuration, or result of `LoginAudit` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `occurredAt` 时应保持 `LoginAudit` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `occurredAt`, preserve `LoginAudit`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant occurredAt
    ) {
    }

    /**
     * 类型 `LoginState` 位于 `AuthenticationFacade` 内，是记录类型，用于承载 `Login State` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `LoginState` is a record inside `AuthenticationFacade` and carries the responsibility, state, or contract for `Login State`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `LoginState` 作为 `AuthenticationFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `LoginState` as the responsibility boundary of `AuthenticationFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param authVersion 记录组件 `authVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authVersion` carries constructor data whose meaning is defined by the record contract.
     * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
     * @param activationCandidateCount 记录组件 `activationCandidateCount` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `activationCandidateCount` carries constructor data whose meaning is defined by the record contract.
     */
    public record LoginState(
            /**
             * 字段 `tenantId` 表示 `LoginState` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `LoginState` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `LoginState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `LoginState`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `authVersion` 表示 `LoginState` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `LoginState` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `LoginState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `LoginState`'s lifecycle, immutability, and thread-safety constraints.
             */
            long authVersion,
            /**
             * 字段 `policyVersion` 表示 `LoginState` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `LoginState` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `LoginState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `LoginState`'s lifecycle, immutability, and thread-safety constraints.
             */
            long policyVersion,
            /**
             * 字段 `activationCandidateCount` 表示 `LoginState` 中与 `activation Candidate Count` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `activationCandidateCount` stores the `activation Candidate Count`-related state, dependency, configuration, or result of `LoginState` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `activationCandidateCount` 时应保持 `LoginState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `activationCandidateCount`, preserve `LoginState`'s lifecycle, immutability, and thread-safety constraints.
             */
            int activationCandidateCount
    ) {

        /**
         * 构造器 `LoginState` 用于创建并初始化 `LoginState` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `LoginState` creates and initializes `LoginState`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `LoginState` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `LoginState`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param authVersion 输入参数 `authVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param policyVersion 输入参数 `policyVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param activationCandidateCount 输入参数 `activationCandidateCount`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public LoginState {
            if (authVersion < 0 || policyVersion < 0 || activationCandidateCount < 0) {
                throw new IllegalArgumentException("login state values must not be negative");
            }
        }
    }
}
