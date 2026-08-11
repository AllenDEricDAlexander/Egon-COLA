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

    /** RFC 7523 Client Assertion 类型；RFC 7523 Client Assertion type. */
    private static final String ASSERTION_TYPE =
            "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    /** Source Client 标识；source Client identifier. */
    private final String clientId;

    /** 每次请求创建新 Assertion 的工厂；factory creating a new assertion for every request. */
    private final Supplier<String> assertions;

    /** RBAC3 Resource URI；RBAC3 Resource URI. */
    private final URI resourceUri;

    /** 请求的 IdP Service Scope；requested IdP service scopes. */
    private final Set<String> scopes;

    /** 提前续签窗口；renewal skew. */
    private final Duration renewalSkew;

    /** UTC 业务时钟；UTC business clock. */
    private final Clock clock;

    /** Token Endpoint 传输边界；Token Endpoint transport boundary. */
    private final TokenEndpoint endpoint;

    /** 按精确租户隔离的 Token 缓存；token cache isolated by exact tenant. */
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

    /** 测试接缝；test seam. */
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

    /** 校验 Token Endpoint 响应并计算续签时间；validates a response and calculates renewal. */
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

    /** 创建表单编码的生产 Token Endpoint；creates the form-encoded production endpoint. */
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

    /** 构造 OAuth2 Client Credentials 表单；builds the OAuth2 Client Credentials form. */
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

    /** 编码一个表单字段；encodes one form field. */
    private static String parameter(String name, String value) {
        return URLEncoder.encode(name, StandardCharsets.UTF_8)
                + '='
                + URLEncoder.encode(value, StandardCharsets.UTF_8);
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
                && !("http".equalsIgnoreCase(value.getScheme())
                && loopback))) {
            throw new IllegalArgumentException(
                    "tokenEndpoint must use HTTPS or loopback HTTP"
            );
        }
        return value;
    }

    private static URI resource(URI value) {
        Objects.requireNonNull(value, "resourceUri");
        if (!value.isAbsolute() || value.getFragment() != null
                || !value.equals(value.normalize())) {
            throw new IllegalArgumentException("resourceUri is invalid");
        }
        return value;
    }

    private static Set<String> normalizedScopes(Set<String> values) {
        Objects.requireNonNull(values, "scopes");
        TreeSet<String> normalized = new TreeSet<>();
        values.forEach(value -> normalized.add(required(value, "scope")));
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("scopes must not be empty");
        }
        return Set.copyOf(normalized);
    }

    private static Duration positive(Duration value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }

    /** Token Endpoint 请求；Token Endpoint request. */
    record TokenRequest(
            String clientId,
            String assertion,
            URI resourceUri,
            String tenantId,
            Set<String> scopes
    ) {
        TokenRequest {
            scopes = Set.copyOf(scopes);
        }
    }

    /** Token Endpoint 响应；Token Endpoint response. */
    record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") long expiresIn
    ) {
    }

    /** 可缓存 Token；cacheable token. */
    private record CachedToken(String value, Instant renewAt) {
    }

    /** 可替换 Token Endpoint 传输；replaceable Token Endpoint transport. */
    @FunctionalInterface
    interface TokenEndpoint {
        TokenResponse issue(TokenRequest request);
    }
}
