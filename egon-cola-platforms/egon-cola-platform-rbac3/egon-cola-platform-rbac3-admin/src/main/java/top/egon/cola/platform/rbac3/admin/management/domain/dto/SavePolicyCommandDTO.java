package top.egon.cola.platform.rbac3.admin.management.domain.dto;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.core.delegation.ManagementPolicyDecisionService;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import top.egon.cola.platform.rbac3.admin.management.domain.po.ManagementPolicySubjectPO;
import top.egon.cola.platform.rbac3.admin.management.domain.po.ManagementPolicyScopePO;

/**
     * 类型 `SavePolicyCommandDTO` 位于 `ManagementPolicyRepository` 内，是记录类型，用于承载 `Save Policy Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SavePolicyCommandDTO` is a record inside `ManagementPolicyRepository` and carries the responsibility, state, or contract for `Save Policy Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SavePolicyCommandDTO` 作为 `ManagementPolicyRepository` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SavePolicyCommandDTO` as the responsibility boundary of `ManagementPolicyRepository`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param policyId 记录组件 `policyId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyId` carries constructor data whose meaning is defined by the record contract.
     * @param policyCode 记录组件 `policyCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyCode` carries constructor data whose meaning is defined by the record contract.
     * @param name 记录组件 `name` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `name` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     * @param maximumAssignmentDays 记录组件 `maximumAssignmentDays` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `maximumAssignmentDays` carries constructor data whose meaning is defined by the record contract.
     * @param maximumRiskLevel 记录组件 `maximumRiskLevel` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `maximumRiskLevel` carries constructor data whose meaning is defined by the record contract.
     * @param requiredAuthenticationStrength 记录组件 `requiredAuthenticationStrength` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `requiredAuthenticationStrength` carries constructor data whose meaning is defined by the record contract.
     * @param requireReason 记录组件 `requireReason` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `requireReason` carries constructor data whose meaning is defined by the record contract.
     * @param requireTicket 记录组件 `requireTicket` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `requireTicket` carries constructor data whose meaning is defined by the record contract.
     * @param includeInheritedSubjectRoles 记录组件 `includeInheritedSubjectRoles` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `includeInheritedSubjectRoles` carries constructor data whose meaning is defined by the record contract.
     * @param requireAllAffiliationsInScope 记录组件 `requireAllAffiliationsInScope` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `requireAllAffiliationsInScope` carries constructor data whose meaning is defined by the record contract.
     * @param subjects 记录组件 `subjects` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `subjects` carries constructor data whose meaning is defined by the record contract.
     * @param scopes 记录组件 `scopes` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopes` carries constructor data whose meaning is defined by the record contract.
     * @param activationRootRoleIds 记录组件 `activationRootRoleIds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `activationRootRoleIds` carries constructor data whose meaning is defined by the record contract.
     * @param operations 记录组件 `operations` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `operations` carries constructor data whose meaning is defined by the record contract.
     * @param expectedVersion 记录组件 `expectedVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedVersion` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     */
    public record SavePolicyCommandDTO(
            /**
             * 字段 `tenantId` 表示 `SavePolicyCommandDTO` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `SavePolicyCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `SavePolicyCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `SavePolicyCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `policyId` 表示 `SavePolicyCommandDTO` 中与 `policy Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyId` stores the `policy Id`-related state, dependency, configuration, or result of `SavePolicyCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyId` 时应保持 `SavePolicyCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyId`, preserve `SavePolicyCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String policyId,
            /**
             * 字段 `policyCode` 表示 `SavePolicyCommandDTO` 中与 `policy Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyCode` stores the `policy Code`-related state, dependency, configuration, or result of `SavePolicyCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyCode` 时应保持 `SavePolicyCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyCode`, preserve `SavePolicyCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String policyCode,
            /**
             * 字段 `name` 表示 `SavePolicyCommandDTO` 中与 `name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `name` stores the `name`-related state, dependency, configuration, or result of `SavePolicyCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `name` 时应保持 `SavePolicyCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `name`, preserve `SavePolicyCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String name,
            /**
             * 字段 `validFrom` 表示 `SavePolicyCommandDTO` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `SavePolicyCommandDTO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `SavePolicyCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `SavePolicyCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validFrom,
            /**
             * 字段 `validTo` 表示 `SavePolicyCommandDTO` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `SavePolicyCommandDTO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `SavePolicyCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `SavePolicyCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validTo,
            /**
             * 字段 `maximumAssignmentDays` 表示 `SavePolicyCommandDTO` 中与 `maximum Assignment Days` 相关的状态、依赖、配置或结果（声明类型 `Integer`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `maximumAssignmentDays` stores the `maximum Assignment Days`-related state, dependency, configuration, or result of `SavePolicyCommandDTO` (declared type `Integer`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `maximumAssignmentDays` 时应保持 `SavePolicyCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `maximumAssignmentDays`, preserve `SavePolicyCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Integer maximumAssignmentDays,
            /**
             * 字段 `maximumRiskLevel` 表示 `SavePolicyCommandDTO` 中与 `maximum Risk Level` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `maximumRiskLevel` stores the `maximum Risk Level`-related state, dependency, configuration, or result of `SavePolicyCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `maximumRiskLevel` 时应保持 `SavePolicyCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `maximumRiskLevel`, preserve `SavePolicyCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String maximumRiskLevel,
            /**
             * 字段 `requiredAuthenticationStrength` 表示 `SavePolicyCommandDTO` 中与 `required Authentication Strength` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requiredAuthenticationStrength` stores the `required Authentication Strength`-related state, dependency, configuration, or result of `SavePolicyCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requiredAuthenticationStrength` 时应保持 `SavePolicyCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requiredAuthenticationStrength`, preserve `SavePolicyCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String requiredAuthenticationStrength,
            /**
             * 字段 `requireReason` 表示 `SavePolicyCommandDTO` 中与 `require Reason` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requireReason` stores the `require Reason`-related state, dependency, configuration, or result of `SavePolicyCommandDTO` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requireReason` 时应保持 `SavePolicyCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requireReason`, preserve `SavePolicyCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean requireReason,
            /**
             * 字段 `requireTicket` 表示 `SavePolicyCommandDTO` 中与 `require Ticket` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requireTicket` stores the `require Ticket`-related state, dependency, configuration, or result of `SavePolicyCommandDTO` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requireTicket` 时应保持 `SavePolicyCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requireTicket`, preserve `SavePolicyCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean requireTicket,
            /**
             * 字段 `includeInheritedSubjectRoles` 表示 `SavePolicyCommandDTO` 中与 `include Inherited ManagementPolicySubjectPO Roles` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `includeInheritedSubjectRoles` stores the `include Inherited ManagementPolicySubjectPO Roles`-related state, dependency, configuration, or result of `SavePolicyCommandDTO` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `includeInheritedSubjectRoles` 时应保持 `SavePolicyCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `includeInheritedSubjectRoles`, preserve `SavePolicyCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean includeInheritedSubjectRoles,
            /**
             * 字段 `requireAllAffiliationsInScope` 表示 `SavePolicyCommandDTO` 中与 `require All Affiliations In ManagementPolicyScopePO` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requireAllAffiliationsInScope` stores the `require All Affiliations In ManagementPolicyScopePO`-related state, dependency, configuration, or result of `SavePolicyCommandDTO` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requireAllAffiliationsInScope` 时应保持 `SavePolicyCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requireAllAffiliationsInScope`, preserve `SavePolicyCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean requireAllAffiliationsInScope,
            /**
             * 字段 `subjects` 表示 `SavePolicyCommandDTO` 中与 `subjects` 相关的状态、依赖、配置或结果（声明类型 `List&lt;ManagementPolicySubjectPO&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `subjects` stores the `subjects`-related state, dependency, configuration, or result of `SavePolicyCommandDTO` (declared type `List&lt;ManagementPolicySubjectPO&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `subjects` 时应保持 `SavePolicyCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `subjects`, preserve `SavePolicyCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<ManagementPolicySubjectPO> subjects,
            /**
             * 字段 `scopes` 表示 `SavePolicyCommandDTO` 中与 `scopes` 相关的状态、依赖、配置或结果（声明类型 `List&lt;ManagementPolicyScopePO&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopes` stores the `scopes`-related state, dependency, configuration, or result of `SavePolicyCommandDTO` (declared type `List&lt;ManagementPolicyScopePO&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopes` 时应保持 `SavePolicyCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopes`, preserve `SavePolicyCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<ManagementPolicyScopePO> scopes,
            /**
             * 字段 `activationRootRoleIds` 表示 `SavePolicyCommandDTO` 中与 `activation Root Role Ids` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `activationRootRoleIds` stores the `activation Root Role Ids`-related state, dependency, configuration, or result of `SavePolicyCommandDTO` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `activationRootRoleIds` 时应保持 `SavePolicyCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `activationRootRoleIds`, preserve `SavePolicyCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<String> activationRootRoleIds,
            /**
             * 字段 `operations` 表示 `SavePolicyCommandDTO` 中与 `operations` 相关的状态、依赖、配置或结果（声明类型 `Set&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `operations` stores the `operations`-related state, dependency, configuration, or result of `SavePolicyCommandDTO` (declared type `Set&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `operations` 时应保持 `SavePolicyCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `operations`, preserve `SavePolicyCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Set<String> operations,
            /**
             * 字段 `expectedVersion` 表示 `SavePolicyCommandDTO` 中与 `expected Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedVersion` stores the `expected Version`-related state, dependency, configuration, or result of `SavePolicyCommandDTO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedVersion` 时应保持 `SavePolicyCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedVersion`, preserve `SavePolicyCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            long expectedVersion,
            /**
             * 字段 `actorId` 表示 `SavePolicyCommandDTO` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `SavePolicyCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `SavePolicyCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `SavePolicyCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId
    ) {
        /**
         * 构造器 `SavePolicyCommandDTO` 用于创建并初始化 `SavePolicyCommandDTO` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `SavePolicyCommandDTO` creates and initializes `SavePolicyCommandDTO`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `SavePolicyCommandDTO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `SavePolicyCommandDTO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param policyId 输入参数 `policyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param policyCode 输入参数 `policyCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param maximumAssignmentDays 输入参数 `maximumAssignmentDays`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param maximumRiskLevel 输入参数 `maximumRiskLevel`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param requiredAuthenticationStrength 输入参数 `requiredAuthenticationStrength`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param requireReason 输入参数 `requireReason`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param requireTicket 输入参数 `requireTicket`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param includeInheritedSubjectRoles 输入参数 `includeInheritedSubjectRoles`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param requireAllAffiliationsInScope 输入参数 `requireAllAffiliationsInScope`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param subjects 输入参数 `subjects`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param scopes 输入参数 `scopes`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param activationRootRoleIds 输入参数 `activationRootRoleIds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param operations 输入参数 `operations`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expectedVersion 输入参数 `expectedVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public SavePolicyCommandDTO {
            subjects = List.copyOf(subjects);
            scopes = List.copyOf(scopes);
            activationRootRoleIds = List.copyOf(activationRootRoleIds);
            operations = Set.copyOf(operations);
        }
    }
