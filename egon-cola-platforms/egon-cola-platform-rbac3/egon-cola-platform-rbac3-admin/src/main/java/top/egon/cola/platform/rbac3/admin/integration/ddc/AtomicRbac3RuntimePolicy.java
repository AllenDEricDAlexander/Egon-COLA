package top.egon.cola.platform.rbac3.admin.integration.ddc;

import top.egon.cola.platform.rbac3.admin.application.port.Rbac3RuntimePolicy;
import top.egon.cola.platform.rbac3.admin.config.Rbac3AdminProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * 类型 `AtomicRbac3RuntimePolicy` 位于当前包内，是类型，用于承载 `Atomic Rbac3 Runtime Policy` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `AtomicRbac3RuntimePolicy` is a type in its package and carries the responsibility, state, or contract for `Atomic Rbac3 Runtime Policy`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Applies validated scalar configuration by atomically replacing a complete snapshot.
 */
public final class AtomicRbac3RuntimePolicy implements Rbac3RuntimePolicy {

    /**
     * 字段 `ACCESS_TOKEN_TTL_KEY` 表示 `AtomicRbac3RuntimePolicy` 中与 `ACCESS TOKEN TTL KEY` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `ACCESS_TOKEN_TTL_KEY` stores the `ACCESS TOKEN TTL KEY`-related state, dependency, configuration, or result of `AtomicRbac3RuntimePolicy` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `ACCESS_TOKEN_TTL_KEY` 时应保持 `AtomicRbac3RuntimePolicy` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `ACCESS_TOKEN_TTL_KEY`, preserve `AtomicRbac3RuntimePolicy`'s lifecycle, immutability, and thread-safety constraints.
     */
    public static final String ACCESS_TOKEN_TTL_KEY =
            "rbac3.access-token-ttl-seconds";
    /**
     * 字段 `REFRESH_TOKEN_TTL_KEY` 表示 `AtomicRbac3RuntimePolicy` 中与 `REFRESH TOKEN TTL KEY` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `REFRESH_TOKEN_TTL_KEY` stores the `REFRESH TOKEN TTL KEY`-related state, dependency, configuration, or result of `AtomicRbac3RuntimePolicy` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `REFRESH_TOKEN_TTL_KEY` 时应保持 `AtomicRbac3RuntimePolicy` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `REFRESH_TOKEN_TTL_KEY`, preserve `AtomicRbac3RuntimePolicy`'s lifecycle, immutability, and thread-safety constraints.
     */
    public static final String REFRESH_TOKEN_TTL_KEY =
            "rbac3.refresh-token-ttl-seconds";
    /**
     * 字段 `SESSION_IDLE_TIMEOUT_KEY` 表示 `AtomicRbac3RuntimePolicy` 中与 `SESSION IDLE TIMEOUT KEY` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `SESSION_IDLE_TIMEOUT_KEY` stores the `SESSION IDLE TIMEOUT KEY`-related state, dependency, configuration, or result of `AtomicRbac3RuntimePolicy` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `SESSION_IDLE_TIMEOUT_KEY` 时应保持 `AtomicRbac3RuntimePolicy` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `SESSION_IDLE_TIMEOUT_KEY`, preserve `AtomicRbac3RuntimePolicy`'s lifecycle, immutability, and thread-safety constraints.
     */
    public static final String SESSION_IDLE_TIMEOUT_KEY =
            "rbac3.session-idle-timeout-seconds";
    /**
     * 字段 `SESSION_ABSOLUTE_TIMEOUT_KEY` 表示 `AtomicRbac3RuntimePolicy` 中与 `SESSION ABSOLUTE TIMEOUT KEY` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `SESSION_ABSOLUTE_TIMEOUT_KEY` stores the `SESSION ABSOLUTE TIMEOUT KEY`-related state, dependency, configuration, or result of `AtomicRbac3RuntimePolicy` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `SESSION_ABSOLUTE_TIMEOUT_KEY` 时应保持 `AtomicRbac3RuntimePolicy` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `SESSION_ABSOLUTE_TIMEOUT_KEY`, preserve `AtomicRbac3RuntimePolicy`'s lifecycle, immutability, and thread-safety constraints.
     */
    public static final String SESSION_ABSOLUTE_TIMEOUT_KEY =
            "rbac3.session-absolute-timeout-seconds";
    /**
     * 字段 `MAXIMUM_ACTIVE_ROOTS_KEY` 表示 `AtomicRbac3RuntimePolicy` 中与 `MAXIMUM ACTIVE ROOTS KEY` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `MAXIMUM_ACTIVE_ROOTS_KEY` stores the `MAXIMUM ACTIVE ROOTS KEY`-related state, dependency, configuration, or result of `AtomicRbac3RuntimePolicy` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `MAXIMUM_ACTIVE_ROOTS_KEY` 时应保持 `AtomicRbac3RuntimePolicy` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `MAXIMUM_ACTIVE_ROOTS_KEY`, preserve `AtomicRbac3RuntimePolicy`'s lifecycle, immutability, and thread-safety constraints.
     */
    public static final String MAXIMUM_ACTIVE_ROOTS_KEY =
            "rbac3.maximum-active-roots";

