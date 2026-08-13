package top.egon.cola.platform.rbac3.admin.auth.application;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 类型 `JwtKeyRingService` 位于当前包内，是类型，用于承载 `Jwt Key Ring Service` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `JwtKeyRingService` is a type in its package and carries the responsibility, state, or contract for `Jwt Key Ring Service`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Auditable JWT public-key lifecycle. Private key material is intentionally absent.
 */
public final class JwtKeyRingService {

    /**
     * 字段 `PUBLIC_JWK_FIELDS` 表示 `JwtKeyRingService` 中与 `PUBLIC JWK FIELDS` 相关的状态、依赖、配置或结果（声明类型 `Set&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `PUBLIC_JWK_FIELDS` stores the `PUBLIC JWK FIELDS`-related state, dependency, configuration, or result of `JwtKeyRingService` (declared type `Set&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `PUBLIC_JWK_FIELDS` 时应保持 `JwtKeyRingService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `PUBLIC_JWK_FIELDS`, preserve `JwtKeyRingService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final Set<String> PUBLIC_JWK_FIELDS = Set.of(
            "kty", "kid", "use", "alg", "n", "e", "crv", "x", "y");

    /**
     * 字段 `keys` 表示 `JwtKeyRingService` 中与 `keys` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, KeyDescriptor&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `keys` stores the `keys`-related state, dependency, configuration, or result of `JwtKeyRingService` (declared type `Map&lt;String, KeyDescriptor&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `keys` 时应保持 `JwtKeyRingService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `keys`, preserve `JwtKeyRingService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Map<String, KeyDescriptor> keys = new LinkedHashMap<>();
    /**
     * 字段 `minimumVerificationRetention` 表示 `JwtKeyRingService` 中与 `minimum Verification Retention` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `minimumVerificationRetention` stores the `minimum Verification Retention`-related state, dependency, configuration, or result of `JwtKeyRingService` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `minimumVerificationRetention` 时应保持 `JwtKeyRingService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `minimumVerificationRetention`, preserve `JwtKeyRingService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Duration minimumVerificationRetention;

    /**
     * 构造器 `JwtKeyRingService` 用于创建并初始化 `JwtKeyRingService` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `JwtKeyRingService` creates and initializes `JwtKeyRingService`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `JwtKeyRingService` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `JwtKeyRingService`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param initialKeys 输入参数 `initialKeys`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param minimumVerificationRetention 输入参数 `minimumVerificationRetention`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public JwtKeyRingService(
            Collection<KeyDescriptor> initialKeys,
            Duration minimumVerificationRetention) {
        this.minimumVerificationRetention = Objects.requireNonNull(
                minimumVerificationRetention, "minimumVerificationRetention");
        if (minimumVerificationRetention.isNegative() || minimumVerificationRetention.isZero()) {
            throw new IllegalArgumentException("minimumVerificationRetention must be positive");
        }
        for (KeyDescriptor key : initialKeys) {
            if (keys.putIfAbsent(key.kid(), sanitize(key)) != null) {
                throw new IllegalArgumentException("duplicate kid " + key.kid());
            }
        }
        requireAtMostOneSigning();
    }

    /**
     * 方法 `publishPrepared` 按照 `JwtKeyRingService` 的职责处理输入，完成 `publish Prepared` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `publishPrepared` processes its inputs according to `JwtKeyRingService`'s responsibility, performs the `publish Prepared` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `publishPrepared` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `publishPrepared`, then continue the business flow using its result, exception, or side effect.
     *
     * @param prepared 输入参数 `prepared`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public synchronized void publishPrepared(KeyDescriptor prepared) {
        if (prepared.state() != KeyState.PREPARED) {
            throw new IllegalArgumentException("new key must be PREPARED");
        }
        if (keys.putIfAbsent(prepared.kid(), sanitize(prepared)) != null) {
            throw new IllegalArgumentException("duplicate kid " + prepared.kid());
        }
    }

    /**
     * 方法 `promoteToSigning` 按照 `JwtKeyRingService` 的职责处理输入，完成 `promote To Signing` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `promoteToSigning` processes its inputs according to `JwtKeyRingService`'s responsibility, performs the `promote To Signing` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `promoteToSigning` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `promoteToSigning`, then continue the business flow using its result, exception, or side effect.
     *
     * @param kid 输入参数 `kid`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public synchronized void promoteToSigning(String kid, Instant now) {
        KeyDescriptor target = requiredKey(kid);
        if (target.state() != KeyState.PREPARED) {
            throw new IllegalStateException("only PREPARED key can become SIGNING");
        }
        keys.replaceAll((keyId, current) -> current.state() == KeyState.SIGNING
                ? current.transition(
                        KeyState.VERIFY_ONLY,
                        current.signingSince(),
                        now.plus(minimumVerificationRetention))
                : current);
        keys.put(kid, target.transition(KeyState.SIGNING, now, null));
        requireAtMostOneSigning();
    }

    /**
     * 方法 `retire` 按照 `JwtKeyRingService` 的职责处理输入，完成 `retire` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `retire` processes its inputs according to `JwtKeyRingService`'s responsibility, performs the `retire` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `retire` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `retire`, then continue the business flow using its result, exception, or side effect.
     *
     * @param kid 输入参数 `kid`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public synchronized void retire(String kid, Instant now) {
        KeyDescriptor current = requiredKey(kid);
        if (current.state() != KeyState.VERIFY_ONLY) {
            throw new IllegalStateException("only VERIFY_ONLY key can be retired");
        }
        if (current.retireNotBefore() == null || now.isBefore(current.retireNotBefore())) {
            throw new IllegalStateException("verification retention window has not elapsed");
        }
        keys.put(kid, current.transition(
                KeyState.RETIRED,
                current.signingSince(),
                current.retireNotBefore()));
    }

    /**
     * 方法 `signingKey` 按照 `JwtKeyRingService` 的职责处理输入，完成 `signing Key` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `signingKey` processes its inputs according to `JwtKeyRingService`'s responsibility, performs the `signing Key` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `signingKey` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `signingKey`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public synchronized KeyDescriptor signingKey() {
        List<KeyDescriptor> signing = keys.values().stream()
                .filter(key -> key.state() == KeyState.SIGNING)
                .toList();
        if (signing.size() != 1) {
            throw new IllegalStateException("exactly one SIGNING key is required");
        }
        return signing.getFirst();
    }

    /**
     * 方法 `publicJwks` 按照 `JwtKeyRingService` 的职责处理输入，完成 `public Jwks` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `publicJwks` processes its inputs according to `JwtKeyRingService`'s responsibility, performs the `public Jwks` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `publicJwks` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `publicJwks`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public synchronized Map<String, Object> publicJwks() {
        List<Map<String, Object>> visible = keys.values().stream()
                .filter(key -> key.state() != KeyState.RETIRED)
                .sorted(Comparator.comparing(KeyDescriptor::kid))
                .map(KeyDescriptor::publicJwk)
                .toList();
        return Map.of("keys", visible);
    }

    /**
     * 方法 `snapshot` 按照 `JwtKeyRingService` 的职责处理输入，完成 `snapshot` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `snapshot` processes its inputs according to `JwtKeyRingService`'s responsibility, performs the `snapshot` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `snapshot` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `snapshot`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public synchronized List<KeyDescriptor> snapshot() {
        return List.copyOf(new ArrayList<>(keys.values()));
    }

    /**
     * 方法 `requiredKey` 按照 `JwtKeyRingService` 的职责处理输入，完成 `required Key` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requiredKey` processes its inputs according to `JwtKeyRingService`'s responsibility, performs the `required Key` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requiredKey` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requiredKey`, then continue the business flow using its result, exception, or side effect.
     *
     * @param kid 输入参数 `kid`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private KeyDescriptor requiredKey(String kid) {
        KeyDescriptor key = keys.get(kid);
        if (key == null) {
            throw new IllegalArgumentException("unknown kid " + kid);
        }
        return key;
    }

    /**
     * 方法 `requireAtMostOneSigning` 按照 `JwtKeyRingService` 的职责处理输入，完成 `require At Most One Signing` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireAtMostOneSigning` processes its inputs according to `JwtKeyRingService`'s responsibility, performs the `require At Most One Signing` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireAtMostOneSigning` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireAtMostOneSigning`, then continue the business flow using its result, exception, or side effect.
     */
    private void requireAtMostOneSigning() {
        long count = keys.values().stream()
                .filter(key -> key.state() == KeyState.SIGNING)
                .count();
        if (count > 1) {
            throw new IllegalArgumentException("only one SIGNING key is allowed");
        }
    }

