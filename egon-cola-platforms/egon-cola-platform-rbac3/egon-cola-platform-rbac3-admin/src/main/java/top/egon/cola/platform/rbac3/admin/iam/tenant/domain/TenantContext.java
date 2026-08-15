package top.egon.cola.platform.rbac3.admin.iam.tenant.domain;

/**
 * 类型 `TenantContext` 位于当前包内，是记录类型，用于承载 `Tenant Context` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `TenantContext` is a record in its package and carries the responsibility, state, or contract for `Tenant Context`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `TenantContext` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `TenantContext` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 *
 * @param authenticatedTenantId 记录组件 `authenticatedTenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authenticatedTenantId` carries constructor data whose meaning is defined by the record contract.
 * @param effectiveTenantId 记录组件 `effectiveTenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `effectiveTenantId` carries constructor data whose meaning is defined by the record contract.
 * @param platformTarget 记录组件 `platformTarget` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `platformTarget` carries constructor data whose meaning is defined by the record contract.
 */
public record TenantContext(
        /**
         * 字段 `authenticatedTenantId` 表示 `TenantContext` 中与 `authenticated Tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `authenticatedTenantId` stores the `authenticated Tenant Id`-related state, dependency, configuration, or result of `TenantContext` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `authenticatedTenantId` 时应保持 `TenantContext` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `authenticatedTenantId`, preserve `TenantContext`'s lifecycle, immutability, and thread-safety constraints.
         */
        String authenticatedTenantId,
        /**
         * 字段 `effectiveTenantId` 表示 `TenantContext` 中与 `effective Tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `effectiveTenantId` stores the `effective Tenant Id`-related state, dependency, configuration, or result of `TenantContext` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `effectiveTenantId` 时应保持 `TenantContext` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `effectiveTenantId`, preserve `TenantContext`'s lifecycle, immutability, and thread-safety constraints.
         */
        String effectiveTenantId,
        /**
         * 字段 `platformTarget` 表示 `TenantContext` 中与 `platform Target` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `platformTarget` stores the `platform Target`-related state, dependency, configuration, or result of `TenantContext` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `platformTarget` 时应保持 `TenantContext` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `platformTarget`, preserve `TenantContext`'s lifecycle, immutability, and thread-safety constraints.
         */
        boolean platformTarget
) {

    /**
     * 字段 `CURRENT` 表示 `TenantContext` 中与 `CURRENT` 相关的状态、依赖、配置或结果（声明类型 `ThreadLocal&lt;TenantContext&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `CURRENT` stores the `CURRENT`-related state, dependency, configuration, or result of `TenantContext` (declared type `ThreadLocal&lt;TenantContext&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `CURRENT` 时应保持 `TenantContext` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `CURRENT`, preserve `TenantContext`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final ThreadLocal<TenantContext> CURRENT = new ThreadLocal<>();

    /**
     * 方法 `set` 按照 `TenantContext` 的职责处理输入，完成 `set` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `set` processes its inputs according to `TenantContext`'s responsibility, performs the `set` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `set` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `set`, then continue the business flow using its result, exception, or side effect.
     *
     * @param context 输入参数 `context`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public static void set(TenantContext context) {
        CURRENT.set(context);
    }

    /**
     * 方法 `requireCurrent` 按照 `TenantContext` 的职责处理输入，完成 `require Current` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireCurrent` processes its inputs according to `TenantContext`'s responsibility, performs the `require Current` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireCurrent` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireCurrent`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public static TenantContext requireCurrent() {
        TenantContext context = CURRENT.get();
        if (context == null) {
            throw new IllegalStateException("tenant context is not available");
        }
        return context;
    }

    /**
     * 方法 `clear` 按照 `TenantContext` 的职责处理输入，完成 `clear` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `clear` processes its inputs according to `TenantContext`'s responsibility, performs the `clear` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `clear` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `clear`, then continue the business flow using its result, exception, or side effect.
     */
    public static void clear() {
        CURRENT.remove();
    }
}
