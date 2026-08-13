package top.egon.cola.platform.rbac3.admin.integration.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * 类型 `GatewayAdminStatusCredentialProvider` 位于当前包内，是接口，用于承载 `Gateway Admin Status Credential Provider` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `GatewayAdminStatusCredentialProvider` is an interface in its package and carries the responsibility, state, or contract for `Gateway Admin Status Credential Provider`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Supplies a short-lived read-only OAuth credential without storing it in properties.
 */
@FunctionalInterface
public interface GatewayAdminStatusCredentialProvider {

    /**
     * 方法 `current` 按照 `GatewayAdminStatusCredentialProvider` 的职责处理输入，完成 `current` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `current` processes its inputs according to `GatewayAdminStatusCredentialProvider`'s responsibility, performs the `current` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `current` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `current`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    Optional<BearerCredential> current();

    /**
     * 方法 `rotatingFile` 按照 `GatewayAdminStatusCredentialProvider` 的职责处理输入，完成 `rotating File` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rotatingFile` processes its inputs according to `GatewayAdminStatusCredentialProvider`'s responsibility, performs the `rotating File` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rotatingFile` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rotatingFile`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tokenFile 输入参数 `tokenFile`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param objectMapper 输入参数 `objectMapper`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    static GatewayAdminStatusCredentialProvider rotatingFile(
            Path tokenFile,
            ObjectMapper objectMapper,
            Clock clock) {
        Objects.requireNonNull(tokenFile, "tokenFile");
        Objects.requireNonNull(objectMapper, "objectMapper");
        Objects.requireNonNull(clock, "clock");
        return () -> {
            if (!Files.isRegularFile(tokenFile)) {
                return Optional.empty();
            }
            try {
                JsonNode json = objectMapper.readTree(Files.readString(tokenFile));
                String token = json.path("accessToken").asText();
                String expiresAt = json.path("expiresAt").asText();
                if (token.isBlank() || expiresAt.isBlank()) {
                    return Optional.empty();
                }
                BearerCredential credential = new BearerCredential(
                        token, Instant.parse(expiresAt));
                return credential.expiresAt().isAfter(clock.instant())
                        ? Optional.of(credential)
                        : Optional.empty();
            } catch (Exception invalid) {
                return Optional.empty();
            }
        };
    }

    /**
     * 类型 `BearerCredential` 位于 `GatewayAdminStatusCredentialProvider` 内，是记录类型，用于承载 `Bearer Credential` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `BearerCredential` is a record inside `GatewayAdminStatusCredentialProvider` and carries the responsibility, state, or contract for `Bearer Credential`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `BearerCredential` 作为 `GatewayAdminStatusCredentialProvider` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `BearerCredential` as the responsibility boundary of `GatewayAdminStatusCredentialProvider`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param accessToken 记录组件 `accessToken` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `accessToken` carries constructor data whose meaning is defined by the record contract.
     * @param expiresAt 记录组件 `expiresAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expiresAt` carries constructor data whose meaning is defined by the record contract.
     */
    record BearerCredential(/**
 * 字段 `accessToken` 表示 `BearerCredential` 中与 `access Token` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `accessToken` stores the `access Token`-related state, dependency, configuration, or result of `BearerCredential` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `accessToken` 时应保持 `BearerCredential` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `accessToken`, preserve `BearerCredential`'s lifecycle, immutability, and thread-safety constraints.
 */ String accessToken, /**
 * 字段 `expiresAt` 表示 `BearerCredential` 中与 `expires At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `expiresAt` stores the `expires At`-related state, dependency, configuration, or result of `BearerCredential` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `expiresAt` 时应保持 `BearerCredential` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `expiresAt`, preserve `BearerCredential`'s lifecycle, immutability, and thread-safety constraints.
 */ Instant expiresAt) {

        /**
         * 构造器 `BearerCredential` 用于创建并初始化 `BearerCredential` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `BearerCredential` creates and initializes `BearerCredential`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `BearerCredential` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `BearerCredential`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param accessToken 输入参数 `accessToken`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expiresAt 输入参数 `expiresAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public BearerCredential {
            if (accessToken == null || accessToken.isBlank()) {
                throw new IllegalArgumentException("accessToken is required");
            }
            accessToken = accessToken.trim();
            expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        }

        /**
         * 方法 `toString` 按照 `BearerCredential` 的职责处理输入，完成 `to String` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `toString` processes its inputs according to `BearerCredential`'s responsibility, performs the `to String` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `toString` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `toString`, then continue the business flow using its result, exception, or side effect.
         *
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        @Override
        public String toString() {
            return "BearerCredential[accessToken=<redacted>, expiresAt=" + expiresAt + ']';
        }
    }
}
