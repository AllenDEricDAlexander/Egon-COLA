package top.egon.cola.platform.rbac3.admin.auth.service;

import top.egon.cola.platform.rbac3.admin.session.service.RefreshTokenService;
import top.egon.cola.platform.rbac3.contract.auth.RefreshResult;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;
import top.egon.cola.platform.rbac3.admin.auth.repository.RefreshStateRepository;
import top.egon.cola.platform.rbac3.admin.auth.service.internal.TransactionBoundary;
import top.egon.cola.platform.rbac3.admin.auth.repository.RefreshRuntimePublisher;
import top.egon.cola.platform.rbac3.admin.auth.repository.RefreshAuditRecorder;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.RefreshAuditVO;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.RefreshAttemptVO;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.RefreshStateVO;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.AccessTokenSubjectVO;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.IssuedAccessTokenVO;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.RotationResultVO;
import top.egon.cola.platform.rbac3.admin.session.domain.enums.RefreshTokenOutcomeEnum;

/**
 * 类型 `RefreshFacade` 位于当前包内，是类型，用于承载 `Refresh Facade` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `RefreshFacade` is a type in its package and carries the responsibility, state, or contract for `Refresh Facade`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Completes refresh rotation by loading the atomically incremented session version.
 */
public final class RefreshFacade {

