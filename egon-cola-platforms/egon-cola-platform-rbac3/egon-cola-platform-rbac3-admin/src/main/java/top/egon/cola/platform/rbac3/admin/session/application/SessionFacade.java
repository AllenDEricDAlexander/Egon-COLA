package top.egon.cola.platform.rbac3.admin.session.application;

import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.application.port.Rbac3RuntimePolicy;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

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
     * 字段 `sessionStore` 表示 `SessionFacade` 中与 `session Store` 相关的状态、依赖、配置或结果（声明类型 `SessionStore`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sessionStore` stores the `session Store`-related state, dependency, configuration, or result of `SessionFacade` (declared type `SessionStore`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sessionStore` 时应保持 `SessionFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sessionStore`, preserve `SessionFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final SessionStore sessionStore;
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
            SessionStore sessionStore,
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
            SessionStore sessionStore,
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
    public IssuedSession create(
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
        SessionRecord session = new SessionRecord(
                Long.toString(entityId),
                tenantId,
                userId,
                sessionId,
                SessionStatus.ACTIVE,
                0,
                authVersion,
                policyVersion,
                true,
                familyId,
                deviceIdHash == null ? null : RefreshTokenService.hash(deviceIdHash),
                now,
                now.plus(policySnapshot.sessionIdleTimeout()),
                absoluteExpiry);
        RefreshTokenService.TokenRecord refreshToken = RefreshTokenService.TokenRecord.active(
                refreshTokenId,
                tenantId,
                sessionId,
                familyId,
                0,
                RefreshTokenService.hash(rawRefreshToken),
                now.plus(policySnapshot.refreshTokenTtl()));
        sessionStore.create(session, refreshToken, now);
        return new IssuedSession(session, rawRefreshToken, refreshToken.expiresAt());
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

    /**
     * 类型 `SessionStore` 位于 `SessionFacade` 内，是接口，用于承载 `Session Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SessionStore` is an interface inside `SessionFacade` and carries the responsibility, state, or contract for `Session Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SessionStore` 作为 `SessionFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SessionStore` as the responsibility boundary of `SessionFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface SessionStore {

        /**
         * 方法 `create` 按照 `SessionStore` 的职责处理输入，完成 `create` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `create` processes its inputs according to `SessionStore`'s responsibility, performs the `create` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `create` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `create`, then continue the business flow using its result, exception, or side effect.
         *
         * @param session 输入参数 `session`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param refreshToken 输入参数 `refreshToken`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void create(
                SessionRecord session,
                RefreshTokenService.TokenRecord refreshToken,
                Instant now);

        /**
         * 方法 `logout` 按照 `SessionStore` 的职责处理输入，完成 `logout` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `logout` processes its inputs according to `SessionStore`'s responsibility, performs the `logout` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
        boolean logout(String tenantId, String userId, String sessionId, Instant now);
    }

    /**
     * 类型 `SessionRecord` 位于 `SessionFacade` 内，是记录类型，用于承载 `Session Record` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SessionRecord` is a record inside `SessionFacade` and carries the responsibility, state, or contract for `Session Record`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SessionRecord` 作为 `SessionFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SessionRecord` as the responsibility boundary of `SessionFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param entityId 记录组件 `entityId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `entityId` carries constructor data whose meaning is defined by the record contract.
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param userId 记录组件 `userId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `userId` carries constructor data whose meaning is defined by the record contract.
     * @param sessionId 记录组件 `sessionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionId` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param sessionVersion 记录组件 `sessionVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionVersion` carries constructor data whose meaning is defined by the record contract.
     * @param authVersion 记录组件 `authVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authVersion` carries constructor data whose meaning is defined by the record contract.
     * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
     * @param activationRequired 记录组件 `activationRequired` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `activationRequired` carries constructor data whose meaning is defined by the record contract.
     * @param tokenFamilyId 记录组件 `tokenFamilyId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tokenFamilyId` carries constructor data whose meaning is defined by the record contract.
     * @param deviceIdHash 记录组件 `deviceIdHash` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `deviceIdHash` carries constructor data whose meaning is defined by the record contract.
     * @param authenticatedAt 记录组件 `authenticatedAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authenticatedAt` carries constructor data whose meaning is defined by the record contract.
     * @param idleExpiresAt 记录组件 `idleExpiresAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `idleExpiresAt` carries constructor data whose meaning is defined by the record contract.
     * @param absoluteExpiresAt 记录组件 `absoluteExpiresAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `absoluteExpiresAt` carries constructor data whose meaning is defined by the record contract.
     */
    public record SessionRecord(
            /**
             * 字段 `entityId` 表示 `SessionRecord` 中与 `entity Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `entityId` stores the `entity Id`-related state, dependency, configuration, or result of `SessionRecord` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `entityId` 时应保持 `SessionRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `entityId`, preserve `SessionRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            String entityId,
            /**
             * 字段 `tenantId` 表示 `SessionRecord` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `SessionRecord` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `SessionRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `SessionRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `userId` 表示 `SessionRecord` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `SessionRecord` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userId` 时应保持 `SessionRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userId`, preserve `SessionRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            String userId,
            /**
             * 字段 `sessionId` 表示 `SessionRecord` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `SessionRecord` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `SessionRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `SessionRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sessionId,
            /**
             * 字段 `status` 表示 `SessionRecord` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `SessionStatus`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `SessionRecord` (declared type `SessionStatus`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `SessionRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `SessionRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            SessionStatus status,
            /**
             * 字段 `sessionVersion` 表示 `SessionRecord` 中与 `session Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionVersion` stores the `session Version`-related state, dependency, configuration, or result of `SessionRecord` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionVersion` 时应保持 `SessionRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionVersion`, preserve `SessionRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            long sessionVersion,
            /**
             * 字段 `authVersion` 表示 `SessionRecord` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `SessionRecord` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `SessionRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `SessionRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            long authVersion,
            /**
             * 字段 `policyVersion` 表示 `SessionRecord` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `SessionRecord` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `SessionRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `SessionRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            long policyVersion,
            /**
             * 字段 `activationRequired` 表示 `SessionRecord` 中与 `activation Required` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `activationRequired` stores the `activation Required`-related state, dependency, configuration, or result of `SessionRecord` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `activationRequired` 时应保持 `SessionRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `activationRequired`, preserve `SessionRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean activationRequired,
            /**
             * 字段 `tokenFamilyId` 表示 `SessionRecord` 中与 `token Family Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tokenFamilyId` stores the `token Family Id`-related state, dependency, configuration, or result of `SessionRecord` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tokenFamilyId` 时应保持 `SessionRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tokenFamilyId`, preserve `SessionRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tokenFamilyId,
            /**
             * 字段 `deviceIdHash` 表示 `SessionRecord` 中与 `device Id Hash` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `deviceIdHash` stores the `device Id Hash`-related state, dependency, configuration, or result of `SessionRecord` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `deviceIdHash` 时应保持 `SessionRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `deviceIdHash`, preserve `SessionRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            String deviceIdHash,
            /**
             * 字段 `authenticatedAt` 表示 `SessionRecord` 中与 `authenticated At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authenticatedAt` stores the `authenticated At`-related state, dependency, configuration, or result of `SessionRecord` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authenticatedAt` 时应保持 `SessionRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authenticatedAt`, preserve `SessionRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant authenticatedAt,
            /**
             * 字段 `idleExpiresAt` 表示 `SessionRecord` 中与 `idle Expires At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `idleExpiresAt` stores the `idle Expires At`-related state, dependency, configuration, or result of `SessionRecord` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `idleExpiresAt` 时应保持 `SessionRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `idleExpiresAt`, preserve `SessionRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant idleExpiresAt,
            /**
             * 字段 `absoluteExpiresAt` 表示 `SessionRecord` 中与 `absolute Expires At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `absoluteExpiresAt` stores the `absolute Expires At`-related state, dependency, configuration, or result of `SessionRecord` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `absoluteExpiresAt` 时应保持 `SessionRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `absoluteExpiresAt`, preserve `SessionRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant absoluteExpiresAt
    ) {
    }

    /**
     * 类型 `IssuedSession` 位于 `SessionFacade` 内，是记录类型，用于承载 `Issued Session` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `IssuedSession` is a record inside `SessionFacade` and carries the responsibility, state, or contract for `Issued Session`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `IssuedSession` 作为 `SessionFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `IssuedSession` as the responsibility boundary of `SessionFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param session 记录组件 `session` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `session` carries constructor data whose meaning is defined by the record contract.
     * @param refreshToken 记录组件 `refreshToken` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `refreshToken` carries constructor data whose meaning is defined by the record contract.
     * @param refreshExpiresAt 记录组件 `refreshExpiresAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `refreshExpiresAt` carries constructor data whose meaning is defined by the record contract.
     */
    public record IssuedSession(
            /**
             * 字段 `session` 表示 `IssuedSession` 中与 `session` 相关的状态、依赖、配置或结果（声明类型 `SessionRecord`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `session` stores the `session`-related state, dependency, configuration, or result of `IssuedSession` (declared type `SessionRecord`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `session` 时应保持 `IssuedSession` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `session`, preserve `IssuedSession`'s lifecycle, immutability, and thread-safety constraints.
             */
            SessionRecord session,
            /**
             * 字段 `refreshToken` 表示 `IssuedSession` 中与 `refresh Token` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `refreshToken` stores the `refresh Token`-related state, dependency, configuration, or result of `IssuedSession` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `refreshToken` 时应保持 `IssuedSession` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `refreshToken`, preserve `IssuedSession`'s lifecycle, immutability, and thread-safety constraints.
             */
            String refreshToken,
            /**
             * 字段 `refreshExpiresAt` 表示 `IssuedSession` 中与 `refresh Expires At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `refreshExpiresAt` stores the `refresh Expires At`-related state, dependency, configuration, or result of `IssuedSession` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `refreshExpiresAt` 时应保持 `IssuedSession` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `refreshExpiresAt`, preserve `IssuedSession`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant refreshExpiresAt
    ) {

        /**
         * 方法 `toString` 按照 `IssuedSession` 的职责处理输入，完成 `to String` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `toString` processes its inputs according to `IssuedSession`'s responsibility, performs the `to String` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `toString` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `toString`, then continue the business flow using its result, exception, or side effect.
         *
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        @Override
        public String toString() {
            return "IssuedSession[session=" + session
                    + ", refreshToken=<redacted>, refreshExpiresAt=" + refreshExpiresAt + ']';
        }
    }

    /**
     * 类型 `SessionStatus` 位于 `SessionFacade` 内，是枚举，用于承载 `Session Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SessionStatus` is an enum inside `SessionFacade` and carries the responsibility, state, or contract for `Session Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SessionStatus` 作为 `SessionFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SessionStatus` as the responsibility boundary of `SessionFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum SessionStatus {
        /**
         * 字段 `ACTIVE` 表示 `SessionStatus` 中与 `ACTIVE` 相关的状态、依赖、配置或结果（声明类型 `SessionStatus`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ACTIVE` stores the `ACTIVE`-related state, dependency, configuration, or result of `SessionStatus` (declared type `SessionStatus`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ACTIVE` 时应保持 `SessionStatus` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ACTIVE`, preserve `SessionStatus`'s lifecycle, immutability, and thread-safety constraints.
         */
        ACTIVE,
        /**
         * 字段 `LOGGED_OUT` 表示 `SessionStatus` 中与 `LOGGED OUT` 相关的状态、依赖、配置或结果（声明类型 `SessionStatus`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `LOGGED_OUT` stores the `LOGGED OUT`-related state, dependency, configuration, or result of `SessionStatus` (declared type `SessionStatus`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `LOGGED_OUT` 时应保持 `SessionStatus` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `LOGGED_OUT`, preserve `SessionStatus`'s lifecycle, immutability, and thread-safety constraints.
         */
        LOGGED_OUT,
        /**
         * 字段 `REVOKED` 表示 `SessionStatus` 中与 `REVOKED` 相关的状态、依赖、配置或结果（声明类型 `SessionStatus`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `REVOKED` stores the `REVOKED`-related state, dependency, configuration, or result of `SessionStatus` (declared type `SessionStatus`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `REVOKED` 时应保持 `SessionStatus` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `REVOKED`, preserve `SessionStatus`'s lifecycle, immutability, and thread-safety constraints.
         */
        REVOKED,
        /**
         * 字段 `EXPIRED` 表示 `SessionStatus` 中与 `EXPIRED` 相关的状态、依赖、配置或结果（声明类型 `SessionStatus`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `EXPIRED` stores the `EXPIRED`-related state, dependency, configuration, or result of `SessionStatus` (declared type `SessionStatus`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `EXPIRED` 时应保持 `SessionStatus` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `EXPIRED`, preserve `SessionStatus`'s lifecycle, immutability, and thread-safety constraints.
         */
        EXPIRED,
        /**
         * 字段 `COMPROMISED` 表示 `SessionStatus` 中与 `COMPROMISED` 相关的状态、依赖、配置或结果（声明类型 `SessionStatus`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `COMPROMISED` stores the `COMPROMISED`-related state, dependency, configuration, or result of `SessionStatus` (declared type `SessionStatus`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `COMPROMISED` 时应保持 `SessionStatus` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `COMPROMISED`, preserve `SessionStatus`'s lifecycle, immutability, and thread-safety constraints.
         */
        COMPROMISED
    }
}
