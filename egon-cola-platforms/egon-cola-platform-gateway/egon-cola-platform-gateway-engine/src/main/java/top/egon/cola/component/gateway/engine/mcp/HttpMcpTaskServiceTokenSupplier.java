package top.egon.cola.component.gateway.engine.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import top.egon.cola.platform.idp.starter.admission.PrivateKeyJwtAssertionFactory;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

/**
 * 通过 IdP Client Credentials 为异步 MCP 任务换取目标 Provider 的 SERVICE Token。
 * Exchanges IdP Client Credentials for target-provider SERVICE tokens used by asynchronous MCP
 * tasks.
 *
 * <p>缓存按 tenant 与 Resource URI 双重隔离，每次续签都会创建新的不可重放
 * {@code private_key_jwt}，任务记录中不保存任何 Access Token。</p>
 *
 * <p>The cache is isolated by both tenant and Resource URI. Every renewal creates a fresh,
 * non-replayable {@code private_key_jwt}, and no access token is stored in durable task data.</p>
 * 补充说明 / Supplementary summary: {@code HttpMcpTaskServiceTokenSupplier} 是类型，位于当前 Gateway 模块的相关包中，负责HttpMCP任务服务TokenSupplier相关的职责与边界。
 * English supplement: {@code HttpMcpTaskServiceTokenSupplier} is a type in the current Gateway module; it owns the http mcp task service token supplier-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class HttpMcpTaskServiceTokenSupplier
        implements McpTaskServiceTokenSupplier {

    /**
     * RFC 7523 Client Assertion 类型；RFC 7523 Client Assertion type.
     * 补充说明 / Supplementary summary: 表示 ASSERTIONTYPE 这一固定值；它属于 {@code HttpMcpTaskServiceTokenSupplier} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English supplement: Represents the fixed value assertion type; it is a state, type, or protocol value of {@code HttpMcpTaskServiceTokenSupplier} and keeps callers aligned with the owning type.
     * 用法 / Usage: 该字段通过 {@code HttpMcpTaskServiceTokenSupplier} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpMcpTaskServiceTokenSupplier}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final String ASSERTION_TYPE =
            "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    /**
     * Source Client 标识；source Client identifier.
     * 补充说明 / Supplementary summary: 保存 客户端Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code HttpMcpTaskServiceTokenSupplier} 在其生命周期内读取或更新。
     * English supplement: Holds the state, dependency, or configuration represented by client id; its type is {@code String}, and {@code HttpMcpTaskServiceTokenSupplier} reads or updates it during its lifecycle.
     * 用法 / Usage: 该字段通过 {@code HttpMcpTaskServiceTokenSupplier} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpMcpTaskServiceTokenSupplier}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String clientId;

    /**
     * 每次请求创建新 Assertion 的工厂；factory creating a fresh assertion per request.
     * 补充说明 / Supplementary summary: 保存 assertions 对应的状态、依赖或配置值；字段类型为 {@code Supplier<String>}，由 {@code HttpMcpTaskServiceTokenSupplier} 在其生命周期内读取或更新。
     * English supplement: Holds the state, dependency, or configuration represented by assertions; its type is {@code Supplier<String>}, and {@code HttpMcpTaskServiceTokenSupplier} reads or updates it during its lifecycle.
     * 用法 / Usage: 该字段通过 {@code HttpMcpTaskServiceTokenSupplier} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpMcpTaskServiceTokenSupplier}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Supplier<String> assertions;

    /**
     * IdP 许可的任务执行 Scope；IdP-authorized task-execution scopes.
     * 补充说明 / Supplementary summary: 保存 scopes 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code HttpMcpTaskServiceTokenSupplier} 在其生命周期内读取或更新。
     * English supplement: Holds the state, dependency, or configuration represented by scopes; its type is {@code Set<String>}, and {@code HttpMcpTaskServiceTokenSupplier} reads or updates it during its lifecycle.
     * 用法 / Usage: 该字段通过 {@code HttpMcpTaskServiceTokenSupplier} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpMcpTaskServiceTokenSupplier}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Set<String> scopes;

    /**
     * 提前续签窗口；renewal skew.
     * 补充说明 / Supplementary summary: 保存 renewalSkew 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code HttpMcpTaskServiceTokenSupplier} 在其生命周期内读取或更新。
     * English supplement: Holds the state, dependency, or configuration represented by renewal skew; its type is {@code Duration}, and {@code HttpMcpTaskServiceTokenSupplier} reads or updates it during its lifecycle.
     * 用法 / Usage: 该字段通过 {@code HttpMcpTaskServiceTokenSupplier} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpMcpTaskServiceTokenSupplier}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Duration renewalSkew;

    /**
     * UTC 业务时钟；UTC business clock.
     * 补充说明 / Supplementary summary: 保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code HttpMcpTaskServiceTokenSupplier} 在其生命周期内读取或更新。
     * English supplement: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code HttpMcpTaskServiceTokenSupplier} reads or updates it during its lifecycle.
     * 用法 / Usage: 该字段通过 {@code HttpMcpTaskServiceTokenSupplier} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpMcpTaskServiceTokenSupplier}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * Token Endpoint 传输边界；Token Endpoint transport boundary.
     * 补充说明 / Supplementary summary: 保存 endpoint 对应的状态、依赖或配置值；字段类型为 {@code TokenEndpoint}，由 {@code HttpMcpTaskServiceTokenSupplier} 在其生命周期内读取或更新。
     * English supplement: Holds the state, dependency, or configuration represented by endpoint; its type is {@code TokenEndpoint}, and {@code HttpMcpTaskServiceTokenSupplier} reads or updates it during its lifecycle.
     * 用法 / Usage: 该字段通过 {@code HttpMcpTaskServiceTokenSupplier} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpMcpTaskServiceTokenSupplier}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final TokenEndpoint endpoint;

    /**
     * tenant 与 Resource 双重隔离的 Token 缓存；token cache isolated by tenant and Resource.
     * 补充说明 / Supplementary summary: 保存 cachedTokens 对应的状态、依赖或配置值；字段类型为 {@code Map<CacheKey, CachedToken>}，由 {@code HttpMcpTaskServiceTokenSupplier} 在其生命周期内读取或更新。
     * English supplement: Holds the state, dependency, or configuration represented by cached tokens; its type is {@code Map<CacheKey, CachedToken>}, and {@code HttpMcpTaskServiceTokenSupplier} reads or updates it during its lifecycle.
     * 用法 / Usage: 该字段通过 {@code HttpMcpTaskServiceTokenSupplier} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpMcpTaskServiceTokenSupplier}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Map<CacheKey, CachedToken> cachedTokens = new HashMap<>();

    /**
     * 创建生产 HTTP SERVICE Token 提供器。
     * Creates the production HTTP SERVICE-token supplier.
     *
     * @param restClient Spring HTTP 客户端；Spring HTTP client
     * @param tokenEndpoint IdP Token Endpoint；IdP Token Endpoint
     * @param assertions {@code private_key_jwt} 工厂；assertion factory
     * @param scopes 任务执行 Scope；task-execution scopes
     * @param renewalSkew 提前续签窗口；renewal skew
     * @param clock UTC 业务时钟；UTC business clock
     * 补充说明 / Supplementary summary: 创建 {@code HttpMcpTaskServiceTokenSupplier} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English supplement: Creates an instance of {@code HttpMcpTaskServiceTokenSupplier} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     */
    public HttpMcpTaskServiceTokenSupplier(
            RestClient restClient,
            URI tokenEndpoint,
            PrivateKeyJwtAssertionFactory assertions,
            Set<String> scopes,
            Duration renewalSkew,
            Clock clock
    ) {
        this(
                Objects.requireNonNull(assertions, "assertions").clientId(),
                assertions::create,
                scopes,
                renewalSkew,
                clock,
                httpEndpoint(restClient, tokenEndpoint)
        );
    }

    /**
     * 测试接缝；test seam.
     * 补充说明 / Supplementary summary: 创建 {@code HttpMcpTaskServiceTokenSupplier} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English supplement: Creates an instance of {@code HttpMcpTaskServiceTokenSupplier} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     */
    HttpMcpTaskServiceTokenSupplier(
            String clientId,
            Supplier<String> assertions,
            Set<String> scopes,
            Duration renewalSkew,
            Clock clock,
            TokenEndpoint endpoint
    ) {
        this.clientId = required(clientId, "clientId");
        this.assertions = Objects.requireNonNull(assertions, "assertions");
        this.scopes = normalizedScopes(scopes);
        this.renewalSkew = positive(renewalSkew, "renewalSkew");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
    }

    /**
     * 获取绑定到精确 tenant 与 Resource 的 Token，必要时同步续签。
     * Gets a token bound to the exact tenant and Resource, renewing synchronously when needed.
     * 补充说明 / Supplementary summary: 执行 issue 操作；该方法是 {@code HttpMcpTaskServiceTokenSupplier} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English supplement: Executes the issue operation; this method is the invocation entry point on {@code HttpMcpTaskServiceTokenSupplier} and performs the corresponding runtime, management, or protocol work.
     * 用法 / Usage: 调用方式 / Usage: {@code HttpMcpTaskServiceTokenSupplier.issue(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    @Override
    public String issue(String tenantId, URI resourceUri) {
        CacheKey key = new CacheKey(
                required(tenantId, "tenantId"),
                resource(resourceUri)
        );
        synchronized (this) {
            CachedToken current = cachedTokens.get(key);
            if (current != null && clock.instant().isBefore(current.renewAt())) {
                return current.value();
            }
            TokenResponse response;
            try {
                response = endpoint.issue(new TokenRequest(
                        clientId,
                        required(assertions.get(), "assertion"),
                        key.resourceUri(),
                        key.tenantId(),
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
            cachedTokens.put(key, issued);
            return issued.value();
        }
    }

    /**
     * 中文说明：执行 token 操作；该方法是 {@code HttpMcpTaskServiceTokenSupplier} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the token operation; this method is the invocation entry point on {@code HttpMcpTaskServiceTokenSupplier} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code HttpMcpTaskServiceTokenSupplier.token(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param response 参数 响应；parameter response。
     * @return 返回 token 的处理结果；returns the result of the operation.
     */
    private CachedToken token(TokenResponse response) {
        if (response == null
                || response.accessToken() == null
                || response.accessToken().isBlank()
                || response.accessToken().length() > 8192
                || !"Bearer".equalsIgnoreCase(response.tokenType())
                || response.expiresIn() <= renewalSkew.toSeconds()) {
            throw new IllegalStateException("IDP_SERVICE_TOKEN_RESPONSE_INVALID");
        }
        return new CachedToken(
                response.accessToken(),
                clock.instant().plusSeconds(response.expiresIn())
                        .minus(renewalSkew)
        );
    }

    /**
     * 中文说明：执行 httpEndpoint 操作；该方法是 {@code HttpMcpTaskServiceTokenSupplier} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the http endpoint operation; this method is the invocation entry point on {@code HttpMcpTaskServiceTokenSupplier} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code HttpMcpTaskServiceTokenSupplier.httpEndpoint(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param restClient 参数 rest客户端；parameter rest client。
     * @param tokenEndpoint 参数 tokenEndpoint；parameter token endpoint。
     * @return 返回 httpEndpoint 的处理结果；returns the result of the operation.
     */
    private static TokenEndpoint httpEndpoint(
            RestClient restClient,
            URI tokenEndpoint
    ) {
        RestClient client = Objects.requireNonNull(restClient, "restClient");
        URI endpoint = secureEndpoint(tokenEndpoint);
        return request -> {
            try {
                return client.post()
                        .uri(endpoint)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(form(request))
                        .retrieve()
                        .body(TokenResponse.class);
            } catch (RuntimeException exception) {
                throw new IllegalStateException(
                        "IDP_SERVICE_TOKEN_UNAVAILABLE",
                        exception
                );
            }
        };
    }

    /**
     * 中文说明：执行 form 操作；该方法是 {@code HttpMcpTaskServiceTokenSupplier} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the form operation; this method is the invocation entry point on {@code HttpMcpTaskServiceTokenSupplier} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code HttpMcpTaskServiceTokenSupplier.form(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 form 的处理结果；returns the result of the operation.
     */
    private static MultiValueMap<String, String> form(TokenRequest request) {
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", request.clientId());
        form.add("client_assertion_type", ASSERTION_TYPE);
        form.add("client_assertion", request.assertion());
        form.add("resource", request.resourceUri().toString());
        form.add("tenant_id", request.tenantId());
        form.add("scope", String.join(" ", request.scopes()));
        return form;
    }

    /**
     * 中文说明：执行 secureEndpoint 操作；该方法是 {@code HttpMcpTaskServiceTokenSupplier} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the secure endpoint operation; this method is the invocation entry point on {@code HttpMcpTaskServiceTokenSupplier} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code HttpMcpTaskServiceTokenSupplier.secureEndpoint(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 secureEndpoint 的处理结果；returns the result of the operation.
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
                && !("http".equalsIgnoreCase(value.getScheme()) && loopback))) {
            throw new IllegalArgumentException(
                    "tokenEndpoint must use HTTPS or loopback HTTP"
            );
        }
        return value;
    }

    /**
     * 中文说明：执行 资源 操作；该方法是 {@code HttpMcpTaskServiceTokenSupplier} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the resource operation; this method is the invocation entry point on {@code HttpMcpTaskServiceTokenSupplier} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code HttpMcpTaskServiceTokenSupplier.resource(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 资源 的处理结果；returns the result of the operation.
     */
    private static URI resource(URI value) {
        Objects.requireNonNull(value, "resourceUri");
        if (!value.isAbsolute()
                || value.getFragment() != null
                || !value.equals(value.normalize())) {
            throw new IllegalArgumentException("resourceUri is invalid");
        }
        return value;
    }

    /**
     * 中文说明：执行 normalizedScopes 操作；该方法是 {@code HttpMcpTaskServiceTokenSupplier} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the normalized scopes operation; this method is the invocation entry point on {@code HttpMcpTaskServiceTokenSupplier} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code HttpMcpTaskServiceTokenSupplier.normalizedScopes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param values 参数 values；parameter values。
     * @return 返回 normalizedScopes 的处理结果；returns the result of the operation.
     */
    private static Set<String> normalizedScopes(Set<String> values) {
        Objects.requireNonNull(values, "scopes");
        TreeSet<String> result = new TreeSet<>();
        values.forEach(value -> result.add(required(value, "scope")));
        if (result.isEmpty()) {
            throw new IllegalArgumentException("scopes must not be empty");
        }
        return Set.copyOf(result);
    }

    /**
     * 中文说明：执行 positive 操作；该方法是 {@code HttpMcpTaskServiceTokenSupplier} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the positive operation; this method is the invocation entry point on {@code HttpMcpTaskServiceTokenSupplier} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code HttpMcpTaskServiceTokenSupplier.positive(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 positive 的处理结果；returns the result of the operation.
     */
    private static Duration positive(Duration value, String field) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code HttpMcpTaskServiceTokenSupplier} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code HttpMcpTaskServiceTokenSupplier} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code HttpMcpTaskServiceTokenSupplier.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    /**
     * Token Endpoint 测试/传输边界；Token Endpoint test and transport seam.
     * 补充说明 / Supplementary summary: {@code TokenEndpoint} 是接口契约，位于当前 Gateway 模块的相关包中，负责TokenEndpoint相关的职责与边界。
     * English supplement: {@code TokenEndpoint} is an interface contract in the current Gateway module; it owns the token endpoint-related responsibility and boundary.
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    @FunctionalInterface
    interface TokenEndpoint {
        /**
         * 中文说明：执行 issue 操作；该方法是 {@code HttpMcpTaskServiceTokenSupplier.TokenEndpoint} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the issue operation; this method is the invocation entry point on {@code HttpMcpTaskServiceTokenSupplier.TokenEndpoint} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code HttpMcpTaskServiceTokenSupplier.TokenEndpoint.issue(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param request 参数 请求；parameter request。
         * @return 返回 issue 的处理结果；returns the result of the operation.
         */
        TokenResponse issue(TokenRequest request);
    }

    /**
     * Token 请求；token request.
     * 补充说明 / Supplementary summary: {@code TokenRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Token请求相关的职责与边界。
     * English supplement: {@code TokenRequest} is an immutable data carrier in the current Gateway module; it owns the token request-related responsibility and boundary.
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.

     * @param clientId 参数 客户端Id；parameter client id。
     * @param assertion 参数 assertion；parameter assertion。
     * @param resourceUri 参数 资源Uri；parameter resource uri。
     * @param tenantId 参数 tenantId；parameter tenant id。
     * @param scopes 参数 scopes；parameter scopes。     */
    record TokenRequest(
            /**
             * 中文说明：保存 客户端Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code HttpMcpTaskServiceTokenSupplier.TokenRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by client id; its type is {@code String}, and {@code HttpMcpTaskServiceTokenSupplier.TokenRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code HttpMcpTaskServiceTokenSupplier.TokenRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpMcpTaskServiceTokenSupplier.TokenRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String clientId,
            /**
             * 中文说明：保存 assertion 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code HttpMcpTaskServiceTokenSupplier.TokenRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by assertion; its type is {@code String}, and {@code HttpMcpTaskServiceTokenSupplier.TokenRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code HttpMcpTaskServiceTokenSupplier.TokenRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpMcpTaskServiceTokenSupplier.TokenRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String assertion,
            /**
             * 中文说明：保存 资源Uri 对应的状态、依赖或配置值；字段类型为 {@code URI}，由 {@code HttpMcpTaskServiceTokenSupplier.TokenRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by resource uri; its type is {@code URI}, and {@code HttpMcpTaskServiceTokenSupplier.TokenRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code HttpMcpTaskServiceTokenSupplier.TokenRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpMcpTaskServiceTokenSupplier.TokenRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            URI resourceUri,
            /**
             * 中文说明：保存 tenantId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code HttpMcpTaskServiceTokenSupplier.TokenRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by tenant id; its type is {@code String}, and {@code HttpMcpTaskServiceTokenSupplier.TokenRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code HttpMcpTaskServiceTokenSupplier.TokenRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpMcpTaskServiceTokenSupplier.TokenRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String tenantId,
            /**
             * 中文说明：保存 scopes 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code HttpMcpTaskServiceTokenSupplier.TokenRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by scopes; its type is {@code Set<String>}, and {@code HttpMcpTaskServiceTokenSupplier.TokenRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code HttpMcpTaskServiceTokenSupplier.TokenRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpMcpTaskServiceTokenSupplier.TokenRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            Set<String> scopes
    ) {
    }

    /**
     * Token 响应；token response.
     * 补充说明 / Supplementary summary: {@code TokenResponse} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Token响应相关的职责与边界。
     * English supplement: {@code TokenResponse} is an immutable data carrier in the current Gateway module; it owns the token response-related responsibility and boundary.
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.

     * @param accessToken 参数 accessToken；parameter access token。
     * @param tokenType 参数 tokenType；parameter token type。
     * @param expiresIn 参数 expiresIn；parameter expires in。     */
    record TokenResponse(
            /**
             * 中文说明：保存 accessToken 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code HttpMcpTaskServiceTokenSupplier.TokenResponse} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by access token; its type is {@code String}, and {@code HttpMcpTaskServiceTokenSupplier.TokenResponse} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code HttpMcpTaskServiceTokenSupplier.TokenResponse} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpMcpTaskServiceTokenSupplier.TokenResponse}; do not couple callers to its representation when the owning type exposes an API.
             */
            @JsonProperty("access_token") String accessToken,
            /**
             * 中文说明：保存 tokenType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code HttpMcpTaskServiceTokenSupplier.TokenResponse} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by token type; its type is {@code String}, and {@code HttpMcpTaskServiceTokenSupplier.TokenResponse} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code HttpMcpTaskServiceTokenSupplier.TokenResponse} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpMcpTaskServiceTokenSupplier.TokenResponse}; do not couple callers to its representation when the owning type exposes an API.
             */
            @JsonProperty("token_type") String tokenType,
            /**
             * 中文说明：保存 expiresIn 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code HttpMcpTaskServiceTokenSupplier.TokenResponse} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expires in; its type is {@code long}, and {@code HttpMcpTaskServiceTokenSupplier.TokenResponse} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code HttpMcpTaskServiceTokenSupplier.TokenResponse} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpMcpTaskServiceTokenSupplier.TokenResponse}; do not couple callers to its representation when the owning type exposes an API.
             */
            @JsonProperty("expires_in") long expiresIn
    ) {
    }

    /**
     * 缓存键；cache key.
     * 补充说明 / Supplementary summary: {@code CacheKey} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Cache键相关的职责与边界。
     * English supplement: {@code CacheKey} is an immutable data carrier in the current Gateway module; it owns the cache key-related responsibility and boundary.
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.

     * @param tenantId 参数 tenantId；parameter tenant id。
     * @param resourceUri 参数 资源Uri；parameter resource uri。     */
    private record CacheKey(
    /**
     * 中文说明：保存 tenantId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code HttpMcpTaskServiceTokenSupplier.CacheKey} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by tenant id; its type is {@code String}, and {@code HttpMcpTaskServiceTokenSupplier.CacheKey} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code HttpMcpTaskServiceTokenSupplier.CacheKey} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpMcpTaskServiceTokenSupplier.CacheKey}; do not couple callers to its representation when the owning type exposes an API.
     */
    String tenantId,
    /**
     * 中文说明：保存 资源Uri 对应的状态、依赖或配置值；字段类型为 {@code URI}，由 {@code HttpMcpTaskServiceTokenSupplier.CacheKey} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by resource uri; its type is {@code URI}, and {@code HttpMcpTaskServiceTokenSupplier.CacheKey} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code HttpMcpTaskServiceTokenSupplier.CacheKey} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpMcpTaskServiceTokenSupplier.CacheKey}; do not couple callers to its representation when the owning type exposes an API.
     */
    URI resourceUri) {
    }

    /**
     * 缓存 Token；cached token.
     * 补充说明 / Supplementary summary: {@code CachedToken} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责CachedToken相关的职责与边界。
     * English supplement: {@code CachedToken} is an immutable data carrier in the current Gateway module; it owns the cached token-related responsibility and boundary.
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.

     * @param value 参数 值；parameter value。
     * @param renewAt 参数 renewAt；parameter renew at。     */
    private record CachedToken(
    /**
     * 中文说明：保存 值 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code HttpMcpTaskServiceTokenSupplier.CachedToken} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by value; its type is {@code String}, and {@code HttpMcpTaskServiceTokenSupplier.CachedToken} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code HttpMcpTaskServiceTokenSupplier.CachedToken} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpMcpTaskServiceTokenSupplier.CachedToken}; do not couple callers to its representation when the owning type exposes an API.
     */
    String value,
    /**
     * 中文说明：保存 renewAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code HttpMcpTaskServiceTokenSupplier.CachedToken} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by renew at; its type is {@code Instant}, and {@code HttpMcpTaskServiceTokenSupplier.CachedToken} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code HttpMcpTaskServiceTokenSupplier.CachedToken} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpMcpTaskServiceTokenSupplier.CachedToken}; do not couple callers to its representation when the owning type exposes an API.
     */
    Instant renewAt) {
    }
}
