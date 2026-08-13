package top.egon.cola.platform.rbac3.admin.session.service;

import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.application.port.Rbac3RuntimePolicy;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.session.repository.SessionRepository;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.SessionRecordVO;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.IssuedSessionVO;
import top.egon.cola.platform.rbac3.admin.session.domain.enums.SessionLifecycleStatusEnum;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.TokenRecordVO;

/**
 * 类型 `SessionFacade` 位于当前包内，是类型，用于承载 `Session Facade` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `SessionFacade` is a type in its package and carries the responsibility, state, or contract for `Session Facade`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Owns the user-session lifecycle while authorization activation remains a separate command.
 */
public final class SessionFacade {

    /**
     * 字段 `REFRESH_TOKEN_BYTES` 表示 `SessionFacade` 中与 `REFRESH TOKEN BYTES` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `REFRESH_TOKEN_BYTES` stores the `REFRESH TOKEN BYTES`-related state, dependency, configuration, or result of `SessionFacade` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `REFRESH_TOKEN_BYTES` 时应保持 `SessionFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `REFRESH_TOKEN_BYTES`, preserve `SessionFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final int REFRESH_TOKEN_BYTES = 32;

    /**
     * 字段 `idGenerator` 表示 `SessionFacade` 中与 `id Generator` 相关的状态、依赖、配置或结果（声明类型 `LongIdGenerator`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `idGenerator` stores the `id Generator`-related state, dependency, configuration, or result of `SessionFacade` (declared type `LongIdGenerator`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `idGenerator` 时应保持 `SessionFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `idGenerator`, preserve `SessionFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final LongIdGenerator idGenerator;
    /**
     * 字段 `sessionStore` 表示 `SessionFacade` 中与 `session Store` 相关的状态、依赖、配置或结果（声明类型 `SessionRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sessionStore` stores the `session Store`-related state, dependency, configuration, or result of `SessionFacade` (declared type `SessionRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sessionStore` 时应保持 `SessionFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sessionStore`, preserve `SessionFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final SessionRepository sessionStore;
    /**
     * 字段 `secureRandom` 表示 `SessionFacade` 中与 `secure Random` 相关的状态、依赖、配置或结果（声明类型 `SecureRandom`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `secureRandom` stores the `secure Random`-related state, dependency, configuration, or result of `SessionFacade` (declared type `SecureRandom`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `secureRandom` 时应保持 `SessionFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `secureRandom`, preserve `SessionFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final SecureRandom secureRandom;
    /**
     * 字段 `runtimePolicy` 表示 `SessionFacade` 中与 `runtime Policy` 相关的状态、依赖、配置或结果（声明类型 `Rbac3RuntimePolicy`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `runtimePolicy` stores the `runtime Policy`-related state, dependency, configuration, or result of `SessionFacade` (declared type `Rbac3RuntimePolicy`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `runtimePolicy` 时应保持 `SessionFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `runtimePolicy`, preserve `SessionFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Rbac3RuntimePolicy runtimePolicy;

    /**
     * 构造器 `SessionFacade` 用于创建并初始化 `SessionFacade` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `SessionFacade` creates and initializes `SessionFacade`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `SessionFacade` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `SessionFacade`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param idGenerator 输入参数 `idGenerator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionStore 输入参数 `sessionStore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param runtimePolicy 输入参数 `runtimePolicy`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public SessionFacade(
            LongIdGenerator idGenerator,
            SessionRepository sessionStore,
            Rbac3RuntimePolicy runtimePolicy) {
        this(idGenerator, sessionStore, new SecureRandom(), runtimePolicy);
    }

    /**
     * 构造器 `SessionFacade` 用于创建并初始化 `SessionFacade` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `SessionFacade` creates and initializes `SessionFacade`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `SessionFacade` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `SessionFacade`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param idGenerator 输入参数 `idGenerator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionStore 输入参数 `sessionStore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param secureRandom 输入参数 `secureRandom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param runtimePolicy 输入参数 `runtimePolicy`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    SessionFacade(
            LongIdGenerator idGenerator,
            SessionRepository sessionStore,
            SecureRandom secureRandom,
            Rbac3RuntimePolicy runtimePolicy) {
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom");
        this.runtimePolicy = Objects.requireNonNull(runtimePolicy, "runtimePolicy");
    }

    /**
     * 方法 `create` 按照 `SessionFacade` 的职责处理输入，完成 `create` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `create` processes its inputs according to `SessionFacade`'s responsibility, performs the `create` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `create` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `create`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param authVersion 输入参数 `authVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policyVersion 输入参数 `policyVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param deviceIdHash 输入参数 `deviceIdHash`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public IssuedSessionVO create(
            String tenantId,
            String userId,
            long authVersion,
            long policyVersion,
            String deviceIdHash,
            Instant now) {
        Rbac3RuntimePolicy.Snapshot policySnapshot = runtimePolicy.current();
        long entityId = idGenerator.nextLongId();
        String sessionId = idGenerator.nextId();
        String familyId = idGenerator.nextId();
        String refreshTokenId = idGenerator.nextId();
        String rawRefreshToken = randomToken();
        Instant absoluteExpiry = now.plus(policySnapshot.sessionAbsoluteTimeout());
        SessionRecordVO session = new SessionRecordVO(
                Long.toString(entityId),
                tenantId,
                userId,
                sessionId,
                SessionLifecycleStatusEnum.ACTIVE,
                0,
                authVersion,
                policyVersion,
                true,
                familyId,
                deviceIdHash == null ? null : RefreshTokenService.hash(deviceIdHash),
                now,
                now.plus(policySnapshot.sessionIdleTimeout()),
                absoluteExpiry);
        TokenRecordVO refreshToken = TokenRecordVO.active(
                refreshTokenId,
                tenantId,
                sessionId,
                familyId,
                0,
                RefreshTokenService.hash(rawRefreshToken),
                now.plus(policySnapshot.refreshTokenTtl()));
        sessionStore.create(session, refreshToken, now);
        return new IssuedSessionVO(session, rawRefreshToken, refreshToken.expiresAt());
    }

    /**
     * 方法 `logout` 按照 `SessionFacade` 的职责处理输入，完成 `logout` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `logout` processes its inputs according to `SessionFacade`'s responsibility, performs the `logout` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `logout` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `logout`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public boolean logout(String tenantId, String userId, String sessionId, Instant now) {
        return sessionStore.logout(tenantId, userId, sessionId, now);
    }

    /**
     * 方法 `randomToken` 按照 `SessionFacade` 的职责处理输入，完成 `random Token` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `randomToken` processes its inputs according to `SessionFacade`'s responsibility, performs the `random Token` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `randomToken` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `randomToken`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String randomToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }




    }
