package top.egon.cola.platform.rbac3.admin.activation.domain.vo;

import top.egon.cola.platform.rbac3.contract.activation.RoleActivationCandidate;
import top.egon.cola.platform.rbac3.contract.activation.RoleActivationCandidateView;
import top.egon.cola.platform.rbac3.core.activation.AuthorizationRuleFacts;
import top.egon.cola.platform.rbac3.core.activation.DsdSetFact;
import top.egon.cola.platform.rbac3.core.activation.EligibleAssignmentFact;
import top.egon.cola.platform.rbac3.core.activation.RoleActivationCandidateResolver;
import top.egon.cola.platform.rbac3.core.activation.UniqueActivationRootSpecification;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
     * 类型 `ActivationFactsVO` 位于 `RoleActivationCandidateService` 内，是记录类型，用于承载 `Activation Facts` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ActivationFactsVO` is a record inside `RoleActivationCandidateService` and carries the responsibility, state, or contract for `Activation Facts`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ActivationFactsVO` 作为 `RoleActivationCandidateService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ActivationFactsVO` as the responsibility boundary of `RoleActivationCandidateService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param userId 记录组件 `userId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `userId` carries constructor data whose meaning is defined by the record contract.
     * @param hierarchy 记录组件 `hierarchy` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `hierarchy` carries constructor data whose meaning is defined by the record contract.
     * @param assignments 记录组件 `assignments` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `assignments` carries constructor data whose meaning is defined by the record contract.
     * @param dsdSets 记录组件 `dsdSets` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `dsdSets` carries constructor data whose meaning is defined by the record contract.
     * @param authorizationFacts 记录组件 `authorizationFacts` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authorizationFacts` carries constructor data whose meaning is defined by the record contract.
     * @param authVersion 记录组件 `authVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authVersion` carries constructor data whose meaning is defined by the record contract.
     * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
     * @param directorySnapshotVersion 记录组件 `directorySnapshotVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `directorySnapshotVersion` carries constructor data whose meaning is defined by the record contract.
     * @param applications 记录组件 `applications` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applications` carries constructor data whose meaning is defined by the record contract.
     * @param roleDisplayNames 记录组件 `roleDisplayNames` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleDisplayNames` carries constructor data whose meaning is defined by the record contract.
     */
    public record ActivationFactsVO(
            /**
             * 字段 `tenantId` 表示 `ActivationFactsVO` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `ActivationFactsVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `ActivationFactsVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `ActivationFactsVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `userId` 表示 `ActivationFactsVO` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `ActivationFactsVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userId` 时应保持 `ActivationFactsVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userId`, preserve `ActivationFactsVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String userId,
            /**
             * 字段 `hierarchy` 表示 `ActivationFactsVO` 中与 `hierarchy` 相关的状态、依赖、配置或结果（声明类型 `RoleHierarchy`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `hierarchy` stores the `hierarchy`-related state, dependency, configuration, or result of `ActivationFactsVO` (declared type `RoleHierarchy`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `hierarchy` 时应保持 `ActivationFactsVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `hierarchy`, preserve `ActivationFactsVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            RoleHierarchy hierarchy,
            /**
             * 字段 `assignments` 表示 `ActivationFactsVO` 中与 `assignments` 相关的状态、依赖、配置或结果（声明类型 `List&lt;EligibleAssignmentFact&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `assignments` stores the `assignments`-related state, dependency, configuration, or result of `ActivationFactsVO` (declared type `List&lt;EligibleAssignmentFact&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `assignments` 时应保持 `ActivationFactsVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `assignments`, preserve `ActivationFactsVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<EligibleAssignmentFact> assignments,
            /**
             * 字段 `dsdSets` 表示 `ActivationFactsVO` 中与 `dsd Sets` 相关的状态、依赖、配置或结果（声明类型 `List&lt;DsdSetFact&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `dsdSets` stores the `dsd Sets`-related state, dependency, configuration, or result of `ActivationFactsVO` (declared type `List&lt;DsdSetFact&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `dsdSets` 时应保持 `ActivationFactsVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `dsdSets`, preserve `ActivationFactsVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<DsdSetFact> dsdSets,
            /**
             * 字段 `authorizationFacts` 表示 `ActivationFactsVO` 中与 `authorization Facts` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationRuleFacts`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authorizationFacts` stores the `authorization Facts`-related state, dependency, configuration, or result of `ActivationFactsVO` (declared type `AuthorizationRuleFacts`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authorizationFacts` 时应保持 `ActivationFactsVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authorizationFacts`, preserve `ActivationFactsVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            AuthorizationRuleFacts authorizationFacts,
            /**
             * 字段 `authVersion` 表示 `ActivationFactsVO` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `ActivationFactsVO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `ActivationFactsVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `ActivationFactsVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            long authVersion,
            /**
             * 字段 `policyVersion` 表示 `ActivationFactsVO` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `ActivationFactsVO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `ActivationFactsVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `ActivationFactsVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            long policyVersion,
            /**
             * 字段 `directorySnapshotVersion` 表示 `ActivationFactsVO` 中与 `directory Snapshot Version` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `directorySnapshotVersion` stores the `directory Snapshot Version`-related state, dependency, configuration, or result of `ActivationFactsVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `directorySnapshotVersion` 时应保持 `ActivationFactsVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `directorySnapshotVersion`, preserve `ActivationFactsVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String directorySnapshotVersion,
            /**
             * 字段 `applications` 表示 `ActivationFactsVO` 中与 `applications` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, ApplicationFactVO&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applications` stores the `applications`-related state, dependency, configuration, or result of `ActivationFactsVO` (declared type `Map&lt;String, ApplicationFactVO&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applications` 时应保持 `ActivationFactsVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applications`, preserve `ActivationFactsVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Map<String, ApplicationFactVO> applications,
            /**
             * 字段 `roleDisplayNames` 表示 `ActivationFactsVO` 中与 `role Display Names` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleDisplayNames` stores the `role Display Names`-related state, dependency, configuration, or result of `ActivationFactsVO` (declared type `Map&lt;String, String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleDisplayNames` 时应保持 `ActivationFactsVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleDisplayNames`, preserve `ActivationFactsVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Map<String, String> roleDisplayNames
    ) {

        /**
         * 构造器 `ActivationFactsVO` 用于创建并初始化 `ActivationFactsVO` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `ActivationFactsVO` creates and initializes `ActivationFactsVO`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `ActivationFactsVO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `ActivationFactsVO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param hierarchy 输入参数 `hierarchy`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param assignments 输入参数 `assignments`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param dsdSets 输入参数 `dsdSets`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param authorizationFacts 输入参数 `authorizationFacts`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param authVersion 输入参数 `authVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param policyVersion 输入参数 `policyVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param directorySnapshotVersion 输入参数 `directorySnapshotVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applications 输入参数 `applications`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param roleDisplayNames 输入参数 `roleDisplayNames`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public ActivationFactsVO {
            Objects.requireNonNull(tenantId, "tenantId");
            Objects.requireNonNull(userId, "userId");
            Objects.requireNonNull(hierarchy, "hierarchy");
            assignments = List.copyOf(assignments);
            dsdSets = List.copyOf(dsdSets);
            Objects.requireNonNull(authorizationFacts, "authorizationFacts");
            Objects.requireNonNull(directorySnapshotVersion, "directorySnapshotVersion");
            applications = Map.copyOf(applications);
            roleDisplayNames = Map.copyOf(roleDisplayNames);
            if (authVersion < 0 || policyVersion < 0) {
                throw new IllegalArgumentException("versions must not be negative");
            }
        }
    }
