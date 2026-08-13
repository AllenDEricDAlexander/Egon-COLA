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
import top.egon.cola.platform.rbac3.admin.participation.service.ParticipationFacade;

/**
     * 类型 `PriorActionRuleVO` 位于 `ParticipationFacade` 内，是记录类型，用于承载 `Prior Action Rule` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `PriorActionRuleVO` is a record inside `ParticipationFacade` and carries the responsibility, state, or contract for `Prior Action Rule`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `PriorActionRuleVO` 作为 `ParticipationFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `PriorActionRuleVO` as the responsibility boundary of `ParticipationFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param ruleId 记录组件 `ruleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `ruleId` carries constructor data whose meaning is defined by the record contract.
     * @param actionCode 记录组件 `actionCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actionCode` carries constructor data whose meaning is defined by the record contract.
     * @param lookbackFrom 记录组件 `lookbackFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `lookbackFrom` carries constructor data whose meaning is defined by the record contract.
     */
    public record PriorActionRuleVO(
            /**
             * 字段 `ruleId` 表示 `PriorActionRuleVO` 中与 `rule Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `ruleId` stores the `rule Id`-related state, dependency, configuration, or result of `PriorActionRuleVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `ruleId` 时应保持 `PriorActionRuleVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `ruleId`, preserve `PriorActionRuleVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String ruleId,
            /**
             * 字段 `actionCode` 表示 `PriorActionRuleVO` 中与 `action Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actionCode` stores the `action Code`-related state, dependency, configuration, or result of `PriorActionRuleVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actionCode` 时应保持 `PriorActionRuleVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actionCode`, preserve `PriorActionRuleVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actionCode,
            /**
             * 字段 `lookbackFrom` 表示 `PriorActionRuleVO` 中与 `lookback From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `lookbackFrom` stores the `lookback From`-related state, dependency, configuration, or result of `PriorActionRuleVO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `lookbackFrom` 时应保持 `PriorActionRuleVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `lookbackFrom`, preserve `PriorActionRuleVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant lookbackFrom) {
        /**
         * 构造器 `PriorActionRuleVO` 用于创建并初始化 `PriorActionRuleVO` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `PriorActionRuleVO` creates and initializes `PriorActionRuleVO`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `PriorActionRuleVO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `PriorActionRuleVO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param ruleId 输入参数 `ruleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actionCode 输入参数 `actionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param lookbackFrom 输入参数 `lookbackFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public PriorActionRuleVO {
            if (ruleId == null || ruleId.isBlank()) {
                throw new IllegalArgumentException("ruleId is required");
            }
            if (actionCode == null || actionCode.isBlank()) {
                throw new IllegalArgumentException("actionCode is required");
            }
            lookbackFrom = Objects.requireNonNull(lookbackFrom, "lookbackFrom");
        }
    }
