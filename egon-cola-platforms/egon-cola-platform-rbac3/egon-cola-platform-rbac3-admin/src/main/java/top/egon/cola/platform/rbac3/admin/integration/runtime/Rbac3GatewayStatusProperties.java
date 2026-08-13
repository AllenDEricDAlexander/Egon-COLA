package top.egon.cola.platform.rbac3.admin.integration.runtime;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;

/**
 * 类型 `Rbac3GatewayStatusProperties` 位于当前包内，是类型，用于承载 `Rbac3 Gateway Status Properties` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3GatewayStatusProperties` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Gateway Status Properties`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `Rbac3GatewayStatusProperties` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `Rbac3GatewayStatusProperties` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@ConfigurationProperties("egon.rbac3.gateway-status")
public class Rbac3GatewayStatusProperties {

    /**
     * 字段 `adminBaseUrl` 表示 `Rbac3GatewayStatusProperties` 中与 `admin Base Url` 相关的状态、依赖、配置或结果（声明类型 `URI`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `adminBaseUrl` stores the `admin Base Url`-related state, dependency, configuration, or result of `Rbac3GatewayStatusProperties` (declared type `URI`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `adminBaseUrl` 时应保持 `Rbac3GatewayStatusProperties` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `adminBaseUrl`, preserve `Rbac3GatewayStatusProperties`'s lifecycle, immutability, and thread-safety constraints.
     */
    private URI adminBaseUrl;
    /**
     * 字段 `gatewayGroupId` 表示 `Rbac3GatewayStatusProperties` 中与 `gateway Group Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `gatewayGroupId` stores the `gateway Group Id`-related state, dependency, configuration, or result of `Rbac3GatewayStatusProperties` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `gatewayGroupId` 时应保持 `Rbac3GatewayStatusProperties` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `gatewayGroupId`, preserve `Rbac3GatewayStatusProperties`'s lifecycle, immutability, and thread-safety constraints.
     */
    private String gatewayGroupId;
    /**
     * 字段 `releaseId` 表示 `Rbac3GatewayStatusProperties` 中与 `release Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `releaseId` stores the `release Id`-related state, dependency, configuration, or result of `Rbac3GatewayStatusProperties` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `releaseId` 时应保持 `Rbac3GatewayStatusProperties` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `releaseId`, preserve `Rbac3GatewayStatusProperties`'s lifecycle, immutability, and thread-safety constraints.
     */
    private String releaseId;
    /**
     * 字段 `oauthTokenFile` 表示 `Rbac3GatewayStatusProperties` 中与 `oauth Token File` 相关的状态、依赖、配置或结果（声明类型 `Path`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `oauthTokenFile` stores the `oauth Token File`-related state, dependency, configuration, or result of `Rbac3GatewayStatusProperties` (declared type `Path`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `oauthTokenFile` 时应保持 `Rbac3GatewayStatusProperties` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `oauthTokenFile`, preserve `Rbac3GatewayStatusProperties`'s lifecycle, immutability, and thread-safety constraints.
     */
    private Path oauthTokenFile;
    /**
     * 字段 `timeout` 表示 `Rbac3GatewayStatusProperties` 中与 `timeout` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `timeout` stores the `timeout`-related state, dependency, configuration, or result of `Rbac3GatewayStatusProperties` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `timeout` 时应保持 `Rbac3GatewayStatusProperties` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `timeout`, preserve `Rbac3GatewayStatusProperties`'s lifecycle, immutability, and thread-safety constraints.
     */
    private Duration timeout = Duration.ofSeconds(3);

    /**
     * 方法 `requireAdminBaseUrl` 按照 `Rbac3GatewayStatusProperties` 的职责处理输入，完成 `require Admin Base Url` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireAdminBaseUrl` processes its inputs according to `Rbac3GatewayStatusProperties`'s responsibility, performs the `require Admin Base Url` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireAdminBaseUrl` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireAdminBaseUrl`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public URI requireAdminBaseUrl() {
        return required(adminBaseUrl, "adminBaseUrl");
    }

    /**
     * 方法 `requireGatewayGroupId` 按照 `Rbac3GatewayStatusProperties` 的职责处理输入，完成 `require Gateway Group Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireGatewayGroupId` processes its inputs according to `Rbac3GatewayStatusProperties`'s responsibility, performs the `require Gateway Group Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireGatewayGroupId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireGatewayGroupId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String requireGatewayGroupId() {
        return required(gatewayGroupId, "gatewayGroupId");
    }

    /**
     * 方法 `requireReleaseId` 按照 `Rbac3GatewayStatusProperties` 的职责处理输入，完成 `require Release Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireReleaseId` processes its inputs according to `Rbac3GatewayStatusProperties`'s responsibility, performs the `require Release Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireReleaseId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireReleaseId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String requireReleaseId() {
        return required(releaseId, "releaseId");
    }

    /**
     * 方法 `requireOauthTokenFile` 按照 `Rbac3GatewayStatusProperties` 的职责处理输入，完成 `require Oauth Token File` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireOauthTokenFile` processes its inputs according to `Rbac3GatewayStatusProperties`'s responsibility, performs the `require Oauth Token File` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireOauthTokenFile` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireOauthTokenFile`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Path requireOauthTokenFile() {
        return required(oauthTokenFile, "oauthTokenFile");
    }

    /**
     * 方法 `requireTimeout` 按照 `Rbac3GatewayStatusProperties` 的职责处理输入，完成 `require Timeout` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireTimeout` processes its inputs according to `Rbac3GatewayStatusProperties`'s responsibility, performs the `require Timeout` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireTimeout` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireTimeout`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Duration requireTimeout() {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException(
                    "egon.rbac3.gateway-status.timeout must be positive");
        }
        return timeout;
    }

    /**
     * 方法 `setAdminBaseUrl` 按照 `Rbac3GatewayStatusProperties` 的职责处理输入，完成 `set Admin Base Url` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `setAdminBaseUrl` processes its inputs according to `Rbac3GatewayStatusProperties`'s responsibility, performs the `set Admin Base Url` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `setAdminBaseUrl` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `setAdminBaseUrl`, then continue the business flow using its result, exception, or side effect.
     *
     * @param adminBaseUrl 输入参数 `adminBaseUrl`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void setAdminBaseUrl(URI adminBaseUrl) {
        this.adminBaseUrl = adminBaseUrl;
    }

    /**
     * 方法 `setGatewayGroupId` 按照 `Rbac3GatewayStatusProperties` 的职责处理输入，完成 `set Gateway Group Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `setGatewayGroupId` processes its inputs according to `Rbac3GatewayStatusProperties`'s responsibility, performs the `set Gateway Group Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `setGatewayGroupId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `setGatewayGroupId`, then continue the business flow using its result, exception, or side effect.
     *
     * @param gatewayGroupId 输入参数 `gatewayGroupId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void setGatewayGroupId(String gatewayGroupId) {
        this.gatewayGroupId = gatewayGroupId;
    }

    /**
     * 方法 `setReleaseId` 按照 `Rbac3GatewayStatusProperties` 的职责处理输入，完成 `set Release Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `setReleaseId` processes its inputs according to `Rbac3GatewayStatusProperties`'s responsibility, performs the `set Release Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `setReleaseId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `setReleaseId`, then continue the business flow using its result, exception, or side effect.
     *
     * @param releaseId 输入参数 `releaseId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void setReleaseId(String releaseId) {
        this.releaseId = releaseId;
    }

    /**
     * 方法 `setOauthTokenFile` 按照 `Rbac3GatewayStatusProperties` 的职责处理输入，完成 `set Oauth Token File` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `setOauthTokenFile` processes its inputs according to `Rbac3GatewayStatusProperties`'s responsibility, performs the `set Oauth Token File` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `setOauthTokenFile` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `setOauthTokenFile`, then continue the business flow using its result, exception, or side effect.
     *
     * @param oauthTokenFile 输入参数 `oauthTokenFile`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void setOauthTokenFile(Path oauthTokenFile) {
        this.oauthTokenFile = oauthTokenFile;
    }

    /**
     * 方法 `setTimeout` 按照 `Rbac3GatewayStatusProperties` 的职责处理输入，完成 `set Timeout` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `setTimeout` processes its inputs according to `Rbac3GatewayStatusProperties`'s responsibility, performs the `set Timeout` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `setTimeout` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `setTimeout`, then continue the business flow using its result, exception, or side effect.
     *
     * @param timeout 输入参数 `timeout`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    /**
     * 方法 `required` 按照 `Rbac3GatewayStatusProperties` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `Rbac3GatewayStatusProperties`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     *
     * @param <T> 类型参数表示必填配置值的具体类型；type parameter representing the required configuration value type.
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param field 输入参数 `field`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static <T> T required(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "egon.rbac3.gateway-status." + field + " is required");
        }
        return value;
    }

    /**
     * 方法 `required` 按照 `Rbac3GatewayStatusProperties` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `Rbac3GatewayStatusProperties`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param field 输入参数 `field`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "egon.rbac3.gateway-status." + field + " is required");
        }
        return value.trim();
    }
}
