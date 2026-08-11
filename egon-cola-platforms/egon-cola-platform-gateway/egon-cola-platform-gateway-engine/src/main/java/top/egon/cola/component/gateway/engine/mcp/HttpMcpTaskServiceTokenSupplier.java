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
 */
public final class HttpMcpTaskServiceTokenSupplier
        implements McpTaskServiceTokenSupplier {

    /** RFC 7523 Client Assertion 类型；RFC 7523 Client Assertion type. */
    private static final String ASSERTION_TYPE =
            "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    /** Source Client 标识；source Client identifier. */
    private final String clientId;

    /** 每次请求创建新 Assertion 的工厂；factory creating a fresh assertion per request. */
    private final Supplier<String> assertions;

    /** IdP 许可的任务执行 Scope；IdP-authorized task-execution scopes. */
    private final Set<String> scopes;

    /** 提前续签窗口；renewal skew. */
    private final Duration renewalSkew;

    /** UTC 业务时钟；UTC business clock. */
    private final Clock clock;

    /** Token Endpoint 传输边界；Token Endpoint transport boundary. */
    private final TokenEndpoint endpoint;

    /** tenant 与 Resource 双重隔离的 Token 缓存；token cache isolated by tenant and Resource. */
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

    /** 测试接缝；test seam. */
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

    private static URI resource(URI value) {
        Objects.requireNonNull(value, "resourceUri");
        if (!value.isAbsolute()
                || value.getFragment() != null
                || !value.equals(value.normalize())) {
            throw new IllegalArgumentException("resourceUri is invalid");
        }
        return value;
    }

    private static Set<String> normalizedScopes(Set<String> values) {
        Objects.requireNonNull(values, "scopes");
        TreeSet<String> result = new TreeSet<>();
        values.forEach(value -> result.add(required(value, "scope")));
        if (result.isEmpty()) {
            throw new IllegalArgumentException("scopes must not be empty");
        }
        return Set.copyOf(result);
    }

    private static Duration positive(Duration value, String field) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    /** Token Endpoint 测试/传输边界；Token Endpoint test and transport seam. */
    @FunctionalInterface
    interface TokenEndpoint {
        TokenResponse issue(TokenRequest request);
    }

    /** Token 请求；token request. */
    record TokenRequest(
            String clientId,
            String assertion,
            URI resourceUri,
            String tenantId,
            Set<String> scopes
    ) {
    }

    /** Token 响应；token response. */
    record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") long expiresIn
    ) {
    }

    /** 缓存键；cache key. */
    private record CacheKey(String tenantId, URI resourceUri) {
    }

    /** 缓存 Token；cached token. */
    private record CachedToken(String value, Instant renewAt) {
    }
}
