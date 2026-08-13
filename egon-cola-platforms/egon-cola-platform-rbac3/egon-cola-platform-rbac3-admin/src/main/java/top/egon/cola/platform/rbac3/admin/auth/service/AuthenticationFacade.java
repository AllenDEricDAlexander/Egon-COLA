package top.egon.cola.platform.rbac3.admin.auth.service;

import top.egon.cola.platform.rbac3.admin.session.service.SessionFacade;
import top.egon.cola.platform.rbac3.contract.auth.LoginRequest;
import top.egon.cola.platform.rbac3.contract.auth.LoginResult;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.auth.repository.LoginStateRepository;
import top.egon.cola.platform.rbac3.admin.auth.repository.LoginRuntimePublisher;
import top.egon.cola.platform.rbac3.admin.auth.repository.LoginAuditRecorder;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.LoginAuditVO;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.LoginStateVO;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.AuthenticatedIdentityVO;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.AccessTokenSubjectVO;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.IssuedAccessTokenVO;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.SessionRecordVO;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.IssuedSessionVO;

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
     * 字段 `loginStateSource` 表示 `AuthenticationFacade` 中与 `login State Source` 相关的状态、依赖、配置或结果（声明类型 `LoginStateRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `loginStateSource` stores the `login State Source`-related state, dependency, configuration, or result of `AuthenticationFacade` (declared type `LoginStateRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `loginStateSource` 时应保持 `AuthenticationFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `loginStateSource`, preserve `AuthenticationFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final LoginStateRepository loginStateSource;
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
            LoginStateRepository loginStateSource,
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
            LoginStateRepository loginStateSource,
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
        AuthenticatedIdentityVO identity =
                authenticator.authenticate(request, now);
        LoginStateVO state = loginStateSource.load(
                identity.tenantId(), identity.userId(), now);
        IssuedSessionVO issued = sessionFacade.create(
                state.tenantId(),
                identity.userId(),
                state.authVersion(),
                state.policyVersion(),
                request.device().deviceId(),
                now);
        SessionRecordVO session = issued.session();
        IssuedAccessTokenVO accessToken = jwtTokenService.issue(
                new AccessTokenSubjectVO(
                        state.tenantId(),
                        identity.userId(),
                        session.sessionId(),
                        session.authVersion(),
                        session.sessionVersion(),
                        session.policyVersion()),
                now);
        try {
            runtimePublisher.publish(session, now);
            auditRecorder.record(new LoginAuditVO(
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





    }
