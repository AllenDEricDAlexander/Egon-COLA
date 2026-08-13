package top.egon.cola.platform.rbac3.admin.authorization.domain.dto;

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
     * 用户 AuthorizationDecisionResourceVO Server 入口判定请求。
     * User AuthorizationDecisionResourceVO Server entry-decision request.
     *
     * @param identitySub IdP 稳定用户主体标识 / stable IdP user subject
     * @param tenantId 租户标识 / tenant identifier
     * @param sessionId IdP 会话标识 / IdP session identifier
     * @param rbacApplicationCode 目标 RBAC3 应用编码 / target RBAC3 application code
     * @param entryPermissionCode 应用入口权限编码 / application entry permission code
     * 语义与用法：将 `ResourceAccessRequestDTO` 作为 `AuthorizationDecisionService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ResourceAccessRequestDTO` as the responsibility boundary of `AuthorizationDecisionService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public record ResourceAccessRequestDTO(
            /**
             * 字段 `identitySub` 表示 `ResourceAccessRequestDTO` 中与 `identity Sub` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `identitySub` stores the `identity Sub`-related state, dependency, configuration, or result of `ResourceAccessRequestDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `identitySub` 时应保持 `ResourceAccessRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `identitySub`, preserve `ResourceAccessRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String identitySub,
            /**
             * 字段 `tenantId` 表示 `ResourceAccessRequestDTO` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `ResourceAccessRequestDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `ResourceAccessRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `ResourceAccessRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `sessionId` 表示 `ResourceAccessRequestDTO` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `ResourceAccessRequestDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `ResourceAccessRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `ResourceAccessRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sessionId,
            /**
             * 字段 `rbacApplicationCode` 表示 `ResourceAccessRequestDTO` 中与 `rbac Application Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `rbacApplicationCode` stores the `rbac Application Code`-related state, dependency, configuration, or result of `ResourceAccessRequestDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `rbacApplicationCode` 时应保持 `ResourceAccessRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `rbacApplicationCode`, preserve `ResourceAccessRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String rbacApplicationCode,
            /**
             * 字段 `entryPermissionCode` 表示 `ResourceAccessRequestDTO` 中与 `entry Permission Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `entryPermissionCode` stores the `entry Permission Code`-related state, dependency, configuration, or result of `ResourceAccessRequestDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `entryPermissionCode` 时应保持 `ResourceAccessRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `entryPermissionCode`, preserve `ResourceAccessRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String entryPermissionCode) {

        /**
         * 校验并规范化资源入口请求。
         * Validates and normalizes the resource-entry request.
         * 用法：通过 `ResourceAccessRequestDTO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `ResourceAccessRequestDTO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param rbacApplicationCode 输入参数 `rbacApplicationCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param entryPermissionCode 输入参数 `entryPermissionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public ResourceAccessRequestDTO {
            identitySub = required(identitySub, "identitySub");
            tenantId = required(tenantId, "tenantId");
            sessionId = required(sessionId, "sessionId");
            rbacApplicationCode = required(rbacApplicationCode, "rbacApplicationCode");
            entryPermissionCode = required(entryPermissionCode, "entryPermissionCode");
        }

        /** 校验必填文本。 / Validates required text. */
        private static String required(String value, String fieldName) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(fieldName + " is required");
            }
            return value.trim();
        }
    }
