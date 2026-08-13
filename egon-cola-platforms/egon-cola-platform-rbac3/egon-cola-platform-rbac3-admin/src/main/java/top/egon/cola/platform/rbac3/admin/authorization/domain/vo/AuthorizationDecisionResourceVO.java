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
     * 类型化授权判定的目标应用资源。
     * Target application resource for a typed authorization decision.
     *
     * @param applicationCode 应用编码 / application code
     * @param resourceCode 资源编码 / resource code
     * 语义与用法：将 `AuthorizationDecisionResourceVO` 作为 `AuthorizationDecisionService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AuthorizationDecisionResourceVO` as the responsibility boundary of `AuthorizationDecisionService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public record AuthorizationDecisionResourceVO(/**
 * 字段 `applicationCode` 表示 `AuthorizationDecisionResourceVO` 中与 `application Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `applicationCode` stores the `application Code`-related state, dependency, configuration, or result of `AuthorizationDecisionResourceVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `applicationCode` 时应保持 `AuthorizationDecisionResourceVO` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `applicationCode`, preserve `AuthorizationDecisionResourceVO`'s lifecycle, immutability, and thread-safety constraints.
 */ String applicationCode, /**
 * 字段 `resourceCode` 表示 `AuthorizationDecisionResourceVO` 中与 `resource Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `resourceCode` stores the `resource Code`-related state, dependency, configuration, or result of `AuthorizationDecisionResourceVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `resourceCode` 时应保持 `AuthorizationDecisionResourceVO` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `resourceCode`, preserve `AuthorizationDecisionResourceVO`'s lifecycle, immutability, and thread-safety constraints.
 */ String resourceCode) {

        /**
         * 校验并规范化目标资源。
         * Validates and normalizes the target resource.
         * 用法：通过 `AuthorizationDecisionResourceVO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `AuthorizationDecisionResourceVO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         * @param applicationCode 输入参数 `applicationCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param resourceCode 输入参数 `resourceCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public AuthorizationDecisionResourceVO {
            applicationCode = required(applicationCode, "applicationCode");
            resourceCode = required(resourceCode, "resourceCode");
        }

        /** 校验必填文本。 / Validates required text. */
        private static String required(String value, String fieldName) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(fieldName + " is required");
            }
            return value.trim();
        }
    }
