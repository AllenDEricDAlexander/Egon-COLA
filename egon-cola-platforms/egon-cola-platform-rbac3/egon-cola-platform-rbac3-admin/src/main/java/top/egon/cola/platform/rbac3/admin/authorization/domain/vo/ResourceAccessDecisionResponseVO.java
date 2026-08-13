package top.egon.cola.platform.rbac3.admin.authorization.domain.vo;

import top.egon.cola.platform.rbac3.contract.authorization.Decision;

import java.time.Instant;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.authorization.domain.vo.ResourceAccessDecisionVO;

/**
 * 最小化的用户 Resource Server 入口授权响应，不暴露角色或权限集合。
 * Minimal user Resource Server entry decision that exposes no roles or permission set.
 *
 * @param decision ALLOW 或 DENY 判定 / ALLOW or DENY decision
 * @param reasonCode 稳定原因码 / stable reason code
 * @param authVersion 用户授权版本；快照不可用时为空 /
 *                    user authorization version, or {@code null} when no snapshot is available
 * @param sessionVersion 会话授权版本；快照不可用时为空 /
 *                       session authorization version, or {@code null} when no snapshot is available
 * @param policyVersion 租户策略版本；快照不可用时为空 /
 *                      tenant policy version, or {@code null} when no snapshot is available
 * @param decidedAt 判定时间 / decision time
 * 语义与用法：将 `ResourceAccessDecisionResponseVO` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `ResourceAccessDecisionResponseVO` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
public record ResourceAccessDecisionResponseVO(
        /**
         * 字段 `decision` 表示 `ResourceAccessDecisionResponseVO` 中与 `decision` 相关的状态、依赖、配置或结果（声明类型 `Decision`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `decision` stores the `decision`-related state, dependency, configuration, or result of `ResourceAccessDecisionResponseVO` (declared type `Decision`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `decision` 时应保持 `ResourceAccessDecisionResponseVO` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `decision`, preserve `ResourceAccessDecisionResponseVO`'s lifecycle, immutability, and thread-safety constraints.
         */
        Decision decision,
        /**
         * 字段 `reasonCode` 表示 `ResourceAccessDecisionResponseVO` 中与 `reason Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `reasonCode` stores the `reason Code`-related state, dependency, configuration, or result of `ResourceAccessDecisionResponseVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `reasonCode` 时应保持 `ResourceAccessDecisionResponseVO` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `reasonCode`, preserve `ResourceAccessDecisionResponseVO`'s lifecycle, immutability, and thread-safety constraints.
         */
        String reasonCode,
        /**
         * 字段 `authVersion` 表示 `ResourceAccessDecisionResponseVO` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `ResourceAccessDecisionResponseVO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `ResourceAccessDecisionResponseVO` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `ResourceAccessDecisionResponseVO`'s lifecycle, immutability, and thread-safety constraints.
         */
        Long authVersion,
        /**
         * 字段 `sessionVersion` 表示 `ResourceAccessDecisionResponseVO` 中与 `session Version` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `sessionVersion` stores the `session Version`-related state, dependency, configuration, or result of `ResourceAccessDecisionResponseVO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `sessionVersion` 时应保持 `ResourceAccessDecisionResponseVO` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `sessionVersion`, preserve `ResourceAccessDecisionResponseVO`'s lifecycle, immutability, and thread-safety constraints.
         */
        Long sessionVersion,
        /**
         * 字段 `policyVersion` 表示 `ResourceAccessDecisionResponseVO` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `ResourceAccessDecisionResponseVO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `ResourceAccessDecisionResponseVO` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `ResourceAccessDecisionResponseVO`'s lifecycle, immutability, and thread-safety constraints.
         */
        Long policyVersion,
        /**
         * 字段 `decidedAt` 表示 `ResourceAccessDecisionResponseVO` 中与 `decided At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `decidedAt` stores the `decided At`-related state, dependency, configuration, or result of `ResourceAccessDecisionResponseVO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `decidedAt` 时应保持 `ResourceAccessDecisionResponseVO` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `decidedAt`, preserve `ResourceAccessDecisionResponseVO`'s lifecycle, immutability, and thread-safety constraints.
         */
        Instant decidedAt) {

    /**
     * 校验最小响应及其版本完整性。
     * Validates the minimal response and the completeness of its versions.
     * 用法：通过 `ResourceAccessDecisionResponseVO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ResourceAccessDecisionResponseVO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     * @param decision 输入参数 `decision`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param authVersion 输入参数 `authVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionVersion 输入参数 `sessionVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policyVersion 输入参数 `policyVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param decidedAt 输入参数 `decidedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public ResourceAccessDecisionResponseVO {
        decision = Objects.requireNonNull(decision, "decision");
        if (reasonCode == null || reasonCode.isBlank()) {
            throw new IllegalArgumentException("reasonCode is required");
        }
        reasonCode = reasonCode.trim();
        decidedAt = Objects.requireNonNull(decidedAt, "decidedAt");
        int versionCount = (authVersion == null ? 0 : 1)
                + (sessionVersion == null ? 0 : 1)
                + (policyVersion == null ? 0 : 1);
        if (versionCount != 0 && versionCount != 3) {
            throw new IllegalArgumentException(
                    "authorization versions must be all present or all absent");
        }
        if ((authVersion != null && authVersion < 0)
                || (sessionVersion != null && sessionVersion < 0)
                || (policyVersion != null && policyVersion < 0)) {
            throw new IllegalArgumentException(
                    "authorization versions must not be negative");
        }
    }

    /**
     * 从应用服务结果创建传输响应。
     * Creates a transport response from the application service result.
     *
     * @param result 应用服务结果 / application service result
     * @return 最小化传输响应 / minimal transport response
     * 用法：调用 `from` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `from`, then continue the business flow using its result, exception, or side effect.
     */
    public static ResourceAccessDecisionResponseVO from(
            ResourceAccessDecisionVO result) {
        Objects.requireNonNull(result, "result");
        return new ResourceAccessDecisionResponseVO(
                result.decision(), result.reasonCode(), result.authVersion(),
                result.sessionVersion(), result.policyVersion(), result.decidedAt());
    }
}
