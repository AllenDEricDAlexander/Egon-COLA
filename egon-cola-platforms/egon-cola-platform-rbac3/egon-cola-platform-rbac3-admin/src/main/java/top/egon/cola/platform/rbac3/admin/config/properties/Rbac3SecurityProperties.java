package top.egon.cola.platform.rbac3.admin.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 类型 `Rbac3SecurityProperties` 位于当前包内，是类型，用于承载 `Rbac3 Security Properties` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3SecurityProperties` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Security Properties`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `Rbac3SecurityProperties` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `Rbac3SecurityProperties` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@ConfigurationProperties(prefix = "egon.rbac3.security")
public class Rbac3SecurityProperties {

    /**
     * 字段 `jwtPrivateKeyFile` 表示 `Rbac3SecurityProperties` 中与 `jwt Private Key File` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `jwtPrivateKeyFile` stores the `jwt Private Key File`-related state, dependency, configuration, or result of `Rbac3SecurityProperties` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `jwtPrivateKeyFile` 时应保持 `Rbac3SecurityProperties` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `jwtPrivateKeyFile`, preserve `Rbac3SecurityProperties`'s lifecycle, immutability, and thread-safety constraints.
     */
    private String jwtPrivateKeyFile;
    /**
     * 字段 `jwtPublicKeyFile` 表示 `Rbac3SecurityProperties` 中与 `jwt Public Key File` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `jwtPublicKeyFile` stores the `jwt Public Key File`-related state, dependency, configuration, or result of `Rbac3SecurityProperties` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `jwtPublicKeyFile` 时应保持 `Rbac3SecurityProperties` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `jwtPublicKeyFile`, preserve `Rbac3SecurityProperties`'s lifecycle, immutability, and thread-safety constraints.
     */
    private String jwtPublicKeyFile;
    /**
     * 字段 `jwtKid` 表示 `Rbac3SecurityProperties` 中与 `jwt Kid` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `jwtKid` stores the `jwt Kid`-related state, dependency, configuration, or result of `Rbac3SecurityProperties` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `jwtKid` 时应保持 `Rbac3SecurityProperties` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `jwtKid`, preserve `Rbac3SecurityProperties`'s lifecycle, immutability, and thread-safety constraints.
     */
    private String jwtKid;
    /**
     * 字段 `issuer` 表示 `Rbac3SecurityProperties` 中与 `issuer` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `issuer` stores the `issuer`-related state, dependency, configuration, or result of `Rbac3SecurityProperties` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `issuer` 时应保持 `Rbac3SecurityProperties` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `issuer`, preserve `Rbac3SecurityProperties`'s lifecycle, immutability, and thread-safety constraints.
     */
    private String issuer;
    /**
     * 字段 `resourceUris` 表示 `Rbac3SecurityProperties` 中与 `resource Uris` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `resourceUris` stores the `resource Uris`-related state, dependency, configuration, or result of `Rbac3SecurityProperties` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `resourceUris` 时应保持 `Rbac3SecurityProperties` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `resourceUris`, preserve `Rbac3SecurityProperties`'s lifecycle, immutability, and thread-safety constraints.
     */
    private List<String> resourceUris = new ArrayList<>();
    /**
     * 字段 `auditCursorSecretFile` 表示 `Rbac3SecurityProperties` 中与 `audit Cursor Secret File` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `auditCursorSecretFile` stores the `audit Cursor Secret File`-related state, dependency, configuration, or result of `Rbac3SecurityProperties` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `auditCursorSecretFile` 时应保持 `Rbac3SecurityProperties` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `auditCursorSecretFile`, preserve `Rbac3SecurityProperties`'s lifecycle, immutability, and thread-safety constraints.
     */
    private String auditCursorSecretFile;
    /**
     * 字段 `verificationKeyRetention` 表示 `Rbac3SecurityProperties` 中与 `verification Key Retention` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `verificationKeyRetention` stores the `verification Key Retention`-related state, dependency, configuration, or result of `Rbac3SecurityProperties` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `verificationKeyRetention` 时应保持 `Rbac3SecurityProperties` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `verificationKeyRetention`, preserve `Rbac3SecurityProperties`'s lifecycle, immutability, and thread-safety constraints.
     */
    private Duration verificationKeyRetention = Duration.ofDays(8);

