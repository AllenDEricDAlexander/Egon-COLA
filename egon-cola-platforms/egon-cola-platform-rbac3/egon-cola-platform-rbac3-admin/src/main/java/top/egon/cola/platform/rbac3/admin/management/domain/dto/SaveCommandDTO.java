package top.egon.cola.platform.rbac3.admin.management.domain.dto;

import top.egon.cola.platform.rbac3.core.delegation.ManagementPolicyDecisionService;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import top.egon.cola.platform.rbac3.admin.management.domain.vo.ManagementPolicyRestrictionsVO;
import top.egon.cola.platform.rbac3.admin.management.domain.vo.ManagementPolicySubjectVO;
import top.egon.cola.platform.rbac3.admin.management.domain.vo.ManagementPolicyScopeVO;

/**
     * 类型 `SaveCommandDTO` 位于 `ManagementPolicyFacade` 内，是记录类型，用于承载 `Save Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SaveCommandDTO` is a record inside `ManagementPolicyFacade` and carries the responsibility, state, or contract for `Save Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SaveCommandDTO` 作为 `ManagementPolicyFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SaveCommandDTO` as the responsibility boundary of `ManagementPolicyFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param policyId 记录组件 `policyId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyId` carries constructor data whose meaning is defined by the record contract.
     * @param policyCode 记录组件 `policyCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyCode` carries constructor data whose meaning is defined by the record contract.
     * @param name 记录组件 `name` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `name` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     * @param restrictions 记录组件 `restrictions` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `restrictions` carries constructor data whose meaning is defined by the record contract.
     * @param subjects 记录组件 `subjects` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `subjects` carries constructor data whose meaning is defined by the record contract.
     * @param scopes 记录组件 `scopes` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopes` carries constructor data whose meaning is defined by the record contract.
     * @param activationRootRoleIds 记录组件 `activationRootRoleIds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `activationRootRoleIds` carries constructor data whose meaning is defined by the record contract.
     * @param operations 记录组件 `operations` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `operations` carries constructor data whose meaning is defined by the record contract.
     * @param expectedVersion 记录组件 `expectedVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedVersion` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     */
    public record SaveCommandDTO(
            /**
             * 字段 `tenantId` 表示 `SaveCommandDTO` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `SaveCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `SaveCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `SaveCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `policyId` 表示 `SaveCommandDTO` 中与 `policy Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyId` stores the `policy Id`-related state, dependency, configuration, or result of `SaveCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyId` 时应保持 `SaveCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyId`, preserve `SaveCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String policyId,
            /**
             * 字段 `policyCode` 表示 `SaveCommandDTO` 中与 `policy Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyCode` stores the `policy Code`-related state, dependency, configuration, or result of `SaveCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyCode` 时应保持 `SaveCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyCode`, preserve `SaveCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String policyCode,
            /**
             * 字段 `name` 表示 `SaveCommandDTO` 中与 `name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `name` stores the `name`-related state, dependency, configuration, or result of `SaveCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `name` 时应保持 `SaveCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `name`, preserve `SaveCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String name,
            /**
             * 字段 `validFrom` 表示 `SaveCommandDTO` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `SaveCommandDTO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `SaveCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `SaveCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validFrom,
            /**
             * 字段 `validTo` 表示 `SaveCommandDTO` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `SaveCommandDTO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `SaveCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `SaveCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validTo,
            /**
             * 字段 `restrictions` 表示 `SaveCommandDTO` 中与 `restrictions` 相关的状态、依赖、配置或结果（声明类型 `ManagementPolicyRestrictionsVO`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `restrictions` stores the `restrictions`-related state, dependency, configuration, or result of `SaveCommandDTO` (declared type `ManagementPolicyRestrictionsVO`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `restrictions` 时应保持 `SaveCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `restrictions`, preserve `SaveCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            ManagementPolicyRestrictionsVO restrictions,
            /**
             * 字段 `subjects` 表示 `SaveCommandDTO` 中与 `subjects` 相关的状态、依赖、配置或结果（声明类型 `List&lt;ManagementPolicySubjectVO&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `subjects` stores the `subjects`-related state, dependency, configuration, or result of `SaveCommandDTO` (declared type `List&lt;ManagementPolicySubjectVO&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `subjects` 时应保持 `SaveCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `subjects`, preserve `SaveCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<ManagementPolicySubjectVO> subjects,
            /**
             * 字段 `scopes` 表示 `SaveCommandDTO` 中与 `scopes` 相关的状态、依赖、配置或结果（声明类型 `List&lt;ManagementPolicyScopeVO&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopes` stores the `scopes`-related state, dependency, configuration, or result of `SaveCommandDTO` (declared type `List&lt;ManagementPolicyScopeVO&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopes` 时应保持 `SaveCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopes`, preserve `SaveCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<ManagementPolicyScopeVO> scopes,
            /**
             * 字段 `activationRootRoleIds` 表示 `SaveCommandDTO` 中与 `activation Root Role Ids` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `activationRootRoleIds` stores the `activation Root Role Ids`-related state, dependency, configuration, or result of `SaveCommandDTO` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `activationRootRoleIds` 时应保持 `SaveCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `activationRootRoleIds`, preserve `SaveCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<String> activationRootRoleIds,
            /**
             * 字段 `operations` 表示 `SaveCommandDTO` 中与 `operations` 相关的状态、依赖、配置或结果（声明类型 `Set&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `operations` stores the `operations`-related state, dependency, configuration, or result of `SaveCommandDTO` (declared type `Set&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `operations` 时应保持 `SaveCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `operations`, preserve `SaveCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Set<String> operations,
            /**
             * 字段 `expectedVersion` 表示 `SaveCommandDTO` 中与 `expected Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedVersion` stores the `expected Version`-related state, dependency, configuration, or result of `SaveCommandDTO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedVersion` 时应保持 `SaveCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedVersion`, preserve `SaveCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            long expectedVersion,
            /**
             * 字段 `actorId` 表示 `SaveCommandDTO` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `SaveCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `SaveCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `SaveCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId
    ) {
        /**
         * 构造器 `SaveCommandDTO` 用于创建并初始化 `SaveCommandDTO` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `SaveCommandDTO` creates and initializes `SaveCommandDTO`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `SaveCommandDTO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `SaveCommandDTO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param policyId 输入参数 `policyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param policyCode 输入参数 `policyCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param restrictions 输入参数 `restrictions`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param subjects 输入参数 `subjects`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param scopes 输入参数 `scopes`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param activationRootRoleIds 输入参数 `activationRootRoleIds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param operations 输入参数 `operations`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expectedVersion 输入参数 `expectedVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public SaveCommandDTO {
            restrictions = Objects.requireNonNull(restrictions, "restrictions");
            subjects = List.copyOf(subjects);
            scopes = List.copyOf(scopes);
            activationRootRoleIds = List.copyOf(activationRootRoleIds);
            operations = Set.copyOf(operations);
            if (subjects.isEmpty() || scopes.isEmpty()
                    || activationRootRoleIds.isEmpty() || operations.isEmpty()) {
                throw new IllegalArgumentException(
                        "management policy subjects, scopes, roles and operations are required");
            }
        }
    }
