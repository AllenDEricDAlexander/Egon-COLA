package top.egon.cola.platform.rbac3.admin.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import top.egon.cola.platform.rbac3.contract.auth.LoginRequest;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import top.egon.cola.platform.rbac3.admin.auth.repository.CredentialRepository;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.AuthenticatedIdentityVO;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.PasswordCredentialVO;
import top.egon.cola.platform.rbac3.admin.auth.domain.exception.AuthenticationFailedException;

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
     * 字段 `credentialStore` 表示 `PasswordIdentityAuthenticator` 中与 `credential Store` 相关的状态、依赖、配置或结果（声明类型 `CredentialRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `credentialStore` stores the `credential Store`-related state, dependency, configuration, or result of `PasswordIdentityAuthenticator` (declared type `CredentialRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `credentialStore` 时应保持 `PasswordIdentityAuthenticator` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `credentialStore`, preserve `PasswordIdentityAuthenticator`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final CredentialRepository credentialStore;
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
            CredentialRepository credentialStore,
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
    public AuthenticatedIdentityVO authenticate(LoginRequest request, Instant now) {
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
    private AuthenticatedIdentityVO authenticateLocked(
            PasswordCredentialVO credential,
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
        return new AuthenticatedIdentityVO(
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
    private static AuthenticationFailedException failed() {
        return new AuthenticationFailedException();
    }



    }
