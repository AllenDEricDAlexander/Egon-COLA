package top.egon.cola.platform.rbac3.admin.session.domain.enums;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.shared.domain.po.TenantScopedPO;
import java.time.Instant;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.session.domain.po.RefreshTokenPO;

/**
     * 类型 `RefreshTokenStatusEnum` 位于 `RefreshTokenEntity` 内，是枚举，用于承载 `RefreshTokenStatusEnum` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RefreshTokenStatusEnum` is an enum inside `RefreshTokenEntity` and carries the responsibility, state, or contract for `RefreshTokenStatusEnum`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RefreshTokenStatusEnum` 作为 `RefreshTokenEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RefreshTokenStatusEnum` as the responsibility boundary of `RefreshTokenEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum RefreshTokenStatusEnum {
        /**
         * 字段 `ACTIVE` 表示 `RefreshTokenStatusEnum` 中与 `ACTIVE` 相关的状态、依赖、配置或结果（声明类型 `RefreshTokenStatusEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ACTIVE` stores the `ACTIVE`-related state, dependency, configuration, or result of `RefreshTokenStatusEnum` (declared type `RefreshTokenStatusEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ACTIVE` 时应保持 `RefreshTokenStatusEnum` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ACTIVE`, preserve `RefreshTokenStatusEnum`'s lifecycle, immutability, and thread-safety constraints.
         */
        ACTIVE,
        /**
         * 字段 `ROTATED` 表示 `RefreshTokenStatusEnum` 中与 `ROTATED` 相关的状态、依赖、配置或结果（声明类型 `RefreshTokenStatusEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ROTATED` stores the `ROTATED`-related state, dependency, configuration, or result of `RefreshTokenStatusEnum` (declared type `RefreshTokenStatusEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ROTATED` 时应保持 `RefreshTokenStatusEnum` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ROTATED`, preserve `RefreshTokenStatusEnum`'s lifecycle, immutability, and thread-safety constraints.
         */
        ROTATED,
        /**
         * 字段 `REUSED_DETECTED` 表示 `RefreshTokenStatusEnum` 中与 `REUSED DETECTED` 相关的状态、依赖、配置或结果（声明类型 `RefreshTokenStatusEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `REUSED_DETECTED` stores the `REUSED DETECTED`-related state, dependency, configuration, or result of `RefreshTokenStatusEnum` (declared type `RefreshTokenStatusEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `REUSED_DETECTED` 时应保持 `RefreshTokenStatusEnum` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `REUSED_DETECTED`, preserve `RefreshTokenStatusEnum`'s lifecycle, immutability, and thread-safety constraints.
         */
        REUSED_DETECTED,
        /**
         * 字段 `REVOKED` 表示 `RefreshTokenStatusEnum` 中与 `REVOKED` 相关的状态、依赖、配置或结果（声明类型 `RefreshTokenStatusEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `REVOKED` stores the `REVOKED`-related state, dependency, configuration, or result of `RefreshTokenStatusEnum` (declared type `RefreshTokenStatusEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `REVOKED` 时应保持 `RefreshTokenStatusEnum` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `REVOKED`, preserve `RefreshTokenStatusEnum`'s lifecycle, immutability, and thread-safety constraints.
         */
        REVOKED,
        /**
         * 字段 `EXPIRED` 表示 `RefreshTokenStatusEnum` 中与 `EXPIRED` 相关的状态、依赖、配置或结果（声明类型 `RefreshTokenStatusEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `EXPIRED` stores the `EXPIRED`-related state, dependency, configuration, or result of `RefreshTokenStatusEnum` (declared type `RefreshTokenStatusEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `EXPIRED` 时应保持 `RefreshTokenStatusEnum` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `EXPIRED`, preserve `RefreshTokenStatusEnum`'s lifecycle, immutability, and thread-safety constraints.
         */
        EXPIRED
    }
