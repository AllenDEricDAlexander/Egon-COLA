package top.egon.cola.platform.rbac3.admin.iam.role.domain.dto;

import top.egon.cola.platform.rbac3.core.hierarchy.RoleEdge;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchyValidator;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.time.Instant;
import java.util.function.Function;

/**
     * 类型 `CreateRoleCommandDTO` 位于 `RoleFacade` 内，是记录类型，用于承载 `Create Role Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `CreateRoleCommandDTO` is a record inside `RoleFacade` and carries the responsibility, state, or contract for `Create Role Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `CreateRoleCommandDTO` 作为 `RoleFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `CreateRoleCommandDTO` as the responsibility boundary of `RoleFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param roleCode 记录组件 `roleCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleCode` carries constructor data whose meaning is defined by the record contract.
     * @param roleName 记录组件 `roleName` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleName` carries constructor data whose meaning is defined by the record contract.
     * @param roleType 记录组件 `roleType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleType` carries constructor data whose meaning is defined by the record contract.
     * @param riskLevel 记录组件 `riskLevel` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `riskLevel` carries constructor data whose meaning is defined by the record contract.
     * @param privileged 记录组件 `privileged` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `privileged` carries constructor data whose meaning is defined by the record contract.
     * @param landingRouteId 记录组件 `landingRouteId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `landingRouteId` carries constructor data whose meaning is defined by the record contract.
     * @param landingPriority 记录组件 `landingPriority` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `landingPriority` carries constructor data whose meaning is defined by the record contract.
     * @param maximumAssignmentDays 记录组件 `maximumAssignmentDays` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `maximumAssignmentDays` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     */
    public record CreateRoleCommandDTO(
            /**
             * 字段 `tenantId` 表示 `CreateRoleCommandDTO` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `CreateRoleCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `CreateRoleCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `CreateRoleCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `applicationId` 表示 `CreateRoleCommandDTO` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `CreateRoleCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `CreateRoleCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `CreateRoleCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationId,
            /**
             * 字段 `roleCode` 表示 `CreateRoleCommandDTO` 中与 `role Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleCode` stores the `role Code`-related state, dependency, configuration, or result of `CreateRoleCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleCode` 时应保持 `CreateRoleCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleCode`, preserve `CreateRoleCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleCode,
            /**
             * 字段 `roleName` 表示 `CreateRoleCommandDTO` 中与 `role Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleName` stores the `role Name`-related state, dependency, configuration, or result of `CreateRoleCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleName` 时应保持 `CreateRoleCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleName`, preserve `CreateRoleCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleName,
            /**
             * 字段 `roleType` 表示 `CreateRoleCommandDTO` 中与 `role Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleType` stores the `role Type`-related state, dependency, configuration, or result of `CreateRoleCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleType` 时应保持 `CreateRoleCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleType`, preserve `CreateRoleCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleType,
            /**
             * 字段 `riskLevel` 表示 `CreateRoleCommandDTO` 中与 `risk Level` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `riskLevel` stores the `risk Level`-related state, dependency, configuration, or result of `CreateRoleCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `riskLevel` 时应保持 `CreateRoleCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `riskLevel`, preserve `CreateRoleCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String riskLevel,
            /**
             * 字段 `privileged` 表示 `CreateRoleCommandDTO` 中与 `privileged` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `privileged` stores the `privileged`-related state, dependency, configuration, or result of `CreateRoleCommandDTO` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `privileged` 时应保持 `CreateRoleCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `privileged`, preserve `CreateRoleCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean privileged,
            /**
             * 字段 `landingRouteId` 表示 `CreateRoleCommandDTO` 中与 `landing Route Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `landingRouteId` stores the `landing Route Id`-related state, dependency, configuration, or result of `CreateRoleCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `landingRouteId` 时应保持 `CreateRoleCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `landingRouteId`, preserve `CreateRoleCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String landingRouteId,
            /**
             * 字段 `landingPriority` 表示 `CreateRoleCommandDTO` 中与 `landing Priority` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `landingPriority` stores the `landing Priority`-related state, dependency, configuration, or result of `CreateRoleCommandDTO` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `landingPriority` 时应保持 `CreateRoleCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `landingPriority`, preserve `CreateRoleCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            int landingPriority,
            /**
             * 字段 `maximumAssignmentDays` 表示 `CreateRoleCommandDTO` 中与 `maximum Assignment Days` 相关的状态、依赖、配置或结果（声明类型 `Integer`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `maximumAssignmentDays` stores the `maximum Assignment Days`-related state, dependency, configuration, or result of `CreateRoleCommandDTO` (declared type `Integer`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `maximumAssignmentDays` 时应保持 `CreateRoleCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `maximumAssignmentDays`, preserve `CreateRoleCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Integer maximumAssignmentDays,
            /**
             * 字段 `actorId` 表示 `CreateRoleCommandDTO` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `CreateRoleCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `CreateRoleCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `CreateRoleCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId
    ) {
    }
