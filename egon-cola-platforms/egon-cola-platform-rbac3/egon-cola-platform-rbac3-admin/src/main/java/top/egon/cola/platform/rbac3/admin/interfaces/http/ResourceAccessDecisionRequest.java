package top.egon.cola.platform.rbac3.admin.interfaces.http;

import jakarta.validation.constraints.NotBlank;
import top.egon.cola.platform.rbac3.admin.authorization.application.AuthorizationDecisionService;

/**
 * 用户访问 OAuth2 Resource Server 的入口授权请求。
 * Request for deciding whether a user may enter an OAuth2 Resource Server.
 *
 * @param identitySub IdP 稳定用户主体标识 / stable IdP user subject
 * @param tid 租户标识 / tenant identifier
 * @param sid IdP 会话标识 / IdP session identifier
 * @param rbacApplicationCode 目标 Resource Server 绑定的 RBAC3 应用编码 /
 *                            RBAC3 application code bound to the target Resource Server
 * @param entryPermissionCode 进入目标应用所需的权限编码 /
 *                            permission code required to enter the target application
 * 语义与用法：将 `ResourceAccessDecisionRequest` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `ResourceAccessDecisionRequest` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
public record ResourceAccessDecisionRequest(
        /**
         * 字段 `identitySub` 表示 `ResourceAccessDecisionRequest` 中与 `identity Sub` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `identitySub` stores the `identity Sub`-related state, dependency, configuration, or result of `ResourceAccessDecisionRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `identitySub` 时应保持 `ResourceAccessDecisionRequest` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `identitySub`, preserve `ResourceAccessDecisionRequest`'s lifecycle, immutability, and thread-safety constraints.
         */
        @NotBlank String identitySub,
        /**
         * 字段 `tid` 表示 `ResourceAccessDecisionRequest` 中与 `tid` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `tid` stores the `tid`-related state, dependency, configuration, or result of `ResourceAccessDecisionRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `tid` 时应保持 `ResourceAccessDecisionRequest` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `tid`, preserve `ResourceAccessDecisionRequest`'s lifecycle, immutability, and thread-safety constraints.
         */
        @NotBlank String tid,
        /**
         * 字段 `sid` 表示 `ResourceAccessDecisionRequest` 中与 `sid` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `sid` stores the `sid`-related state, dependency, configuration, or result of `ResourceAccessDecisionRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `sid` 时应保持 `ResourceAccessDecisionRequest` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `sid`, preserve `ResourceAccessDecisionRequest`'s lifecycle, immutability, and thread-safety constraints.
         */
        @NotBlank String sid,
        /**
         * 字段 `rbacApplicationCode` 表示 `ResourceAccessDecisionRequest` 中与 `rbac Application Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `rbacApplicationCode` stores the `rbac Application Code`-related state, dependency, configuration, or result of `ResourceAccessDecisionRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `rbacApplicationCode` 时应保持 `ResourceAccessDecisionRequest` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `rbacApplicationCode`, preserve `ResourceAccessDecisionRequest`'s lifecycle, immutability, and thread-safety constraints.
         */
        @NotBlank String rbacApplicationCode,
        /**
         * 字段 `entryPermissionCode` 表示 `ResourceAccessDecisionRequest` 中与 `entry Permission Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `entryPermissionCode` stores the `entry Permission Code`-related state, dependency, configuration, or result of `ResourceAccessDecisionRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `entryPermissionCode` 时应保持 `ResourceAccessDecisionRequest` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `entryPermissionCode`, preserve `ResourceAccessDecisionRequest`'s lifecycle, immutability, and thread-safety constraints.
         */
        @NotBlank String entryPermissionCode) {

    /**
     * 校验并规范化传输请求。
     * Validates and normalizes the transport request.
     * 用法：通过 `ResourceAccessDecisionRequest` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ResourceAccessDecisionRequest`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tid 输入参数 `tid`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sid 输入参数 `sid`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param rbacApplicationCode 输入参数 `rbacApplicationCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param entryPermissionCode 输入参数 `entryPermissionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public ResourceAccessDecisionRequest {
        identitySub = required(identitySub, "identitySub");
        tid = required(tid, "tid");
        sid = required(sid, "sid");
        rbacApplicationCode = required(rbacApplicationCode, "rbacApplicationCode");
        entryPermissionCode = required(entryPermissionCode, "entryPermissionCode");
    }

    /**
     * 转换为授权应用服务命令。
     * Converts this transport request to an authorization application command.
     *
     * @return 资源入口授权命令 / resource-entry authorization command
     * 用法：调用 `toCommand` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `toCommand`, then continue the business flow using its result, exception, or side effect.
     */
    public AuthorizationDecisionService.ResourceAccessRequest toCommand() {
        return new AuthorizationDecisionService.ResourceAccessRequest(
                identitySub, tid, sid, rbacApplicationCode, entryPermissionCode);
    }

    /**
     * 校验必填文本并移除首尾空白。
     * Validates required text and trims surrounding whitespace.
     *
     * @param value 待校验值 / value to validate
     * @param fieldName 字段名 / field name
     * @return 规范化文本 / normalized text
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     */
    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
