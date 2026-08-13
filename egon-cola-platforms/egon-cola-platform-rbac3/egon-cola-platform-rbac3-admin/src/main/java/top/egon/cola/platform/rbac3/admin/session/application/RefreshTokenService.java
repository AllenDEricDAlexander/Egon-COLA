package top.egon.cola.platform.rbac3.admin.session.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import java.util.function.Function;

/**
 * 类型 `RefreshTokenService` 位于当前包内，是类型，用于承载 `Refresh Token Service` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `RefreshTokenService` is a type in its package and carries the responsibility, state, or contract for `Refresh Token Service`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Rotates opaque refresh tokens through a store-provided database lock boundary.
 */
public final class RefreshTokenService {

    /**
     * 字段 `TOKEN_BYTES` 表示 `RefreshTokenService` 中与 `TOKEN BYTES` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `TOKEN_BYTES` stores the `TOKEN BYTES`-related state, dependency, configuration, or result of `RefreshTokenService` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `TOKEN_BYTES` 时应保持 `RefreshTokenService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `TOKEN_BYTES`, preserve `RefreshTokenService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final int TOKEN_BYTES = 32;

    /**
     * 字段 `store` 表示 `RefreshTokenService` 中与 `store` 相关的状态、依赖、配置或结果（声明类型 `RefreshTokenStore`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `store` stores the `store`-related state, dependency, configuration, or result of `RefreshTokenService` (declared type `RefreshTokenStore`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `store` 时应保持 `RefreshTokenService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `store`, preserve `RefreshTokenService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RefreshTokenStore store;
    /**
     * 字段 `secureRandom` 表示 `RefreshTokenService` 中与 `secure Random` 相关的状态、依赖、配置或结果（声明类型 `SecureRandom`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `secureRandom` stores the `secure Random`-related state, dependency, configuration, or result of `RefreshTokenService` (declared type `SecureRandom`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `secureRandom` 时应保持 `RefreshTokenService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `secureRandom`, preserve `RefreshTokenService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final SecureRandom secureRandom;

    /**
     * 构造器 `RefreshTokenService` 用于创建并初始化 `RefreshTokenService` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RefreshTokenService` creates and initializes `RefreshTokenService`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RefreshTokenService` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RefreshTokenService`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param store 输入参数 `store`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public RefreshTokenService(RefreshTokenStore store) {
        this(store, new SecureRandom());
    }

    /**
     * 构造器 `RefreshTokenService` 用于创建并初始化 `RefreshTokenService` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RefreshTokenService` creates and initializes `RefreshTokenService`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RefreshTokenService` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RefreshTokenService`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param store 输入参数 `store`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param secureRandom 输入参数 `secureRandom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    RefreshTokenService(RefreshTokenStore store, SecureRandom secureRandom) {
        this.store = Objects.requireNonNull(store, "store");
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom");
    }

    /**
     * 方法 `rotate` 按照 `RefreshTokenService` 的职责处理输入，完成 `rotate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rotate` processes its inputs according to `RefreshTokenService`'s responsibility, performs the `rotate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rotate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rotate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param rawToken 输入参数 `rawToken`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public RotationResult rotate(String rawToken, Instant now) {
        Objects.requireNonNull(rawToken, "rawToken");
        Objects.requireNonNull(now, "now");
        if (rawToken.isBlank() || rawToken.length() > 512) {
            return new RotationResult(Outcome.INVALID, null, null);
        }
        return store.withLockedToken(hash(rawToken), current -> rotateLocked(current, now));
    }

    /**
     * 方法 `rotateLocked` 按照 `RefreshTokenService` 的职责处理输入，完成 `rotate Locked` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rotateLocked` processes its inputs according to `RefreshTokenService`'s responsibility, performs the `rotate Locked` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rotateLocked` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rotateLocked`, then continue the business flow using its result, exception, or side effect.
     *
     * @param current 输入参数 `current`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private RotationResult rotateLocked(TokenRecord current, Instant now) {
        if (current == null) {
            return new RotationResult(Outcome.INVALID, null, null);
        }
        if (current.status() == TokenStatus.ROTATED
                || current.status() == TokenStatus.REUSED_DETECTED) {
            store.compromiseFamily(current.familyId(), now);
            return new RotationResult(Outcome.REPLAY_DETECTED, null, current.familyId());
        }
        if (current.status() != TokenStatus.ACTIVE || !current.expiresAt().isAfter(now)) {
            return new RotationResult(Outcome.INVALID, null, current.familyId());
        }
        byte[] random = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(random);
        String nextRawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        TokenRecord rotated = current.rotated(now);
        TokenRecord next = TokenRecord.active(
                current.tokenId() + ':' + (current.generation() + 1),
                current.tenantId(),
                current.sessionId(),
                current.familyId(),
                current.generation() + 1,
                hash(nextRawToken),
                current.expiresAt());
        store.rotate(rotated, next);
        return new RotationResult(Outcome.ROTATED, nextRawToken, current.familyId());
    }

    /**
     * 方法 `hash` 按照 `RefreshTokenService` 的职责处理输入，完成 `hash` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `hash` processes its inputs according to `RefreshTokenService`'s responsibility, performs the `hash` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `hash` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `hash`, then continue the business flow using its result, exception, or side effect.
     *
     * @param rawToken 输入参数 `rawToken`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public static String hash(String rawToken) {
        Objects.requireNonNull(rawToken, "rawToken");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    /**
     * 类型 `RefreshTokenStore` 位于 `RefreshTokenService` 内，是接口，用于承载 `Refresh Token Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RefreshTokenStore` is an interface inside `RefreshTokenService` and carries the responsibility, state, or contract for `Refresh Token Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     * Implementations must hold a cross-instance atomic lock for the whole callback.
     */
    public interface RefreshTokenStore {

