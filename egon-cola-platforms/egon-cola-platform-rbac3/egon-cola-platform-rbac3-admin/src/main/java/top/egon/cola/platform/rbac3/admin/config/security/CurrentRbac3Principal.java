package top.egon.cola.platform.rbac3.admin.config.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Set;

/**
 * 类型 `CurrentRbac3Principal` 位于当前包内，是记录类型，用于承载 `Current Rbac3 Principal` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `CurrentRbac3Principal` is a record in its package and carries the responsibility, state, or contract for `Current Rbac3 Principal`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `CurrentRbac3Principal` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `CurrentRbac3Principal` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 *
 * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
 * @param identitySub 记录组件 `identitySub` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `identitySub` carries constructor data whose meaning is defined by the record contract.
 * @param userId 记录组件 `userId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `userId` carries constructor data whose meaning is defined by the record contract.
 * @param sessionId 记录组件 `sessionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionId` carries constructor data whose meaning is defined by the record contract.
 * @param authVersion 记录组件 `authVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authVersion` carries constructor data whose meaning is defined by the record contract.
 * @param sessionVersion 记录组件 `sessionVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionVersion` carries constructor data whose meaning is defined by the record contract.
 * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
 * @param permissions 记录组件 `permissions` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `permissions` carries constructor data whose meaning is defined by the record contract.
 * @param platformAdministrator 记录组件 `platformAdministrator` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `platformAdministrator` carries constructor data whose meaning is defined by the record contract.
 */
public record CurrentRbac3Principal(
        /**
         * 字段 `tenantId` 表示 `CurrentRbac3Principal` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `CurrentRbac3Principal` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `CurrentRbac3Principal` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `CurrentRbac3Principal`'s lifecycle, immutability, and thread-safety constraints.
         */
        String tenantId,
        /**
         * 字段 `identitySub` 表示 `CurrentRbac3Principal` 中与 `identity Sub` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `identitySub` stores the `identity Sub`-related state, dependency, configuration, or result of `CurrentRbac3Principal` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `identitySub` 时应保持 `CurrentRbac3Principal` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `identitySub`, preserve `CurrentRbac3Principal`'s lifecycle, immutability, and thread-safety constraints.
         */
        String identitySub,
        /**
         * 字段 `userId` 表示 `CurrentRbac3Principal` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `CurrentRbac3Principal` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `userId` 时应保持 `CurrentRbac3Principal` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `userId`, preserve `CurrentRbac3Principal`'s lifecycle, immutability, and thread-safety constraints.
         */
        String userId,
        /**
         * 字段 `sessionId` 表示 `CurrentRbac3Principal` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `CurrentRbac3Principal` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `CurrentRbac3Principal` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `CurrentRbac3Principal`'s lifecycle, immutability, and thread-safety constraints.
         */
        String sessionId,
        /**
         * 字段 `authVersion` 表示 `CurrentRbac3Principal` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `CurrentRbac3Principal` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `CurrentRbac3Principal` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `CurrentRbac3Principal`'s lifecycle, immutability, and thread-safety constraints.
         */
        long authVersion,
        /**
         * 字段 `sessionVersion` 表示 `CurrentRbac3Principal` 中与 `session Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `sessionVersion` stores the `session Version`-related state, dependency, configuration, or result of `CurrentRbac3Principal` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `sessionVersion` 时应保持 `CurrentRbac3Principal` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `sessionVersion`, preserve `CurrentRbac3Principal`'s lifecycle, immutability, and thread-safety constraints.
         */
        long sessionVersion,
        /**
         * 字段 `policyVersion` 表示 `CurrentRbac3Principal` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `CurrentRbac3Principal` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `CurrentRbac3Principal` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `CurrentRbac3Principal`'s lifecycle, immutability, and thread-safety constraints.
         */
        long policyVersion,
        /**
         * 字段 `permissions` 表示 `CurrentRbac3Principal` 中与 `permissions` 相关的状态、依赖、配置或结果（声明类型 `Set&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `permissions` stores the `permissions`-related state, dependency, configuration, or result of `CurrentRbac3Principal` (declared type `Set&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `permissions` 时应保持 `CurrentRbac3Principal` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `permissions`, preserve `CurrentRbac3Principal`'s lifecycle, immutability, and thread-safety constraints.
         */
        Set<String> permissions,
        /**
         * 字段 `platformAdministrator` 表示 `CurrentRbac3Principal` 中与 `platform Administrator` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `platformAdministrator` stores the `platform Administrator`-related state, dependency, configuration, or result of `CurrentRbac3Principal` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `platformAdministrator` 时应保持 `CurrentRbac3Principal` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `platformAdministrator`, preserve `CurrentRbac3Principal`'s lifecycle, immutability, and thread-safety constraints.
         */
        boolean platformAdministrator
) {

    /**
     * 构造器 `CurrentRbac3Principal` 用于创建并初始化 `CurrentRbac3Principal` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `CurrentRbac3Principal` creates and initializes `CurrentRbac3Principal`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `CurrentRbac3Principal` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `CurrentRbac3Principal`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param authVersion 输入参数 `authVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionVersion 输入参数 `sessionVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policyVersion 输入参数 `policyVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param permissions 输入参数 `permissions`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param platformAdministrator 输入参数 `platformAdministrator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public CurrentRbac3Principal {
        tenantId = required(tenantId, "tenantId");
        identitySub = required(identitySub, "identitySub");
        userId = required(userId, "userId");
        sessionId = required(sessionId, "sessionId");
        permissions = Set.copyOf(permissions);
    }

    /**
     * 构造器 `CurrentRbac3Principal` 用于创建并初始化 `CurrentRbac3Principal` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `CurrentRbac3Principal` creates and initializes `CurrentRbac3Principal`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `CurrentRbac3Principal` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `CurrentRbac3Principal`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param authVersion 输入参数 `authVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionVersion 输入参数 `sessionVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policyVersion 输入参数 `policyVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param permissions 输入参数 `permissions`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param platformAdministrator 输入参数 `platformAdministrator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public CurrentRbac3Principal(
            String tenantId,
            String userId,
            String sessionId,
            long authVersion,
            long sessionVersion,
            long policyVersion,
            Set<String> permissions,
            boolean platformAdministrator) {
        this(tenantId, userId, userId, sessionId, authVersion, sessionVersion,
                policyVersion, permissions, platformAdministrator);
    }

    /**
     * 方法 `authorities` 按照 `CurrentRbac3Principal` 的职责处理输入，完成 `authorities` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `authorities` processes its inputs according to `CurrentRbac3Principal`'s responsibility, performs the `authorities` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `authorities` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `authorities`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Collection<? extends GrantedAuthority> authorities() {
        return permissions.stream()
                .sorted()
                .map(permission -> new SimpleGrantedAuthority("RBAC3_" + permission))
                .toList();
    }

    /**
     * 方法 `hasPermission` 按照 `CurrentRbac3Principal` 的职责处理输入，完成 `has Permission` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `hasPermission` processes its inputs according to `CurrentRbac3Principal`'s responsibility, performs the `has Permission` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `hasPermission` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `hasPermission`, then continue the business flow using its result, exception, or side effect.
     *
     * @param permission 输入参数 `permission`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    /**
     * 方法 `required` 按照 `CurrentRbac3Principal` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `CurrentRbac3Principal`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param field 输入参数 `field`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