    /**
     * 方法 `sanitize` 按照 `JwtKeyRingService` 的职责处理输入，完成 `sanitize` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `sanitize` processes its inputs according to `JwtKeyRingService`'s responsibility, performs the `sanitize` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `sanitize` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `sanitize`, then continue the business flow using its result, exception, or side effect.
     *
     * @param descriptor 输入参数 `descriptor`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static KeyDescriptor sanitize(KeyDescriptor descriptor) {
        Map<String, Object> publicJwk = new LinkedHashMap<>();
        descriptor.publicJwk().forEach((name, value) -> {
            if (PUBLIC_JWK_FIELDS.contains(name)) {
                publicJwk.put(name, value);
            }
        });
        publicJwk.put("kid", descriptor.kid());
        publicJwk.put("alg", descriptor.algorithm());
        return new KeyDescriptor(
                descriptor.kid(),
                descriptor.algorithm(),
                publicJwk,
                descriptor.state(),
                descriptor.signingSince(),
                descriptor.retireNotBefore());
    }

    /**
     * 类型 `KeyDescriptor` 位于 `JwtKeyRingService` 内，是记录类型，用于承载 `Key Descriptor` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `KeyDescriptor` is a record inside `JwtKeyRingService` and carries the responsibility, state, or contract for `Key Descriptor`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `KeyDescriptor` 作为 `JwtKeyRingService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `KeyDescriptor` as the responsibility boundary of `JwtKeyRingService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param kid 记录组件 `kid` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `kid` carries constructor data whose meaning is defined by the record contract.
     * @param algorithm 记录组件 `algorithm` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `algorithm` carries constructor data whose meaning is defined by the record contract.
     * @param publicJwk 记录组件 `publicJwk` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `publicJwk` carries constructor data whose meaning is defined by the record contract.
     * @param state 记录组件 `state` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `state` carries constructor data whose meaning is defined by the record contract.
     * @param signingSince 记录组件 `signingSince` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `signingSince` carries constructor data whose meaning is defined by the record contract.
     * @param retireNotBefore 记录组件 `retireNotBefore` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `retireNotBefore` carries constructor data whose meaning is defined by the record contract.
     */
    public record KeyDescriptor(
            /**
             * 字段 `kid` 表示 `KeyDescriptor` 中与 `kid` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `kid` stores the `kid`-related state, dependency, configuration, or result of `KeyDescriptor` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `kid` 时应保持 `KeyDescriptor` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `kid`, preserve `KeyDescriptor`'s lifecycle, immutability, and thread-safety constraints.
             */
            String kid,
            /**
             * 字段 `algorithm` 表示 `KeyDescriptor` 中与 `algorithm` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `algorithm` stores the `algorithm`-related state, dependency, configuration, or result of `KeyDescriptor` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `algorithm` 时应保持 `KeyDescriptor` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `algorithm`, preserve `KeyDescriptor`'s lifecycle, immutability, and thread-safety constraints.
             */
            String algorithm,
            /**
             * 字段 `publicJwk` 表示 `KeyDescriptor` 中与 `public Jwk` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, Object&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `publicJwk` stores the `public Jwk`-related state, dependency, configuration, or result of `KeyDescriptor` (declared type `Map&lt;String, Object&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `publicJwk` 时应保持 `KeyDescriptor` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `publicJwk`, preserve `KeyDescriptor`'s lifecycle, immutability, and thread-safety constraints.
             */
            Map<String, Object> publicJwk,
            /**
             * 字段 `state` 表示 `KeyDescriptor` 中与 `state` 相关的状态、依赖、配置或结果（声明类型 `KeyState`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `state` stores the `state`-related state, dependency, configuration, or result of `KeyDescriptor` (declared type `KeyState`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `state` 时应保持 `KeyDescriptor` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `state`, preserve `KeyDescriptor`'s lifecycle, immutability, and thread-safety constraints.
             */
            KeyState state,
            /**
             * 字段 `signingSince` 表示 `KeyDescriptor` 中与 `signing Since` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `signingSince` stores the `signing Since`-related state, dependency, configuration, or result of `KeyDescriptor` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `signingSince` 时应保持 `KeyDescriptor` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `signingSince`, preserve `KeyDescriptor`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant signingSince,
            /**
             * 字段 `retireNotBefore` 表示 `KeyDescriptor` 中与 `retire Not Before` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `retireNotBefore` stores the `retire Not Before`-related state, dependency, configuration, or result of `KeyDescriptor` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `retireNotBefore` 时应保持 `KeyDescriptor` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `retireNotBefore`, preserve `KeyDescriptor`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant retireNotBefore
    ) {

        /**
         * 构造器 `KeyDescriptor` 用于创建并初始化 `KeyDescriptor` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `KeyDescriptor` creates and initializes `KeyDescriptor`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `KeyDescriptor` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `KeyDescriptor`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param kid 输入参数 `kid`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param algorithm 输入参数 `algorithm`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param publicJwk 输入参数 `publicJwk`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param state 输入参数 `state`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param signingSince 输入参数 `signingSince`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param retireNotBefore 输入参数 `retireNotBefore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public KeyDescriptor {
            kid = required(kid, "kid");
            algorithm = required(algorithm, "algorithm");
            if (!"RS256".equals(algorithm)) {
                throw new IllegalArgumentException("only RS256 is supported");
            }
            publicJwk = Map.copyOf(Objects.requireNonNull(publicJwk, "publicJwk"));
            state = Objects.requireNonNull(state, "state");
            if (state == KeyState.SIGNING && signingSince == null) {
                throw new IllegalArgumentException("SIGNING key requires signingSince");
            }
        }

        /**
         * 方法 `transition` 按照 `KeyDescriptor` 的职责处理输入，完成 `transition` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `transition` processes its inputs according to `KeyDescriptor`'s responsibility, performs the `transition` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `transition` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `transition`, then continue the business flow using its result, exception, or side effect.
         *
         * @param nextState 输入参数 `nextState`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param nextSigningSince 输入参数 `nextSigningSince`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param nextRetireNotBefore 输入参数 `nextRetireNotBefore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        KeyDescriptor transition(
                KeyState nextState,
                Instant nextSigningSince,
                Instant nextRetireNotBefore) {
            return new KeyDescriptor(
                    kid,
                    algorithm,
                    publicJwk,
                    nextState,
                    nextSigningSince,
                    nextRetireNotBefore);
        }
    }

    /**
     * 类型 `KeyState` 位于 `JwtKeyRingService` 内，是枚举，用于承载 `Key State` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `KeyState` is an enum inside `JwtKeyRingService` and carries the responsibility, state, or contract for `Key State`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `KeyState` 作为 `JwtKeyRingService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `KeyState` as the responsibility boundary of `JwtKeyRingService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum KeyState {
        /**
         * 字段 `PREPARED` 表示 `KeyState` 中与 `PREPARED` 相关的状态、依赖、配置或结果（声明类型 `KeyState`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `PREPARED` stores the `PREPARED`-related state, dependency, configuration, or result of `KeyState` (declared type `KeyState`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `PREPARED` 时应保持 `KeyState` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `PREPARED`, preserve `KeyState`'s lifecycle, immutability, and thread-safety constraints.
         */
        PREPARED,
        /**
         * 字段 `SIGNING` 表示 `KeyState` 中与 `SIGNING` 相关的状态、依赖、配置或结果（声明类型 `KeyState`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `SIGNING` stores the `SIGNING`-related state, dependency, configuration, or result of `KeyState` (declared type `KeyState`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `SIGNING` 时应保持 `KeyState` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `SIGNING`, preserve `KeyState`'s lifecycle, immutability, and thread-safety constraints.
         */
        SIGNING,
        /**
         * 字段 `VERIFY_ONLY` 表示 `KeyState` 中与 `VERIFY ONLY` 相关的状态、依赖、配置或结果（声明类型 `KeyState`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `VERIFY_ONLY` stores the `VERIFY ONLY`-related state, dependency, configuration, or result of `KeyState` (declared type `KeyState`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `VERIFY_ONLY` 时应保持 `KeyState` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `VERIFY_ONLY`, preserve `KeyState`'s lifecycle, immutability, and thread-safety constraints.
         */
        VERIFY_ONLY,
        /**
         * 字段 `RETIRED` 表示 `KeyState` 中与 `RETIRED` 相关的状态、依赖、配置或结果（声明类型 `KeyState`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `RETIRED` stores the `RETIRED`-related state, dependency, configuration, or result of `KeyState` (declared type `KeyState`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `RETIRED` 时应保持 `KeyState` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `RETIRED`, preserve `KeyState`'s lifecycle, immutability, and thread-safety constraints.
         */
        RETIRED
    }

    /**
     * 方法 `required` 按照 `JwtKeyRingService` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `JwtKeyRingService`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param fieldName 输入参数 `fieldName`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
