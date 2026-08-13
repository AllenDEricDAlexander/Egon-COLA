package top.egon.cola.platform.rbac3.admin.session.repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import java.util.function.Function;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.TokenRecordVO;

/**
     * 类型 `RefreshTokenRepository` 位于 `RefreshTokenService` 内，是接口，用于承载 `Refresh Token Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RefreshTokenRepository` is an interface inside `RefreshTokenService` and carries the responsibility, state, or contract for `Refresh Token Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     * Implementations must hold a cross-instance atomic lock for the whole callback.
     */
    public interface RefreshTokenRepository {

        /**
         * 方法 `withLockedToken` 按照 `RefreshTokenRepository` 的职责处理输入，完成 `with Locked Token` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `withLockedToken` processes its inputs according to `RefreshTokenRepository`'s responsibility, performs the `with Locked Token` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `withLockedToken` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `withLockedToken`, then continue the business flow using its result, exception, or side effect.
         *
         * @param <T> 类型参数表示令牌回调结果的具体类型；type parameter representing the token callback result type.
         * @param tokenHash 输入参数 `tokenHash`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param action 输入参数 `action`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        <T> T withLockedToken(String tokenHash, Function<TokenRecordVO, T> action);

        /**
         * 方法 `rotate` 按照 `RefreshTokenRepository` 的职责处理输入，完成 `rotate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `rotate` processes its inputs according to `RefreshTokenRepository`'s responsibility, performs the `rotate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         * Implementations must lock the owning session before changing either token.
         * Role activation uses the same session lock, which serializes refresh and
         * activation version increments without a distributed lock.
         * @param oldToken 输入参数 `oldToken`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param newToken 输入参数 `newToken`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void rotate(TokenRecordVO oldToken, TokenRecordVO newToken);

        /**
         * 方法 `compromiseFamily` 按照 `RefreshTokenRepository` 的职责处理输入，完成 `compromise Family` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `compromiseFamily` processes its inputs according to `RefreshTokenRepository`'s responsibility, performs the `compromise Family` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `compromiseFamily` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `compromiseFamily`, then continue the business flow using its result, exception, or side effect.
         *
         * @param familyId 输入参数 `familyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param detectedAt 输入参数 `detectedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void compromiseFamily(String familyId, Instant detectedAt);
    }
