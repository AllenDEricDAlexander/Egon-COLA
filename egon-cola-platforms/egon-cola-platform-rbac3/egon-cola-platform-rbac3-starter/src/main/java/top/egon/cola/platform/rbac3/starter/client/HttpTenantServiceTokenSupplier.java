package top.egon.cola.platform.rbac3.starter.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.egon.cola.platform.idp.starter.admission.PrivateKeyJwtAssertionFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 通过 IdP Client Credentials 为精确目标租户提供短期 SERVICE Access Token。
 * Supplies short-lived SERVICE access tokens for exact target tenants through IdP Client
 * Credentials.
 *
 * <p>每个租户使用独立缓存；请求使用端点绑定且不可重放的 {@code private_key_jwt}，不会把
 * 私钥或 Client Assertion 写入缓存。</p>
 *
 * <p>Each tenant has an isolated cache. Requests use endpoint-bound, non-replayable
 * {@code private_key_jwt} assertions, while neither private-key material nor assertions are
 * cached.</p>
 */
public final class HttpTenantServiceTokenSupplier
        implements Function<String, String> {

    /** RFC 7523 Client Assertion 类型；RFC 7523 Client Assertion type.
     * 含义与用法：读取、传递或更新 `ASSERTION_TYPE` 时应保持 `HttpTenantServiceTokenSupplier` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `ASSERTION_TYPE`, preserve `HttpTenantServiceTokenSupplier`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final String ASSERTION_TYPE =
            "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    /** Source Client 标识；source Client identifier.
     * 含义与用法：读取、传递或更新 `clientId` 时应保持 `HttpTenantServiceTokenSupplier` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `clientId`, preserve `HttpTenantServiceTokenSupplier`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final String clientId;

    /** 每次请求创建新 Assertion 的工厂；factory creating a new assertion for every request.
     * 含义与用法：读取、传递或更新 `assertions` 时应保持 `HttpTenantServiceTokenSupplier` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `assertions`, preserve `HttpTenantServiceTokenSupplier`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Supplier<String> assertions;

    /**
     * 字段 `resourceUri` 表示 `HttpTenantServiceTokenSupplier` 中与 `resource Uri` 相关的状态、依赖、配置或结果（声明类型 `URI`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `resourceUri` stores the `resource Uri`-related state, dependency, configuration, or result of `HttpTenantServiceTokenSupplier` (declared type `URI`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * RBAC3 Resource URI；RBAC3 Resource URI.
     */
    private final URI resourceUri;

    /** 请求的 IdP Service Scope；requested IdP service scopes.
     * 含义与用法：读取、传递或更新 `scopes` 时应保持 `HttpTenantServiceTokenSupplier` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `scopes`, preserve `HttpTenantServiceTokenSupplier`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Set<String> scopes;

    /** 提前续签窗口；renewal skew.
     * 含义与用法：读取、传递或更新 `renewalSkew` 时应保持 `HttpTenantServiceTokenSupplier` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `renewalSkew`, preserve `HttpTenantServiceTokenSupplier`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Duration renewalSkew;

    /** UTC 业务时钟；UTC business clock.
     * 含义与用法：读取、传递或更新 `clock` 时应保持 `HttpTenantServiceTokenSupplier` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `clock`, preserve `HttpTenantServiceTokenSupplier`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Clock clock;

    /** Token Endpoint 传输边界；Token Endpoint transport boundary.
     * 含义与用法：读取、传递或更新 `endpoint` 时应保持 `HttpTenantServiceTokenSupplier` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `endpoint`, preserve `HttpTenantServiceTokenSupplier`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final TokenEndpoint endpoint;

    /** 按精确租户隔离的 Token 缓存；token cache isolated by exact tenant.
     * 含义与用法：读取、传递或更新 `cachedByTenant` 时应保持 `HttpTenantServiceTokenSupplier` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `cachedByTenant`, preserve `HttpTenantServiceTokenSupplier`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Map<String, CachedToken> cachedByTenant = new HashMap<>();

    /**
     * 创建生产 HTTP Token 提供器。
     * Creates the production HTTP token supplier.
     *
     * @param tokenEndpoint IdP Token Endpoint；IdP Token Endpoint
     * @param assertions {@code private_key_jwt} Assertion 工厂；assertion factory
     * @param objectMapper OAuth JSON 响应解码器；OAuth JSON response decoder
     * @param resourceUri 目标 RBAC3 Resource URI；target RBAC3 Resource URI
     * @param scopes 请求的 Service Scope；requested service scopes
     * @param renewalSkew 提前续签窗口；renewal skew
     * @param clock UTC 业务时钟；UTC business clock
     * 用法：通过 `HttpTenantServiceTokenSupplier` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `HttpTenantServiceTokenSupplier`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    public HttpTenantServiceTokenSupplier(
            URI tokenEndpoint,
            PrivateKeyJwtAssertionFactory assertions,
            ObjectMapper objectMapper,
            URI resourceUri,
            Set<String> scopes,
            Duration renewalSkew,
            Clock clock
    ) {
        this(
                Objects.requireNonNull(assertions, "assertions").clientId(),
                assertions::create,
                resourceUri,
                scopes,
                renewalSkew,
                clock,
                httpEndpoint(tokenEndpoint, objectMapper)
        );
    }

    /** 测试接缝；test seam.
     * 用法：通过 `HttpTenantServiceTokenSupplier` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `HttpTenantServiceTokenSupplier`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     * @param clientId 输入参数 `clientId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param assertions 输入参数 `assertions`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param resourceUri 输入参数 `resourceUri`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param scopes 输入参数 `scopes`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param renewalSkew 输入参数 `renewalSkew`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param endpoint 输入参数 `endpoint`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    HttpTenantServiceTokenSupplier(
            String clientId,
            Supplier<String> assertions,
            URI resourceUri,
            Set<String> scopes,
            Duration renewalSkew,
            Clock clock,
            TokenEndpoint endpoint
    ) {
        this.clientId = required(clientId, "clientId");
        this.assertions = Objects.requireNonNull(assertions, "assertions");
        this.resourceUri = resource(resourceUri);
        this.scopes = normalizedScopes(scopes);
        this.renewalSkew = positive(renewalSkew, "renewalSkew");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
    }

    /**
     * 返回绑定到精确目标租户的可复用 Token，必要时同步续签。
     * Returns a reusable token bound to the exact target tenant, renewing synchronously when
     * necessary.
     *
     * @param tenantId 精确目标租户；exact target tenant
     * @return 不带 Bearer 前缀的 Access Token；access token without the Bearer prefix
     * 用法：调用 `apply` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `apply`, then continue the business flow using its result, exception, or side effect.
     */
    @Override
    public String apply(String tenantId) {
        String exactTenantId = required(tenantId, "tenantId");
        synchronized (this) {
            CachedToken current = cachedByTenant.get(exactTenantId);
            if (current != null
                    && clock.instant().isBefore(current.renewAt())) {
                return current.value();
            }
            TokenResponse response;
            try {
                response = endpoint.issue(new TokenRequest(
                        clientId,
                        required(assertions.get(), "assertion"),
                        resourceUri,
                        exactTenantId,
                        scopes
                ));
            } catch (IllegalStateException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                throw new IllegalStateException(
                        "IDP_SERVICE_TOKEN_UNAVAILABLE",
                        exception
                );
            }
            CachedToken issued = token(response);
            cachedByTenant.put(exactTenantId, issued);
            return issued.value();
        }
    }

    /** 校验 Token Endpoint 响应并计算续签时间；validates a response and calculates renewal.
     * 用法：调用 `token` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `token`, then continue the business flow using its result, exception, or side effect.
     * @param response 输入参数 `response`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private CachedToken token(TokenResponse response) {
        if (response == null
                || response.accessToken() == null
                || response.accessToken().isBlank()
                || response.accessToken().length() > 8192
                || !"Bearer".equalsIgnoreCase(response.tokenType())
                || response.expiresIn() <= renewalSkew.toSeconds()) {
            throw new IllegalStateException(
                    "IDP_SERVICE_TOKEN_RESPONSE_INVALID"
            );
        }
        Instant renewAt = clock.instant()
                .plusSeconds(response.expiresIn())
                .minus(renewalSkew);
        return new CachedToken(response.accessToken(), renewAt);
    }

    /** 创建表单编码的生产 Token Endpoint；creates the form-encoded production endpoint.
     * 用法：调用 `httpEndpoint` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `httpEndpoint`, then continue the business flow using its result, exception, or side effect.
     * @param tokenEndpoint 输入参数 `tokenEndpoint`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param objectMapper 输入参数 `objectMapper`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static TokenEndpoint httpEndpoint(
            URI tokenEndpoint,
            ObjectMapper objectMapper
    ) {
        URI endpoint = secureEndpoint(tokenEndpoint);
        ObjectMapper mapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        return request -> {
            try {
                HttpRequest httpRequest = HttpRequest.newBuilder(endpoint)
                        .timeout(Duration.ofSeconds(5))
                        .header(
                                "Content-Type",
                                "application/x-www-form-urlencoded"
                        )
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                form(request)
                        ))
                        .build();
                var response = client.send(httpRequest, BodyHandlers.ofString());
                if (response.statusCode() < 200
                        || response.statusCode() >= 300) {
                    throw new IllegalStateException(
                            "IDP_SERVICE_TOKEN_UNAVAILABLE"
                    );
                }
                return mapper.readValue(
                        response.body(),
                        TokenResponse.class
                );
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "IDP_SERVICE_TOKEN_UNAVAILABLE",
                        exception
                );
            } catch (IOException | RuntimeException exception) {
                throw new IllegalStateException(
                        "IDP_SERVICE_TOKEN_UNAVAILABLE",
                        exception
                );
            }
        };
    }

    /** 构造 OAuth2 Client Credentials 表单；builds the OAuth2 Client Credentials form.
     * 用法：调用 `form` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `form`, then continue the business flow using its result, exception, or side effect.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String form(TokenRequest request) {
        return parameter("grant_type", "client_credentials")
                + '&' + parameter("client_id", request.clientId())
                + '&' + parameter("client_assertion_type", ASSERTION_TYPE)
                + '&' + parameter("client_assertion", request.assertion())
                + '&' + parameter(
                "resource",
                request.resourceUri().toString()
        )
                + '&' + parameter("tenant_id", request.tenantId())
                + '&' + parameter(
                "scope",
                String.join(" ", request.scopes())
        );
    }

    /** 编码一个表单字段；encodes one form field.
     * 用法：调用 `parameter` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `parameter`, then continue the business flow using its result, exception, or side effect.
     * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String parameter(String name, String value) {
        return URLEncoder.encode(name, StandardCharsets.UTF_8)
                + '='
                + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * 方法 `secureEndpoint` 按照 `HttpTenantServiceTokenSupplier` 的职责处理输入，完成 `secure Endpoint` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `secureEndpoint` processes its inputs according to `HttpTenantServiceTokenSupplier`'s responsibility, performs the `secure Endpoint` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `secureEndpoint` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `secureEndpoint`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static URI secureEndpoint(URI value) {
        Objects.requireNonNull(value, "tokenEndpoint");
        boolean loopback = "localhost".equalsIgnoreCase(value.getHost())
                || "127.0.0.1".equals(value.getHost())
                || "::1".equals(value.getHost());
        if (!value.isAbsolute()
                || value.getFragment() != null
                || value.getQuery() != null
                || !value.equals(value.normalize())
                || (!"https".equalsIgnoreCase(value.getScheme())
                && !("http".equalsIgnoreCase(value.getScheme())
                && loopback))) {
            throw new IllegalArgumentException(
                    "tokenEndpoint must use HTTPS or loopback HTTP"
            );
        }
        return value;
    }

    /**
     * 方法 `resource` 按照 `HttpTenantServiceTokenSupplier` 的职责处理输入，完成 `resource` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `resource` processes its inputs according to `HttpTenantServiceTokenSupplier`'s responsibility, performs the `resource` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `resource` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `resource`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static URI resource(URI value) {
        Objects.requireNonNull(value, "resourceUri");
        if (!value.isAbsolute() || value.getFragment() != null
                || !value.equals(value.normalize())) {
            throw new IllegalArgumentException("resourceUri is invalid");
        }
        return value;
    }

    /**
     * 方法 `normalizedScopes` 按照 `HttpTenantServiceTokenSupplier` 的职责处理输入，完成 `normalized Scopes` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `normalizedScopes` processes its inputs according to `HttpTenantServiceTokenSupplier`'s responsibility, performs the `normalized Scopes` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `normalizedScopes` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `normalizedScopes`, then continue the business flow using its result, exception, or side effect.
     *
     * @param values 输入参数 `values`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static Set<String> normalizedScopes(Set<String> values) {
        Objects.requireNonNull(values, "scopes");
        TreeSet<String> normalized = new TreeSet<>();
        values.forEach(value -> normalized.add(required(value, "scope")));
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("scopes must not be empty");
        }
        return Set.copyOf(normalized);
    }

    /**
     * 方法 `positive` 按照 `HttpTenantServiceTokenSupplier` 的职责处理输入，完成 `positive` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `positive` processes its inputs according to `HttpTenantServiceTokenSupplier`'s responsibility, performs the `positive` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `positive` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `positive`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static Duration positive(Duration value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    /**
     * 方法 `required` 按照 `HttpTenantServiceTokenSupplier` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `HttpTenantServiceTokenSupplier`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }

    /** Token Endpoint 请求；Token Endpoint request.
     * 语义与用法：将 `TokenRequest` 作为 `HttpTenantServiceTokenSupplier` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `TokenRequest` as the responsibility boundary of `HttpTenantServiceTokenSupplier`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param clientId 记录组件 `clientId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `clientId` carries constructor data whose meaning is defined by the record contract.
     * @param assertion 记录组件 `assertion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `assertion` carries constructor data whose meaning is defined by the record contract.
     * @param resourceUri 记录组件 `resourceUri` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `resourceUri` carries constructor data whose meaning is defined by the record contract.
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param scopes 记录组件 `scopes` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopes` carries constructor data whose meaning is defined by the record contract.
     */
    record TokenRequest(
            /**
             * 字段 `clientId` 表示 `TokenRequest` 中与 `client Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `clientId` stores the `client Id`-related state, dependency, configuration, or result of `TokenRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `clientId` 时应保持 `TokenRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `clientId`, preserve `TokenRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String clientId,
            /**
             * 字段 `assertion` 表示 `TokenRequest` 中与 `assertion` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `assertion` stores the `assertion`-related state, dependency, configuration, or result of `TokenRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `assertion` 时应保持 `TokenRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `assertion`, preserve `TokenRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String assertion,
            /**
             * 字段 `resourceUri` 表示 `TokenRequest` 中与 `resource Uri` 相关的状态、依赖、配置或结果（声明类型 `URI`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `resourceUri` stores the `resource Uri`-related state, dependency, configuration, or result of `TokenRequest` (declared type `URI`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `resourceUri` 时应保持 `TokenRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `resourceUri`, preserve `TokenRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            URI resourceUri,
            /**
             * 字段 `tenantId` 表示 `TokenRequest` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `TokenRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `TokenRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `TokenRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `scopes` 表示 `TokenRequest` 中与 `scopes` 相关的状态、依赖、配置或结果（声明类型 `Set&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopes` stores the `scopes`-related state, dependency, configuration, or result of `TokenRequest` (declared type `Set&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopes` 时应保持 `TokenRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopes`, preserve `TokenRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            Set<String> scopes
    ) {
        /**
         * 构造器 `TokenRequest` 用于创建并初始化 `TokenRequest` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `TokenRequest` creates and initializes `TokenRequest`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `TokenRequest` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `TokenRequest`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param clientId 输入参数 `clientId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param assertion 输入参数 `assertion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param resourceUri 输入参数 `resourceUri`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param scopes 输入参数 `scopes`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        TokenRequest {
            scopes = Set.copyOf(scopes);
        }
    }

    /** Token Endpoint 响应；Token Endpoint response.
     * 语义与用法：将 `TokenResponse` 作为 `HttpTenantServiceTokenSupplier` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `TokenResponse` as the responsibility boundary of `HttpTenantServiceTokenSupplier`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param accessToken 记录组件 `accessToken` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `accessToken` carries constructor data whose meaning is defined by the record contract.
     * @param tokenType 记录组件 `tokenType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tokenType` carries constructor data whose meaning is defined by the record contract.
     * @param expiresIn 记录组件 `expiresIn` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expiresIn` carries constructor data whose meaning is defined by the record contract.
     */
    record TokenResponse(
            /**
             * 字段 `accessToken` 表示 `TokenResponse` 中与 `access Token` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `accessToken` stores the `access Token`-related state, dependency, configuration, or result of `TokenResponse` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `accessToken` 时应保持 `TokenResponse` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `accessToken`, preserve `TokenResponse`'s lifecycle, immutability, and thread-safety constraints.
             */
            @JsonProperty("access_token") String accessToken,
            /**
             * 字段 `tokenType` 表示 `TokenResponse` 中与 `token Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tokenType` stores the `token Type`-related state, dependency, configuration, or result of `TokenResponse` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tokenType` 时应保持 `TokenResponse` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tokenType`, preserve `TokenResponse`'s lifecycle, immutability, and thread-safety constraints.
             */
            @JsonProperty("token_type") String tokenType,
            /**
             * 字段 `expiresIn` 表示 `TokenResponse` 中与 `expires In` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expiresIn` stores the `expires In`-related state, dependency, configuration, or result of `TokenResponse` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expiresIn` 时应保持 `TokenResponse` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expiresIn`, preserve `TokenResponse`'s lifecycle, immutability, and thread-safety constraints.
             */
            @JsonProperty("expires_in") long expiresIn
    ) {
    }

    /** 可缓存 Token；cacheable token.
     * 语义与用法：将 `CachedToken` 作为 `HttpTenantServiceTokenSupplier` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `CachedToken` as the responsibility boundary of `HttpTenantServiceTokenSupplier`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param value 记录组件 `value` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `value` carries constructor data whose meaning is defined by the record contract.
     * @param renewAt 记录组件 `renewAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `renewAt` carries constructor data whose meaning is defined by the record contract.
     */
    private record CachedToken(/**
 * 字段 `value` 表示 `CachedToken` 中与 `value` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `value` stores the `value`-related state, dependency, configuration, or result of `CachedToken` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `value` 时应保持 `CachedToken` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `value`, preserve `CachedToken`'s lifecycle, immutability, and thread-safety constraints.
 */ String value, /**
 * 字段 `renewAt` 表示 `CachedToken` 中与 `renew At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `renewAt` stores the `renew At`-related state, dependency, configuration, or result of `CachedToken` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `renewAt` 时应保持 `CachedToken` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `renewAt`, preserve `CachedToken`'s lifecycle, immutability, and thread-safety constraints.
 */ Instant renewAt) {
    }

    /** 可替换 Token Endpoint 传输；replaceable Token Endpoint transport.
     * 语义与用法：将 `TokenEndpoint` 作为 `HttpTenantServiceTokenSupplier` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `TokenEndpoint` as the responsibility boundary of `HttpTenantServiceTokenSupplier`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    interface TokenEndpoint {
        /**
         * 方法 `issue` 按照 `TokenEndpoint` 的职责处理输入，完成 `issue` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `issue` processes its inputs according to `TokenEndpoint`'s responsibility, performs the `issue` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `issue` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `issue`, then continue the business flow using its result, exception, or side effect.
         *
         * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        TokenResponse issue(TokenRequest request);
    }
}
