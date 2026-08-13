package top.egon.cola.platform.rbac3.admin.participation.domain.dto;

import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.participation.BusinessParticipationCommand;
import top.egon.cola.platform.rbac3.core.participation.OperationSodSpecification;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import top.egon.cola.platform.rbac3.admin.participation.service.ParticipationFacade;

/**
     * 类型 `ConflictQueryDTO` 位于 `ParticipationFacade` 内，是记录类型，用于承载 `Conflict Query` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ConflictQueryDTO` is a record inside `ParticipationFacade` and carries the responsibility, state, or contract for `Conflict Query`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ConflictQueryDTO` 作为 `ParticipationFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ConflictQueryDTO` as the responsibility boundary of `ParticipationFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param applicationCode 记录组件 `applicationCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationCode` carries constructor data whose meaning is defined by the record contract.
     * @param businessResource 记录组件 `businessResource` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `businessResource` carries constructor data whose meaning is defined by the record contract.
     * @param businessId 记录组件 `businessId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `businessId` carries constructor data whose meaning is defined by the record contract.
     * @param actorUserId 记录组件 `actorUserId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorUserId` carries constructor data whose meaning is defined by the record contract.
     * @param requestedAction 记录组件 `requestedAction` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `requestedAction` carries constructor data whose meaning is defined by the record contract.
     */
    public record ConflictQueryDTO(
            /**
             * 字段 `applicationCode` 表示 `ConflictQueryDTO` 中与 `application Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationCode` stores the `application Code`-related state, dependency, configuration, or result of `ConflictQueryDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationCode` 时应保持 `ConflictQueryDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationCode`, preserve `ConflictQueryDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationCode,
            /**
             * 字段 `businessResource` 表示 `ConflictQueryDTO` 中与 `business Resource` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `businessResource` stores the `business Resource`-related state, dependency, configuration, or result of `ConflictQueryDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `businessResource` 时应保持 `ConflictQueryDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `businessResource`, preserve `ConflictQueryDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String businessResource,
            /**
             * 字段 `businessId` 表示 `ConflictQueryDTO` 中与 `business Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `businessId` stores the `business Id`-related state, dependency, configuration, or result of `ConflictQueryDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `businessId` 时应保持 `ConflictQueryDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `businessId`, preserve `ConflictQueryDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String businessId,
            /**
             * 字段 `actorUserId` 表示 `ConflictQueryDTO` 中与 `actor User Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorUserId` stores the `actor User Id`-related state, dependency, configuration, or result of `ConflictQueryDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorUserId` 时应保持 `ConflictQueryDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorUserId`, preserve `ConflictQueryDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorUserId,
            /**
             * 字段 `requestedAction` 表示 `ConflictQueryDTO` 中与 `requested Action` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requestedAction` stores the `requested Action`-related state, dependency, configuration, or result of `ConflictQueryDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requestedAction` 时应保持 `ConflictQueryDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requestedAction`, preserve `ConflictQueryDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String requestedAction) {
    }
