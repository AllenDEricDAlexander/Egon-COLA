package top.egon.cola.platform.rbac3.starter.cache;

import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.starter.client.Rbac3AuthorizationClient;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类型 `SingleFlightSnapshotLoader` 位于当前包内，是类型，用于承载 `Single Flight Snapshot Loader` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `SingleFlightSnapshotLoader` is a type in its package and carries the responsibility, state, or contract for `Single Flight Snapshot Loader`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * Cache-aside loader that coalesces concurrent misses for the same session key.
 */
public final class SingleFlightSnapshotLoader {

    /**
     * 字段 `cache` 表示 `SingleFlightSnapshotLoader` 中与 `cache` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationSnapshotCache`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `cache` stores the `cache`-related state, dependency, configuration, or result of `SingleFlightSnapshotLoader` (declared type `AuthorizationSnapshotCache`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `cache` 时应保持 `SingleFlightSnapshotLoader` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `cache`, preserve `SingleFlightSnapshotLoader`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AuthorizationSnapshotCache cache;
    /**
     * 字段 `client` 表示 `SingleFlightSnapshotLoader` 中与 `client` 相关的状态、依赖、配置或结果（声明类型 `Rbac3AuthorizationClient`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `client` stores the `client`-related state, dependency, configuration, or result of `SingleFlightSnapshotLoader` (declared type `Rbac3AuthorizationClient`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `client` 时应保持 `SingleFlightSnapshotLoader` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `client`, preserve `SingleFlightSnapshotLoader`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Rbac3AuthorizationClient client;
    /**
     * 字段 `systemCode` 表示 `SingleFlightSnapshotLoader` 中与 `system Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `systemCode` stores the `system Code`-related state, dependency, configuration, or result of `SingleFlightSnapshotLoader` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `systemCode` 时应保持 `SingleFlightSnapshotLoader` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `systemCode`, preserve `SingleFlightSnapshotLoader`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final String systemCode;
    /**
     * 字段 `cacheTtl` 表示 `SingleFlightSnapshotLoader` 中与 `cache Ttl` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `cacheTtl` stores the `cache Ttl`-related state, dependency, configuration, or result of `SingleFlightSnapshotLoader` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `cacheTtl` 时应保持 `SingleFlightSnapshotLoader` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `cacheTtl`, preserve `SingleFlightSnapshotLoader`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Duration cacheTtl;
    /**
     * 字段 `clock` 表示 `SingleFlightSnapshotLoader` 中与 `clock` 相关的状态、依赖、配置或结果（声明类型 `Clock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `clock` stores the `clock`-related state, dependency, configuration, or result of `SingleFlightSnapshotLoader` (declared type `Clock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `clock` 时应保持 `SingleFlightSnapshotLoader` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `clock`, preserve `SingleFlightSnapshotLoader`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Clock clock;
    /**
     * 字段 `flights` 表示 `SingleFlightSnapshotLoader` 中与 `flights` 相关的状态、依赖、配置或结果（声明类型 `ConcurrentHashMap&lt;AuthorizationSnapshotCache.Key, CompletableFuture&lt;SystemAuthorizationSnapshot&gt;&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `flights` stores the `flights`-related state, dependency, configuration, or result of `SingleFlightSnapshotLoader` (declared type `ConcurrentHashMap&lt;AuthorizationSnapshotCache.Key, CompletableFuture&lt;SystemAuthorizationSnapshot&gt;&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `flights` 时应保持 `SingleFlightSnapshotLoader` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `flights`, preserve `SingleFlightSnapshotLoader`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ConcurrentHashMap<AuthorizationSnapshotCache.Key,
            CompletableFuture<SystemAuthorizationSnapshot>> flights =
            new ConcurrentHashMap<>();

    /**
     * 构造器 `SingleFlightSnapshotLoader` 用于创建并初始化 `SingleFlightSnapshotLoader` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `SingleFlightSnapshotLoader` creates and initializes `SingleFlightSnapshotLoader`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `SingleFlightSnapshotLoader` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `SingleFlightSnapshotLoader`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param cache 输入参数 `cache`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param client 输入参数 `client`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param systemCode 输入参数 `systemCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param cacheTtl 输入参数 `cacheTtl`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public SingleFlightSnapshotLoader(
            AuthorizationSnapshotCache cache,
            Rbac3AuthorizationClient client,
            String systemCode,
            Duration cacheTtl,
            Clock clock) {
        this.cache = Objects.requireNonNull(cache, "cache");
        this.client = Objects.requireNonNull(client, "client");
        this.systemCode = required(systemCode, "systemCode");
        this.cacheTtl = Objects.requireNonNull(cacheTtl, "cacheTtl");
        if (cacheTtl.compareTo(Duration.ofSeconds(1)) < 0
                || cacheTtl.compareTo(Duration.ofMinutes(10)) > 0) {
            throw new IllegalArgumentException("cacheTtl is outside the safe range");
        }
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * 方法 `load` 按照 `SingleFlightSnapshotLoader` 的职责处理输入，完成 `load` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `load` processes its inputs according to `SingleFlightSnapshotLoader`'s responsibility, performs the `load` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `load` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `load`, then continue the business flow using its result, exception, or side effect.
     *
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public SystemAuthorizationSnapshot load(IdentityPrincipal principal) {
        Objects.requireNonNull(principal, "principal");
        var key = new AuthorizationSnapshotCache.Key(
                systemCode, principal.tenantId(), principal.sessionId());
        return cached(key).filter(snapshot -> boundTo(snapshot, principal))
                .orElseGet(() -> join(key, principal));
    }

    /**
     * 方法 `join` 按照 `SingleFlightSnapshotLoader` 的职责处理输入，完成 `join` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `join` processes its inputs according to `SingleFlightSnapshotLoader`'s responsibility, performs the `join` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `join` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `join`, then continue the business flow using its result, exception, or side effect.
     *
     * @param key 输入参数 `key`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private SystemAuthorizationSnapshot join(
            AuthorizationSnapshotCache.Key key,
            IdentityPrincipal principal) {
        CompletableFuture<SystemAuthorizationSnapshot> created = new CompletableFuture<>();
        CompletableFuture<SystemAuthorizationSnapshot> active =
                flights.putIfAbsent(key, created);
        if (active == null) {
            active = created;
            try {
                SystemAuthorizationSnapshot snapshot = cached(key)
                        .filter(value -> boundTo(value, principal))
                        .orElseGet(() -> fetch(principal));
                created.complete(snapshot);
            } catch (Throwable failure) {
                created.completeExceptionally(failure);
            } finally {
                flights.remove(key, created);
            }
        }
        try {
            SystemAuthorizationSnapshot snapshot = active.join();
            if (!boundTo(snapshot, principal)) {
                throw new Rbac3AuthorizationClient.AuthorizationDeniedException(
                        "RBAC3_AUTHORIZATION_BINDING_MISMATCH");
            }
            return snapshot;
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new Rbac3AuthorizationClient.AuthorizationUnavailableException(
                    "RBAC3_AUTHORIZATION_FETCH_FAILED", cause);
        }
    }

    /**
     * 方法 `fetch` 按照 `SingleFlightSnapshotLoader` 的职责处理输入，完成 `fetch` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `fetch` processes its inputs according to `SingleFlightSnapshotLoader`'s responsibility, performs the `fetch` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `fetch` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `fetch`, then continue the business flow using its result, exception, or side effect.
     *
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private SystemAuthorizationSnapshot fetch(IdentityPrincipal principal) {
        try {
            SystemAuthorizationSnapshot snapshot = client.fetch(systemCode, principal);
            if (!boundTo(snapshot, principal)) {
                throw new Rbac3AuthorizationClient.AuthorizationDeniedException(
                        "RBAC3_AUTHORIZATION_BINDING_MISMATCH");
            }
            Duration remaining = Duration.between(clock.instant(), snapshot.expiresAt());
            Duration ttl = remaining.compareTo(cacheTtl) < 0 ? remaining : cacheTtl;
            var key = new AuthorizationSnapshotCache.Key(
                    systemCode, principal.tenantId(), principal.sessionId());
            cache.put(key, snapshot, ttl);
            return snapshot;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new Rbac3AuthorizationClient.AuthorizationUnavailableException(
                    "RBAC3_AUTHORIZATION_FETCH_INTERRUPTED", exception);
        }
    }

    /**
     * 方法 `cached` 按照 `SingleFlightSnapshotLoader` 的职责处理输入，完成 `cached` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `cached` processes its inputs according to `SingleFlightSnapshotLoader`'s responsibility, performs the `cached` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `cached` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `cached`, then continue the business flow using its result, exception, or side effect.
     *
     * @param key 输入参数 `key`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private java.util.Optional<SystemAuthorizationSnapshot> cached(
            AuthorizationSnapshotCache.Key key) {
        try {
            return cache.get(key);
        } catch (Rbac3AuthorizationClient.AuthorizationUnavailableException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new Rbac3AuthorizationClient.AuthorizationUnavailableException(
                    "RBAC3_AUTHORIZATION_CACHE_UNAVAILABLE", exception);
        }
    }

    /**
     * 方法 `boundTo` 按照 `SingleFlightSnapshotLoader` 的职责处理输入，完成 `bound To` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `boundTo` processes its inputs according to `SingleFlightSnapshotLoader`'s responsibility, performs the `bound To` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `boundTo` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `boundTo`, then continue the business flow using its result, exception, or side effect.
     *
     * @param snapshot 输入参数 `snapshot`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private boolean boundTo(
            SystemAuthorizationSnapshot snapshot,
            IdentityPrincipal principal) {
        return snapshot.systemCode().equals(systemCode)
                && snapshot.tenantId().equals(principal.tenantId())
                && snapshot.sessionId().equals(principal.sessionId())
                && snapshot.identitySub().equals(principal.subject())
                && snapshot.expiresAt().isAfter(clock.instant());
    }

    /**
     * 方法 `required` 按照 `SingleFlightSnapshotLoader` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `SingleFlightSnapshotLoader`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
}
