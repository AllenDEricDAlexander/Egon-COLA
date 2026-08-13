package top.egon.cola.platform.rbac3.admin.activation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.activation.domain.po.SessionActiveRolePO;

/**
     * 类型 `SessionActiveRoleKey` 位于 `SessionActiveRolePO` 内，是记录类型，用于承载 `SessionActiveRoleKey` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SessionActiveRoleKey` is a record inside `SessionActiveRolePO` and carries the responsibility, state, or contract for `SessionActiveRoleKey`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SessionActiveRoleKey` 作为 `SessionActiveRolePO` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SessionActiveRoleKey` as the responsibility boundary of `SessionActiveRolePO`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param sessionId 记录组件 `sessionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionId` carries constructor data whose meaning is defined by the record contract.
     * @param rootRoleId 记录组件 `rootRoleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `rootRoleId` carries constructor data whose meaning is defined by the record contract.
     */
    public record SessionActiveRoleKey(/**
 * 字段 `tenantId` 表示 `SessionActiveRoleKey` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `SessionActiveRoleKey` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `SessionActiveRoleKey` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `SessionActiveRoleKey`'s lifecycle, immutability, and thread-safety constraints.
 */ Long tenantId, /**
 * 字段 `sessionId` 表示 `SessionActiveRoleKey` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `SessionActiveRoleKey` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `SessionActiveRoleKey` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `SessionActiveRoleKey`'s lifecycle, immutability, and thread-safety constraints.
 */ Long sessionId, /**
 * 字段 `rootRoleId` 表示 `SessionActiveRoleKey` 中与 `root Role Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `rootRoleId` stores the `root Role Id`-related state, dependency, configuration, or result of `SessionActiveRoleKey` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `rootRoleId` 时应保持 `SessionActiveRoleKey` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `rootRoleId`, preserve `SessionActiveRoleKey`'s lifecycle, immutability, and thread-safety constraints.
 */ Long rootRoleId)
            implements Serializable {
    }