        /**
         * 方法 `withLockedToken` 按照 `RefreshTokenStore` 的职责处理输入，完成 `with Locked Token` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `withLockedToken` processes its inputs according to `RefreshTokenStore`'s responsibility, performs the `with Locked Token` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `withLockedToken` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `withLockedToken`, then continue the business flow using its result, exception, or side effect.
         *
         * @param <T> 类型参数表示令牌回调结果的具体类型；type parameter representing the token callback result type.
         * @param tokenHash 输入参数 `tokenHash`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param action 输入参数 `action`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        <T> T withLockedToken(String tokenHash, Function<TokenRecord, T> action);

        /**
         * 方法 `rotate` 按照 `RefreshTokenStore` 的职责处理输入，完成 `rotate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `rotate` processes its inputs according to `RefreshTokenStore`'s responsibility, performs the `rotate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         * Implementations must lock the owning session before changing either token.
         * Role activation uses the same session lock, which serializes refresh and
         * activation version increments without a distributed lock.
         * @param oldToken 输入参数 `oldToken`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param newToken 输入参数 `newToken`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void rotate(TokenRecord oldToken, TokenRecord newToken);

        /**
         * 方法 `compromiseFamily` 按照 `RefreshTokenStore` 的职责处理输入，完成 `compromise Family` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `compromiseFamily` processes its inputs according to `RefreshTokenStore`'s responsibility, performs the `compromise Family` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `compromiseFamily` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `compromiseFamily`, then continue the business flow using its result, exception, or side effect.
         *
         * @param familyId 输入参数 `familyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param detectedAt 输入参数 `detectedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void compromiseFamily(String familyId, Instant detectedAt);
    }

    /**
     * 类型 `TokenRecord` 位于 `RefreshTokenService` 内，是记录类型，用于承载 `Token Record` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `TokenRecord` is a record inside `RefreshTokenService` and carries the responsibility, state, or contract for `Token Record`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `TokenRecord` 作为 `RefreshTokenService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `TokenRecord` as the responsibility boundary of `RefreshTokenService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tokenId 记录组件 `tokenId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tokenId` carries constructor data whose meaning is defined by the record contract.
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param sessionId 记录组件 `sessionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionId` carries constructor data whose meaning is defined by the record contract.
     * @param familyId 记录组件 `familyId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `familyId` carries constructor data whose meaning is defined by the record contract.
     * @param generation 记录组件 `generation` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `generation` carries constructor data whose meaning is defined by the record contract.
     * @param tokenHash 记录组件 `tokenHash` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tokenHash` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param expiresAt 记录组件 `expiresAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expiresAt` carries constructor data whose meaning is defined by the record contract.
     * @param rotatedAt 记录组件 `rotatedAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `rotatedAt` carries constructor data whose meaning is defined by the record contract.
     */
    public record TokenRecord(
            /**
             * 字段 `tokenId` 表示 `TokenRecord` 中与 `token Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tokenId` stores the `token Id`-related state, dependency, configuration, or result of `TokenRecord` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tokenId` 时应保持 `TokenRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tokenId`, preserve `TokenRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tokenId,
            /**
             * 字段 `tenantId` 表示 `TokenRecord` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `TokenRecord` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `TokenRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `TokenRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `sessionId` 表示 `TokenRecord` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `TokenRecord` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `TokenRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `TokenRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sessionId,
            /**
             * 字段 `familyId` 表示 `TokenRecord` 中与 `family Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `familyId` stores the `family Id`-related state, dependency, configuration, or result of `TokenRecord` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `familyId` 时应保持 `TokenRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `familyId`, preserve `TokenRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            String familyId,
            /**
             * 字段 `generation` 表示 `TokenRecord` 中与 `generation` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `generation` stores the `generation`-related state, dependency, configuration, or result of `TokenRecord` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `generation` 时应保持 `TokenRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `generation`, preserve `TokenRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            long generation,
            /**
             * 字段 `tokenHash` 表示 `TokenRecord` 中与 `token Hash` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tokenHash` stores the `token Hash`-related state, dependency, configuration, or result of `TokenRecord` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tokenHash` 时应保持 `TokenRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tokenHash`, preserve `TokenRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tokenHash,
            /**
             * 字段 `status` 表示 `TokenRecord` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `TokenStatus`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `TokenRecord` (declared type `TokenStatus`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `TokenRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `TokenRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            TokenStatus status,
            /**
             * 字段 `expiresAt` 表示 `TokenRecord` 中与 `expires At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expiresAt` stores the `expires At`-related state, dependency, configuration, or result of `TokenRecord` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expiresAt` 时应保持 `TokenRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expiresAt`, preserve `TokenRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant expiresAt,
            /**
             * 字段 `rotatedAt` 表示 `TokenRecord` 中与 `rotated At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `rotatedAt` stores the `rotated At`-related state, dependency, configuration, or result of `TokenRecord` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `rotatedAt` 时应保持 `TokenRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `rotatedAt`, preserve `TokenRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant rotatedAt
    ) {

        /**
         * 构造器 `TokenRecord` 用于创建并初始化 `TokenRecord` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `TokenRecord` creates and initializes `TokenRecord`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `TokenRecord` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `TokenRecord`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tokenId 输入参数 `tokenId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param familyId 输入参数 `familyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param generation 输入参数 `generation`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param tokenHash 输入参数 `tokenHash`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expiresAt 输入参数 `expiresAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param rotatedAt 输入参数 `rotatedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public TokenRecord {
            Objects.requireNonNull(tokenId, "tokenId");
            Objects.requireNonNull(tenantId, "tenantId");
            Objects.requireNonNull(sessionId, "sessionId");
            Objects.requireNonNull(familyId, "familyId");
            Objects.requireNonNull(tokenHash, "tokenHash");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(expiresAt, "expiresAt");
            if (generation < 0) {
                throw new IllegalArgumentException("generation must not be negative");
            }
        }

        /**
         * 方法 `active` 按照 `TokenRecord` 的职责处理输入，完成 `active` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `active` processes its inputs according to `TokenRecord`'s responsibility, performs the `active` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `active` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `active`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tokenId 输入参数 `tokenId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param familyId 输入参数 `familyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param generation 输入参数 `generation`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param tokenHash 输入参数 `tokenHash`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expiresAt 输入参数 `expiresAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        public static TokenRecord active(
                String tokenId,
                String tenantId,
                String sessionId,
                String familyId,
                long generation,
                String tokenHash,
                Instant expiresAt) {
            return new TokenRecord(
                    tokenId,
                    tenantId,
                    sessionId,
                    familyId,
                    generation,
                    tokenHash,
                    TokenStatus.ACTIVE,
                    expiresAt,
                    null);
        }

        /**
         * 方法 `rotated` 按照 `TokenRecord` 的职责处理输入，完成 `rotated` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `rotated` processes its inputs according to `TokenRecord`'s responsibility, performs the `rotated` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `rotated` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `rotated`, then continue the business flow using its result, exception, or side effect.
         *
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        TokenRecord rotated(Instant now) {
            return new TokenRecord(
                    tokenId,
                    tenantId,
                    sessionId,
                    familyId,
                    generation,
                    tokenHash,
                    TokenStatus.ROTATED,
                    expiresAt,
                    now);
        }

        /**
         * 方法 `toString` 按照 `TokenRecord` 的职责处理输入，完成 `to String` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `toString` processes its inputs according to `TokenRecord`'s responsibility, performs the `to String` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `toString` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `toString`, then continue the business flow using its result, exception, or side effect.
         *
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        @Override
        public String toString() {
            return "TokenRecord[tokenId=" + tokenId
                    + ", tenantId=" + tenantId
                    + ", sessionId=" + sessionId
                    + ", familyId=" + familyId
                    + ", generation=" + generation
                    + ", tokenHash=<redacted>, status=" + status
                    + ", expiresAt=" + expiresAt
                    + ", rotatedAt=" + rotatedAt + ']';
        }
    }

    /**
     * 类型 `RotationResult` 位于 `RefreshTokenService` 内，是记录类型，用于承载 `Rotation Result` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RotationResult` is a record inside `RefreshTokenService` and carries the responsibility, state, or contract for `Rotation Result`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RotationResult` 作为 `RefreshTokenService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RotationResult` as the responsibility boundary of `RefreshTokenService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param outcome 记录组件 `outcome` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `outcome` carries constructor data whose meaning is defined by the record contract.
     * @param refreshToken 记录组件 `refreshToken` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `refreshToken` carries constructor data whose meaning is defined by the record contract.
     * @param familyId 记录组件 `familyId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `familyId` carries constructor data whose meaning is defined by the record contract.
     */
    public record RotationResult(
            /**
             * 字段 `outcome` 表示 `RotationResult` 中与 `outcome` 相关的状态、依赖、配置或结果（声明类型 `Outcome`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `outcome` stores the `outcome`-related state, dependency, configuration, or result of `RotationResult` (declared type `Outcome`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `outcome` 时应保持 `RotationResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `outcome`, preserve `RotationResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            Outcome outcome,
            /**
             * 字段 `refreshToken` 表示 `RotationResult` 中与 `refresh Token` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `refreshToken` stores the `refresh Token`-related state, dependency, configuration, or result of `RotationResult` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `refreshToken` 时应保持 `RotationResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `refreshToken`, preserve `RotationResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            String refreshToken,
            /**
             * 字段 `familyId` 表示 `RotationResult` 中与 `family Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `familyId` stores the `family Id`-related state, dependency, configuration, or result of `RotationResult` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `familyId` 时应保持 `RotationResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `familyId`, preserve `RotationResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            String familyId
    ) {

        /**
         * 方法 `toString` 按照 `RotationResult` 的职责处理输入，完成 `to String` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `toString` processes its inputs according to `RotationResult`'s responsibility, performs the `to String` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `toString` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `toString`, then continue the business flow using its result, exception, or side effect.
         *
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        @Override
        public String toString() {
            return "RotationResult[outcome=" + outcome
                    + ", refreshToken=<redacted>, familyId=" + familyId + ']';
        }
    }

    /**
     * 类型 `Outcome` 位于 `RefreshTokenService` 内，是枚举，用于承载 `Outcome` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Outcome` is an enum inside `RefreshTokenService` and carries the responsibility, state, or contract for `Outcome`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Outcome` 作为 `RefreshTokenService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Outcome` as the responsibility boundary of `RefreshTokenService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum Outcome {
        /**
         * 字段 `ROTATED` 表示 `Outcome` 中与 `ROTATED` 相关的状态、依赖、配置或结果（声明类型 `Outcome`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ROTATED` stores the `ROTATED`-related state, dependency, configuration, or result of `Outcome` (declared type `Outcome`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ROTATED` 时应保持 `Outcome` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ROTATED`, preserve `Outcome`'s lifecycle, immutability, and thread-safety constraints.
         */
        ROTATED,
        /**
         * 字段 `REPLAY_DETECTED` 表示 `Outcome` 中与 `REPLAY DETECTED` 相关的状态、依赖、配置或结果（声明类型 `Outcome`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `REPLAY_DETECTED` stores the `REPLAY DETECTED`-related state, dependency, configuration, or result of `Outcome` (declared type `Outcome`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `REPLAY_DETECTED` 时应保持 `Outcome` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `REPLAY_DETECTED`, preserve `Outcome`'s lifecycle, immutability, and thread-safety constraints.
         */
        REPLAY_DETECTED,
        /**
         * 字段 `INVALID` 表示 `Outcome` 中与 `INVALID` 相关的状态、依赖、配置或结果（声明类型 `Outcome`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `INVALID` stores the `INVALID`-related state, dependency, configuration, or result of `Outcome` (declared type `Outcome`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `INVALID` 时应保持 `Outcome` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `INVALID`, preserve `Outcome`'s lifecycle, immutability, and thread-safety constraints.
         */
        INVALID
    }

    /**
     * 类型 `TokenStatus` 位于 `RefreshTokenService` 内，是枚举，用于承载 `Token Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `TokenStatus` is an enum inside `RefreshTokenService` and carries the responsibility, state, or contract for `Token Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `TokenStatus` 作为 `RefreshTokenService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `TokenStatus` as the responsibility boundary of `RefreshTokenService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum TokenStatus {
        /**
         * 字段 `ACTIVE` 表示 `TokenStatus` 中与 `ACTIVE` 相关的状态、依赖、配置或结果（声明类型 `TokenStatus`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ACTIVE` stores the `ACTIVE`-related state, dependency, configuration, or result of `TokenStatus` (declared type `TokenStatus`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ACTIVE` 时应保持 `TokenStatus` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ACTIVE`, preserve `TokenStatus`'s lifecycle, immutability, and thread-safety constraints.
         */
        ACTIVE,
        /**
         * 字段 `ROTATED` 表示 `TokenStatus` 中与 `ROTATED` 相关的状态、依赖、配置或结果（声明类型 `TokenStatus`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ROTATED` stores the `ROTATED`-related state, dependency, configuration, or result of `TokenStatus` (declared type `TokenStatus`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ROTATED` 时应保持 `TokenStatus` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ROTATED`, preserve `TokenStatus`'s lifecycle, immutability, and thread-safety constraints.
         */
        ROTATED,
        /**
         * 字段 `REUSED_DETECTED` 表示 `TokenStatus` 中与 `REUSED DETECTED` 相关的状态、依赖、配置或结果（声明类型 `TokenStatus`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `REUSED_DETECTED` stores the `REUSED DETECTED`-related state, dependency, configuration, or result of `TokenStatus` (declared type `TokenStatus`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `REUSED_DETECTED` 时应保持 `TokenStatus` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `REUSED_DETECTED`, preserve `TokenStatus`'s lifecycle, immutability, and thread-safety constraints.
         */
        REUSED_DETECTED,
        /**
         * 字段 `REVOKED` 表示 `TokenStatus` 中与 `REVOKED` 相关的状态、依赖、配置或结果（声明类型 `TokenStatus`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `REVOKED` stores the `REVOKED`-related state, dependency, configuration, or result of `TokenStatus` (declared type `TokenStatus`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `REVOKED` 时应保持 `TokenStatus` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `REVOKED`, preserve `TokenStatus`'s lifecycle, immutability, and thread-safety constraints.
         */
        REVOKED,
        /**
         * 字段 `EXPIRED` 表示 `TokenStatus` 中与 `EXPIRED` 相关的状态、依赖、配置或结果（声明类型 `TokenStatus`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `EXPIRED` stores the `EXPIRED`-related state, dependency, configuration, or result of `TokenStatus` (declared type `TokenStatus`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `EXPIRED` 时应保持 `TokenStatus` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `EXPIRED`, preserve `TokenStatus`'s lifecycle, immutability, and thread-safety constraints.
         */
        EXPIRED
    }

