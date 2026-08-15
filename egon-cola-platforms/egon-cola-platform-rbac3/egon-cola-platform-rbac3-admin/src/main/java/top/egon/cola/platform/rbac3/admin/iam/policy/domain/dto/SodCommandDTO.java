package top.egon.cola.platform.rbac3.admin.iam.policy.domain.dto;

import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.iam.policy.domain.enums.ConstraintTypeEnum;

/**
     * 类型 `SodCommandDTO` 位于 `ConstraintFacade` 内，是记录类型，用于承载 `Sod Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SodCommandDTO` is a record inside `ConstraintFacade` and carries the responsibility, state, or contract for `Sod Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SodCommandDTO` 作为 `ConstraintFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SodCommandDTO` as the responsibility boundary of `ConstraintFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param constraintType 记录组件 `constraintType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `constraintType` carries constructor data whose meaning is defined by the record contract.
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param roleIds 记录组件 `roleIds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleIds` carries constructor data whose meaning is defined by the record contract.
     * @param maximumActiveRoles 记录组件 `maximumActiveRoles` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `maximumActiveRoles` carries constructor data whose meaning is defined by the record contract.
     */
    public record SodCommandDTO(
            /**
             * 字段 `constraintType` 表示 `SodCommandDTO` 中与 `constraint Type` 相关的状态、依赖、配置或结果（声明类型 `ConstraintTypeEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `constraintType` stores the `constraint Type`-related state, dependency, configuration, or result of `SodCommandDTO` (declared type `ConstraintTypeEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `constraintType` 时应保持 `SodCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `constraintType`, preserve `SodCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            ConstraintTypeEnum constraintType,
            /**
             * 字段 `applicationId` 表示 `SodCommandDTO` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `SodCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `SodCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `SodCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationId,
            /**
             * 字段 `roleIds` 表示 `SodCommandDTO` 中与 `role Ids` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleIds` stores the `role Ids`-related state, dependency, configuration, or result of `SodCommandDTO` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleIds` 时应保持 `SodCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleIds`, preserve `SodCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<String> roleIds,
            /**
             * 字段 `maximumActiveRoles` 表示 `SodCommandDTO` 中与 `maximum Active Roles` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `maximumActiveRoles` stores the `maximum Active Roles`-related state, dependency, configuration, or result of `SodCommandDTO` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `maximumActiveRoles` 时应保持 `SodCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `maximumActiveRoles`, preserve `SodCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            int maximumActiveRoles
    ) {

        /**
         * 构造器 `SodCommandDTO` 用于创建并初始化 `SodCommandDTO` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `SodCommandDTO` creates and initializes `SodCommandDTO`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `SodCommandDTO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `SodCommandDTO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param constraintType 输入参数 `constraintType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param roleIds 输入参数 `roleIds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param maximumActiveRoles 输入参数 `maximumActiveRoles`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public SodCommandDTO {
            constraintType = Objects.requireNonNull(constraintType, "constraintType");
            roleIds = List.copyOf(Objects.requireNonNull(roleIds, "roleIds"));
        }
    }
