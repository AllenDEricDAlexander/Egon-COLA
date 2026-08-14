package top.egon.cola.platform.rbac3.admin.authorization.domain.vo;

import top.egon.cola.platform.rbac3.contract.authorization.Decision;

import java.time.Instant;
import java.util.Objects;

/**
     * 最小用户 AuthorizationDecisionResourceVO Server 入口判定结果。
     * Minimal user AuthorizationDecisionResourceVO Server entry-decision result.
     *
     * @param decision ALLOW 或 DENY 判定 / ALLOW or DENY decision
     * @param reasonCode 稳定原因码 / stable reason code
     * @param authVersion 用户授权版本；无快照时为空 / user authorization version, nullable without a snapshot
 * @param policyVersion 策略版本；无快照时为空 / policy version, nullable without a snapshot
     * @param policyVersion 策略版本；无快照时为空 / policy version, nullable without a snapshot
     * @param decidedAt 判定时间 / decision time
     * 语义与用法：将 `ResourceAccessDecisionVO` 作为 `AuthorizationDecisionService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ResourceAccessDecisionVO` as the responsibility boundary of `AuthorizationDecisionService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public record ResourceAccessDecisionVO(
            /**
             * 字段 `decision` 表示 `ResourceAccessDecisionVO` 中与 `decision` 相关的状态、依赖、配置或结果（声明类型 `Decision`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `decision` stores the `decision`-related state, dependency, configuration, or result of `ResourceAccessDecisionVO` (declared type `Decision`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `decision` 时应保持 `ResourceAccessDecisionVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `decision`, preserve `ResourceAccessDecisionVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Decision decision,
            /**
             * 字段 `reasonCode` 表示 `ResourceAccessDecisionVO` 中与 `reason Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reasonCode` stores the `reason Code`-related state, dependency, configuration, or result of `ResourceAccessDecisionVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reasonCode` 时应保持 `ResourceAccessDecisionVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reasonCode`, preserve `ResourceAccessDecisionVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String reasonCode,
            /**
             * 字段 `authVersion` 表示 `ResourceAccessDecisionVO` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `ResourceAccessDecisionVO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `ResourceAccessDecisionVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `ResourceAccessDecisionVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long authVersion,
            /**
             * 字段 `policyVersion` 表示策略版本；Field `policyVersion` stores the policy version in `ResourceAccessDecisionVO`.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `ResourceAccessDecisionVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: preserve the lifecycle, immutability, and thread-safety constraints of `ResourceAccessDecisionVO` when reading or passing `policyVersion`.
             */
            Long policyVersion,
            /**
             * 字段 `decidedAt` 表示 `ResourceAccessDecisionVO` 中与 `decided At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `ResourceAccessDecisionVO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `ResourceAccessDecisionVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `ResourceAccessDecisionVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant decidedAt) {

        /**
         * 校验最小资源入口判定。
         * Validates the minimal resource-entry decision.
         * 用法：通过 `ResourceAccessDecisionVO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `ResourceAccessDecisionVO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         * @param decision 输入参数 `decision`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param authVersion 输入参数 `authVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param policyVersion 输入参数 `policyVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param policyVersion 输入参数 `policyVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param decidedAt 输入参数 `decidedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public ResourceAccessDecisionVO {
            decision = Objects.requireNonNull(decision, "decision");
            reasonCode = required(reasonCode, "reasonCode");
            decidedAt = Objects.requireNonNull(decidedAt, "decidedAt");
            int versionCount = (authVersion == null ? 0 : 1)
                    + (policyVersion == null ? 0 : 1);
            if (versionCount != 0 && versionCount != 2) {
                throw new IllegalArgumentException(
                        "authorization versions must be all present or all absent");
            }
            if ((authVersion != null && authVersion < 0)
                    || (policyVersion != null && policyVersion < 0)) {
                throw new IllegalArgumentException(
                        "authorization versions must not be negative");
            }
        }

        /** 校验必填文本。 / Validates required text. */
        private static String required(String value, String fieldName) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(fieldName + " is required");
            }
            return value.trim();
        }
    }
