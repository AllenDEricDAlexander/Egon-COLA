package top.egon.cola.platform.rbac3.starter.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;

/**
 * 类型 `HttpRbac3AuthorizationClient` 位于当前包内，是类型，用于承载 `Http Rbac3 Authorization Client` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `HttpRbac3AuthorizationClient` is a type in its package and carries the responsibility, state, or contract for `Http Rbac3 Authorization Client`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * HTTP client for the service-credential protected RBAC3 snapshot endpoint.
 */
public final class HttpRbac3AuthorizationClient
        implements Rbac3AuthorizationClient {

    /**
     * 字段 `endpoint` 表示 `HttpRbac3AuthorizationClient` 中与 `endpoint` 相关的状态、依赖、配置或结果（声明类型 `URI`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `endpoint` stores the `endpoint`-related state, dependency, configuration, or result of `HttpRbac3AuthorizationClient` (declared type `URI`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `endpoint` 时应保持 `HttpRbac3AuthorizationClient` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `endpoint`, preserve `HttpRbac3AuthorizationClient`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final URI endpoint;
    /**
     * 字段 `credentials` 表示 `HttpRbac3AuthorizationClient` 中与 `credentials` 相关的状态、依赖、配置或结果（声明类型 `Function&lt;String, String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `credentials` stores the `credentials`-related state, dependency, configuration, or result of `HttpRbac3AuthorizationClient` (declared type `Function&lt;String, String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `credentials` 时应保持 `HttpRbac3AuthorizationClient` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `credentials`, preserve `HttpRbac3AuthorizationClient`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Function<String, String> credentials;
    /**
     * 字段 `timeout` 表示 `HttpRbac3AuthorizationClient` 中与 `timeout` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `timeout` stores the `timeout`-related state, dependency, configuration, or result of `HttpRbac3AuthorizationClient` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `timeout` 时应保持 `HttpRbac3AuthorizationClient` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `timeout`, preserve `HttpRbac3AuthorizationClient`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Duration timeout;
    /**
     * 字段 `objectMapper` 表示 `HttpRbac3AuthorizationClient` 中与 `object Mapper` 相关的状态、依赖、配置或结果（声明类型 `ObjectMapper`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `objectMapper` stores the `object Mapper`-related state, dependency, configuration, or result of `HttpRbac3AuthorizationClient` (declared type `ObjectMapper`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `objectMapper` 时应保持 `HttpRbac3AuthorizationClient` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `objectMapper`, preserve `HttpRbac3AuthorizationClient`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ObjectMapper objectMapper;
    /**
     * 字段 `transport` 表示 `HttpRbac3AuthorizationClient` 中与 `transport` 相关的状态、依赖、配置或结果（声明类型 `Transport`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `transport` stores the `transport`-related state, dependency, configuration, or result of `HttpRbac3AuthorizationClient` (declared type `Transport`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `transport` 时应保持 `HttpRbac3AuthorizationClient` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `transport`, preserve `HttpRbac3AuthorizationClient`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Transport transport;

    /**
     * 构造器 `HttpRbac3AuthorizationClient` 用于创建并初始化 `HttpRbac3AuthorizationClient` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `HttpRbac3AuthorizationClient` creates and initializes `HttpRbac3AuthorizationClient`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `HttpRbac3AuthorizationClient` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `HttpRbac3AuthorizationClient`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param endpoint 输入参数 `endpoint`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param credentialFile 输入参数 `credentialFile`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param timeout 输入参数 `timeout`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param objectMapper 输入参数 `objectMapper`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public HttpRbac3AuthorizationClient(
            URI endpoint,
            Path credentialFile,
            Duration timeout,
            ObjectMapper objectMapper) {
        this(endpoint, tenantId -> credential(credentialFile), timeout, objectMapper,
                new JdkTransport(timeout));
    }

    /**
     * 构造器 `HttpRbac3AuthorizationClient` 用于创建并初始化 `HttpRbac3AuthorizationClient` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `HttpRbac3AuthorizationClient` creates and initializes `HttpRbac3AuthorizationClient`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `HttpRbac3AuthorizationClient` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `HttpRbac3AuthorizationClient`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param endpoint 输入参数 `endpoint`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param credentials 输入参数 `credentials`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param timeout 输入参数 `timeout`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param objectMapper 输入参数 `objectMapper`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public HttpRbac3AuthorizationClient(
            URI endpoint,
            Function<String, String> credentials,
            Duration timeout,
            ObjectMapper objectMapper) {
        this(endpoint, credentials, timeout, objectMapper,
                new JdkTransport(timeout));
    }

    /**
     * 构造器 `HttpRbac3AuthorizationClient` 用于创建并初始化 `HttpRbac3AuthorizationClient` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `HttpRbac3AuthorizationClient` creates and initializes `HttpRbac3AuthorizationClient`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `HttpRbac3AuthorizationClient` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `HttpRbac3AuthorizationClient`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param endpoint 输入参数 `endpoint`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param credentialFile 输入参数 `credentialFile`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param timeout 输入参数 `timeout`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param objectMapper 输入参数 `objectMapper`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param transport 输入参数 `transport`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    HttpRbac3AuthorizationClient(
            URI endpoint,
            Path credentialFile,
            Duration timeout,
            ObjectMapper objectMapper,
            Transport transport) {
        this(
                endpoint,
                tenantId -> credential(credentialFile),
                timeout,
                objectMapper,
                transport
        );
    }

    /**
     * 构造器 `HttpRbac3AuthorizationClient` 用于创建并初始化 `HttpRbac3AuthorizationClient` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `HttpRbac3AuthorizationClient` creates and initializes `HttpRbac3AuthorizationClient`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `HttpRbac3AuthorizationClient` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `HttpRbac3AuthorizationClient`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param endpoint 输入参数 `endpoint`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param credentials 输入参数 `credentials`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param timeout 输入参数 `timeout`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param objectMapper 输入参数 `objectMapper`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param transport 输入参数 `transport`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    HttpRbac3AuthorizationClient(
            URI endpoint,
            Function<String, String> credentials,
            Duration timeout,
            ObjectMapper objectMapper,
            Transport transport) {
        this.endpoint = secureEndpoint(endpoint);
        this.credentials = Objects.requireNonNull(credentials, "credentials");
        this.timeout = bounded(timeout);
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.transport = Objects.requireNonNull(transport, "transport");
    }

    /**
     * 方法 `fetch` 按照 `HttpRbac3AuthorizationClient` 的职责处理输入，完成 `fetch` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `fetch` processes its inputs according to `HttpRbac3AuthorizationClient`'s responsibility, performs the `fetch` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `fetch` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `fetch`, then continue the business flow using its result, exception, or side effect.
     *
     * @param systemCode 输入参数 `systemCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     * @throws InterruptedException 当输入违反契约或依赖不可用时抛出；thrown when the contract is violated or a dependency is unavailable.
     */
    @Override
    public SystemAuthorizationSnapshot fetch(
            String systemCode,
            IdentityPrincipal principal) throws InterruptedException {
        Objects.requireNonNull(principal, "principal");
        URI uri = endpoint.resolve("/internal/v1/authorization/contexts/"
                + encode(principal.tenantId()) + '/' + encode(principal.sessionId())
                + "?systemCode=" + encode(systemCode)
                + "&identitySub=" + encode(principal.subject()));
        try {
            HttpResponse response = transport.get(
                    uri,
                    credential(credentials.apply(principal.tenantId())),
                    timeout
            );
            if (response.statusCode() == 401 || response.statusCode() == 403
                    || response.statusCode() == 404) {
                throw new AuthorizationDeniedException(
                        "RBAC3_AUTHORIZATION_DENIED");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AuthorizationUnavailableException(
                        "RBAC3_AUTHORIZATION_UNAVAILABLE");
            }
            JsonNode data = objectMapper.readTree(response.body()).get("data");
            if (data == null || data.isNull()) {
                throw new AuthorizationUnavailableException(
                        "RBAC3_AUTHORIZATION_RESPONSE_INVALID");
            }
            return objectMapper.treeToValue(data, SystemAuthorizationSnapshot.class);
        } catch (AuthorizationDeniedException | AuthorizationUnavailableException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new AuthorizationUnavailableException(
                    "RBAC3_AUTHORIZATION_UNAVAILABLE", exception);
        }
    }

    /**
     * 方法 `credential` 按照 `HttpRbac3AuthorizationClient` 的职责处理输入，完成 `credential` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `credential` processes its inputs according to `HttpRbac3AuthorizationClient`'s responsibility, performs the `credential` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `credential` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `credential`, then continue the business flow using its result, exception, or side effect.
     *
     * @param credentialFile 输入参数 `credentialFile`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String credential(Path credentialFile) {
        try {
            Objects.requireNonNull(credentialFile, "credentialFile");
            String value = Files.readString(credentialFile).trim();
            return credential(value);
        } catch (IOException exception) {
            throw new AuthorizationUnavailableException(
                    "RBAC3_SERVICE_CREDENTIAL_UNAVAILABLE", exception);
        }
    }

    /**
     * 方法 `credential` 按照 `HttpRbac3AuthorizationClient` 的职责处理输入，完成 `credential` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `credential` processes its inputs according to `HttpRbac3AuthorizationClient`'s responsibility, performs the `credential` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `credential` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `credential`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String credential(String value) {
        if (value == null || value.isBlank() || value.length() > 8192) {
            throw new AuthorizationUnavailableException(
                    "RBAC3_SERVICE_CREDENTIAL_UNAVAILABLE"
            );
        }
        return value.trim();
    }

    /**
     * 方法 `secureEndpoint` 按照 `HttpRbac3AuthorizationClient` 的职责处理输入，完成 `secure Endpoint` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `secureEndpoint` processes its inputs according to `HttpRbac3AuthorizationClient`'s responsibility, performs the `secure Endpoint` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `secureEndpoint` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `secureEndpoint`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static URI secureEndpoint(URI value) {
        Objects.requireNonNull(value, "endpoint");
        boolean loopback = "localhost".equalsIgnoreCase(value.getHost())
                || "127.0.0.1".equals(value.getHost())
                || "::1".equals(value.getHost());
        if (!"https".equalsIgnoreCase(value.getScheme())
                && !("http".equalsIgnoreCase(value.getScheme()) && loopback)) {
            throw new IllegalArgumentException(
                    "RBAC3 authorization endpoint must use HTTPS or loopback HTTP");
        }
        return value;
    }

    /**
     * 方法 `bounded` 按照 `HttpRbac3AuthorizationClient` 的职责处理输入，完成 `bounded` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `bounded` processes its inputs according to `HttpRbac3AuthorizationClient`'s responsibility, performs the `bounded` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `bounded` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `bounded`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static Duration bounded(Duration value) {
        Objects.requireNonNull(value, "timeout");
        if (value.compareTo(Duration.ofMillis(100)) < 0
                || value.compareTo(Duration.ofSeconds(5)) > 0) {
            throw new IllegalArgumentException("timeout is outside the safe range");
        }
        return value;
    }

    /**
     * 方法 `encode` 按照 `HttpRbac3AuthorizationClient` 的职责处理输入，完成 `encode` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `encode` processes its inputs according to `HttpRbac3AuthorizationClient`'s responsibility, performs the `encode` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `encode` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `encode`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * 类型 `Transport` 位于 `HttpRbac3AuthorizationClient` 内，是接口，用于承载 `Transport` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Transport` is an interface inside `HttpRbac3AuthorizationClient` and carries the responsibility, state, or contract for `Transport`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Transport` 作为 `HttpRbac3AuthorizationClient` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Transport` as the responsibility boundary of `HttpRbac3AuthorizationClient`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    interface Transport {
        /**
         * 方法 `get` 按照 `Transport` 的职责处理输入，完成 `get` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `get` processes its inputs according to `Transport`'s responsibility, performs the `get` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `get` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `get`, then continue the business flow using its result, exception, or side effect.
         *
         * @param uri 输入参数 `uri`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param bearerToken 输入参数 `bearerToken`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param timeout 输入参数 `timeout`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         * @throws IOException 当输入违反契约或依赖不可用时抛出；thrown when the contract is violated or a dependency is unavailable.
         * @throws InterruptedException 当输入违反契约或依赖不可用时抛出；thrown when the contract is violated or a dependency is unavailable.
         */
        HttpResponse get(URI uri, String bearerToken, Duration timeout)
                throws IOException, InterruptedException;
    }

    /**
     * 类型 `HttpResponse` 位于 `HttpRbac3AuthorizationClient` 内，是记录类型，用于承载 `Http Response` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `HttpResponse` is a record inside `HttpRbac3AuthorizationClient` and carries the responsibility, state, or contract for `Http Response`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `HttpResponse` 作为 `HttpRbac3AuthorizationClient` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `HttpResponse` as the responsibility boundary of `HttpRbac3AuthorizationClient`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param statusCode 记录组件 `statusCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `statusCode` carries constructor data whose meaning is defined by the record contract.
     * @param body 记录组件 `body` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `body` carries constructor data whose meaning is defined by the record contract.
     */
    record HttpResponse(/**
 * 字段 `statusCode` 表示 `HttpResponse` 中与 `status Code` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `statusCode` stores the `status Code`-related state, dependency, configuration, or result of `HttpResponse` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `statusCode` 时应保持 `HttpResponse` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `statusCode`, preserve `HttpResponse`'s lifecycle, immutability, and thread-safety constraints.
 */ int statusCode, /**
 * 字段 `body` 表示 `HttpResponse` 中与 `body` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `body` stores the `body`-related state, dependency, configuration, or result of `HttpResponse` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `body` 时应保持 `HttpResponse` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `body`, preserve `HttpResponse`'s lifecycle, immutability, and thread-safety constraints.
 */ String body) {
    }

    /**
     * 类型 `JdkTransport` 位于 `HttpRbac3AuthorizationClient` 内，是类型，用于承载 `Jdk Transport` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `JdkTransport` is a type inside `HttpRbac3AuthorizationClient` and carries the responsibility, state, or contract for `Jdk Transport`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `JdkTransport` 作为 `HttpRbac3AuthorizationClient` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `JdkTransport` as the responsibility boundary of `HttpRbac3AuthorizationClient`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    private static final class JdkTransport implements Transport {

        /**
         * 字段 `client` 表示 `JdkTransport` 中与 `client` 相关的状态、依赖、配置或结果（声明类型 `HttpClient`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `client` stores the `client`-related state, dependency, configuration, or result of `JdkTransport` (declared type `HttpClient`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `client` 时应保持 `JdkTransport` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `client`, preserve `JdkTransport`'s lifecycle, immutability, and thread-safety constraints.
         */
        private final HttpClient client;

        /**
         * 构造器 `JdkTransport` 用于创建并初始化 `JdkTransport` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `JdkTransport` creates and initializes `JdkTransport`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `JdkTransport` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `JdkTransport`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param connectTimeout 输入参数 `connectTimeout`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        private JdkTransport(Duration connectTimeout) {
            this.client = HttpClient.newBuilder()
                    .connectTimeout(connectTimeout)
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();
        }

        /**
         * 方法 `get` 按照 `JdkTransport` 的职责处理输入，完成 `get` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `get` processes its inputs according to `JdkTransport`'s responsibility, performs the `get` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `get` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `get`, then continue the business flow using its result, exception, or side effect.
         *
         * @param uri 输入参数 `uri`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param bearerToken 输入参数 `bearerToken`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param timeout 输入参数 `timeout`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         * @throws IOException 当输入违反契约或依赖不可用时抛出；thrown when the contract is violated or a dependency is unavailable.
         * @throws InterruptedException 当输入违反契约或依赖不可用时抛出；thrown when the contract is violated or a dependency is unavailable.
         */
        @Override
        public HttpResponse get(URI uri, String bearerToken, Duration timeout)
                throws IOException, InterruptedException {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(timeout)
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + bearerToken)
                    .GET()
                    .build();
            var response = client.send(request, BodyHandlers.ofString());
            return new HttpResponse(response.statusCode(), response.body());
        }
    }
}