    /**
     * 字段 `refreshTokenService` 表示 `RefreshFacade` 中与 `refresh Token Service` 相关的状态、依赖、配置或结果（声明类型 `RefreshTokenService`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `refreshTokenService` stores the `refresh Token Service`-related state, dependency, configuration, or result of `RefreshFacade` (declared type `RefreshTokenService`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `refreshTokenService` 时应保持 `RefreshFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `refreshTokenService`, preserve `RefreshFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RefreshTokenService refreshTokenService;
    /**
     * 字段 `refreshStateSource` 表示 `RefreshFacade` 中与 `refresh State Source` 相关的状态、依赖、配置或结果（声明类型 `RefreshStateRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `refreshStateSource` stores the `refresh State Source`-related state, dependency, configuration, or result of `RefreshFacade` (declared type `RefreshStateRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `refreshStateSource` 时应保持 `RefreshFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `refreshStateSource`, preserve `RefreshFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RefreshStateRepository refreshStateSource;
    /**
     * 字段 `jwtTokenService` 表示 `RefreshFacade` 中与 `jwt Token Service` 相关的状态、依赖、配置或结果（声明类型 `JwtTokenService`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `jwtTokenService` stores the `jwt Token Service`-related state, dependency, configuration, or result of `RefreshFacade` (declared type `JwtTokenService`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `jwtTokenService` 时应保持 `RefreshFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `jwtTokenService`, preserve `RefreshFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final JwtTokenService jwtTokenService;
    /**
     * 字段 `transactionBoundary` 表示 `RefreshFacade` 中与 `transaction Boundary` 相关的状态、依赖、配置或结果（声明类型 `TransactionBoundary`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `transactionBoundary` stores the `transaction Boundary`-related state, dependency, configuration, or result of `RefreshFacade` (declared type `TransactionBoundary`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `transactionBoundary` 时应保持 `RefreshFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `transactionBoundary`, preserve `RefreshFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final TransactionBoundary transactionBoundary;
    /**
     * 字段 `runtimePublisher` 表示 `RefreshFacade` 中与 `runtime Publisher` 相关的状态、依赖、配置或结果（声明类型 `RefreshRuntimePublisher`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `runtimePublisher` stores the `runtime Publisher`-related state, dependency, configuration, or result of `RefreshFacade` (declared type `RefreshRuntimePublisher`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `runtimePublisher` 时应保持 `RefreshFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `runtimePublisher`, preserve `RefreshFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RefreshRuntimePublisher runtimePublisher;
    /**
     * 字段 `auditRecorder` 表示 `RefreshFacade` 中与 `audit Recorder` 相关的状态、依赖、配置或结果（声明类型 `RefreshAuditRecorder`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `auditRecorder` stores the `audit Recorder`-related state, dependency, configuration, or result of `RefreshFacade` (declared type `RefreshAuditRecorder`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `auditRecorder` 时应保持 `RefreshFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `auditRecorder`, preserve `RefreshFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RefreshAuditRecorder auditRecorder;

    /**
     * 构造器 `RefreshFacade` 用于创建并初始化 `RefreshFacade` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RefreshFacade` creates and initializes `RefreshFacade`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RefreshFacade` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RefreshFacade`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param refreshTokenService 输入参数 `refreshTokenService`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param refreshStateSource 输入参数 `refreshStateSource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param jwtTokenService 输入参数 `jwtTokenService`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public RefreshFacade(
            RefreshTokenService refreshTokenService,
            RefreshStateRepository refreshStateSource,
            JwtTokenService jwtTokenService) {
        this(refreshTokenService, refreshStateSource, jwtTokenService,
                work -> work.get(), (state, generatedAt) -> {
                }, audit -> {
                });
    }

    /**
     * 构造器 `RefreshFacade` 用于创建并初始化 `RefreshFacade` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RefreshFacade` creates and initializes `RefreshFacade`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RefreshFacade` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RefreshFacade`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param refreshTokenService 输入参数 `refreshTokenService`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param refreshStateSource 输入参数 `refreshStateSource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param jwtTokenService 输入参数 `jwtTokenService`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param transactionBoundary 输入参数 `transactionBoundary`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param runtimePublisher 输入参数 `runtimePublisher`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public RefreshFacade(
            RefreshTokenService refreshTokenService,
            RefreshStateRepository refreshStateSource,
            JwtTokenService jwtTokenService,
            TransactionBoundary transactionBoundary,
            RefreshRuntimePublisher runtimePublisher) {
        this(refreshTokenService, refreshStateSource, jwtTokenService,
                transactionBoundary, runtimePublisher, audit -> {
                });
    }

    /**
     * 构造器 `RefreshFacade` 用于创建并初始化 `RefreshFacade` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RefreshFacade` creates and initializes `RefreshFacade`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RefreshFacade` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RefreshFacade`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param refreshTokenService 输入参数 `refreshTokenService`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param refreshStateSource 输入参数 `refreshStateSource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param jwtTokenService 输入参数 `jwtTokenService`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param transactionBoundary 输入参数 `transactionBoundary`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param runtimePublisher 输入参数 `runtimePublisher`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param auditRecorder 输入参数 `auditRecorder`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public RefreshFacade(
            RefreshTokenService refreshTokenService,
            RefreshStateRepository refreshStateSource,
            JwtTokenService jwtTokenService,
            TransactionBoundary transactionBoundary,
            RefreshRuntimePublisher runtimePublisher,
            RefreshAuditRecorder auditRecorder) {
        this.refreshTokenService = Objects.requireNonNull(
                refreshTokenService, "refreshTokenService");
        this.refreshStateSource = Objects.requireNonNull(
                refreshStateSource, "refreshStateSource");
        this.jwtTokenService = Objects.requireNonNull(jwtTokenService, "jwtTokenService");
        this.transactionBoundary = Objects.requireNonNull(
                transactionBoundary, "transactionBoundary");
        this.runtimePublisher = Objects.requireNonNull(runtimePublisher, "runtimePublisher");
        this.auditRecorder = Objects.requireNonNull(auditRecorder, "auditRecorder");
    }

    /**
     * 方法 `refresh` 按照 `RefreshFacade` 的职责处理输入，完成 `refresh` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `refresh` processes its inputs according to `RefreshFacade`'s responsibility, performs the `refresh` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `refresh` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `refresh`, then continue the business flow using its result, exception, or side effect.
     *
     * @param rawRefreshToken 输入参数 `rawRefreshToken`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public RefreshResult refresh(String rawRefreshToken, Instant now) {
        RefreshAttemptVO attempt = transactionBoundary.execute(
                () -> refreshAtomically(rawRefreshToken, now));
        if (attempt.reasonCode() != null) {
            throw new Rbac3RuleViolation(attempt.reasonCode());
        }
        return attempt.result();
    }

    /**
     * 方法 `refreshAtomically` 按照 `RefreshFacade` 的职责处理输入，完成 `refresh Atomically` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `refreshAtomically` processes its inputs according to `RefreshFacade`'s responsibility, performs the `refresh Atomically` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `refreshAtomically` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `refreshAtomically`, then continue the business flow using its result, exception, or side effect.
     *
     * @param rawRefreshToken 输入参数 `rawRefreshToken`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private RefreshAttemptVO refreshAtomically(String rawRefreshToken, Instant now) {
        RotationResultVO rotation = refreshTokenService.rotate(
                rawRefreshToken, now);
        if (rotation.outcome() == RefreshTokenOutcomeEnum.REPLAY_DETECTED) {
            return RefreshAttemptVO.rejected("REFRESH_TOKEN_REUSED");
        }
        if (rotation.outcome() != RefreshTokenOutcomeEnum.ROTATED) {
            return RefreshAttemptVO.rejected("AUTHENTICATION_FAILED");
        }
        RefreshStateVO state = refreshStateSource.load(rotation.familyId());
        IssuedAccessTokenVO accessToken = jwtTokenService.issue(
                new AccessTokenSubjectVO(
                        state.tenantId(),
                        state.userId(),
                        state.sessionId(),
                        state.authVersion(),
                        state.sessionVersion(),
                        state.policyVersion()),
                now);
        runtimePublisher.publish(state, now);
        auditRecorder.record(new RefreshAuditVO(
                state.tenantId(), state.userId(), state.sessionId(),
                state.sessionVersion(), state.policyVersion(), now));
        return RefreshAttemptVO.succeeded(new RefreshResult(
                "Bearer",
                accessToken.token(),
                Duration.between(now, accessToken.expiresAt()).toSeconds(),
                rotation.refreshToken(),
                Duration.between(now, state.refreshExpiresAt()).toSeconds(),
                state.sessionId(),
                state.authVersion(),
                state.sessionVersion(),
                state.policyVersion(),
                state.roleActivationRequired(),
                state.activationReasonCode(),
                !state.roleActivationRequired()));
    }







    }