    /**
     * 类型 `FamilyStatus` 位于 `RefreshTokenService` 内，是枚举，用于承载 `Family Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `FamilyStatus` is an enum inside `RefreshTokenService` and carries the responsibility, state, or contract for `Family Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `FamilyStatus` 作为 `RefreshTokenService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `FamilyStatus` as the responsibility boundary of `RefreshTokenService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum FamilyStatus {
        /**
         * 字段 `ACTIVE` 表示 `FamilyStatus` 中与 `ACTIVE` 相关的状态、依赖、配置或结果（声明类型 `FamilyStatus`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ACTIVE` stores the `ACTIVE`-related state, dependency, configuration, or result of `FamilyStatus` (declared type `FamilyStatus`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ACTIVE` 时应保持 `FamilyStatus` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ACTIVE`, preserve `FamilyStatus`'s lifecycle, immutability, and thread-safety constraints.
         */
        ACTIVE,
        /**
         * 字段 `COMPROMISED` 表示 `FamilyStatus` 中与 `COMPROMISED` 相关的状态、依赖、配置或结果（声明类型 `FamilyStatus`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `COMPROMISED` stores the `COMPROMISED`-related state, dependency, configuration, or result of `FamilyStatus` (declared type `FamilyStatus`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `COMPROMISED` 时应保持 `FamilyStatus` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `COMPROMISED`, preserve `FamilyStatus`'s lifecycle, immutability, and thread-safety constraints.
         */
        COMPROMISED
    }
}
