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
     * 关联租户、IdP 主体、RBAC 用户与不可变授权快照的记录。
     * Record associating a tenant, IdP subject, RBAC user, and immutable authorization snapshot.
     *
     * @param tenantId 租户标识 / tenant identifier
     * @param identitySub IdP 稳定主体标识 / stable IdP subject
     * @param userId RBAC 用户标识 / RBAC user identifier
     * @param snapshot 会话授权快照 / session authorization snapshot
     * 语义与用法：将 `SnapshotRecordVO` 作为 `AuthorizationDecisionService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SnapshotRecordVO` as the responsibility boundary of `AuthorizationDecisionService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public record SnapshotRecordVO(
            /**
             * 字段 `tenantId` 表示 `SnapshotRecordVO` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `SnapshotRecordVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `SnapshotRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `SnapshotRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `identitySub` 表示 `SnapshotRecordVO` 中与 `identity Sub` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `identitySub` stores the `identity Sub`-related state, dependency, configuration, or result of `SnapshotRecordVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `identitySub` 时应保持 `SnapshotRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `identitySub`, preserve `SnapshotRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String identitySub,
            /**
             * 字段 `userId` 表示 `SnapshotRecordVO` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `SnapshotRecordVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userId` 时应保持 `SnapshotRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userId`, preserve `SnapshotRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String userId,
            /**
             * 字段 `snapshot` 表示 `SnapshotRecordVO` 中与 `snapshot` 相关的状态、依赖、配置或结果（声明类型 `SessionAuthorizationSnapshot`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `snapshot` stores the `snapshot`-related state, dependency, configuration, or result of `SnapshotRecordVO` (declared type `SessionAuthorizationSnapshot`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `snapshot` 时应保持 `SnapshotRecordVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `snapshot`, preserve `SnapshotRecordVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            SessionAuthorizationSnapshot snapshot) {

        /**
         * 校验并规范化快照记录。
         * Validates and normalizes the snapshot record.
         * 用法：通过 `SnapshotRecordVO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `SnapshotRecordVO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param snapshot 输入参数 `snapshot`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public SnapshotRecordVO {
            tenantId = required(tenantId, "tenantId");
            identitySub = required(identitySub, "identitySub");
            userId = required(userId, "userId");
            snapshot = Objects.requireNonNull(snapshot, "snapshot");
        }

        /**
         * 使用同一值作为兼容的 IdP 主体和 RBAC 用户标识创建记录。
         * Creates a record using the same compatibility value for IdP subject and RBAC user ID.
         *
         * @param tenantId 租户标识 / tenant identifier
         * @param userId 用户标识，同时作为 IdP 主体 / user identifier, also used as IdP subject
         * @param snapshot 会话授权快照 / session authorization snapshot
         */
        public SnapshotRecordVO(
                String tenantId,
                String userId,
                SessionAuthorizationSnapshot snapshot) {
            this(tenantId, userId, userId, snapshot);
        }

        /** 校验必填文本。 / Validates required text. */
        private static String required(String value, String fieldName) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(fieldName + " is required");
            }
            return value.trim();
        }
    }
