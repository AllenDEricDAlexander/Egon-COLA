package top.egon.cola.platform.rbac3.admin.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 类型 `Rbac3AdminProperties` 位于当前包内，是类型，用于承载 `Rbac3 Admin Properties` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3AdminProperties` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Admin Properties`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `Rbac3AdminProperties` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `Rbac3AdminProperties` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@ConfigurationProperties(prefix = "egon.rbac3")
public class Rbac3AdminProperties {

    /**
     * 字段 `accessTokenTtl` 表示 `Rbac3AdminProperties` 中与 `access Token Ttl` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `accessTokenTtl` stores the `access Token Ttl`-related state, dependency, configuration, or result of `Rbac3AdminProperties` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `accessTokenTtl` 时应保持 `Rbac3AdminProperties` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `accessTokenTtl`, preserve `Rbac3AdminProperties`'s lifecycle, immutability, and thread-safety constraints.
     */
    private Duration accessTokenTtl = Duration.ofMinutes(15);
    /**
     * 字段 `refreshTokenTtl` 表示 `Rbac3AdminProperties` 中与 `refresh Token Ttl` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `refreshTokenTtl` stores the `refresh Token Ttl`-related state, dependency, configuration, or result of `Rbac3AdminProperties` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `refreshTokenTtl` 时应保持 `Rbac3AdminProperties` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `refreshTokenTtl`, preserve `Rbac3AdminProperties`'s lifecycle, immutability, and thread-safety constraints.
     */
    private Duration refreshTokenTtl = Duration.ofDays(7);
    /**
     * 字段 `sessionIdleTimeout` 表示 `Rbac3AdminProperties` 中与 `session Idle Timeout` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sessionIdleTimeout` stores the `session Idle Timeout`-related state, dependency, configuration, or result of `Rbac3AdminProperties` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sessionIdleTimeout` 时应保持 `Rbac3AdminProperties` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sessionIdleTimeout`, preserve `Rbac3AdminProperties`'s lifecycle, immutability, and thread-safety constraints.
     */
    private Duration sessionIdleTimeout = Duration.ofMinutes(30);
    /**
     * 字段 `sessionAbsoluteTimeout` 表示 `Rbac3AdminProperties` 中与 `session Absolute Timeout` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sessionAbsoluteTimeout` stores the `session Absolute Timeout`-related state, dependency, configuration, or result of `Rbac3AdminProperties` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sessionAbsoluteTimeout` 时应保持 `Rbac3AdminProperties` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sessionAbsoluteTimeout`, preserve `Rbac3AdminProperties`'s lifecycle, immutability, and thread-safety constraints.
     */
    private Duration sessionAbsoluteTimeout = Duration.ofHours(12);
    /**
     * 字段 `maximumActiveRoots` 表示 `Rbac3AdminProperties` 中与 `maximum Active Roots` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `maximumActiveRoots` stores the `maximum Active Roots`-related state, dependency, configuration, or result of `Rbac3AdminProperties` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `maximumActiveRoots` 时应保持 `Rbac3AdminProperties` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `maximumActiveRoots`, preserve `Rbac3AdminProperties`'s lifecycle, immutability, and thread-safety constraints.
     */
    private int maximumActiveRoots = 16;
    /**
     * 字段 `platformTargetingEnabled` 表示 `Rbac3AdminProperties` 中与 `platform Targeting Enabled` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `platformTargetingEnabled` stores the `platform Targeting Enabled`-related state, dependency, configuration, or result of `Rbac3AdminProperties` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `platformTargetingEnabled` 时应保持 `Rbac3AdminProperties` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `platformTargetingEnabled`, preserve `Rbac3AdminProperties`'s lifecycle, immutability, and thread-safety constraints.
     */
    private boolean platformTargetingEnabled;
    /**
     * 字段 `componentKeys` 表示 `Rbac3AdminProperties` 中与 `component Keys` 相关的状态、依赖、配置或结果（声明类型 `Set&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `componentKeys` stores the `component Keys`-related state, dependency, configuration, or result of `Rbac3AdminProperties` (declared type `Set&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `componentKeys` 时应保持 `Rbac3AdminProperties` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `componentKeys`, preserve `Rbac3AdminProperties`'s lifecycle, immutability, and thread-safety constraints.
     */
    private Set<String> componentKeys = new LinkedHashSet<>();