    /**
     * 字段 `CONFIG_KEYS` 表示 `AtomicRbac3RuntimePolicy` 中与 `CONFIG KEYS` 相关的状态、依赖、配置或结果（声明类型 `Set&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `CONFIG_KEYS` stores the `CONFIG KEYS`-related state, dependency, configuration, or result of `AtomicRbac3RuntimePolicy` (declared type `Set&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `CONFIG_KEYS` 时应保持 `AtomicRbac3RuntimePolicy` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `CONFIG_KEYS`, preserve `AtomicRbac3RuntimePolicy`'s lifecycle, immutability, and thread-safety constraints.
     */
    public static final Set<String> CONFIG_KEYS = Set.of(
            ACCESS_TOKEN_TTL_KEY,
            REFRESH_TOKEN_TTL_KEY,
            SESSION_IDLE_TIMEOUT_KEY,
            SESSION_ABSOLUTE_TIMEOUT_KEY,
            MAXIMUM_ACTIVE_ROOTS_KEY);

    /**
     * 字段 `UNSIGNED_INTEGER` 表示 `AtomicRbac3RuntimePolicy` 中与 `UNSIGNED INTEGER` 相关的状态、依赖、配置或结果（声明类型 `Pattern`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `UNSIGNED_INTEGER` stores the `UNSIGNED INTEGER`-related state, dependency, configuration, or result of `AtomicRbac3RuntimePolicy` (declared type `Pattern`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `UNSIGNED_INTEGER` 时应保持 `AtomicRbac3RuntimePolicy` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `UNSIGNED_INTEGER`, preserve `AtomicRbac3RuntimePolicy`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final Pattern UNSIGNED_INTEGER = Pattern.compile("[0-9]+");
    /**
     * 字段 `POLICY_ERROR_CODES` 表示 `AtomicRbac3RuntimePolicy` 中与 `POLICY ERROR CODES` 相关的状态、依赖、配置或结果（声明类型 `Set&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `POLICY_ERROR_CODES` stores the `POLICY ERROR CODES`-related state, dependency, configuration, or result of `AtomicRbac3RuntimePolicy` (declared type `Set&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `POLICY_ERROR_CODES` 时应保持 `AtomicRbac3RuntimePolicy` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `POLICY_ERROR_CODES`, preserve `AtomicRbac3RuntimePolicy`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final Set<String> POLICY_ERROR_CODES = Set.of(
            "ACCESS_TOKEN_TTL_OUT_OF_RANGE",
            "REFRESH_TOKEN_TTL_OUT_OF_RANGE",
            "SESSION_IDLE_TIMEOUT_OUT_OF_RANGE",
            "SESSION_ABSOLUTE_TIMEOUT_OUT_OF_RANGE",
            "MAXIMUM_ACTIVE_ROOTS_OUT_OF_RANGE",
            "IDLE_EXCEEDS_ABSOLUTE",
            "REFRESH_BELOW_ABSOLUTE",
            "INVALID_CONFIG_VERSION");

    /**
     * 字段 `snapshot` 表示 `AtomicRbac3RuntimePolicy` 中与 `snapshot` 相关的状态、依赖、配置或结果（声明类型 `AtomicReference&lt;Snapshot&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `snapshot` stores the `snapshot`-related state, dependency, configuration, or result of `AtomicRbac3RuntimePolicy` (declared type `AtomicReference&lt;Snapshot&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `snapshot` 时应保持 `AtomicRbac3RuntimePolicy` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `snapshot`, preserve `AtomicRbac3RuntimePolicy`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AtomicReference<Snapshot> snapshot;
    /**
     * 字段 `lastApplyFailure` 表示 `AtomicRbac3RuntimePolicy` 中与 `last Apply Failure` 相关的状态、依赖、配置或结果（声明类型 `AtomicReference&lt;ApplyFailure&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `lastApplyFailure` stores the `last Apply Failure`-related state, dependency, configuration, or result of `AtomicRbac3RuntimePolicy` (declared type `AtomicReference&lt;ApplyFailure&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `lastApplyFailure` 时应保持 `AtomicRbac3RuntimePolicy` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `lastApplyFailure`, preserve `AtomicRbac3RuntimePolicy`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AtomicReference<ApplyFailure> lastApplyFailure = new AtomicReference<>();

    /**
     * 构造器 `AtomicRbac3RuntimePolicy` 用于创建并初始化 `AtomicRbac3RuntimePolicy` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `AtomicRbac3RuntimePolicy` creates and initializes `AtomicRbac3RuntimePolicy`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `AtomicRbac3RuntimePolicy` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `AtomicRbac3RuntimePolicy`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param properties 输入参数 `properties`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public AtomicRbac3RuntimePolicy(Rbac3AdminProperties properties) {
        Objects.requireNonNull(properties, "properties");
        Map<String, Long> versions = new LinkedHashMap<>();
        CONFIG_KEYS.stream().sorted().forEach(key -> versions.put(key, 0L));
        snapshot = new AtomicReference<>(new Snapshot(
                properties.getAccessTokenTtl(),
                properties.getRefreshTokenTtl(),
                properties.getSessionIdleTimeout(),
                properties.getSessionAbsoluteTimeout(),
                properties.getMaximumActiveRoots(),
                versions));
    }

    /**
     * 方法 `current` 按照 `AtomicRbac3RuntimePolicy` 的职责处理输入，完成 `current` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `current` processes its inputs according to `AtomicRbac3RuntimePolicy`'s responsibility, performs the `current` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `current` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `current`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public Snapshot current() {
        return snapshot.get();
    }

    /**
     * 方法 `lastApplyFailure` 按照 `AtomicRbac3RuntimePolicy` 的职责处理输入，完成 `last Apply Failure` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `lastApplyFailure` processes its inputs according to `AtomicRbac3RuntimePolicy`'s responsibility, performs the `last Apply Failure` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `lastApplyFailure` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `lastApplyFailure`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Optional<ApplyFailure> lastApplyFailure() {
        return Optional.ofNullable(lastApplyFailure.get());
    }

    /**
     * 方法 `apply` 按照 `AtomicRbac3RuntimePolicy` 的职责处理输入，完成 `apply` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `apply` processes its inputs according to `AtomicRbac3RuntimePolicy`'s responsibility, performs the `apply` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `apply` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `apply`, then continue the business flow using its result, exception, or side effect.
     *
     * @param key 输入参数 `key`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param rawValue 输入参数 `rawValue`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param version 输入参数 `version`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public synchronized void apply(String key, String rawValue, long version) {
        try {
            requireKnownKey(key);
            if (version < 0) {
                throw new PolicyApplyException("INVALID_VERSION");
            }
            long parsed = parse(rawValue);
            Snapshot candidate = candidate(snapshot.get(), key, parsed, version);
            snapshot.set(candidate);
            ApplyFailure previousFailure = lastApplyFailure.get();
            if (previousFailure != null && previousFailure.key().equals(key)) {
                lastApplyFailure.compareAndSet(previousFailure, null);
            }
        } catch (RuntimeException failure) {
            String errorCode = errorCode(failure);
            lastApplyFailure.set(new ApplyFailure(safeKey(key), version, errorCode));
            throw new IllegalArgumentException(
                    "RBAC3 runtime policy rejected key=" + safeKey(key)
                            + " version=" + version + " code=" + errorCode,
                    failure);
        }
    }

    /**
     * 方法 `candidate` 按照 `AtomicRbac3RuntimePolicy` 的职责处理输入，完成 `candidate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `candidate` processes its inputs according to `AtomicRbac3RuntimePolicy`'s responsibility, performs the `candidate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `candidate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `candidate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param current 输入参数 `current`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param key 输入参数 `key`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param version 输入参数 `version`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private Snapshot candidate(Snapshot current, String key, long value, long version) {
        Duration accessTokenTtl = current.accessTokenTtl();
        Duration refreshTokenTtl = current.refreshTokenTtl();
        Duration sessionIdleTimeout = current.sessionIdleTimeout();
        Duration sessionAbsoluteTimeout = current.sessionAbsoluteTimeout();
        int maximumActiveRoots = current.maximumActiveRoots();
        switch (key) {
            case ACCESS_TOKEN_TTL_KEY -> accessTokenTtl = Duration.ofSeconds(value);
            case REFRESH_TOKEN_TTL_KEY -> refreshTokenTtl = Duration.ofSeconds(value);
            case SESSION_IDLE_TIMEOUT_KEY -> sessionIdleTimeout = Duration.ofSeconds(value);
            case SESSION_ABSOLUTE_TIMEOUT_KEY ->
                    sessionAbsoluteTimeout = Duration.ofSeconds(value);
            case MAXIMUM_ACTIVE_ROOTS_KEY -> maximumActiveRoots = integer(value);
            default -> throw new PolicyApplyException("UNKNOWN_KEY");
        }
        Map<String, Long> versions = new LinkedHashMap<>(current.configVersions());
        versions.put(key, version);
        return new Snapshot(
                accessTokenTtl,
                refreshTokenTtl,
                sessionIdleTimeout,
                sessionAbsoluteTimeout,
                maximumActiveRoots,
                versions);
    }

    /**
     * 方法 `requireKnownKey` 按照 `AtomicRbac3RuntimePolicy` 的职责处理输入，完成 `require Known Key` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireKnownKey` processes its inputs according to `AtomicRbac3RuntimePolicy`'s responsibility, performs the `require Known Key` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireKnownKey` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireKnownKey`, then continue the business flow using its result, exception, or side effect.
     *
     * @param key 输入参数 `key`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void requireKnownKey(String key) {
        if (key == null || !CONFIG_KEYS.contains(key)) {
            throw new PolicyApplyException("UNKNOWN_KEY");
        }
    }

    /**
     * 方法 `parse` 按照 `AtomicRbac3RuntimePolicy` 的职责处理输入，完成 `parse` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `parse` processes its inputs according to `AtomicRbac3RuntimePolicy`'s responsibility, performs the `parse` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `parse` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `parse`, then continue the business flow using its result, exception, or side effect.
     *
     * @param rawValue 输入参数 `rawValue`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private long parse(String rawValue) {
        if (rawValue == null || !UNSIGNED_INTEGER.matcher(rawValue).matches()) {
            throw new PolicyApplyException("INVALID_INTEGER");
        }
        try {
            return Long.parseLong(rawValue);
        } catch (NumberFormatException invalid) {
            throw new PolicyApplyException("INVALID_INTEGER", invalid);
        }
    }

    /**
     * 方法 `integer` 按照 `AtomicRbac3RuntimePolicy` 的职责处理输入，完成 `integer` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `integer` processes its inputs according to `AtomicRbac3RuntimePolicy`'s responsibility, performs the `integer` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `integer` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `integer`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private int integer(long value) {
        try {
            return Math.toIntExact(value);
        } catch (ArithmeticException overflow) {
            throw new PolicyApplyException("MAXIMUM_ACTIVE_ROOTS_OUT_OF_RANGE", overflow);
        }
    }

    /**
     * 方法 `errorCode` 按照 `AtomicRbac3RuntimePolicy` 的职责处理输入，完成 `error Code` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `errorCode` processes its inputs according to `AtomicRbac3RuntimePolicy`'s responsibility, performs the `error Code` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `errorCode` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `errorCode`, then continue the business flow using its result, exception, or side effect.
     *
     * @param failure 输入参数 `failure`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String errorCode(RuntimeException failure) {
        if (failure instanceof PolicyApplyException policyFailure) {
            return policyFailure.errorCode;
        }
        String message = failure.getMessage();
        return POLICY_ERROR_CODES.contains(message) ? message : "INVALID_POLICY";
    }

    /**
     * 方法 `safeKey` 按照 `AtomicRbac3RuntimePolicy` 的职责处理输入，完成 `safe Key` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `safeKey` processes its inputs according to `AtomicRbac3RuntimePolicy`'s responsibility, performs the `safe Key` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `safeKey` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `safeKey`, then continue the business flow using its result, exception, or side effect.
     *
     * @param key 输入参数 `key`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String safeKey(String key) {
        return key == null || key.isBlank() ? "<missing>" : key;
    }

    /**
     * 类型 `ApplyFailure` 位于 `AtomicRbac3RuntimePolicy` 内，是记录类型，用于承载 `Apply Failure` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ApplyFailure` is a record inside `AtomicRbac3RuntimePolicy` and carries the responsibility, state, or contract for `Apply Failure`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ApplyFailure` 作为 `AtomicRbac3RuntimePolicy` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ApplyFailure` as the responsibility boundary of `AtomicRbac3RuntimePolicy`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param key 记录组件 `key` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `key` carries constructor data whose meaning is defined by the record contract.
     * @param targetVersion 记录组件 `targetVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `targetVersion` carries constructor data whose meaning is defined by the record contract.
     * @param errorCode 记录组件 `errorCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `errorCode` carries constructor data whose meaning is defined by the record contract.
     */
    public record ApplyFailure(/**
 * 字段 `key` 表示 `ApplyFailure` 中与 `key` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `key` stores the `key`-related state, dependency, configuration, or result of `ApplyFailure` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `key` 时应保持 `ApplyFailure` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `key`, preserve `ApplyFailure`'s lifecycle, immutability, and thread-safety constraints.
 */ String key, /**
 * 字段 `targetVersion` 表示 `ApplyFailure` 中与 `target Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `targetVersion` stores the `target Version`-related state, dependency, configuration, or result of `ApplyFailure` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `targetVersion` 时应保持 `ApplyFailure` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `targetVersion`, preserve `ApplyFailure`'s lifecycle, immutability, and thread-safety constraints.
 */ long targetVersion, /**
 * 字段 `errorCode` 表示 `ApplyFailure` 中与 `error Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `errorCode` stores the `error Code`-related state, dependency, configuration, or result of `ApplyFailure` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `errorCode` 时应保持 `ApplyFailure` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `errorCode`, preserve `ApplyFailure`'s lifecycle, immutability, and thread-safety constraints.
 */ String errorCode) {

        /**
         * 构造器 `ApplyFailure` 用于创建并初始化 `ApplyFailure` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `ApplyFailure` creates and initializes `ApplyFailure`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `ApplyFailure` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `ApplyFailure`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param key 输入参数 `key`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param targetVersion 输入参数 `targetVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param errorCode 输入参数 `errorCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public ApplyFailure {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(errorCode, "errorCode");
        }
    }

    /**
     * 类型 `PolicyApplyException` 位于 `AtomicRbac3RuntimePolicy` 内，是类型，用于承载 `Policy Apply Exception` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `PolicyApplyException` is a type inside `AtomicRbac3RuntimePolicy` and carries the responsibility, state, or contract for `Policy Apply Exception`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `PolicyApplyException` 作为 `AtomicRbac3RuntimePolicy` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `PolicyApplyException` as the responsibility boundary of `AtomicRbac3RuntimePolicy`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    private static final class PolicyApplyException extends IllegalArgumentException {

        /**
         * 字段 `errorCode` 表示 `PolicyApplyException` 中与 `error Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `errorCode` stores the `error Code`-related state, dependency, configuration, or result of `PolicyApplyException` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `errorCode` 时应保持 `PolicyApplyException` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `errorCode`, preserve `PolicyApplyException`'s lifecycle, immutability, and thread-safety constraints.
         */
        private final String errorCode;

        /**
         * 构造器 `PolicyApplyException` 用于创建并初始化 `PolicyApplyException` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `PolicyApplyException` creates and initializes `PolicyApplyException`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `PolicyApplyException` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `PolicyApplyException`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param errorCode 输入参数 `errorCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        private PolicyApplyException(String errorCode) {
            super(errorCode);
            this.errorCode = errorCode;
        }

        /**
         * 构造器 `PolicyApplyException` 用于创建并初始化 `PolicyApplyException` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `PolicyApplyException` creates and initializes `PolicyApplyException`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `PolicyApplyException` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `PolicyApplyException`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param errorCode 输入参数 `errorCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param cause 输入参数 `cause`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        private PolicyApplyException(String errorCode, Throwable cause) {
            super(errorCode, cause);
            this.errorCode = errorCode;
        }
    }
}
