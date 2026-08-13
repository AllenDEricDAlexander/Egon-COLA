package top.egon.cola.platform.rbac3.admin.audit.domain.vo;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.audit.repository.internal.AuditCursorCodec;

/**
     * 类型 `CursorPositionVO` 位于 `AuditCursorCodec` 内，是记录类型，用于承载 `Cursor Position` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `CursorPositionVO` is a record inside `AuditCursorCodec` and carries the responsibility, state, or contract for `Cursor Position`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `CursorPositionVO` 作为 `AuditCursorCodec` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `CursorPositionVO` as the responsibility boundary of `AuditCursorCodec`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param createdAt 记录组件 `createdAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `createdAt` carries constructor data whose meaning is defined by the record contract.
     * @param id 记录组件 `id` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `id` carries constructor data whose meaning is defined by the record contract.
     */
    public record CursorPositionVO(/**
 * 字段 `createdAt` 表示 `CursorPositionVO` 中与 `created At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `createdAt` stores the `created At`-related state, dependency, configuration, or result of `CursorPositionVO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `createdAt` 时应保持 `CursorPositionVO` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `createdAt`, preserve `CursorPositionVO`'s lifecycle, immutability, and thread-safety constraints.
 */ Instant createdAt, /**
 * 字段 `id` 表示 `CursorPositionVO` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `id` stores the `id`-related state, dependency, configuration, or result of `CursorPositionVO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `id` 时应保持 `CursorPositionVO` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `id`, preserve `CursorPositionVO`'s lifecycle, immutability, and thread-safety constraints.
 */ long id) {
        /**
         * 构造器 `CursorPositionVO` 用于创建并初始化 `CursorPositionVO` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `CursorPositionVO` creates and initializes `CursorPositionVO`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `CursorPositionVO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `CursorPositionVO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param createdAt 输入参数 `createdAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public CursorPositionVO {
            createdAt = Objects.requireNonNull(createdAt, "createdAt");
            if (id < 1L) {
                throw new IllegalArgumentException("id must be positive");
            }
        }
    }