    /**
     * 方法 `getAccessTokenTtl` 按照 `Rbac3AdminProperties` 的职责处理输入，完成 `get Access Token Ttl` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getAccessTokenTtl` processes its inputs according to `Rbac3AdminProperties`'s responsibility, performs the `get Access Token Ttl` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getAccessTokenTtl` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getAccessTokenTtl`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    /**
     * 方法 `setAccessTokenTtl` 按照 `Rbac3AdminProperties` 的职责处理输入，完成 `set Access Token Ttl` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `setAccessTokenTtl` processes its inputs according to `Rbac3AdminProperties`'s responsibility, performs the `set Access Token Ttl` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `setAccessTokenTtl` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `setAccessTokenTtl`, then continue the business flow using its result, exception, or side effect.
     *
     * @param accessTokenTtl 输入参数 `accessTokenTtl`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    /**
     * 方法 `getRefreshTokenTtl` 按照 `Rbac3AdminProperties` 的职责处理输入，完成 `get Refresh Token Ttl` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getRefreshTokenTtl` processes its inputs according to `Rbac3AdminProperties`'s responsibility, performs the `get Refresh Token Ttl` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getRefreshTokenTtl` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getRefreshTokenTtl`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Duration getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    /**
     * 方法 `setRefreshTokenTtl` 按照 `Rbac3AdminProperties` 的职责处理输入，完成 `set Refresh Token Ttl` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `setRefreshTokenTtl` processes its inputs according to `Rbac3AdminProperties`'s responsibility, performs the `set Refresh Token Ttl` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `setRefreshTokenTtl` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `setRefreshTokenTtl`, then continue the business flow using its result, exception, or side effect.
     *
     * @param refreshTokenTtl 输入参数 `refreshTokenTtl`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void setRefreshTokenTtl(Duration refreshTokenTtl) {
        this.refreshTokenTtl = refreshTokenTtl;
    }

    /**
     * 方法 `getSessionIdleTimeout` 按照 `Rbac3AdminProperties` 的职责处理输入，完成 `get Session Idle Timeout` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getSessionIdleTimeout` processes its inputs according to `Rbac3AdminProperties`'s responsibility, performs the `get Session Idle Timeout` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getSessionIdleTimeout` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getSessionIdleTimeout`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Duration getSessionIdleTimeout() {
        return sessionIdleTimeout;
    }

    /**
     * 方法 `setSessionIdleTimeout` 按照 `Rbac3AdminProperties` 的职责处理输入，完成 `set Session Idle Timeout` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `setSessionIdleTimeout` processes its inputs according to `Rbac3AdminProperties`'s responsibility, performs the `set Session Idle Timeout` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `setSessionIdleTimeout` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `setSessionIdleTimeout`, then continue the business flow using its result, exception, or side effect.
     *
     * @param sessionIdleTimeout 输入参数 `sessionIdleTimeout`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void setSessionIdleTimeout(Duration sessionIdleTimeout) {
        if (sessionIdleTimeout == null || sessionIdleTimeout.isNegative()
                || sessionIdleTimeout.isZero()) {
            throw new IllegalArgumentException("sessionIdleTimeout must be positive");
        }
        this.sessionIdleTimeout = sessionIdleTimeout;
    }

    /**
     * 方法 `getSessionAbsoluteTimeout` 按照 `Rbac3AdminProperties` 的职责处理输入，完成 `get Session Absolute Timeout` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getSessionAbsoluteTimeout` processes its inputs according to `Rbac3AdminProperties`'s responsibility, performs the `get Session Absolute Timeout` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getSessionAbsoluteTimeout` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getSessionAbsoluteTimeout`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Duration getSessionAbsoluteTimeout() {
        return sessionAbsoluteTimeout;
    }

    /**
     * 方法 `setSessionAbsoluteTimeout` 按照 `Rbac3AdminProperties` 的职责处理输入，完成 `set Session Absolute Timeout` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `setSessionAbsoluteTimeout` processes its inputs according to `Rbac3AdminProperties`'s responsibility, performs the `set Session Absolute Timeout` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `setSessionAbsoluteTimeout` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `setSessionAbsoluteTimeout`, then continue the business flow using its result, exception, or side effect.
     *
     * @param sessionAbsoluteTimeout 输入参数 `sessionAbsoluteTimeout`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void setSessionAbsoluteTimeout(Duration sessionAbsoluteTimeout) {
        if (sessionAbsoluteTimeout == null || sessionAbsoluteTimeout.isNegative()
                || sessionAbsoluteTimeout.isZero()) {
            throw new IllegalArgumentException("sessionAbsoluteTimeout must be positive");
        }
        this.sessionAbsoluteTimeout = sessionAbsoluteTimeout;
    }

    /**
     * 方法 `getMaximumActiveRoots` 按照 `Rbac3AdminProperties` 的职责处理输入，完成 `get Maximum Active Roots` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getMaximumActiveRoots` processes its inputs according to `Rbac3AdminProperties`'s responsibility, performs the `get Maximum Active Roots` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getMaximumActiveRoots` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getMaximumActiveRoots`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public int getMaximumActiveRoots() {
        return maximumActiveRoots;
    }

    /**
     * 方法 `setMaximumActiveRoots` 按照 `Rbac3AdminProperties` 的职责处理输入，完成 `set Maximum Active Roots` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `setMaximumActiveRoots` processes its inputs according to `Rbac3AdminProperties`'s responsibility, performs the `set Maximum Active Roots` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `setMaximumActiveRoots` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `setMaximumActiveRoots`, then continue the business flow using its result, exception, or side effect.
     *
     * @param maximumActiveRoots 输入参数 `maximumActiveRoots`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void setMaximumActiveRoots(int maximumActiveRoots) {
        if (maximumActiveRoots < 1 || maximumActiveRoots > 32) {
            throw new IllegalArgumentException("maximumActiveRoots must be between 1 and 32");
        }
        this.maximumActiveRoots = maximumActiveRoots;
    }

    /**
     * 方法 `isPlatformTargetingEnabled` 按照 `Rbac3AdminProperties` 的职责处理输入，完成 `is Platform Targeting Enabled` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `isPlatformTargetingEnabled` processes its inputs according to `Rbac3AdminProperties`'s responsibility, performs the `is Platform Targeting Enabled` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `isPlatformTargetingEnabled` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `isPlatformTargetingEnabled`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public boolean isPlatformTargetingEnabled() {
        return platformTargetingEnabled;
    }

    /**
     * 方法 `setPlatformTargetingEnabled` 按照 `Rbac3AdminProperties` 的职责处理输入，完成 `set Platform Targeting Enabled` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `setPlatformTargetingEnabled` processes its inputs according to `Rbac3AdminProperties`'s responsibility, performs the `set Platform Targeting Enabled` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `setPlatformTargetingEnabled` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `setPlatformTargetingEnabled`, then continue the business flow using its result, exception, or side effect.
     *
     * @param platformTargetingEnabled 输入参数 `platformTargetingEnabled`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void setPlatformTargetingEnabled(boolean platformTargetingEnabled) {
        this.platformTargetingEnabled = platformTargetingEnabled;
    }

    /**
     * 方法 `getComponentKeys` 按照 `Rbac3AdminProperties` 的职责处理输入，完成 `get Component Keys` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getComponentKeys` processes its inputs according to `Rbac3AdminProperties`'s responsibility, performs the `get Component Keys` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getComponentKeys` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getComponentKeys`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Set<String> getComponentKeys() {
        return Set.copyOf(componentKeys);
    }

    /**
     * 方法 `setComponentKeys` 按照 `Rbac3AdminProperties` 的职责处理输入，完成 `set Component Keys` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `setComponentKeys` processes its inputs according to `Rbac3AdminProperties`'s responsibility, performs the `set Component Keys` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `setComponentKeys` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `setComponentKeys`, then continue the business flow using its result, exception, or side effect.
     *
     * @param componentKeys 输入参数 `componentKeys`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void setComponentKeys(Set<String> componentKeys) {
        this.componentKeys = new LinkedHashSet<>(componentKeys == null
                ? Set.of() : componentKeys);
    }
}
