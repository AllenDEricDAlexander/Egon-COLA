package top.egon.cola.platform.rbac3.admin.session.domain.vo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import java.util.function.Function;
import top.egon.cola.platform.rbac3.admin.session.domain.enums.RefreshTokenTokenStatusEnum;

/**
     * 类型 `TokenRecordVO` 位于 `RefreshTokenService` 内，是记录类型，用于承载 `Token Record` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `TokenRecordVO` is a record inside `RefreshTokenService` and carries the responsibility, state, or contract for `Token Record`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `TokenRecordVO` 作为 `RefreshTokenService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `TokenRecordVO` as the responsibility boundary of `RefreshTokenService`, following its existing construction, interface, or Spring-assembly mechanism.
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
    public record TokenRecordVO(
            /**
             * 字段 `tokenId` 表示 `TokenRecordVO` 中与 `token Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tokenId` stores the `token Id`-related state, dependency, configuration, or result of `TokenRecordVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tokenId` 时应保持 `TokenRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tokenId`, preserve `TokenRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tokenId,
            /**
             * 字段 `tenantId` 表示 `TokenRecordVO` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `TokenRecordVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `TokenRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `TokenRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `sessionId` 表示 `TokenRecordVO` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `TokenRecordVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `TokenRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `TokenRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sessionId,
            /**
             * 字段 `familyId` 表示 `TokenRecordVO` 中与 `family Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `familyId` stores the `family Id`-related state, dependency, configuration, or result of `TokenRecordVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `familyId` 时应保持 `TokenRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `familyId`, preserve `TokenRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String familyId,
            /**
             * 字段 `generation` 表示 `TokenRecordVO` 中与 `generation` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `generation` stores the `generation`-related state, dependency, configuration, or result of `TokenRecordVO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `generation` 时应保持 `TokenRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `generation`, preserve `TokenRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            long generation,
            /**
             * 字段 `tokenHash` 表示 `TokenRecordVO` 中与 `token Hash` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tokenHash` stores the `token Hash`-related state, dependency, configuration, or result of `TokenRecordVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tokenHash` 时应保持 `TokenRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tokenHash`, preserve `TokenRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tokenHash,
            /**
             * 字段 `status` 表示 `TokenRecordVO` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `RefreshTokenTokenStatusEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `TokenRecordVO` (declared type `RefreshTokenTokenStatusEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `TokenRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `TokenRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            RefreshTokenTokenStatusEnum status,
            /**
             * 字段 `expiresAt` 表示 `TokenRecordVO` 中与 `expires At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expiresAt` stores the `expires At`-related state, dependency, configuration, or result of `TokenRecordVO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expiresAt` 时应保持 `TokenRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expiresAt`, preserve `TokenRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant expiresAt,
            /**
             * 字段 `rotatedAt` 表示 `TokenRecordVO` 中与 `rotated At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `rotatedAt` stores the `rotated At`-related state, dependency, configuration, or result of `TokenRecordVO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `rotatedAt` 时应保持 `TokenRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `rotatedAt`, preserve `TokenRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant rotatedAt
    ) {

        /**
         * 构造器 `TokenRecordVO` 用于创建并初始化 `TokenRecordVO` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `TokenRecordVO` creates and initializes `TokenRecordVO`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `TokenRecordVO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `TokenRecordVO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
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
        public TokenRecordVO {
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
         * 方法 `active` 按照 `TokenRecordVO` 的职责处理输入，完成 `active` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `active` processes its inputs according to `TokenRecordVO`'s responsibility, performs the `active` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
        public static TokenRecordVO active(
                String tokenId,
                String tenantId,
                String sessionId,
                String familyId,
                long generation,
                String tokenHash,
                Instant expiresAt) {
            return new TokenRecordVO(
                    tokenId,
                    tenantId,
                    sessionId,
                    familyId,
                    generation,
                    tokenHash,
                    RefreshTokenTokenStatusEnum.ACTIVE,
                    expiresAt,
                    null);
        }

        /**
         * 方法 `rotated` 按照 `TokenRecordVO` 的职责处理输入，完成 `rotated` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `rotated` processes its inputs according to `TokenRecordVO`'s responsibility, performs the `rotated` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `rotated` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `rotated`, then continue the business flow using its result, exception, or side effect.
         *
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        public TokenRecordVO rotated(Instant now) {
            return new TokenRecordVO(
                    tokenId,
                    tenantId,
                    sessionId,
                    familyId,
                    generation,
                    tokenHash,
                    RefreshTokenTokenStatusEnum.ROTATED,
                    expiresAt,
                    now);
        }

        /**
         * 方法 `toString` 按照 `TokenRecordVO` 的职责处理输入，完成 `to String` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `toString` processes its inputs according to `TokenRecordVO`'s responsibility, performs the `to String` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `toString` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `toString`, then continue the business flow using its result, exception, or side effect.
         *
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        @Override
        public String toString() {
            return "TokenRecordVO[tokenId=" + tokenId
                    + ", tenantId=" + tenantId
                    + ", sessionId=" + sessionId
                    + ", familyId=" + familyId
                    + ", generation=" + generation
                    + ", tokenHash=<redacted>, status=" + status
                    + ", expiresAt=" + expiresAt
                    + ", rotatedAt=" + rotatedAt + ']';
        }
    }
