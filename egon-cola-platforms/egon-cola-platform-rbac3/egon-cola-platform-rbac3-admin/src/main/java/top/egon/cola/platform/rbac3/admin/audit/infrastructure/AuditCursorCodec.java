package top.egon.cola.platform.rbac3.admin.audit.infrastructure;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/**
 * 类型 `AuditCursorCodec` 位于当前包内，是类型，用于承载 `Audit Cursor Codec` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `AuditCursorCodec` is a type in its package and carries the responsibility, state, or contract for `Audit Cursor Codec`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Signs audit cursors and binds them to the effective tenant and exact filter.
 */
public final class AuditCursorCodec {

    /**
     * 字段 `HMAC_ALGORITHM` 表示 `AuditCursorCodec` 中与 `HMAC ALGORITHM` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `HMAC_ALGORITHM` stores the `HMAC ALGORITHM`-related state, dependency, configuration, or result of `AuditCursorCodec` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `HMAC_ALGORITHM` 时应保持 `AuditCursorCodec` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `HMAC_ALGORITHM`, preserve `AuditCursorCodec`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    /**
     * 字段 `secret` 表示 `AuditCursorCodec` 中与 `secret` 相关的状态、依赖、配置或结果（声明类型 `byte[]`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `secret` stores the `secret`-related state, dependency, configuration, or result of `AuditCursorCodec` (declared type `byte[]`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `secret` 时应保持 `AuditCursorCodec` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `secret`, preserve `AuditCursorCodec`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final byte[] secret;

    /**
     * 构造器 `AuditCursorCodec` 用于创建并初始化 `AuditCursorCodec` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `AuditCursorCodec` creates and initializes `AuditCursorCodec`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `AuditCursorCodec` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `AuditCursorCodec`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param secret 输入参数 `secret`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public AuditCursorCodec(byte[] secret) {
        Objects.requireNonNull(secret, "secret");
        if (secret.length < 32) {
            throw new IllegalArgumentException("audit cursor signing key must be at least 32 bytes");
        }
        this.secret = Arrays.copyOf(secret, secret.length);
    }

    /**
     * 方法 `encode` 按照 `AuditCursorCodec` 的职责处理输入，完成 `encode` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `encode` processes its inputs according to `AuditCursorCodec`'s responsibility, performs the `encode` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `encode` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `encode`, then continue the business flow using its result, exception, or side effect.
     *
     * @param position 输入参数 `position`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param filterDigest 输入参数 `filterDigest`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String encode(
            CursorPosition position,
            String tenantId,
            String filterDigest) {
        Objects.requireNonNull(position, "position");
        String payload = position.createdAt().toEpochMilli() + ":" + position.id();
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        byte[] signature = sign(binding(tenantId, filterDigest, payload));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payloadBytes)
                + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    }

    /**
     * 方法 `decode` 按照 `AuditCursorCodec` 的职责处理输入，完成 `decode` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `decode` processes its inputs according to `AuditCursorCodec`'s responsibility, performs the `decode` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `decode` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `decode`, then continue the business flow using its result, exception, or side effect.
     *
     * @param cursor 输入参数 `cursor`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param filterDigest 输入参数 `filterDigest`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public CursorPosition decode(
            String cursor,
            String tenantId,
            String filterDigest) {
        try {
            String[] parts = Objects.requireNonNull(cursor, "cursor").split("\\.", -1);
            if (parts.length != 2) {
                throw invalid();
            }
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[0]);
            byte[] actualSignature = Base64.getUrlDecoder().decode(parts[1]);
            String payload = new String(payloadBytes, StandardCharsets.UTF_8);
            byte[] expectedSignature = sign(binding(tenantId, filterDigest, payload));
            if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
                throw invalid();
            }
            String[] values = payload.split(":", -1);
            if (values.length != 2) {
                throw invalid();
            }
            long epochMillis = Long.parseLong(values[0]);
            long id = Long.parseLong(values[1]);
            if (id < 1L) {
                throw invalid();
            }
            return new CursorPosition(Instant.ofEpochMilli(epochMillis), id);
        } catch (IllegalArgumentException | NullPointerException error) {
            if (error instanceof IllegalArgumentException illegal
                    && "audit cursor is invalid".equals(illegal.getMessage())) {
                throw illegal;
            }
            throw invalid();
        }
    }

    /**
     * 方法 `binding` 按照 `AuditCursorCodec` 的职责处理输入，完成 `binding` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `binding` processes its inputs according to `AuditCursorCodec`'s responsibility, performs the `binding` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `binding` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `binding`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param filterDigest 输入参数 `filterDigest`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param payload 输入参数 `payload`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private byte[] binding(String tenantId, String filterDigest, String payload) {
        String bound = required(tenantId, "tenantId") + '\u001f'
                + required(filterDigest, "filterDigest") + '\u001f' + payload;
        return bound.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 方法 `sign` 按照 `AuditCursorCodec` 的职责处理输入，完成 `sign` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `sign` processes its inputs according to `AuditCursorCodec`'s responsibility, performs the `sign` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `sign` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `sign`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private byte[] sign(byte[] value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return mac.doFinal(value);
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("cannot sign audit cursor", error);
        }
    }

    /**
     * 方法 `invalid` 按照 `AuditCursorCodec` 的职责处理输入，完成 `invalid` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `invalid` processes its inputs according to `AuditCursorCodec`'s responsibility, performs the `invalid` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `invalid` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `invalid`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private IllegalArgumentException invalid() {
        return new IllegalArgumentException("audit cursor is invalid");
    }

    /**
     * 方法 `required` 按照 `AuditCursorCodec` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `AuditCursorCodec`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param fieldName 输入参数 `fieldName`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    /**
     * 类型 `CursorPosition` 位于 `AuditCursorCodec` 内，是记录类型，用于承载 `Cursor Position` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `CursorPosition` is a record inside `AuditCursorCodec` and carries the responsibility, state, or contract for `Cursor Position`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `CursorPosition` 作为 `AuditCursorCodec` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `CursorPosition` as the responsibility boundary of `AuditCursorCodec`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param createdAt 记录组件 `createdAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `createdAt` carries constructor data whose meaning is defined by the record contract.
     * @param id 记录组件 `id` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `id` carries constructor data whose meaning is defined by the record contract.
     */
    public record CursorPosition(/**
 * 字段 `createdAt` 表示 `CursorPosition` 中与 `created At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `createdAt` stores the `created At`-related state, dependency, configuration, or result of `CursorPosition` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `createdAt` 时应保持 `CursorPosition` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `createdAt`, preserve `CursorPosition`'s lifecycle, immutability, and thread-safety constraints.
 */ Instant createdAt, /**
 * 字段 `id` 表示 `CursorPosition` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `id` stores the `id`-related state, dependency, configuration, or result of `CursorPosition` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `id` 时应保持 `CursorPosition` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `id`, preserve `CursorPosition`'s lifecycle, immutability, and thread-safety constraints.
 */ long id) {
        /**
         * 构造器 `CursorPosition` 用于创建并初始化 `CursorPosition` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `CursorPosition` creates and initializes `CursorPosition`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `CursorPosition` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `CursorPosition`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param createdAt 输入参数 `createdAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public CursorPosition {
            createdAt = Objects.requireNonNull(createdAt, "createdAt");
            if (id < 1L) {
                throw new IllegalArgumentException("id must be positive");
            }
        }
    }
}
