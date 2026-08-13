package top.egon.cola.platform.rbac3.starter.cache;

import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类型 `AuthorizationSnapshotCache` 位于当前包内，是类型，用于承载 `Authorization Snapshot Cache` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `AuthorizationSnapshotCache` is a type in its package and carries the responsibility, state, or contract for `Authorization Snapshot Cache`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Downstream-owned two-level authorization cache whose near entries never
 * outlive either the Redis TTL or the snapshot expiry.
 */
public final class AuthorizationSnapshotCache {

    /**
     * 字段 `store` 表示 `AuthorizationSnapshotCache` 中与 `store` 相关的状态、依赖、配置或结果（声明类型 `SnapshotStore`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `store` stores the `store`-related state, dependency, configuration, or result of `AuthorizationSnapshotCache` (declared type `SnapshotStore`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `store` 时应保持 `AuthorizationSnapshotCache` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `store`, preserve `AuthorizationSnapshotCache`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final SnapshotStore store;
    /**
     * 字段 `clock` 表示 `AuthorizationSnapshotCache` 中与 `clock` 相关的状态、依赖、配置或结果（声明类型 `Clock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `clock` stores the `clock`-related state, dependency, configuration, or result of `AuthorizationSnapshotCache` (declared type `Clock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `clock` 时应保持 `AuthorizationSnapshotCache` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `clock`, preserve `AuthorizationSnapshotCache`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Clock clock;
    /**
     * 字段 `nearTtl` 表示 `AuthorizationSnapshotCache` 中与 `near Ttl` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `nearTtl` stores the `near Ttl`-related state, dependency, configuration, or result of `AuthorizationSnapshotCache` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `nearTtl` 时应保持 `AuthorizationSnapshotCache` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `nearTtl`, preserve `AuthorizationSnapshotCache`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Duration nearTtl;
    /**
     * 字段 `near` 表示 `AuthorizationSnapshotCache` 中与 `near` 相关的状态、依赖、配置或结果（声明类型 `ConcurrentHashMap&lt;Key, NearEntry&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `near` stores the `near`-related state, dependency, configuration, or result of `AuthorizationSnapshotCache` (declared type `ConcurrentHashMap&lt;Key, NearEntry&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `near` 时应保持 `AuthorizationSnapshotCache` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `near`, preserve `AuthorizationSnapshotCache`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ConcurrentHashMap<Key, NearEntry> near = new ConcurrentHashMap<>();

    /**
     * 构造器 `AuthorizationSnapshotCache` 用于创建并初始化 `AuthorizationSnapshotCache` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `AuthorizationSnapshotCache` creates and initializes `AuthorizationSnapshotCache`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `AuthorizationSnapshotCache` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `AuthorizationSnapshotCache`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param store 输入参数 `store`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param nearTtl 输入参数 `nearTtl`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public AuthorizationSnapshotCache(
            SnapshotStore store,
            Clock clock,
            Duration nearTtl) {
        this.store = Objects.requireNonNull(store, "store");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.nearTtl = bounded(nearTtl, Duration.ZERO, Duration.ofSeconds(5),
                "nearTtl");
    }

    /**
     * 方法 `get` 按照 `AuthorizationSnapshotCache` 的职责处理输入，完成 `get` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `get` processes its inputs according to `AuthorizationSnapshotCache`'s responsibility, performs the `get` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `get` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `get`, then continue the business flow using its result, exception, or side effect.
     *
     * @param key 输入参数 `key`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Optional<SystemAuthorizationSnapshot> get(Key key) {
        Objects.requireNonNull(key, "key");
        Instant now = clock.instant();
        NearEntry local = near.get(key);
        if (local != null) {
            if (local.validAt(now)) {
                return Optional.of(local.snapshot());
            }
            near.remove(key, local);
        }
        Optional<SystemAuthorizationSnapshot> stored = store.get(key)
                .filter(snapshot -> validBinding(key, snapshot))
                .filter(snapshot -> snapshot.expiresAt().isAfter(now));
        stored.ifPresent(snapshot -> near.put(
                key, new NearEntry(snapshot, minimum(
                        now.plus(nearTtl), snapshot.expiresAt()))));
        return stored;
    }

    /**
     * 方法 `put` 按照 `AuthorizationSnapshotCache` 的职责处理输入，完成 `put` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `put` processes its inputs according to `AuthorizationSnapshotCache`'s responsibility, performs the `put` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `put` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `put`, then continue the business flow using its result, exception, or side effect.
     *
     * @param key 输入参数 `key`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param snapshot 输入参数 `snapshot`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param ttl 输入参数 `ttl`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void put(
            Key key,
            SystemAuthorizationSnapshot snapshot,
            Duration ttl) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(snapshot, "snapshot");
        if (!validBinding(key, snapshot)) {
            throw new IllegalArgumentException("snapshot does not match cache key");
        }
        Instant now = clock.instant();
        if (!snapshot.expiresAt().isAfter(now)) {
            throw new IllegalArgumentException("snapshot has expired");
        }
        Duration boundedTtl = bounded(ttl, Duration.ofMillis(1),
                Duration.ofMinutes(10), "ttl");
        Duration effectiveTtl = minimum(
                boundedTtl, Duration.between(now, snapshot.expiresAt()));
        store.put(key, snapshot, effectiveTtl);
        near.put(key, new NearEntry(snapshot, minimum(
                now.plus(nearTtl), now.plus(effectiveTtl), snapshot.expiresAt())));
    }

    /**
     * 方法 `invalidate` 按照 `AuthorizationSnapshotCache` 的职责处理输入，完成 `invalidate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `invalidate` processes its inputs according to `AuthorizationSnapshotCache`'s responsibility, performs the `invalidate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `invalidate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `invalidate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param key 输入参数 `key`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void invalidate(Key key) {
        near.remove(Objects.requireNonNull(key, "key"));
        store.invalidate(key);
    }

    /**
     * 方法 `invalidateUser` 按照 `AuthorizationSnapshotCache` 的职责处理输入，完成 `invalidate User` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `invalidateUser` processes its inputs according to `AuthorizationSnapshotCache`'s responsibility, performs the `invalidate User` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `invalidateUser` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `invalidateUser`, then continue the business flow using its result, exception, or side effect.
     *
     * @param systemCode 输入参数 `systemCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void invalidateUser(
            String systemCode,
            String tenantId,
            String identitySub) {
        String system = required(systemCode, "systemCode");
        String tenant = required(tenantId, "tenantId");
        String subject = required(identitySub, "identitySub");
        near.entrySet().removeIf(entry -> entry.getKey().systemCode().equals(system)
                && entry.getKey().tenantId().equals(tenant)
                && entry.getValue().snapshot().identitySub().equals(subject));
        store.invalidateUser(system, tenant, subject);
    }

    /**
     * 方法 `invalidateTenant` 按照 `AuthorizationSnapshotCache` 的职责处理输入，完成 `invalidate Tenant` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `invalidateTenant` processes its inputs according to `AuthorizationSnapshotCache`'s responsibility, performs the `invalidate Tenant` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `invalidateTenant` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `invalidateTenant`, then continue the business flow using its result, exception, or side effect.
     *
     * @param systemCode 输入参数 `systemCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void invalidateTenant(String systemCode, String tenantId) {
        String system = required(systemCode, "systemCode");
        String tenant = required(tenantId, "tenantId");
        near.keySet().removeIf(key -> key.systemCode().equals(system)
                && key.tenantId().equals(tenant));
        store.invalidateTenant(system, tenant);
    }

    /**
     * 方法 `validBinding` 按照 `AuthorizationSnapshotCache` 的职责处理输入，完成 `valid Binding` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `validBinding` processes its inputs according to `AuthorizationSnapshotCache`'s responsibility, performs the `valid Binding` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `validBinding` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `validBinding`, then continue the business flow using its result, exception, or side effect.
     *
     * @param key 输入参数 `key`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param snapshot 输入参数 `snapshot`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private boolean validBinding(Key key, SystemAuthorizationSnapshot snapshot) {
        return key.systemCode().equals(snapshot.systemCode())
                && key.tenantId().equals(snapshot.tenantId())
                && key.sessionId().equals(snapshot.sessionId());
    }

    /**
     * 方法 `bounded` 按照 `AuthorizationSnapshotCache` 的职责处理输入，完成 `bounded` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `bounded` processes its inputs according to `AuthorizationSnapshotCache`'s responsibility, performs the `bounded` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `bounded` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `bounded`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param minimum 输入参数 `minimum`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param maximum 输入参数 `maximum`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static Duration bounded(
            Duration value,
            Duration minimum,
            Duration maximum,
            String name) {
        Objects.requireNonNull(value, name);
        if (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
            throw new IllegalArgumentException(name + " is outside the safe range");
        }
        return value;
    }

    /**
     * 方法 `minimum` 按照 `AuthorizationSnapshotCache` 的职责处理输入，完成 `minimum` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `minimum` processes its inputs according to `AuthorizationSnapshotCache`'s responsibility, performs the `minimum` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `minimum` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `minimum`, then continue the business flow using its result, exception, or side effect.
     *
     * @param left 输入参数 `left`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param right 输入参数 `right`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static Duration minimum(Duration left, Duration right) {
        return left.compareTo(right) <= 0 ? left : right;
    }

    /**
     * 方法 `minimum` 按照 `AuthorizationSnapshotCache` 的职责处理输入，完成 `minimum` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `minimum` processes its inputs according to `AuthorizationSnapshotCache`'s responsibility, performs the `minimum` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `minimum` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `minimum`, then continue the business flow using its result, exception, or side effect.
     *
     * @param values 输入参数 `values`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static Instant minimum(Instant... values) {
        Instant result = Objects.requireNonNull(values[0], "values[0]");
        for (int index = 1; index < values.length; index++) {
            if (values[index].isBefore(result)) {
                result = values[index];
            }
        }
        return result;
    }

    /**
     * 方法 `required` 按照 `AuthorizationSnapshotCache` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `AuthorizationSnapshotCache`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 类型 `SnapshotStore` 位于 `AuthorizationSnapshotCache` 内，是接口，用于承载 `Snapshot Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SnapshotStore` is an interface inside `AuthorizationSnapshotCache` and carries the responsibility, state, or contract for `Snapshot Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SnapshotStore` 作为 `AuthorizationSnapshotCache` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SnapshotStore` as the responsibility boundary of `AuthorizationSnapshotCache`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface SnapshotStore {

        /**
         * 方法 `get` 按照 `SnapshotStore` 的职责处理输入，完成 `get` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `get` processes its inputs according to `SnapshotStore`'s responsibility, performs the `get` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `get` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `get`, then continue the business flow using its result, exception, or side effect.
         *
         * @param key 输入参数 `key`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        Optional<SystemAuthorizationSnapshot> get(Key key);

        /**
         * 方法 `put` 按照 `SnapshotStore` 的职责处理输入，完成 `put` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `put` processes its inputs according to `SnapshotStore`'s responsibility, performs the `put` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `put` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `put`, then continue the business flow using its result, exception, or side effect.
         *
         * @param key 输入参数 `key`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param snapshot 输入参数 `snapshot`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param ttl 输入参数 `ttl`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void put(Key key, SystemAuthorizationSnapshot snapshot, Duration ttl);

        /**
         * 方法 `invalidate` 按照 `SnapshotStore` 的职责处理输入，完成 `invalidate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `invalidate` processes its inputs according to `SnapshotStore`'s responsibility, performs the `invalidate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `invalidate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `invalidate`, then continue the business flow using its result, exception, or side effect.
         *
         * @param key 输入参数 `key`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void invalidate(Key key);

        /**
         * 方法 `invalidateUser` 按照 `SnapshotStore` 的职责处理输入，完成 `invalidate User` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `invalidateUser` processes its inputs according to `SnapshotStore`'s responsibility, performs the `invalidate User` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `invalidateUser` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `invalidateUser`, then continue the business flow using its result, exception, or side effect.
         *
         * @param systemCode 输入参数 `systemCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void invalidateUser(String systemCode, String tenantId, String identitySub);

        /**
         * 方法 `invalidateTenant` 按照 `SnapshotStore` 的职责处理输入，完成 `invalidate Tenant` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `invalidateTenant` processes its inputs according to `SnapshotStore`'s responsibility, performs the `invalidate Tenant` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `invalidateTenant` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `invalidateTenant`, then continue the business flow using its result, exception, or side effect.
         *
         * @param systemCode 输入参数 `systemCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void invalidateTenant(String systemCode, String tenantId);
    }

    /**
     * 类型 `Key` 位于 `AuthorizationSnapshotCache` 内，是记录类型，用于承载 `Key` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Key` is a record inside `AuthorizationSnapshotCache` and carries the responsibility, state, or contract for `Key`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Key` 作为 `AuthorizationSnapshotCache` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Key` as the responsibility boundary of `AuthorizationSnapshotCache`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param systemCode 记录组件 `systemCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `systemCode` carries constructor data whose meaning is defined by the record contract.
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param sessionId 记录组件 `sessionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionId` carries constructor data whose meaning is defined by the record contract.
     */
    public record Key(/**
 * 字段 `systemCode` 表示 `Key` 中与 `system Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `systemCode` stores the `system Code`-related state, dependency, configuration, or result of `Key` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `systemCode` 时应保持 `Key` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `systemCode`, preserve `Key`'s lifecycle, immutability, and thread-safety constraints.
 */ String systemCode, /**
 * 字段 `tenantId` 表示 `Key` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `Key` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `Key` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `Key`'s lifecycle, immutability, and thread-safety constraints.
 */ String tenantId, /**
 * 字段 `sessionId` 表示 `Key` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `Key` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `Key` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `Key`'s lifecycle, immutability, and thread-safety constraints.
 */ String sessionId) {

        /**
         * 构造器 `Key` 用于创建并初始化 `Key` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `Key` creates and initializes `Key`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `Key` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `Key`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param systemCode 输入参数 `systemCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public Key {
            systemCode = required(systemCode, "systemCode");
            tenantId = required(tenantId, "tenantId");
            sessionId = required(sessionId, "sessionId");
        }

        /**
         * 方法 `redisKey` 按照 `Key` 的职责处理输入，完成 `redis Key` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `redisKey` processes its inputs according to `Key`'s responsibility, performs the `redis Key` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `redisKey` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `redisKey`, then continue the business flow using its result, exception, or side effect.
         *
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        public String redisKey() {
            return "rbac3:authorization:" + systemCode + ':' + tenantId + ':' + sessionId;
        }

        /**
         * 方法 `userIndex` 按照 `Key` 的职责处理输入，完成 `user Index` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `userIndex` processes its inputs according to `Key`'s responsibility, performs the `user Index` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `userIndex` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `userIndex`, then continue the business flow using its result, exception, or side effect.
         *
         * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        public String userIndex(String identitySub) {
            return "rbac3:authorization:index:user:" + systemCode + ':'
                    + tenantId + ':' + required(identitySub, "identitySub");
        }

        /**
         * 方法 `tenantIndex` 按照 `Key` 的职责处理输入，完成 `tenant Index` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `tenantIndex` processes its inputs according to `Key`'s responsibility, performs the `tenant Index` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `tenantIndex` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `tenantIndex`, then continue the business flow using its result, exception, or side effect.
         *
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        public String tenantIndex() {
            return "rbac3:authorization:index:tenant:" + systemCode + ':' + tenantId;
        }
    }

    /**
     * 类型 `NearEntry` 位于 `AuthorizationSnapshotCache` 内，是记录类型，用于承载 `Near Entry` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `NearEntry` is a record inside `AuthorizationSnapshotCache` and carries the responsibility, state, or contract for `Near Entry`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `NearEntry` 作为 `AuthorizationSnapshotCache` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `NearEntry` as the responsibility boundary of `AuthorizationSnapshotCache`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param snapshot 记录组件 `snapshot` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `snapshot` carries constructor data whose meaning is defined by the record contract.
     * @param expiresAt 记录组件 `expiresAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expiresAt` carries constructor data whose meaning is defined by the record contract.
     */
    private record NearEntry(
            /**
             * 字段 `snapshot` 表示 `NearEntry` 中与 `snapshot` 相关的状态、依赖、配置或结果（声明类型 `SystemAuthorizationSnapshot`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `snapshot` stores the `snapshot`-related state, dependency, configuration, or result of `NearEntry` (declared type `SystemAuthorizationSnapshot`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `snapshot` 时应保持 `NearEntry` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `snapshot`, preserve `NearEntry`'s lifecycle, immutability, and thread-safety constraints.
             */
            SystemAuthorizationSnapshot snapshot,
            /**
             * 字段 `expiresAt` 表示 `NearEntry` 中与 `expires At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expiresAt` stores the `expires At`-related state, dependency, configuration, or result of `NearEntry` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expiresAt` 时应保持 `NearEntry` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expiresAt`, preserve `NearEntry`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant expiresAt) {

        /**
         * 方法 `validAt` 按照 `NearEntry` 的职责处理输入，完成 `valid At` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `validAt` processes its inputs according to `NearEntry`'s responsibility, performs the `valid At` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `validAt` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `validAt`, then continue the business flow using its result, exception, or side effect.
         *
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        private boolean validAt(Instant now) {
            return expiresAt.isAfter(now) && snapshot.expiresAt().isAfter(now);
        }
    }
}
