package top.egon.cola.platform.rbac3.admin.authorization.domain.vo;

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

/**
     * 类型化授权判定的用户主体定位信息。
     * User-subject locator for a typed authorization decision.
     *
     * @param tenantId 租户标识 / tenant identifier
     * @param userId RBAC 用户标识 / RBAC user identifier
     * @param sessionId 会话标识 / session identifier
     * 语义与用法：将 `AuthorizationDecisionSubjectVO` 作为 `AuthorizationDecisionService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AuthorizationDecisionSubjectVO` as the responsibility boundary of `AuthorizationDecisionService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public record AuthorizationDecisionSubjectVO(/**
 * 字段 `tenantId` 表示 `AuthorizationDecisionSubjectVO` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `AuthorizationDecisionSubjectVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `AuthorizationDecisionSubjectVO` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `AuthorizationDecisionSubjectVO`'s lifecycle, immutability, and thread-safety constraints.
 */ String tenantId, /**
 * 字段 `userId` 表示 `AuthorizationDecisionSubjectVO` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `AuthorizationDecisionSubjectVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `userId` 时应保持 `AuthorizationDecisionSubjectVO` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `userId`, preserve `AuthorizationDecisionSubjectVO`'s lifecycle, immutability, and thread-safety constraints.
 */ String userId, /**
 * 字段 `sessionId` 表示 `AuthorizationDecisionSubjectVO` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `AuthorizationDecisionSubjectVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `AuthorizationDecisionSubjectVO` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `AuthorizationDecisionSubjectVO`'s lifecycle, immutability, and thread-safety constraints.
 */ String sessionId) {

        /**
         * 校验并规范化主体定位信息。
         * Validates and normalizes the subject locator.
         * 用法：通过 `AuthorizationDecisionSubjectVO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `AuthorizationDecisionSubjectVO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public AuthorizationDecisionSubjectVO {
            tenantId = required(tenantId, "tenantId");
            userId = required(userId, "userId");
            sessionId = required(sessionId, "sessionId");
        }

        /** 校验必填文本。 / Validates required text. */
        private static String required(String value, String fieldName) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(fieldName + " is required");
            }
            return value.trim();
        }
    }
