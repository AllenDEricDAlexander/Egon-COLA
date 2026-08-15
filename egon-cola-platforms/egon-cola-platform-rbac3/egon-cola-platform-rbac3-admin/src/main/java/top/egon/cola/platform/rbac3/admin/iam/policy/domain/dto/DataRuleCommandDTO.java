package top.egon.cola.platform.rbac3.admin.iam.policy.domain.dto;

import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.iam.policy.domain.vo.RuleReferenceVO;

/**
     * 类型 `DataRuleCommandDTO` 位于 `ConstraintFacade` 内，是记录类型，用于承载 `Data Rule Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `DataRuleCommandDTO` is a record inside `ConstraintFacade` and carries the responsibility, state, or contract for `Data Rule Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `DataRuleCommandDTO` 作为 `ConstraintFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `DataRuleCommandDTO` as the responsibility boundary of `ConstraintFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param ruleId 记录组件 `ruleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `ruleId` carries constructor data whose meaning is defined by the record contract.
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param roleId 记录组件 `roleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleId` carries constructor data whose meaning is defined by the record contract.
     * @param permissionId 记录组件 `permissionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `permissionId` carries constructor data whose meaning is defined by the record contract.
     * @param scopeType 记录组件 `scopeType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopeType` carries constructor data whose meaning is defined by the record contract.
     * @param directorySnapshotVersion 记录组件 `directorySnapshotVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `directorySnapshotVersion` carries constructor data whose meaning is defined by the record contract.
     * @param references 记录组件 `references` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `references` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     * @param expectedVersion 记录组件 `expectedVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedVersion` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     */
    public record DataRuleCommandDTO(
            /**
             * 字段 `tenantId` 表示 `DataRuleCommandDTO` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `DataRuleCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `DataRuleCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `DataRuleCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `ruleId` 表示 `DataRuleCommandDTO` 中与 `rule Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `ruleId` stores the `rule Id`-related state, dependency, configuration, or result of `DataRuleCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `ruleId` 时应保持 `DataRuleCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `ruleId`, preserve `DataRuleCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String ruleId,
            /**
             * 字段 `applicationId` 表示 `DataRuleCommandDTO` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `DataRuleCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `DataRuleCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `DataRuleCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationId,
            /**
             * 字段 `roleId` 表示 `DataRuleCommandDTO` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `DataRuleCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleId` 时应保持 `DataRuleCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleId`, preserve `DataRuleCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleId,
            /**
             * 字段 `permissionId` 表示 `DataRuleCommandDTO` 中与 `permission Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `permissionId` stores the `permission Id`-related state, dependency, configuration, or result of `DataRuleCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `permissionId` 时应保持 `DataRuleCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `permissionId`, preserve `DataRuleCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String permissionId,
            /**
             * 字段 `scopeType` 表示 `DataRuleCommandDTO` 中与 `scope Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopeType` stores the `scope Type`-related state, dependency, configuration, or result of `DataRuleCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopeType` 时应保持 `DataRuleCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopeType`, preserve `DataRuleCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String scopeType,
            /**
             * 字段 `directorySnapshotVersion` 表示 `DataRuleCommandDTO` 中与 `directory Snapshot Version` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `directorySnapshotVersion` stores the `directory Snapshot Version`-related state, dependency, configuration, or result of `DataRuleCommandDTO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `directorySnapshotVersion` 时应保持 `DataRuleCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `directorySnapshotVersion`, preserve `DataRuleCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long directorySnapshotVersion,
            /**
             * 字段 `references` 表示 `DataRuleCommandDTO` 中与 `references` 相关的状态、依赖、配置或结果（声明类型 `List&lt;RuleReferenceVO&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `references` stores the `references`-related state, dependency, configuration, or result of `DataRuleCommandDTO` (declared type `List&lt;RuleReferenceVO&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `references` 时应保持 `DataRuleCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `references`, preserve `DataRuleCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<RuleReferenceVO> references,
            /**
             * 字段 `validFrom` 表示 `DataRuleCommandDTO` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `java.time.Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `DataRuleCommandDTO` (declared type `java.time.Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `DataRuleCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `DataRuleCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            java.time.Instant validFrom,
            /**
             * 字段 `validTo` 表示 `DataRuleCommandDTO` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `java.time.Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `DataRuleCommandDTO` (declared type `java.time.Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `DataRuleCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `DataRuleCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            java.time.Instant validTo,
            /**
             * 字段 `expectedVersion` 表示 `DataRuleCommandDTO` 中与 `expected Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedVersion` stores the `expected Version`-related state, dependency, configuration, or result of `DataRuleCommandDTO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedVersion` 时应保持 `DataRuleCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedVersion`, preserve `DataRuleCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            long expectedVersion,
            /**
             * 字段 `actorId` 表示 `DataRuleCommandDTO` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `DataRuleCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `DataRuleCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `DataRuleCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId
    ) {

        /**
         * 构造器 `DataRuleCommandDTO` 用于创建并初始化 `DataRuleCommandDTO` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `DataRuleCommandDTO` creates and initializes `DataRuleCommandDTO`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `DataRuleCommandDTO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `DataRuleCommandDTO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param ruleId 输入参数 `ruleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param permissionId 输入参数 `permissionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param scopeType 输入参数 `scopeType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param directorySnapshotVersion 输入参数 `directorySnapshotVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param references 输入参数 `references`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expectedVersion 输入参数 `expectedVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public DataRuleCommandDTO {
            references = List.copyOf(references);
        }
    }
