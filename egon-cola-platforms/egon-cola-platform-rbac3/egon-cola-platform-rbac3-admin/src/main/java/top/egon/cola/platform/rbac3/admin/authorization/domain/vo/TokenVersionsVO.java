package top.egon.cola.platform.rbac3.admin.authorization.domain.vo;

import top.egon.cola.platform.rbac3.admin.config.security.CurrentRbac3Principal;
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.authorization.AppAuthorizationContext;
import top.egon.cola.platform.rbac3.contract.authorization.AuthorizationDecision;
import top.egon.cola.platform.rbac3.contract.authorization.DataScopeDecision;
import top.egon.cola.platform.rbac3.contract.authorization.Decision;
import top.egon.cola.platform.rbac3.contract.authorization.FieldPolicyDecision;
import top.egon.cola.platform.rbac3.contract.authorization.SessionAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import top.egon.cola.platform.rbac3.admin.authorization.service.AuthorizationDecisionService;

/**
     * Token 携带的用户、会话和策略授权版本。
     * User, session, and policy authorization versions carried by a token.
     *
     * @param authVersion 用户授权版本 / user authorization version
     * @param sessionVersion 会话版本 / session version
     * @param policyVersion 策略版本 / policy version
     * 语义与用法：将 `TokenVersionsVO` 作为 `AuthorizationDecisionService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `TokenVersionsVO` as the responsibility boundary of `AuthorizationDecisionService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public record TokenVersionsVO(/**
 * 字段 `authVersion` 表示 `TokenVersionsVO` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `TokenVersionsVO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `TokenVersionsVO` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `TokenVersionsVO`'s lifecycle, immutability, and thread-safety constraints.
 */ long authVersion, /**
 * 字段 `sessionVersion` 表示 `TokenVersionsVO` 中与 `session Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `sessionVersion` stores the `session Version`-related state, dependency, configuration, or result of `TokenVersionsVO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `sessionVersion` 时应保持 `TokenVersionsVO` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `sessionVersion`, preserve `TokenVersionsVO`'s lifecycle, immutability, and thread-safety constraints.
 */ long sessionVersion, /**
 * 字段 `policyVersion` 表示 `TokenVersionsVO` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `TokenVersionsVO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `TokenVersionsVO` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `TokenVersionsVO`'s lifecycle, immutability, and thread-safety constraints.
 */ long policyVersion) {

        /**
         * 校验授权版本均为非负数。
         * Validates that all authorization versions are non-negative.
         * 用法：通过 `TokenVersionsVO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `TokenVersionsVO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         * @param authVersion 输入参数 `authVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionVersion 输入参数 `sessionVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param policyVersion 输入参数 `policyVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public TokenVersionsVO {
            if (authVersion < 0 || sessionVersion < 0 || policyVersion < 0) {
                throw new IllegalArgumentException("token versions must not be negative");
            }
        }
    }
