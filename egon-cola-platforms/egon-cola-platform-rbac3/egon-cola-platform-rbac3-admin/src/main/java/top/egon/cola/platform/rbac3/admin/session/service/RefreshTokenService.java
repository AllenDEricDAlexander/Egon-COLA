package top.egon.cola.platform.rbac3.admin.session.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import java.util.function.Function;
import top.egon.cola.platform.rbac3.admin.session.repository.RefreshTokenRepository;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.TokenRecordVO;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.RotationResultVO;
import top.egon.cola.platform.rbac3.admin.session.domain.enums.RefreshTokenOutcomeEnum;
import top.egon.cola.platform.rbac3.admin.session.domain.enums.RefreshTokenTokenStatusEnum;
import top.egon.cola.platform.rbac3.admin.session.domain.enums.RefreshTokenFamilyStatusEnum;

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
     * 字段 `store` 表示 `RefreshTokenService` 中与 `store` 相关的状态、依赖、配置或结果（声明类型 `RefreshTokenRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `store` stores the `store`-related state, dependency, configuration, or result of `RefreshTokenService` (declared type `RefreshTokenRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `store` 时应保持 `RefreshTokenService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `store`, preserve `RefreshTokenService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RefreshTokenRepository store;
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
    public RefreshTokenService(RefreshTokenRepository store) {
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
    RefreshTokenService(RefreshTokenRepository store, SecureRandom secureRandom) {
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
    public RotationResultVO rotate(String rawToken, Instant now) {
        Objects.requireNonNull(rawToken, "rawToken");
        Objects.requireNonNull(now, "now");
        if (rawToken.isBlank() || rawToken.length() > 512) {
            return new RotationResultVO(RefreshTokenOutcomeEnum.INVALID, null, null);
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
    private RotationResultVO rotateLocked(TokenRecordVO current, Instant now) {
        if (current == null) {
            return new RotationResultVO(RefreshTokenOutcomeEnum.INVALID, null, null);
        }
        if (current.status() == RefreshTokenTokenStatusEnum.ROTATED
                || current.status() == RefreshTokenTokenStatusEnum.REUSED_DETECTED) {
            store.compromiseFamily(current.familyId(), now);
            return new RotationResultVO(RefreshTokenOutcomeEnum.REPLAY_DETECTED, null, current.familyId());
        }
        if (current.status() != RefreshTokenTokenStatusEnum.ACTIVE || !current.expiresAt().isAfter(now)) {
            return new RotationResultVO(RefreshTokenOutcomeEnum.INVALID, null, current.familyId());
        }
        byte[] random = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(random);
        String nextRawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        TokenRecordVO rotated = current.rotated(now);
        TokenRecordVO next = TokenRecordVO.active(
                current.tokenId() + ':' + (current.generation() + 1),
                current.tenantId(),
                current.sessionId(),
                current.familyId(),
                current.generation() + 1,
                hash(nextRawToken),
                current.expiresAt());
        store.rotate(rotated, next);
        return new RotationResultVO(RefreshTokenOutcomeEnum.ROTATED, nextRawToken, current.familyId());
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






    }