    /**
     * 方法 `getJwtPrivateKeyFile` 按照 `Rbac3SecurityProperties` 的职责处理输入，完成 `get Jwt Private Key File` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getJwtPrivateKeyFile` processes its inputs according to `Rbac3SecurityProperties`'s responsibility, performs the `get Jwt Private Key File` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getJwtPrivateKeyFile` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getJwtPrivateKeyFile`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getJwtPrivateKeyFile() {
        return jwtPrivateKeyFile;
    }

    /**
     * 方法 `setJwtPrivateKeyFile` 按照 `Rbac3SecurityProperties` 的职责处理输入，完成 `set Jwt Private Key File` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `setJwtPrivateKeyFile` processes its inputs according to `Rbac3SecurityProperties`'s responsibility, performs the `set Jwt Private Key File` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `setJwtPrivateKeyFile` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `setJwtPrivateKeyFile`, then continue the business flow using its result, exception, or side effect.
     *
     * @param jwtPrivateKeyFile 输入参数 `jwtPrivateKeyFile`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void setJwtPrivateKeyFile(String jwtPrivateKeyFile) {
        this.jwtPrivateKeyFile = jwtPrivateKeyFile;
    }

    /**
     * 方法 `getJwtPublicKeyFile` 按照 `Rbac3SecurityProperties` 的职责处理输入，完成 `get Jwt Public Key File` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getJwtPublicKeyFile` processes its inputs according to `Rbac3SecurityProperties`'s responsibility, performs the `get Jwt Public Key File` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getJwtPublicKeyFile` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getJwtPublicKeyFile`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getJwtPublicKeyFile() {
        return jwtPublicKeyFile;
    }

    /**
     * 方法 `setJwtPublicKeyFile` 按照 `Rbac3SecurityProperties` 的职责处理输入，完成 `set Jwt Public Key File` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `setJwtPublicKeyFile` processes its inputs according to `Rbac3SecurityProperties`'s responsibility, performs the `set Jwt Public Key File` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `setJwtPublicKeyFile` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `setJwtPublicKeyFile`, then continue the business flow using its result, exception, or side effect.
     *
     * @param jwtPublicKeyFile 输入参数 `jwtPublicKeyFile`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void setJwtPublicKeyFile(String jwtPublicKeyFile) {
        this.jwtPublicKeyFile = jwtPublicKeyFile;
    }

    /**
     * 方法 `getJwtKid` 按照 `Rbac3SecurityProperties` 的职责处理输入，完成 `get Jwt Kid` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getJwtKid` processes its inputs according to `Rbac3SecurityProperties`'s responsibility, performs the `get Jwt Kid` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getJwtKid` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getJwtKid`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getJwtKid() {
        return jwtKid;
    }

    /**
     * 方法 `setJwtKid` 按照 `Rbac3SecurityProperties` 的职责处理输入，完成 `set Jwt Kid` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `setJwtKid` processes its inputs according to `Rbac3SecurityProperties`'s responsibility, performs the `set Jwt Kid` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `setJwtKid` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `setJwtKid`, then continue the business flow using its result, exception, or side effect.
     *
     * @param jwtKid 输入参数 `jwtKid`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void setJwtKid(String jwtKid) {
        this.jwtKid = jwtKid;
    }

    /**
     * 方法 `getIssuer` 按照 `Rbac3SecurityProperties` 的职责处理输入，完成 `get Issuer` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getIssuer` processes its inputs according to `Rbac3SecurityProperties`'s responsibility, performs the `get Issuer` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getIssuer` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getIssuer`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * 方法 `setIssuer` 按照 `Rbac3SecurityProperties` 的职责处理输入，完成 `set Issuer` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `setIssuer` processes its inputs according to `Rbac3SecurityProperties`'s responsibility, performs the `set Issuer` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `setIssuer` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `setIssuer`, then continue the business flow using its result, exception, or side effect.
     *
     * @param issuer 输入参数 `issuer`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    /**
     * 方法 `getResourceUris` 按照 `Rbac3SecurityProperties` 的职责处理输入，完成 `get Resource Uris` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getResourceUris` processes its inputs according to `Rbac3SecurityProperties`'s responsibility, performs the `get Resource Uris` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getResourceUris` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getResourceUris`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public List<String> getResourceUris() {
        return List.copyOf(resourceUris);
    }

    /**
     * 方法 `setResourceUris` 按照 `Rbac3SecurityProperties` 的职责处理输入，完成 `set Resource Uris` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `setResourceUris` processes its inputs according to `Rbac3SecurityProperties`'s responsibility, performs the `set Resource Uris` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `setResourceUris` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `setResourceUris`, then continue the business flow using its result, exception, or side effect.
     *
     * @param resourceUris 输入参数 `resourceUris`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void setResourceUris(List<String> resourceUris) {
        this.resourceUris = new ArrayList<>(
                resourceUris == null ? List.of() : resourceUris);
    }

    /**
     * 方法 `getAuditCursorSecretFile` 按照 `Rbac3SecurityProperties` 的职责处理输入，完成 `get Audit Cursor Secret File` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getAuditCursorSecretFile` processes its inputs according to `Rbac3SecurityProperties`'s responsibility, performs the `get Audit Cursor Secret File` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getAuditCursorSecretFile` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getAuditCursorSecretFile`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getAuditCursorSecretFile() {
        return auditCursorSecretFile;
    }

    /**
     * 方法 `setAuditCursorSecretFile` 按照 `Rbac3SecurityProperties` 的职责处理输入，完成 `set Audit Cursor Secret File` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `setAuditCursorSecretFile` processes its inputs according to `Rbac3SecurityProperties`'s responsibility, performs the `set Audit Cursor Secret File` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `setAuditCursorSecretFile` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `setAuditCursorSecretFile`, then continue the business flow using its result, exception, or side effect.
     *
     * @param auditCursorSecretFile 输入参数 `auditCursorSecretFile`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void setAuditCursorSecretFile(String auditCursorSecretFile) {
        this.auditCursorSecretFile = auditCursorSecretFile;
    }

    /**
     * 方法 `getVerificationKeyRetention` 按照 `Rbac3SecurityProperties` 的职责处理输入，完成 `get Verification Key Retention` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getVerificationKeyRetention` processes its inputs according to `Rbac3SecurityProperties`'s responsibility, performs the `get Verification Key Retention` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getVerificationKeyRetention` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getVerificationKeyRetention`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Duration getVerificationKeyRetention() {
        return verificationKeyRetention;
    }

    /**
     * 方法 `setVerificationKeyRetention` 按照 `Rbac3SecurityProperties` 的职责处理输入，完成 `set Verification Key Retention` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `setVerificationKeyRetention` processes its inputs according to `Rbac3SecurityProperties`'s responsibility, performs the `set Verification Key Retention` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `setVerificationKeyRetention` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `setVerificationKeyRetention`, then continue the business flow using its result, exception, or side effect.
     *
     * @param verificationKeyRetention 输入参数 `verificationKeyRetention`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void setVerificationKeyRetention(Duration verificationKeyRetention) {
        this.verificationKeyRetention = verificationKeyRetention;
    }

    /**
     * 方法 `requirePrivateKeyFile` 按照 `Rbac3SecurityProperties` 的职责处理输入，完成 `require Private Key File` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requirePrivateKeyFile` processes its inputs according to `Rbac3SecurityProperties`'s responsibility, performs the `require Private Key File` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requirePrivateKeyFile` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requirePrivateKeyFile`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String requirePrivateKeyFile() {
        return required(jwtPrivateKeyFile, "jwtPrivateKeyFile");
    }

    /**
     * 方法 `requirePublicKeyFile` 按照 `Rbac3SecurityProperties` 的职责处理输入，完成 `require Public Key File` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requirePublicKeyFile` processes its inputs according to `Rbac3SecurityProperties`'s responsibility, performs the `require Public Key File` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requirePublicKeyFile` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requirePublicKeyFile`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String requirePublicKeyFile() {
        return required(jwtPublicKeyFile, "jwtPublicKeyFile");
    }

    /**
     * 方法 `requireKid` 按照 `Rbac3SecurityProperties` 的职责处理输入，完成 `require Kid` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireKid` processes its inputs according to `Rbac3SecurityProperties`'s responsibility, performs the `require Kid` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireKid` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireKid`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String requireKid() {
        return required(jwtKid, "jwtKid");
    }

    /**
     * 方法 `requireIssuer` 按照 `Rbac3SecurityProperties` 的职责处理输入，完成 `require Issuer` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireIssuer` processes its inputs according to `Rbac3SecurityProperties`'s responsibility, performs the `require Issuer` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireIssuer` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireIssuer`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String requireIssuer() {
        return required(issuer, "issuer");
    }

    /**
     * 方法 `requireResourceUris` 按照 `Rbac3SecurityProperties` 的职责处理输入，完成 `require Resource Uris` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireResourceUris` processes its inputs according to `Rbac3SecurityProperties`'s responsibility, performs the `require Resource Uris` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireResourceUris` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireResourceUris`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public List<String> requireResourceUris() {
        List<String> values = resourceUris.stream()
                .map(value -> required(value, "resourceUri"))
                .toList();
        if (values.isEmpty()) {
            throw new IllegalStateException(
                    "at least one JWT Resource URI is required");
        }
        return values;
    }

    /**
     * 方法 `requireAuditCursorSecretFile` 按照 `Rbac3SecurityProperties` 的职责处理输入，完成 `require Audit Cursor Secret File` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireAuditCursorSecretFile` processes its inputs according to `Rbac3SecurityProperties`'s responsibility, performs the `require Audit Cursor Secret File` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireAuditCursorSecretFile` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireAuditCursorSecretFile`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String requireAuditCursorSecretFile() {
        return required(auditCursorSecretFile, "auditCursorSecretFile");
    }

    /**
     * 方法 `requireVerificationKeyRetention` 按照 `Rbac3SecurityProperties` 的职责处理输入，完成 `require Verification Key Retention` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireVerificationKeyRetention` processes its inputs according to `Rbac3SecurityProperties`'s responsibility, performs the `require Verification Key Retention` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireVerificationKeyRetention` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireVerificationKeyRetention`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Duration requireVerificationKeyRetention() {
        if (verificationKeyRetention == null || verificationKeyRetention.isNegative()
                || verificationKeyRetention.isZero()) {
            throw new IllegalStateException("verificationKeyRetention must be positive");
        }
        return verificationKeyRetention;
    }

    /**
     * 方法 `required` 按照 `Rbac3SecurityProperties` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `Rbac3SecurityProperties`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
            throw new IllegalStateException(fieldName + " is required");
        }
        return value.trim();
    }
}
