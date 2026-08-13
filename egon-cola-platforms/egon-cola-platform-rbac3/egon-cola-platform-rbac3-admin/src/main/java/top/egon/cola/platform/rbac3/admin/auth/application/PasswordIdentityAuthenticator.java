package top.egon.cola.platform.rbac3.admin.auth.application;

import org.springframework.security.crypto.password.PasswordEncoder;
import top.egon.cola.platform.rbac3.contract.auth.LoginRequest;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/**
 * 类型 `PasswordIdentityAuthenticator` 位于当前包内，是类型，用于承载 `Password Identity Authenticator` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `PasswordIdentityAuthenticator` is a type in its package and carries the responsibility, state, or contract for `Password Identity Authenticator`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Local password authenticator with an enumeration-safe failure contract.
 */
public final class PasswordIdentityAuthenticator implements IdentityAuthenticatorStrategy {

    /**
     * 字段 `MAX_FAILURES` 表示 `PasswordIdentityAuthenticator` 中与 `MAX FAILURES` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `MAX_FAILURES` stores the `MAX FAILURES`-related state, dependency, configuration, or result of `PasswordIdentityAuthenticator` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `MAX_FAILURES` 时应保持 `PasswordIdentityAuthenticator` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `MAX_FAILURES`, preserve `PasswordIdentityAuthenticator`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final int MAX_FAILURES = 5;
    /**
     * 字段 `LOCK_DURATION` 表示 `PasswordIdentityAuthenticator` 中与 `LOCK DURATION` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `LOCK_DURATION` stores the `LOCK DURATION`-related state, dependency, configuration, or result of `PasswordIdentityAuthenticator` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `LOCK_DURATION` 时应保持 `PasswordIdentityAuthenticator` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `LOCK_DURATION`, preserve `PasswordIdentityAuthenticator`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    /**
     * 字段 `MAX_BCRYPT_BYTES` 表示 `PasswordIdentityAuthenticator` 中与 `MAX BCRYPT BYTES` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `MAX_BCRYPT_BYTES` stores the `MAX BCRYPT BYTES`-related state, dependency, configuration, or result of `PasswordIdentityAuthenticator` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `MAX_BCRYPT_BYTES` 时应保持 `PasswordIdentityAuthenticator` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `MAX_BCRYPT_BYTES`, preserve `PasswordIdentityAuthenticator`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final int MAX_BCRYPT_BYTES = 72;

    /**
     * 字段 `credentialStore` 表示 `PasswordIdentityAuthenticator` 中与 `credential Store` 相关的状态、依赖、配置或结果（声明类型 `CredentialStore`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `credentialStore` stores the `credential Store`-related state, dependency, configuration, or result of `PasswordIdentityAuthenticator` (declared type `CredentialStore`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `credentialStore` 时应保持 `PasswordIdentityAuthenticator` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `credentialStore`, preserve `PasswordIdentityAuthenticator`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final CredentialStore credentialStore;
    /**
     * 字段 `passwordEncoder` 表示 `PasswordIdentityAuthenticator` 中与 `password Encoder` 相关的状态、依赖、配置或结果（声明类型 `PasswordEncoder`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `passwordEncoder` stores the `password Encoder`-related state, dependency, configuration, or result of `PasswordIdentityAuthenticator` (declared type `PasswordEncoder`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `passwordEncoder` 时应保持 `PasswordIdentityAuthenticator` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `passwordEncoder`, preserve `PasswordIdentityAuthenticator`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final PasswordEncoder passwordEncoder;
    /**
     * 字段 `dummyHash` 表示 `PasswordIdentityAuthenticator` 中与 `dummy Hash` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `dummyHash` stores the `dummy Hash`-related state, dependency, configuration, or result of `PasswordIdentityAuthenticator` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `dummyHash` 时应保持 `PasswordIdentityAuthenticator` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `dummyHash`, preserve `PasswordIdentityAuthenticator`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final String dummyHash;

    /**
     * 构造器 `PasswordIdentityAuthenticator` 用于创建并初始化 `PasswordIdentityAuthenticator` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `PasswordIdentityAuthenticator` creates and initializes `PasswordIdentityAuthenticator`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `PasswordIdentityAuthenticator` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `PasswordIdentityAuthenticator`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param credentialStore 输入参数 `credentialStore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param passwordEncoder 输入参数 `passwordEncoder`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public PasswordIdentityAuthenticator(
            CredentialStore credentialStore,
            PasswordEncoder passwordEncoder) {
        this.credentialStore = Objects.requireNonNull(credentialStore, "credentialStore");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder");
        this.dummyHash = passwordEncoder.encode("rbac3-enumeration-resistant-dummy-password");
    }

    /**
     * 方法 `authenticate` 按照 `PasswordIdentityAuthenticator` 的职责处理输入，完成 `authenticate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `authenticate` processes its inputs according to `PasswordIdentityAuthenticator`'s responsibility, performs the `authenticate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `authenticate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `authenticate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public AuthenticatedIdentity authenticate(LoginRequest request, Instant now) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(now, "now");
        String tenantCode = normalize(request.tenantCode());
        String username = normalize(request.username());
        return credentialStore.withCredential(tenantCode, username,
                credential -> authenticateLocked(credential, request.password(), now));
    }

    /**
     * 方法 `authenticateLocked` 按照 `PasswordIdentityAuthenticator` 的职责处理输入，完成 `authenticate Locked` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `authenticateLocked` processes its inputs according to `PasswordIdentityAuthenticator`'s responsibility, performs the `authenticate Locked` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `authenticateLocked` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `authenticateLocked`, then continue the business flow using its result, exception, or side effect.
     *
     * @param credential 输入参数 `credential`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param rawPassword 输入参数 `rawPassword`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private AuthenticatedIdentity authenticateLocked(
            PasswordCredential credential,
            String rawPassword,
            Instant now) {
        if (credential == null) {
            passwordEncoder.matches(safePassword(rawPassword), dummyHash);
            throw failed();
        }
        if (!credential.active()
                || credential.lockedUntil() != null && now.isBefore(credential.lockedUntil())) {
            passwordEncoder.matches(safePassword(rawPassword), dummyHash);
            throw failed();
        }
        boolean overlong = rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_BYTES;
        boolean passwordMatches = passwordEncoder.matches(
                safePassword(rawPassword), credential.passwordHash());
        if (overlong || !passwordMatches) {
            int failureCount = credential.failureCount() + 1;
            Instant lockedUntil = failureCount >= MAX_FAILURES
                    ? now.plus(LOCK_DURATION)
                    : null;
            credentialStore.save(credential.failed(failureCount, lockedUntil));
            throw failed();
        }
        credentialStore.save(credential.succeeded());
        if (passwordEncoder.upgradeEncoding(credential.passwordHash())) {
            credentialStore.updatePasswordHash(
                    credential,
                    passwordEncoder.encode(rawPassword),
                    now);
        }
        return new AuthenticatedIdentity(
                credential.tenantCode(),
                credential.userId(),
                "PASSWORD",
                1);
    }

    /**
     * 方法 `safePassword` 按照 `PasswordIdentityAuthenticator` 的职责处理输入，完成 `safe Password` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `safePassword` processes its inputs according to `PasswordIdentityAuthenticator`'s responsibility, performs the `safe Password` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `safePassword` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `safePassword`, then continue the business flow using its result, exception, or side effect.
     *
     * @param rawPassword 输入参数 `rawPassword`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String safePassword(String rawPassword) {
        return rawPassword.getBytes(StandardCharsets.UTF_8).length <= MAX_BCRYPT_BYTES
                ? rawPassword
                : "rbac3-overlong-password-dummy";
    }

    /**
     * 方法 `normalize` 按照 `PasswordIdentityAuthenticator` 的职责处理输入，完成 `normalize` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `normalize` processes its inputs according to `PasswordIdentityAuthenticator`'s responsibility, performs the `normalize` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `normalize` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `normalize`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String normalize(String value) {
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
    }

    /**
     * 方法 `failed` 按照 `PasswordIdentityAuthenticator` 的职责处理输入，完成 `failed` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `failed` processes its inputs according to `PasswordIdentityAuthenticator`'s responsibility, performs the `failed` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `failed` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `failed`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static AuthenticationFailed failed() {
        return new AuthenticationFailed();
    }

    /**
     * 类型 `CredentialStore` 位于 `PasswordIdentityAuthenticator` 内，是接口，用于承载 `Credential Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `CredentialStore` is an interface inside `PasswordIdentityAuthenticator` and carries the responsibility, state, or contract for `Credential Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `CredentialStore` 作为 `PasswordIdentityAuthenticator` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `CredentialStore` as the responsibility boundary of `PasswordIdentityAuthenticator`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface CredentialStore {

        /**
         * 方法 `withCredential` 按照 `CredentialStore` 的职责处理输入，完成 `with Credential` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `withCredential` processes its inputs according to `CredentialStore`'s responsibility, performs the `with Credential` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `withCredential` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
          * Usage: provide contract-compliant arguments before calling `withCredential`, then continue the business flow using its result, exception, or side effect.
          *
          * @param <T> 类型参数表示凭据回调结果的具体类型；type parameter representing the credential callback result type.
          * @param tenantCode 输入参数 `tenantCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param normalizedUsername 输入参数 `normalizedUsername`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param action 输入参数 `action`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        <T> T withCredential(
                String tenantCode,
                String normalizedUsername,
                Function<PasswordCredential, T> action);

        /**
         * 方法 `save` 按照 `CredentialStore` 的职责处理输入，完成 `save` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `save` processes its inputs according to `CredentialStore`'s responsibility, performs the `save` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `save` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `save`, then continue the business flow using its result, exception, or side effect.
         *
         * @param credential 输入参数 `credential`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void save(PasswordCredential credential);

        /**
         * 方法 `updatePasswordHash` 按照 `CredentialStore` 的职责处理输入，完成 `update Password Hash` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `updatePasswordHash` processes its inputs according to `CredentialStore`'s responsibility, performs the `update Password Hash` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `updatePasswordHash` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `updatePasswordHash`, then continue the business flow using its result, exception, or side effect.
         *
         * @param credential 输入参数 `credential`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param passwordHash 输入参数 `passwordHash`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param changedAt 输入参数 `changedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        default void updatePasswordHash(
                PasswordCredential credential,
                String passwordHash,
                Instant changedAt) {
        }
    }

    /**
     * 类型 `PasswordCredential` 位于 `PasswordIdentityAuthenticator` 内，是记录类型，用于承载 `Password Credential` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `PasswordCredential` is a record inside `PasswordIdentityAuthenticator` and carries the responsibility, state, or contract for `Password Credential`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `PasswordCredential` 作为 `PasswordIdentityAuthenticator` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `PasswordCredential` as the responsibility boundary of `PasswordIdentityAuthenticator`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantCode 记录组件 `tenantCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantCode` carries constructor data whose meaning is defined by the record contract.
     * @param normalizedUsername 记录组件 `normalizedUsername` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `normalizedUsername` carries constructor data whose meaning is defined by the record contract.
     * @param userId 记录组件 `userId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `userId` carries constructor data whose meaning is defined by the record contract.
     * @param passwordHash 记录组件 `passwordHash` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `passwordHash` carries constructor data whose meaning is defined by the record contract.
     * @param failureCount 记录组件 `failureCount` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `failureCount` carries constructor data whose meaning is defined by the record contract.
     * @param lockedUntil 记录组件 `lockedUntil` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `lockedUntil` carries constructor data whose meaning is defined by the record contract.
     * @param active 记录组件 `active` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `active` carries constructor data whose meaning is defined by the record contract.
     */
    public record PasswordCredential(
            /**
             * 字段 `tenantCode` 表示 `PasswordCredential` 中与 `tenant Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantCode` stores the `tenant Code`-related state, dependency, configuration, or result of `PasswordCredential` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantCode` 时应保持 `PasswordCredential` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantCode`, preserve `PasswordCredential`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantCode,
            /**
             * 字段 `normalizedUsername` 表示 `PasswordCredential` 中与 `normalized Username` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `normalizedUsername` stores the `normalized Username`-related state, dependency, configuration, or result of `PasswordCredential` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `normalizedUsername` 时应保持 `PasswordCredential` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `normalizedUsername`, preserve `PasswordCredential`'s lifecycle, immutability, and thread-safety constraints.
             */
            String normalizedUsername,
            /**
             * 字段 `userId` 表示 `PasswordCredential` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `PasswordCredential` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userId` 时应保持 `PasswordCredential` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userId`, preserve `PasswordCredential`'s lifecycle, immutability, and thread-safety constraints.
             */
            String userId,
            /**
             * 字段 `passwordHash` 表示 `PasswordCredential` 中与 `password Hash` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `passwordHash` stores the `password Hash`-related state, dependency, configuration, or result of `PasswordCredential` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `passwordHash` 时应保持 `PasswordCredential` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `passwordHash`, preserve `PasswordCredential`'s lifecycle, immutability, and thread-safety constraints.
             */
            String passwordHash,
            /**
             * 字段 `failureCount` 表示 `PasswordCredential` 中与 `failure Count` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `failureCount` stores the `failure Count`-related state, dependency, configuration, or result of `PasswordCredential` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `failureCount` 时应保持 `PasswordCredential` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `failureCount`, preserve `PasswordCredential`'s lifecycle, immutability, and thread-safety constraints.
             */
            int failureCount,
            /**
             * 字段 `lockedUntil` 表示 `PasswordCredential` 中与 `locked Until` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `lockedUntil` stores the `locked Until`-related state, dependency, configuration, or result of `PasswordCredential` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `lockedUntil` 时应保持 `PasswordCredential` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `lockedUntil`, preserve `PasswordCredential`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant lockedUntil,
            /**
             * 字段 `active` 表示 `PasswordCredential` 中与 `active` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `active` stores the `active`-related state, dependency, configuration, or result of `PasswordCredential` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `active` 时应保持 `PasswordCredential` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `active`, preserve `PasswordCredential`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean active
    ) {

        /**
         * 构造器 `PasswordCredential` 用于创建并初始化 `PasswordCredential` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `PasswordCredential` creates and initializes `PasswordCredential`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `PasswordCredential` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `PasswordCredential`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tenantCode 输入参数 `tenantCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param normalizedUsername 输入参数 `normalizedUsername`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param passwordHash 输入参数 `passwordHash`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param failureCount 输入参数 `failureCount`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param lockedUntil 输入参数 `lockedUntil`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param active 输入参数 `active`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public PasswordCredential {
            Objects.requireNonNull(tenantCode, "tenantCode");
            Objects.requireNonNull(normalizedUsername, "normalizedUsername");
            Objects.requireNonNull(userId, "userId");
            Objects.requireNonNull(passwordHash, "passwordHash");
            if (failureCount < 0) {
                throw new IllegalArgumentException("failureCount must not be negative");
            }
        }

        /**
         * 方法 `failed` 按照 `PasswordCredential` 的职责处理输入，完成 `failed` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `failed` processes its inputs according to `PasswordCredential`'s responsibility, performs the `failed` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `failed` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `failed`, then continue the business flow using its result, exception, or side effect.
         *
         * @param newFailureCount 输入参数 `newFailureCount`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param newLockedUntil 输入参数 `newLockedUntil`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        PasswordCredential failed(int newFailureCount, Instant newLockedUntil) {
            return new PasswordCredential(
                    tenantCode,
                    normalizedUsername,
                    userId,
                    passwordHash,
                    newFailureCount,
                    newLockedUntil,
                    active);
        }

        /**
         * 方法 `succeeded` 按照 `PasswordCredential` 的职责处理输入，完成 `succeeded` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `succeeded` processes its inputs according to `PasswordCredential`'s responsibility, performs the `succeeded` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `succeeded` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `succeeded`, then continue the business flow using its result, exception, or side effect.
         *
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        PasswordCredential succeeded() {
            return new PasswordCredential(
                    tenantCode,
                    normalizedUsername,
                    userId,
                    passwordHash,
                    0,
                    null,
                    active);
        }

        /**
         * 方法 `toString` 按照 `PasswordCredential` 的职责处理输入，完成 `to String` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `toString` processes its inputs according to `PasswordCredential`'s responsibility, performs the `to String` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `toString` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `toString`, then continue the business flow using its result, exception, or side effect.
         *
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        @Override
        public String toString() {
            return "PasswordCredential[tenantCode=" + tenantCode
                    + ", normalizedUsername=" + normalizedUsername
                    + ", userId=" + userId
                    + ", passwordHash=<redacted>, failureCount=" + failureCount
                    + ", lockedUntil=" + lockedUntil
                    + ", active=" + active + ']';
        }
    }

    /**
     * 类型 `AuthenticationFailed` 位于 `PasswordIdentityAuthenticator` 内，是类型，用于承载 `Authentication Failed` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AuthenticationFailed` is a type inside `PasswordIdentityAuthenticator` and carries the responsibility, state, or contract for `Authentication Failed`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AuthenticationFailed` 作为 `PasswordIdentityAuthenticator` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AuthenticationFailed` as the responsibility boundary of `PasswordIdentityAuthenticator`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public static final class AuthenticationFailed extends RuntimeException {

        /**
         * 字段 `REASON_CODE` 表示 `AuthenticationFailed` 中与 `REASON CODE` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `REASON_CODE` stores the `REASON CODE`-related state, dependency, configuration, or result of `AuthenticationFailed` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `REASON_CODE` 时应保持 `AuthenticationFailed` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `REASON_CODE`, preserve `AuthenticationFailed`'s lifecycle, immutability, and thread-safety constraints.
         */
        private static final String REASON_CODE = "AUTHENTICATION_FAILED";

        /**
         * 构造器 `AuthenticationFailed` 用于创建并初始化 `AuthenticationFailed` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `AuthenticationFailed` creates and initializes `AuthenticationFailed`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `AuthenticationFailed` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `AuthenticationFailed`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         */
        private AuthenticationFailed() {
            super(REASON_CODE);
        }

        /**
         * 方法 `reasonCode` 按照 `AuthenticationFailed` 的职责处理输入，完成 `reason Code` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `reasonCode` processes its inputs according to `AuthenticationFailed`'s responsibility, performs the `reason Code` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `reasonCode` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `reasonCode`, then continue the business flow using its result, exception, or side effect.
         *
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        public String reasonCode() {
            return REASON_CODE;
        }
    }
}
