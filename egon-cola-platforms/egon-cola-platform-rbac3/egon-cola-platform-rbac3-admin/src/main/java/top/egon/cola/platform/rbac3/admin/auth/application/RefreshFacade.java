package top.egon.cola.platform.rbac3.admin.auth.application;

import top.egon.cola.platform.rbac3.admin.session.application.RefreshTokenService;
import top.egon.cola.platform.rbac3.contract.auth.RefreshResult;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

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
     * 字段 `refreshStateSource` 表示 `RefreshFacade` 中与 `refresh State Source` 相关的状态、依赖、配置或结果（声明类型 `RefreshStateSource`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `refreshStateSource` stores the `refresh State Source`-related state, dependency, configuration, or result of `RefreshFacade` (declared type `RefreshStateSource`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `refreshStateSource` 时应保持 `RefreshFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `refreshStateSource`, preserve `RefreshFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RefreshStateSource refreshStateSource;
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
            RefreshStateSource refreshStateSource,
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
            RefreshStateSource refreshStateSource,
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
            RefreshStateSource refreshStateSource,
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
        RefreshAttempt attempt = transactionBoundary.execute(
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
    private RefreshAttempt refreshAtomically(String rawRefreshToken, Instant now) {
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(
                rawRefreshToken, now);
        if (rotation.outcome() == RefreshTokenService.Outcome.REPLAY_DETECTED) {
            return RefreshAttempt.rejected("REFRESH_TOKEN_REUSED");
        }
        if (rotation.outcome() != RefreshTokenService.Outcome.ROTATED) {
            return RefreshAttempt.rejected("AUTHENTICATION_FAILED");
        }
        RefreshState state = refreshStateSource.load(rotation.familyId());
        JwtTokenService.IssuedAccessToken accessToken = jwtTokenService.issue(
                new JwtTokenService.AccessTokenSubject(
                        state.tenantId(),
                        state.userId(),
                        state.sessionId(),
                        state.authVersion(),
                        state.sessionVersion(),
                        state.policyVersion()),
                now);
        runtimePublisher.publish(state, now);
        auditRecorder.record(new RefreshAudit(
                state.tenantId(), state.userId(), state.sessionId(),
                state.sessionVersion(), state.policyVersion(), now));
        return RefreshAttempt.succeeded(new RefreshResult(
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

    /**
     * 类型 `RefreshStateSource` 位于 `RefreshFacade` 内，是接口，用于承载 `Refresh State Source` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RefreshStateSource` is an interface inside `RefreshFacade` and carries the responsibility, state, or contract for `Refresh State Source`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RefreshStateSource` 作为 `RefreshFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RefreshStateSource` as the responsibility boundary of `RefreshFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface RefreshStateSource {

        /**
         * 方法 `load` 按照 `RefreshStateSource` 的职责处理输入，完成 `load` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `load` processes its inputs according to `RefreshStateSource`'s responsibility, performs the `load` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `load` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `load`, then continue the business flow using its result, exception, or side effect.
         *
         * @param familyId 输入参数 `familyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        RefreshState load(String familyId);
    }

    /**
     * 类型 `TransactionBoundary` 位于 `RefreshFacade` 内，是接口，用于承载 `Transaction Boundary` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `TransactionBoundary` is an interface inside `RefreshFacade` and carries the responsibility, state, or contract for `Transaction Boundary`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `TransactionBoundary` 作为 `RefreshFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `TransactionBoundary` as the responsibility boundary of `RefreshFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface TransactionBoundary {

        /**
         * 方法 `execute` 按照 `TransactionBoundary` 的职责处理输入，完成 `execute` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `execute` processes its inputs according to `TransactionBoundary`'s responsibility, performs the `execute` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `execute` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `execute`, then continue the business flow using its result, exception, or side effect.
         *
         * @param work 输入参数 `work`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        RefreshAttempt execute(Supplier<RefreshAttempt> work);
    }

    /**
     * 类型 `RefreshRuntimePublisher` 位于 `RefreshFacade` 内，是接口，用于承载 `Refresh Runtime Publisher` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RefreshRuntimePublisher` is an interface inside `RefreshFacade` and carries the responsibility, state, or contract for `Refresh Runtime Publisher`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RefreshRuntimePublisher` 作为 `RefreshFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RefreshRuntimePublisher` as the responsibility boundary of `RefreshFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface RefreshRuntimePublisher {

        /**
         * 方法 `publish` 按照 `RefreshRuntimePublisher` 的职责处理输入，完成 `publish` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `publish` processes its inputs according to `RefreshRuntimePublisher`'s responsibility, performs the `publish` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `publish` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `publish`, then continue the business flow using its result, exception, or side effect.
         *
         * @param state 输入参数 `state`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param generatedAt 输入参数 `generatedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void publish(RefreshState state, Instant generatedAt);
    }

    /**
     * 类型 `RefreshAuditRecorder` 位于 `RefreshFacade` 内，是接口，用于承载 `Refresh Audit Recorder` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RefreshAuditRecorder` is an interface inside `RefreshFacade` and carries the responsibility, state, or contract for `Refresh Audit Recorder`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RefreshAuditRecorder` 作为 `RefreshFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RefreshAuditRecorder` as the responsibility boundary of `RefreshFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface RefreshAuditRecorder {

        /**
         * 方法 `record` 按照 `RefreshAuditRecorder` 的职责处理输入，完成 `record` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `record` processes its inputs according to `RefreshAuditRecorder`'s responsibility, performs the `record` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `record` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `record`, then continue the business flow using its result, exception, or side effect.
         *
         * @param audit 输入参数 `audit`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void record(RefreshAudit audit);
    }

    /**
     * 类型 `RefreshAudit` 位于 `RefreshFacade` 内，是记录类型，用于承载 `Refresh Audit` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RefreshAudit` is a record inside `RefreshFacade` and carries the responsibility, state, or contract for `Refresh Audit`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RefreshAudit` 作为 `RefreshFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RefreshAudit` as the responsibility boundary of `RefreshFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param userId 记录组件 `userId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `userId` carries constructor data whose meaning is defined by the record contract.
     * @param sessionId 记录组件 `sessionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionId` carries constructor data whose meaning is defined by the record contract.
     * @param sessionVersion 记录组件 `sessionVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionVersion` carries constructor data whose meaning is defined by the record contract.
     * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
     * @param occurredAt 记录组件 `occurredAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `occurredAt` carries constructor data whose meaning is defined by the record contract.
     */
    public record RefreshAudit(
            /**
             * 字段 `tenantId` 表示 `RefreshAudit` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `RefreshAudit` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `RefreshAudit` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `RefreshAudit`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `userId` 表示 `RefreshAudit` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `RefreshAudit` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userId` 时应保持 `RefreshAudit` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userId`, preserve `RefreshAudit`'s lifecycle, immutability, and thread-safety constraints.
             */
            String userId,
            /**
             * 字段 `sessionId` 表示 `RefreshAudit` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `RefreshAudit` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `RefreshAudit` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `RefreshAudit`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sessionId,
            /**
             * 字段 `sessionVersion` 表示 `RefreshAudit` 中与 `session Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionVersion` stores the `session Version`-related state, dependency, configuration, or result of `RefreshAudit` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionVersion` 时应保持 `RefreshAudit` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionVersion`, preserve `RefreshAudit`'s lifecycle, immutability, and thread-safety constraints.
             */
            long sessionVersion,
            /**
             * 字段 `policyVersion` 表示 `RefreshAudit` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `RefreshAudit` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `RefreshAudit` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `RefreshAudit`'s lifecycle, immutability, and thread-safety constraints.
             */
            long policyVersion,
            /**
             * 字段 `occurredAt` 表示 `RefreshAudit` 中与 `occurred At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `occurredAt` stores the `occurred At`-related state, dependency, configuration, or result of `RefreshAudit` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `occurredAt` 时应保持 `RefreshAudit` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `occurredAt`, preserve `RefreshAudit`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant occurredAt
    ) {
    }

    /**
     * 类型 `RefreshAttempt` 位于 `RefreshFacade` 内，是记录类型，用于承载 `Refresh Attempt` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RefreshAttempt` is a record inside `RefreshFacade` and carries the responsibility, state, or contract for `Refresh Attempt`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RefreshAttempt` 作为 `RefreshFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RefreshAttempt` as the responsibility boundary of `RefreshFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param result 记录组件 `result` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `result` carries constructor data whose meaning is defined by the record contract.
     * @param reasonCode 记录组件 `reasonCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `reasonCode` carries constructor data whose meaning is defined by the record contract.
     */
    public record RefreshAttempt(/**
 * 字段 `result` 表示 `RefreshAttempt` 中与 `result` 相关的状态、依赖、配置或结果（声明类型 `RefreshResult`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `result` stores the `result`-related state, dependency, configuration, or result of `RefreshAttempt` (declared type `RefreshResult`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `result` 时应保持 `RefreshAttempt` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `result`, preserve `RefreshAttempt`'s lifecycle, immutability, and thread-safety constraints.
 */ RefreshResult result, /**
 * 字段 `reasonCode` 表示 `RefreshAttempt` 中与 `reason Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `reasonCode` stores the `reason Code`-related state, dependency, configuration, or result of `RefreshAttempt` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `reasonCode` 时应保持 `RefreshAttempt` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `reasonCode`, preserve `RefreshAttempt`'s lifecycle, immutability, and thread-safety constraints.
 */ String reasonCode) {

        /**
         * 方法 `succeeded` 按照 `RefreshAttempt` 的职责处理输入，完成 `succeeded` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `succeeded` processes its inputs according to `RefreshAttempt`'s responsibility, performs the `succeeded` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `succeeded` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `succeeded`, then continue the business flow using its result, exception, or side effect.
         *
         * @param result 输入参数 `result`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        public static RefreshAttempt succeeded(RefreshResult result) {
            return new RefreshAttempt(Objects.requireNonNull(result, "result"), null);
        }

        /**
         * 方法 `rejected` 按照 `RefreshAttempt` 的职责处理输入，完成 `rejected` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `rejected` processes its inputs according to `RefreshAttempt`'s responsibility, performs the `rejected` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `rejected` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `rejected`, then continue the business flow using its result, exception, or side effect.
         *
         * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        public static RefreshAttempt rejected(String reasonCode) {
            return new RefreshAttempt(null, Objects.requireNonNull(reasonCode, "reasonCode"));
        }
    }

    /**
     * 类型 `RefreshState` 位于 `RefreshFacade` 内，是记录类型，用于承载 `Refresh State` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RefreshState` is a record inside `RefreshFacade` and carries the responsibility, state, or contract for `Refresh State`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RefreshState` 作为 `RefreshFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RefreshState` as the responsibility boundary of `RefreshFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param userId 记录组件 `userId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `userId` carries constructor data whose meaning is defined by the record contract.
     * @param sessionId 记录组件 `sessionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionId` carries constructor data whose meaning is defined by the record contract.
     * @param authVersion 记录组件 `authVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authVersion` carries constructor data whose meaning is defined by the record contract.
     * @param sessionVersion 记录组件 `sessionVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionVersion` carries constructor data whose meaning is defined by the record contract.
     * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
     * @param refreshExpiresAt 记录组件 `refreshExpiresAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `refreshExpiresAt` carries constructor data whose meaning is defined by the record contract.
     * @param roleActivationRequired 记录组件 `roleActivationRequired` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleActivationRequired` carries constructor data whose meaning is defined by the record contract.
     * @param activationReasonCode 记录组件 `activationReasonCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `activationReasonCode` carries constructor data whose meaning is defined by the record contract.
     */
    public record RefreshState(
            /**
             * 字段 `tenantId` 表示 `RefreshState` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `RefreshState` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `RefreshState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `RefreshState`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `userId` 表示 `RefreshState` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `RefreshState` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userId` 时应保持 `RefreshState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userId`, preserve `RefreshState`'s lifecycle, immutability, and thread-safety constraints.
             */
            String userId,
            /**
             * 字段 `sessionId` 表示 `RefreshState` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `RefreshState` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `RefreshState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `RefreshState`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sessionId,
            /**
             * 字段 `authVersion` 表示 `RefreshState` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `RefreshState` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `RefreshState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `RefreshState`'s lifecycle, immutability, and thread-safety constraints.
             */
            long authVersion,
            /**
             * 字段 `sessionVersion` 表示 `RefreshState` 中与 `session Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionVersion` stores the `session Version`-related state, dependency, configuration, or result of `RefreshState` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionVersion` 时应保持 `RefreshState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionVersion`, preserve `RefreshState`'s lifecycle, immutability, and thread-safety constraints.
             */
            long sessionVersion,
            /**
             * 字段 `policyVersion` 表示 `RefreshState` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `RefreshState` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `RefreshState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `RefreshState`'s lifecycle, immutability, and thread-safety constraints.
             */
            long policyVersion,
            /**
             * 字段 `refreshExpiresAt` 表示 `RefreshState` 中与 `refresh Expires At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `refreshExpiresAt` stores the `refresh Expires At`-related state, dependency, configuration, or result of `RefreshState` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `refreshExpiresAt` 时应保持 `RefreshState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `refreshExpiresAt`, preserve `RefreshState`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant refreshExpiresAt,
            /**
             * 字段 `roleActivationRequired` 表示 `RefreshState` 中与 `role Activation Required` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleActivationRequired` stores the `role Activation Required`-related state, dependency, configuration, or result of `RefreshState` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleActivationRequired` 时应保持 `RefreshState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleActivationRequired`, preserve `RefreshState`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean roleActivationRequired,
            /**
             * 字段 `activationReasonCode` 表示 `RefreshState` 中与 `activation Reason Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `activationReasonCode` stores the `activation Reason Code`-related state, dependency, configuration, or result of `RefreshState` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `activationReasonCode` 时应保持 `RefreshState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `activationReasonCode`, preserve `RefreshState`'s lifecycle, immutability, and thread-safety constraints.
             */
            String activationReasonCode
    ) {
    }
}
