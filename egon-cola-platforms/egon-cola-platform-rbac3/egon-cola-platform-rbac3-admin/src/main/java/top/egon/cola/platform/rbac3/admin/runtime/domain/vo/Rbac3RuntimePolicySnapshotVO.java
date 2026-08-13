package top.egon.cola.platform.rbac3.admin.runtime.domain.vo;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
     * 类型 `Rbac3RuntimePolicySnapshotVO` 位于 `Rbac3RuntimePolicy` 内，是记录类型，用于承载 `Rbac3RuntimePolicySnapshotVO` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Rbac3RuntimePolicySnapshotVO` is a record inside `Rbac3RuntimePolicy` and carries the responsibility, state, or contract for `Rbac3RuntimePolicySnapshotVO`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Rbac3RuntimePolicySnapshotVO` 作为 `Rbac3RuntimePolicy` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Rbac3RuntimePolicySnapshotVO` as the responsibility boundary of `Rbac3RuntimePolicy`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param accessTokenTtl 记录组件 `accessTokenTtl` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `accessTokenTtl` carries constructor data whose meaning is defined by the record contract.
     * @param refreshTokenTtl 记录组件 `refreshTokenTtl` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `refreshTokenTtl` carries constructor data whose meaning is defined by the record contract.
     * @param sessionIdleTimeout 记录组件 `sessionIdleTimeout` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionIdleTimeout` carries constructor data whose meaning is defined by the record contract.
     * @param sessionAbsoluteTimeout 记录组件 `sessionAbsoluteTimeout` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionAbsoluteTimeout` carries constructor data whose meaning is defined by the record contract.
     * @param maximumActiveRoots 记录组件 `maximumActiveRoots` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `maximumActiveRoots` carries constructor data whose meaning is defined by the record contract.
     * @param configVersions 记录组件 `configVersions` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `configVersions` carries constructor data whose meaning is defined by the record contract.
     */
    public record Rbac3RuntimePolicySnapshotVO(
            /**
             * 字段 `accessTokenTtl` 表示 `Rbac3RuntimePolicySnapshotVO` 中与 `access Token Ttl` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `accessTokenTtl` stores the `access Token Ttl`-related state, dependency, configuration, or result of `Rbac3RuntimePolicySnapshotVO` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `accessTokenTtl` 时应保持 `Rbac3RuntimePolicySnapshotVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `accessTokenTtl`, preserve `Rbac3RuntimePolicySnapshotVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Duration accessTokenTtl,
            /**
             * 字段 `refreshTokenTtl` 表示 `Rbac3RuntimePolicySnapshotVO` 中与 `refresh Token Ttl` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `refreshTokenTtl` stores the `refresh Token Ttl`-related state, dependency, configuration, or result of `Rbac3RuntimePolicySnapshotVO` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `refreshTokenTtl` 时应保持 `Rbac3RuntimePolicySnapshotVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `refreshTokenTtl`, preserve `Rbac3RuntimePolicySnapshotVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Duration refreshTokenTtl,
            /**
             * 字段 `sessionIdleTimeout` 表示 `Rbac3RuntimePolicySnapshotVO` 中与 `session Idle Timeout` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionIdleTimeout` stores the `session Idle Timeout`-related state, dependency, configuration, or result of `Rbac3RuntimePolicySnapshotVO` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionIdleTimeout` 时应保持 `Rbac3RuntimePolicySnapshotVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionIdleTimeout`, preserve `Rbac3RuntimePolicySnapshotVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Duration sessionIdleTimeout,
            /**
             * 字段 `sessionAbsoluteTimeout` 表示 `Rbac3RuntimePolicySnapshotVO` 中与 `session Absolute Timeout` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionAbsoluteTimeout` stores the `session Absolute Timeout`-related state, dependency, configuration, or result of `Rbac3RuntimePolicySnapshotVO` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionAbsoluteTimeout` 时应保持 `Rbac3RuntimePolicySnapshotVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionAbsoluteTimeout`, preserve `Rbac3RuntimePolicySnapshotVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Duration sessionAbsoluteTimeout,
            /**
             * 字段 `maximumActiveRoots` 表示 `Rbac3RuntimePolicySnapshotVO` 中与 `maximum Active Roots` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `maximumActiveRoots` stores the `maximum Active Roots`-related state, dependency, configuration, or result of `Rbac3RuntimePolicySnapshotVO` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `maximumActiveRoots` 时应保持 `Rbac3RuntimePolicySnapshotVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `maximumActiveRoots`, preserve `Rbac3RuntimePolicySnapshotVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            int maximumActiveRoots,
            /**
             * 字段 `configVersions` 表示 `Rbac3RuntimePolicySnapshotVO` 中与 `config Versions` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, Long&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `configVersions` stores the `config Versions`-related state, dependency, configuration, or result of `Rbac3RuntimePolicySnapshotVO` (declared type `Map&lt;String, Long&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `configVersions` 时应保持 `Rbac3RuntimePolicySnapshotVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `configVersions`, preserve `Rbac3RuntimePolicySnapshotVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Map<String, Long> configVersions
    ) {

        /**
         * 字段 `MIN_ACCESS_TOKEN_TTL` 表示 `Rbac3RuntimePolicySnapshotVO` 中与 `MIN ACCESS TOKEN TTL` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `MIN_ACCESS_TOKEN_TTL` stores the `MIN ACCESS TOKEN TTL`-related state, dependency, configuration, or result of `Rbac3RuntimePolicySnapshotVO` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `MIN_ACCESS_TOKEN_TTL` 时应保持 `Rbac3RuntimePolicySnapshotVO` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `MIN_ACCESS_TOKEN_TTL`, preserve `Rbac3RuntimePolicySnapshotVO`'s lifecycle, immutability, and thread-safety constraints.
         */
        private static final Duration MIN_ACCESS_TOKEN_TTL = Duration.ofMinutes(5);
        /**
         * 字段 `MAX_ACCESS_TOKEN_TTL` 表示 `Rbac3RuntimePolicySnapshotVO` 中与 `MAX ACCESS TOKEN TTL` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `MAX_ACCESS_TOKEN_TTL` stores the `MAX ACCESS TOKEN TTL`-related state, dependency, configuration, or result of `Rbac3RuntimePolicySnapshotVO` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `MAX_ACCESS_TOKEN_TTL` 时应保持 `Rbac3RuntimePolicySnapshotVO` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `MAX_ACCESS_TOKEN_TTL`, preserve `Rbac3RuntimePolicySnapshotVO`'s lifecycle, immutability, and thread-safety constraints.
         */
        private static final Duration MAX_ACCESS_TOKEN_TTL = Duration.ofMinutes(30);
        /**
         * 字段 `MIN_REFRESH_TOKEN_TTL` 表示 `Rbac3RuntimePolicySnapshotVO` 中与 `MIN REFRESH TOKEN TTL` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `MIN_REFRESH_TOKEN_TTL` stores the `MIN REFRESH TOKEN TTL`-related state, dependency, configuration, or result of `Rbac3RuntimePolicySnapshotVO` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `MIN_REFRESH_TOKEN_TTL` 时应保持 `Rbac3RuntimePolicySnapshotVO` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `MIN_REFRESH_TOKEN_TTL`, preserve `Rbac3RuntimePolicySnapshotVO`'s lifecycle, immutability, and thread-safety constraints.
         */
        private static final Duration MIN_REFRESH_TOKEN_TTL = Duration.ofDays(1);
        /**
         * 字段 `MAX_REFRESH_TOKEN_TTL` 表示 `Rbac3RuntimePolicySnapshotVO` 中与 `MAX REFRESH TOKEN TTL` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `MAX_REFRESH_TOKEN_TTL` stores the `MAX REFRESH TOKEN TTL`-related state, dependency, configuration, or result of `Rbac3RuntimePolicySnapshotVO` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `MAX_REFRESH_TOKEN_TTL` 时应保持 `Rbac3RuntimePolicySnapshotVO` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `MAX_REFRESH_TOKEN_TTL`, preserve `Rbac3RuntimePolicySnapshotVO`'s lifecycle, immutability, and thread-safety constraints.
         */
        private static final Duration MAX_REFRESH_TOKEN_TTL = Duration.ofDays(30);
        /**
         * 字段 `MIN_IDLE_TIMEOUT` 表示 `Rbac3RuntimePolicySnapshotVO` 中与 `MIN IDLE TIMEOUT` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `MIN_IDLE_TIMEOUT` stores the `MIN IDLE TIMEOUT`-related state, dependency, configuration, or result of `Rbac3RuntimePolicySnapshotVO` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `MIN_IDLE_TIMEOUT` 时应保持 `Rbac3RuntimePolicySnapshotVO` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `MIN_IDLE_TIMEOUT`, preserve `Rbac3RuntimePolicySnapshotVO`'s lifecycle, immutability, and thread-safety constraints.
         */
        private static final Duration MIN_IDLE_TIMEOUT = Duration.ofMinutes(5);
        /**
         * 字段 `MAX_IDLE_TIMEOUT` 表示 `Rbac3RuntimePolicySnapshotVO` 中与 `MAX IDLE TIMEOUT` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `MAX_IDLE_TIMEOUT` stores the `MAX IDLE TIMEOUT`-related state, dependency, configuration, or result of `Rbac3RuntimePolicySnapshotVO` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `MAX_IDLE_TIMEOUT` 时应保持 `Rbac3RuntimePolicySnapshotVO` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `MAX_IDLE_TIMEOUT`, preserve `Rbac3RuntimePolicySnapshotVO`'s lifecycle, immutability, and thread-safety constraints.
         */
        private static final Duration MAX_IDLE_TIMEOUT = Duration.ofHours(8);
        /**
         * 字段 `MIN_ABSOLUTE_TIMEOUT` 表示 `Rbac3RuntimePolicySnapshotVO` 中与 `MIN ABSOLUTE TIMEOUT` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `MIN_ABSOLUTE_TIMEOUT` stores the `MIN ABSOLUTE TIMEOUT`-related state, dependency, configuration, or result of `Rbac3RuntimePolicySnapshotVO` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `MIN_ABSOLUTE_TIMEOUT` 时应保持 `Rbac3RuntimePolicySnapshotVO` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `MIN_ABSOLUTE_TIMEOUT`, preserve `Rbac3RuntimePolicySnapshotVO`'s lifecycle, immutability, and thread-safety constraints.
         */
        private static final Duration MIN_ABSOLUTE_TIMEOUT = Duration.ofHours(1);
        /**
         * 字段 `MAX_ABSOLUTE_TIMEOUT` 表示 `Rbac3RuntimePolicySnapshotVO` 中与 `MAX ABSOLUTE TIMEOUT` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `MAX_ABSOLUTE_TIMEOUT` stores the `MAX ABSOLUTE TIMEOUT`-related state, dependency, configuration, or result of `Rbac3RuntimePolicySnapshotVO` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `MAX_ABSOLUTE_TIMEOUT` 时应保持 `Rbac3RuntimePolicySnapshotVO` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `MAX_ABSOLUTE_TIMEOUT`, preserve `Rbac3RuntimePolicySnapshotVO`'s lifecycle, immutability, and thread-safety constraints.
         */
        private static final Duration MAX_ABSOLUTE_TIMEOUT = Duration.ofHours(24);

        /**
         * 构造器 `Rbac3RuntimePolicySnapshotVO` 用于创建并初始化 `Rbac3RuntimePolicySnapshotVO` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `Rbac3RuntimePolicySnapshotVO` creates and initializes `Rbac3RuntimePolicySnapshotVO`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `Rbac3RuntimePolicySnapshotVO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `Rbac3RuntimePolicySnapshotVO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param accessTokenTtl 输入参数 `accessTokenTtl`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param refreshTokenTtl 输入参数 `refreshTokenTtl`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionIdleTimeout 输入参数 `sessionIdleTimeout`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionAbsoluteTimeout 输入参数 `sessionAbsoluteTimeout`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param maximumActiveRoots 输入参数 `maximumActiveRoots`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param configVersions 输入参数 `configVersions`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public Rbac3RuntimePolicySnapshotVO {
            accessTokenTtl = Objects.requireNonNull(accessTokenTtl, "accessTokenTtl");
            refreshTokenTtl = Objects.requireNonNull(refreshTokenTtl, "refreshTokenTtl");
            sessionIdleTimeout = Objects.requireNonNull(
                    sessionIdleTimeout, "sessionIdleTimeout");
            sessionAbsoluteTimeout = Objects.requireNonNull(
                    sessionAbsoluteTimeout, "sessionAbsoluteTimeout");
            configVersions = Map.copyOf(Objects.requireNonNull(
                    configVersions, "configVersions"));
            requireRange(accessTokenTtl, MIN_ACCESS_TOKEN_TTL, MAX_ACCESS_TOKEN_TTL,
                    "ACCESS_TOKEN_TTL_OUT_OF_RANGE");
            requireRange(sessionIdleTimeout, MIN_IDLE_TIMEOUT, MAX_IDLE_TIMEOUT,
                    "SESSION_IDLE_TIMEOUT_OUT_OF_RANGE");
            requireRange(sessionAbsoluteTimeout, MIN_ABSOLUTE_TIMEOUT, MAX_ABSOLUTE_TIMEOUT,
                    "SESSION_ABSOLUTE_TIMEOUT_OUT_OF_RANGE");
            if (maximumActiveRoots < 1 || maximumActiveRoots > 32) {
                throw new IllegalArgumentException("MAXIMUM_ACTIVE_ROOTS_OUT_OF_RANGE");
            }
            if (sessionIdleTimeout.compareTo(sessionAbsoluteTimeout) > 0) {
                throw new IllegalArgumentException("IDLE_EXCEEDS_ABSOLUTE");
            }
            if (refreshTokenTtl.compareTo(sessionAbsoluteTimeout) < 0) {
                throw new IllegalArgumentException("REFRESH_BELOW_ABSOLUTE");
            }
            requireRange(refreshTokenTtl, MIN_REFRESH_TOKEN_TTL, MAX_REFRESH_TOKEN_TTL,
                    "REFRESH_TOKEN_TTL_OUT_OF_RANGE");
            configVersions.forEach((key, version) -> {
                if (key == null || key.isBlank() || version == null || version < 0) {
                    throw new IllegalArgumentException("INVALID_CONFIG_VERSION");
                }
            });
        }

        /**
         * 方法 `requireRange` 按照 `Rbac3RuntimePolicySnapshotVO` 的职责处理输入，完成 `require Range` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `requireRange` processes its inputs according to `Rbac3RuntimePolicySnapshotVO`'s responsibility, performs the `require Range` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `requireRange` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `requireRange`, then continue the business flow using its result, exception, or side effect.
         *
         * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param minimum 输入参数 `minimum`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param maximum 输入参数 `maximum`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param errorCode 输入参数 `errorCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        private static void requireRange(
                Duration value,
                Duration minimum,
                Duration maximum,
                String errorCode) {
            if (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
                throw new IllegalArgumentException(errorCode);
            }
        }
    }
