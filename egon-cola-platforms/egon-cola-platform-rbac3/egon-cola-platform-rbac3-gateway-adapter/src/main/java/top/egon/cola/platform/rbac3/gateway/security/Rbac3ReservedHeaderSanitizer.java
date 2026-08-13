package top.egon.cola.platform.rbac3.gateway.security;

import java.util.Set;

/**
 * 类型 `Rbac3ReservedHeaderSanitizer` 位于当前包内，是类型，用于承载 `Rbac3 Reserved Header Sanitizer` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3ReservedHeaderSanitizer` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Reserved Header Sanitizer`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Declares the only RBAC3 identity headers the Gateway may generate.
 */
public final class Rbac3ReservedHeaderSanitizer {

    /**
     * 字段 `RESERVED` 表示 `Rbac3ReservedHeaderSanitizer` 中与 `RESERVED` 相关的状态、依赖、配置或结果（声明类型 `Set&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `RESERVED` stores the `RESERVED`-related state, dependency, configuration, or result of `Rbac3ReservedHeaderSanitizer` (declared type `Set&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `RESERVED` 时应保持 `Rbac3ReservedHeaderSanitizer` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `RESERVED`, preserve `Rbac3ReservedHeaderSanitizer`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final Set<String> RESERVED = Set.of(
            "authorization",
            "x-egon-gateway-tenant-id",
            "x-egon-gateway-user-id",
            "x-egon-gateway-session-id",
            "x-egon-gateway-auth-version",
            "x-egon-gateway-session-version",
            "x-egon-gateway-policy-version",
            "x-egon-gateway-trace-id"
    );

    /**
     * 方法 `fieldsToRemove` 按照 `Rbac3ReservedHeaderSanitizer` 的职责处理输入，完成 `fields To Remove` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `fieldsToRemove` processes its inputs according to `Rbac3ReservedHeaderSanitizer`'s responsibility, performs the `fields To Remove` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `fieldsToRemove` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `fieldsToRemove`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Set<String> fieldsToRemove() {
        return RESERVED;
    }

    /**
     * 方法 `trustedIdentityHeader` 按照 `Rbac3ReservedHeaderSanitizer` 的职责处理输入，完成 `trusted Identity Header` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `trustedIdentityHeader` processes its inputs according to `Rbac3ReservedHeaderSanitizer`'s responsibility, performs the `trusted Identity Header` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `trustedIdentityHeader` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `trustedIdentityHeader`, then continue the business flow using its result, exception, or side effect.
     *
     * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public boolean trustedIdentityHeader(String name) {
        return name != null && RESERVED.contains(name.toLowerCase(java.util.Locale.ROOT))
                && !"authorization".equalsIgnoreCase(name);
    }
}
