package top.egon.cola.platform.rbac3.starter.authorization;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 类型 `AuthorizationBootstrapService` 位于当前包内，是类型，用于承载 `Authorization Bootstrap Service` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `AuthorizationBootstrapService` is a type in its package and carries the responsibility, state, or contract for `Authorization Bootstrap Service`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * Builds the browser bootstrap view from the current bound authorization context.
 */
public final class AuthorizationBootstrapService {

    /**
     * 字段 `contextSource` 表示 `AuthorizationBootstrapService` 中与 `context Source` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationService.RuntimeContextSource`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `contextSource` stores the `context Source`-related state, dependency, configuration, or result of `AuthorizationBootstrapService` (declared type `AuthorizationService.RuntimeContextSource`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `contextSource` 时应保持 `AuthorizationBootstrapService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `contextSource`, preserve `AuthorizationBootstrapService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AuthorizationService.RuntimeContextSource contextSource;

    /**
     * 构造器 `AuthorizationBootstrapService` 用于创建并初始化 `AuthorizationBootstrapService` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `AuthorizationBootstrapService` creates and initializes `AuthorizationBootstrapService`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `AuthorizationBootstrapService` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `AuthorizationBootstrapService`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param contextSource 输入参数 `contextSource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public AuthorizationBootstrapService(
            AuthorizationService.RuntimeContextSource contextSource) {
        this.contextSource = Objects.requireNonNull(contextSource, "contextSource");
    }

    /**
     * 方法 `current` 按照 `AuthorizationBootstrapService` 的职责处理输入，完成 `current` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `current` processes its inputs according to `AuthorizationBootstrapService`'s responsibility, performs the `current` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `current` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `current`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public BootstrapView current() {
        var context = contextSource.load();
        var identity = context.identity();
        var snapshot = context.snapshot();
        return new BootstrapView(
                identity.subject(), identity.tenantId(), identity.sessionId(),
                snapshot.rbac3UserId(), snapshot.systemCode(),
                snapshot.permissions().stream().sorted().toList(),
                snapshot.activeRoleIds().stream().sorted().toList(),
                snapshot.authVersion(), snapshot.contextVersion(),
                snapshot.policyVersion(), snapshot.generatedAt(), snapshot.expiresAt());
    }

    /**
     * 类型 `BootstrapView` 位于 `AuthorizationBootstrapService` 内，是记录类型，用于承载 `Bootstrap View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `BootstrapView` is a record inside `AuthorizationBootstrapService` and carries the responsibility, state, or contract for `Bootstrap View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `BootstrapView` 作为 `AuthorizationBootstrapService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `BootstrapView` as the responsibility boundary of `AuthorizationBootstrapService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param identitySub 记录组件 `identitySub` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `identitySub` carries constructor data whose meaning is defined by the record contract.
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param sessionId 记录组件 `sessionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionId` carries constructor data whose meaning is defined by the record contract.
     * @param rbac3UserId 记录组件 `rbac3UserId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `rbac3UserId` carries constructor data whose meaning is defined by the record contract.
     * @param systemCode 记录组件 `systemCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `systemCode` carries constructor data whose meaning is defined by the record contract.
     * @param permissions 记录组件 `permissions` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `permissions` carries constructor data whose meaning is defined by the record contract.
     * @param activeRoleIds 记录组件 `activeRoleIds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `activeRoleIds` carries constructor data whose meaning is defined by the record contract.
     * @param authVersion 记录组件 `authVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authVersion` carries constructor data whose meaning is defined by the record contract.
     * @param contextVersion 记录组件 `contextVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `contextVersion` carries constructor data whose meaning is defined by the record contract.
     * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
     * @param generatedAt 记录组件 `generatedAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `generatedAt` carries constructor data whose meaning is defined by the record contract.
     * @param expiresAt 记录组件 `expiresAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expiresAt` carries constructor data whose meaning is defined by the record contract.
     */
    public record BootstrapView(
            /**
             * 字段 `identitySub` 表示 `BootstrapView` 中与 `identity Sub` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `identitySub` stores the `identity Sub`-related state, dependency, configuration, or result of `BootstrapView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `identitySub` 时应保持 `BootstrapView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `identitySub`, preserve `BootstrapView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String identitySub,
            /**
             * 字段 `tenantId` 表示 `BootstrapView` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `BootstrapView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `BootstrapView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `BootstrapView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `sessionId` 表示 `BootstrapView` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `BootstrapView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `BootstrapView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `BootstrapView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sessionId,
            /**
             * 字段 `rbac3UserId` 表示 `BootstrapView` 中与 `rbac3 User Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `rbac3UserId` stores the `rbac3 User Id`-related state, dependency, configuration, or result of `BootstrapView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `rbac3UserId` 时应保持 `BootstrapView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `rbac3UserId`, preserve `BootstrapView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String rbac3UserId,
            /**
             * 字段 `systemCode` 表示 `BootstrapView` 中与 `system Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `systemCode` stores the `system Code`-related state, dependency, configuration, or result of `BootstrapView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `systemCode` 时应保持 `BootstrapView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `systemCode`, preserve `BootstrapView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String systemCode,
            /**
             * 字段 `permissions` 表示 `BootstrapView` 中与 `permissions` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `permissions` stores the `permissions`-related state, dependency, configuration, or result of `BootstrapView` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `permissions` 时应保持 `BootstrapView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `permissions`, preserve `BootstrapView`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<String> permissions,
            /**
             * 字段 `activeRoleIds` 表示 `BootstrapView` 中与 `active Role Ids` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `activeRoleIds` stores the `active Role Ids`-related state, dependency, configuration, or result of `BootstrapView` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `activeRoleIds` 时应保持 `BootstrapView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `activeRoleIds`, preserve `BootstrapView`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<String> activeRoleIds,
            /**
             * 字段 `authVersion` 表示 `BootstrapView` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `BootstrapView` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `BootstrapView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `BootstrapView`'s lifecycle, immutability, and thread-safety constraints.
             */
            long authVersion,
            /**
             * 字段 `contextVersion` 表示 `BootstrapView` 中与 `context Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `contextVersion` stores the `context Version`-related state, dependency, configuration, or result of `BootstrapView` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `contextVersion` 时应保持 `BootstrapView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `contextVersion`, preserve `BootstrapView`'s lifecycle, immutability, and thread-safety constraints.
             */
            long contextVersion,
            /**
             * 字段 `policyVersion` 表示 `BootstrapView` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `BootstrapView` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `BootstrapView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `BootstrapView`'s lifecycle, immutability, and thread-safety constraints.
             */
            long policyVersion,
            /**
             * 字段 `generatedAt` 表示 `BootstrapView` 中与 `generated At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `generatedAt` stores the `generated At`-related state, dependency, configuration, or result of `BootstrapView` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `generatedAt` 时应保持 `BootstrapView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `generatedAt`, preserve `BootstrapView`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant generatedAt,
            /**
             * 字段 `expiresAt` 表示 `BootstrapView` 中与 `expires At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expiresAt` stores the `expires At`-related state, dependency, configuration, or result of `BootstrapView` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expiresAt` 时应保持 `BootstrapView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expiresAt`, preserve `BootstrapView`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant expiresAt) {

        /**
         * 构造器 `BootstrapView` 用于创建并初始化 `BootstrapView` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `BootstrapView` creates and initializes `BootstrapView`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `BootstrapView` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `BootstrapView`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param rbac3UserId 输入参数 `rbac3UserId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param systemCode 输入参数 `systemCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param permissions 输入参数 `permissions`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param activeRoleIds 输入参数 `activeRoleIds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param authVersion 输入参数 `authVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param contextVersion 输入参数 `contextVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param policyVersion 输入参数 `policyVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param generatedAt 输入参数 `generatedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expiresAt 输入参数 `expiresAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public BootstrapView {
            permissions = List.copyOf(permissions);
            activeRoleIds = List.copyOf(activeRoleIds);
        }
    }
}
