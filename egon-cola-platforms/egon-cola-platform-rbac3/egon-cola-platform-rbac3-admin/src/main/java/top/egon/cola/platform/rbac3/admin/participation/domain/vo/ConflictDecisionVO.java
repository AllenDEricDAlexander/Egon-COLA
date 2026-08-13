package top.egon.cola.platform.rbac3.admin.participation.domain.vo;

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

/**
     * 类型 `ConflictDecisionVO` 位于 `ParticipationFacade` 内，是记录类型，用于承载 `Conflict Decision` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ConflictDecisionVO` is a record inside `ParticipationFacade` and carries the responsibility, state, or contract for `Conflict Decision`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ConflictDecisionVO` 作为 `ParticipationFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ConflictDecisionVO` as the responsibility boundary of `ParticipationFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param allowed 记录组件 `allowed` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `allowed` carries constructor data whose meaning is defined by the record contract.
     * @param reasonCode 记录组件 `reasonCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `reasonCode` carries constructor data whose meaning is defined by the record contract.
     * @param evidenceIds 记录组件 `evidenceIds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `evidenceIds` carries constructor data whose meaning is defined by the record contract.
     * @param conflictingPriorActions 记录组件 `conflictingPriorActions` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `conflictingPriorActions` carries constructor data whose meaning is defined by the record contract.
     */
    public record ConflictDecisionVO(
            /**
             * 字段 `allowed` 表示 `ConflictDecisionVO` 中与 `allowed` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `allowed` stores the `allowed`-related state, dependency, configuration, or result of `ConflictDecisionVO` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `allowed` 时应保持 `ConflictDecisionVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `allowed`, preserve `ConflictDecisionVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean allowed,
            /**
             * 字段 `reasonCode` 表示 `ConflictDecisionVO` 中与 `reason Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reasonCode` stores the `reason Code`-related state, dependency, configuration, or result of `ConflictDecisionVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reasonCode` 时应保持 `ConflictDecisionVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reasonCode`, preserve `ConflictDecisionVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String reasonCode,
            /**
             * 字段 `evidenceIds` 表示 `ConflictDecisionVO` 中与 `evidence Ids` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `evidenceIds` stores the `evidence Ids`-related state, dependency, configuration, or result of `ConflictDecisionVO` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `evidenceIds` 时应保持 `ConflictDecisionVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `evidenceIds`, preserve `ConflictDecisionVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<String> evidenceIds,
            /**
             * 字段 `conflictingPriorActions` 表示 `ConflictDecisionVO` 中与 `conflicting Prior Actions` 相关的状态、依赖、配置或结果（声明类型 `Set&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `conflictingPriorActions` stores the `conflicting Prior Actions`-related state, dependency, configuration, or result of `ConflictDecisionVO` (declared type `Set&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `conflictingPriorActions` 时应保持 `ConflictDecisionVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `conflictingPriorActions`, preserve `ConflictDecisionVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Set<String> conflictingPriorActions) {
    }
