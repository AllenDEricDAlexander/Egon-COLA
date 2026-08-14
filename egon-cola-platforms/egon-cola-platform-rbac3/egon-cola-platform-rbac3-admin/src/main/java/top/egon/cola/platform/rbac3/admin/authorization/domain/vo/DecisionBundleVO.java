package top.egon.cola.platform.rbac3.admin.authorization.domain.vo;

import top.egon.cola.platform.rbac3.contract.authorization.AuthorizationDecision;
import top.egon.cola.platform.rbac3.contract.authorization.DataScopeDecision;
import top.egon.cola.platform.rbac3.contract.authorization.FieldPolicyDecision;

/**
     * 函数、数据范围和字段策略的类型化判定组合。
     * Typed bundle of function, data-scope, and field-policy decisions.
     *
     * @param functionDecision 函数权限判定 / function-permission decision
     * @param dataScopeDecision 可选数据范围判定 / optional data-scope decision
     * @param fieldPolicyDecision 可选字段策略判定 / optional field-policy decision
     * @param snapshotChecksum 判定快照校验和 / decision snapshot checksum
     * 语义与用法：将 `DecisionBundleVO` 作为 `AuthorizationDecisionService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `DecisionBundleVO` as the responsibility boundary of `AuthorizationDecisionService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public record DecisionBundleVO(
            /**
             * 字段 `functionDecision` 表示 `DecisionBundleVO` 中与 `function Decision` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationDecision`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `functionDecision` stores the `function Decision`-related state, dependency, configuration, or result of `DecisionBundleVO` (declared type `AuthorizationDecision`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `functionDecision` 时应保持 `DecisionBundleVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `functionDecision`, preserve `DecisionBundleVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            AuthorizationDecision functionDecision,
            /**
             * 字段 `dataScopeDecision` 表示 `DecisionBundleVO` 中与 `data Scope Decision` 相关的状态、依赖、配置或结果（声明类型 `DataScopeDecision`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `dataScopeDecision` stores the `data Scope Decision`-related state, dependency, configuration, or result of `DecisionBundleVO` (declared type `DataScopeDecision`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `dataScopeDecision` 时应保持 `DecisionBundleVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `dataScopeDecision`, preserve `DecisionBundleVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            DataScopeDecision dataScopeDecision,
            /**
             * 字段 `fieldPolicyDecision` 表示 `DecisionBundleVO` 中与 `field Policy Decision` 相关的状态、依赖、配置或结果（声明类型 `FieldPolicyDecision`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `fieldPolicyDecision` stores the `field Policy Decision`-related state, dependency, configuration, or result of `DecisionBundleVO` (declared type `FieldPolicyDecision`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `fieldPolicyDecision` 时应保持 `DecisionBundleVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `fieldPolicyDecision`, preserve `DecisionBundleVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            FieldPolicyDecision fieldPolicyDecision,
            /**
             * 字段 `snapshotChecksum` 表示 `DecisionBundleVO` 中与 `snapshot Checksum` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `snapshotChecksum` stores the `snapshot Checksum`-related state, dependency, configuration, or result of `DecisionBundleVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `snapshotChecksum` 时应保持 `DecisionBundleVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `snapshotChecksum`, preserve `DecisionBundleVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String snapshotChecksum) {
    }
