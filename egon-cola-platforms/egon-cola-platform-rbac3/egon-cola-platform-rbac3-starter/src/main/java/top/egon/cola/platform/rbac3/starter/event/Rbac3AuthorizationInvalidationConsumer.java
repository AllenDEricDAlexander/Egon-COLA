package top.egon.cola.platform.rbac3.starter.event;

import top.egon.cola.platform.rbac3.starter.cache.AuthorizationSnapshotCache;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 类型 `Rbac3AuthorizationInvalidationConsumer` 位于当前包内，是类型，用于承载 `Rbac3 Authorization Invalidation Consumer` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3AuthorizationInvalidationConsumer` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Authorization Invalidation Consumer`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * Applies monotonic RBAC3 invalidations without Redis key scans.
 */
public final class Rbac3AuthorizationInvalidationConsumer {

    /**
     * 字段 `systemCode` 表示 `Rbac3AuthorizationInvalidationConsumer` 中与 `system Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `systemCode` stores the `system Code`-related state, dependency, configuration, or result of `Rbac3AuthorizationInvalidationConsumer` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `systemCode` 时应保持 `Rbac3AuthorizationInvalidationConsumer` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `systemCode`, preserve `Rbac3AuthorizationInvalidationConsumer`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final String systemCode;
    /**
     * 字段 `cache` 表示 `Rbac3AuthorizationInvalidationConsumer` 中与 `cache` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationSnapshotCache`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `cache` stores the `cache`-related state, dependency, configuration, or result of `Rbac3AuthorizationInvalidationConsumer` (declared type `AuthorizationSnapshotCache`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `cache` 时应保持 `Rbac3AuthorizationInvalidationConsumer` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `cache`, preserve `Rbac3AuthorizationInvalidationConsumer`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AuthorizationSnapshotCache cache;
    /**
     * 字段 `versions` 表示 `Rbac3AuthorizationInvalidationConsumer` 中与 `versions` 相关的状态、依赖、配置或结果（声明类型 `ConcurrentHashMap&lt;String, Long&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `versions` stores the `versions`-related state, dependency, configuration, or result of `Rbac3AuthorizationInvalidationConsumer` (declared type `ConcurrentHashMap&lt;String, Long&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `versions` 时应保持 `Rbac3AuthorizationInvalidationConsumer` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `versions`, preserve `Rbac3AuthorizationInvalidationConsumer`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ConcurrentHashMap<String, Long> versions = new ConcurrentHashMap<>();

    /**
     * 构造器 `Rbac3AuthorizationInvalidationConsumer` 用于创建并初始化 `Rbac3AuthorizationInvalidationConsumer` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `Rbac3AuthorizationInvalidationConsumer` creates and initializes `Rbac3AuthorizationInvalidationConsumer`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `Rbac3AuthorizationInvalidationConsumer` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `Rbac3AuthorizationInvalidationConsumer`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param systemCode 输入参数 `systemCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param cache 输入参数 `cache`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public Rbac3AuthorizationInvalidationConsumer(
            String systemCode,
            AuthorizationSnapshotCache cache) {
        this.systemCode = required(systemCode, "systemCode");
        this.cache = Objects.requireNonNull(cache, "cache");
    }

    /**
     * 方法 `accept` 按照 `Rbac3AuthorizationInvalidationConsumer` 的职责处理输入，完成 `accept` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `accept` processes its inputs according to `Rbac3AuthorizationInvalidationConsumer`'s responsibility, performs the `accept` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `accept` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `accept`, then continue the business flow using its result, exception, or side effect.
     *
     * @param event 输入参数 `event`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void accept(Event event) {
        Objects.requireNonNull(event, "event");
        if (!systemCode.equals(event.systemCode())) {
            return;
        }
        String scope = scope(event);
        AtomicBoolean accepted = new AtomicBoolean();
        versions.compute(scope, (ignored, current) -> {
            if (current == null || event.version() > current) {
                accepted.set(true);
                return event.version();
            }
            return current;
        });
        if (!accepted.get()) {
            return;
        }
        switch (event.type()) {
            case "RBAC_AUTHORIZATION_CONTEXT_CHANGED" -> cache.invalidate(
                    new AuthorizationSnapshotCache.Key(
                            systemCode, event.tenantId(),
                            required(event.sessionId(), "sessionId")));
            case "RBAC_USER_AUTHORIZATION_CHANGED", "RBAC_IDENTITY_MAPPING_CHANGED" ->
                    cache.invalidateUser(systemCode, event.tenantId(),
                            required(event.identitySub(), "identitySub"));
            case "RBAC_TENANT_POLICY_CHANGED" ->
                    cache.invalidateTenant(systemCode, event.tenantId());
            default -> throw new IllegalArgumentException(
                    "unsupported RBAC3 invalidation event: " + event.type());
        }
    }

    /**
     * 方法 `scope` 按照 `Rbac3AuthorizationInvalidationConsumer` 的职责处理输入，完成 `scope` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `scope` processes its inputs according to `Rbac3AuthorizationInvalidationConsumer`'s responsibility, performs the `scope` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `scope` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `scope`, then continue the business flow using its result, exception, or side effect.
     *
     * @param event 输入参数 `event`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String scope(Event event) {
        return switch (event.type()) {
            case "RBAC_AUTHORIZATION_CONTEXT_CHANGED" -> event.type() + ':'
                    + event.tenantId() + ':' + required(event.sessionId(), "sessionId");
            case "RBAC_USER_AUTHORIZATION_CHANGED", "RBAC_IDENTITY_MAPPING_CHANGED" ->
                    event.type() + ':' + event.tenantId() + ':'
                            + required(event.identitySub(), "identitySub");
            case "RBAC_TENANT_POLICY_CHANGED" -> event.type() + ':' + event.tenantId();
            default -> event.type() + ':' + event.eventId();
        };
    }

    /**
     * 方法 `required` 按照 `Rbac3AuthorizationInvalidationConsumer` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `Rbac3AuthorizationInvalidationConsumer`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }

    /**
     * 类型 `Event` 位于 `Rbac3AuthorizationInvalidationConsumer` 内，是记录类型，用于承载 `Event` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Event` is a record inside `Rbac3AuthorizationInvalidationConsumer` and carries the responsibility, state, or contract for `Event`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Event` 作为 `Rbac3AuthorizationInvalidationConsumer` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Event` as the responsibility boundary of `Rbac3AuthorizationInvalidationConsumer`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param eventId 记录组件 `eventId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `eventId` carries constructor data whose meaning is defined by the record contract.
     * @param type 记录组件 `type` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `type` carries constructor data whose meaning is defined by the record contract.
     * @param systemCode 记录组件 `systemCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `systemCode` carries constructor data whose meaning is defined by the record contract.
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param identitySub 记录组件 `identitySub` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `identitySub` carries constructor data whose meaning is defined by the record contract.
     * @param sessionId 记录组件 `sessionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionId` carries constructor data whose meaning is defined by the record contract.
     * @param version 记录组件 `version` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `version` carries constructor data whose meaning is defined by the record contract.
     */
    public record Event(
            /**
             * 字段 `eventId` 表示 `Event` 中与 `event Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `eventId` stores the `event Id`-related state, dependency, configuration, or result of `Event` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `eventId` 时应保持 `Event` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `eventId`, preserve `Event`'s lifecycle, immutability, and thread-safety constraints.
             */
            String eventId,
            /**
             * 字段 `type` 表示 `Event` 中与 `type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `type` stores the `type`-related state, dependency, configuration, or result of `Event` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `type` 时应保持 `Event` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `type`, preserve `Event`'s lifecycle, immutability, and thread-safety constraints.
             */
            String type,
            /**
             * 字段 `systemCode` 表示 `Event` 中与 `system Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `systemCode` stores the `system Code`-related state, dependency, configuration, or result of `Event` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `systemCode` 时应保持 `Event` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `systemCode`, preserve `Event`'s lifecycle, immutability, and thread-safety constraints.
             */
            String systemCode,
            /**
             * 字段 `tenantId` 表示 `Event` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `Event` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `Event` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `Event`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `identitySub` 表示 `Event` 中与 `identity Sub` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `identitySub` stores the `identity Sub`-related state, dependency, configuration, or result of `Event` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `identitySub` 时应保持 `Event` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `identitySub`, preserve `Event`'s lifecycle, immutability, and thread-safety constraints.
             */
            String identitySub,
            /**
             * 字段 `sessionId` 表示 `Event` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `Event` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `Event` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `Event`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sessionId,
            /**
             * 字段 `version` 表示 `Event` 中与 `version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `version` stores the `version`-related state, dependency, configuration, or result of `Event` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `version` 时应保持 `Event` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `version`, preserve `Event`'s lifecycle, immutability, and thread-safety constraints.
             */
            long version) {

        /**
         * 构造器 `Event` 用于创建并初始化 `Event` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `Event` creates and initializes `Event`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `Event` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `Event`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param eventId 输入参数 `eventId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param type 输入参数 `type`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param systemCode 输入参数 `systemCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param version 输入参数 `version`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public Event {
            eventId = required(eventId, "eventId");
            type = required(type, "type");
            systemCode = required(systemCode, "systemCode");
            tenantId = required(tenantId, "tenantId");
            if (version < 0) {
                throw new IllegalArgumentException("version must not be negative");
            }
        }
    }
}
