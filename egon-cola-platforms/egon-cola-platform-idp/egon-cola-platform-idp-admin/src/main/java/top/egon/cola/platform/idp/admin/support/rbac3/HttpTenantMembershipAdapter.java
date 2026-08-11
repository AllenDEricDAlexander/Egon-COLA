package top.egon.cola.platform.idp.admin.support.rbac3;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import top.egon.cola.platform.idp.core.port.TenantMembershipPort;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 调用 RBAC3 内部身份接口解析用户租户成员关系。
 *
 * <p>Calls RBAC3 internal identity endpoints to resolve user tenant memberships.</p>
 */
public final class HttpTenantMembershipAdapter
        implements TenantMembershipPort {

    /** HTTP 客户端；HTTP client. */
    private final RestClient restClient;

    /** RBAC3 基础地址；RBAC3 base URL. */
    private final String baseUrl;

    /** IdP 服务身份 Authorization 请求头来源；IdP service Authorization header source. */
    private final Supplier<String> authorizationHeader;

    /**
     * 创建 RBAC3 租户成员关系适配器。
     *
     * <p>Creates the RBAC3 tenant-membership adapter.</p>
     *
     * @param restClient HTTP 客户端；HTTP client
     * @param baseUrl RBAC3 基础地址；RBAC3 base URL
     * @param authorizationHeader IdP 服务身份请求头来源；IdP service header source
     */
    public HttpTenantMembershipAdapter(
            RestClient restClient,
            String baseUrl,
            Supplier<String> authorizationHeader
    ) {
        this.restClient = Objects.requireNonNull(restClient, "restClient");
        this.baseUrl = validBaseUrl(baseUrl);
        this.authorizationHeader = Objects.requireNonNull(
                authorizationHeader,
                "authorizationHeader"
        );
    }

    /**
     * 解析一个用户在指定租户和 Client 上下文中的成员关系。
     *
     * <p>Resolves one user's membership in a tenant and Client context.</p>
     *
     * @param identitySub 用户身份标识；user identity subject
     * @param tenantId 租户标识；tenant identifier
     * @param clientId Client 标识；Client identifier
     * @return 已解析成员关系；resolved membership
     */
    @Override
    public TenantMembership resolve(
            String identitySub,
            String tenantId,
            String clientId
    ) {
        try {
            MembershipEnvelope response = restClient.post()
                    .uri(baseUrl + "/internal/v1/identity/resolve")
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            serviceAuthorization()
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ResolveRequest(
                            required(identitySub, "identitySub"),
                            required(tenantId, "tenantId"),
                            required(clientId, "clientId")
                    ))
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            (request, responseStatus) -> {
                                throw membershipFailure();
                            }
                    )
                    .body(MembershipEnvelope.class);
            return toDomain(response == null ? null : response.data(),
                    identitySub, tenantId);
        } catch (TenantMembershipException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw membershipFailure();
        }
    }

    /**
     * 列出用户在一个 Client 上下文中可用的 ACTIVE 租户成员关系。
     *
     * <p>Lists the user's ACTIVE tenant memberships in one Client context.</p>
     *
     * @param identitySub 用户身份标识；user identity subject
     * @param clientId Client 标识；Client identifier
     * @return ACTIVE 成员关系列表；ACTIVE membership list
     */
    @Override
    public List<TenantMembership> list(
            String identitySub,
            String clientId
    ) {
        try {
            String subject = required(identitySub, "identitySub");
            MembershipListEnvelope response = restClient.get()
                    .uri(
                            baseUrl
                                    + "/internal/v1/identity/{identitySub}"
                                    + "/tenants?clientId={clientId}",
                            subject,
                            required(clientId, "clientId")
                    )
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            serviceAuthorization()
                    )
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            (request, responseStatus) -> {
                                throw membershipFailure();
                            }
                    )
                    .body(MembershipListEnvelope.class);
            if (response == null || response.data() == null) {
                throw membershipFailure();
            }
            return response.data().stream()
                    .map(value -> toDomain(
                            value,
                            subject,
                            value.tenantId()
                    ))
                    .filter(value -> value.status()
                            == MembershipStatus.ACTIVE)
                    .toList();
        } catch (TenantMembershipException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw membershipFailure();
        }
    }

    /**
     * 校验 RBAC3 响应绑定并映射领域成员关系。
     *
     * <p>Validates RBAC3 response binding and maps a domain membership.</p>
     *
     * @param response RBAC3 成员响应；RBAC3 membership response
     * @param expectedIdentitySub 期望用户身份；expected user identity
     * @param expectedTenantId 期望租户；expected tenant
     * @return 已验证成员关系；validated membership
     */
    private TenantMembership toDomain(
            MembershipResponse response,
            String expectedIdentitySub,
            String expectedTenantId
    ) {
        if (response == null
                || !expectedIdentitySub.equals(response.identitySub())
                || !expectedTenantId.equals(response.tenantId())) {
            throw membershipFailure();
        }
        try {
            return new TenantMembership(
                    required(response.identitySub(), "identitySub"),
                    required(response.tenantId(), "tenantId"),
                    required(response.rbac3UserId(), "rbac3UserId"),
                    required(
                            response.tenantDisplayName(),
                            "tenantDisplayName"
                    ),
                    MembershipStatus.valueOf(required(
                            response.status(),
                            "status"
                    ))
            );
        } catch (IllegalArgumentException exception) {
            throw membershipFailure();
        }
    }

    /**
     * 获取并校验服务身份请求头。
     *
     * <p>Obtains and validates the service-identity request header.</p>
     *
     * @return 可安全发送的 Authorization 请求头；safe Authorization header
     */
    private String serviceAuthorization() {
        String value = authorizationHeader.get();
        if (value == null
                || value.isBlank()
                || !value.equals(value.trim())
                || value.contains("\r")
                || value.contains("\n")) {
            throw membershipFailure();
        }
        return value;
    }

    /**
     * 校验并规范化 RBAC3 基础地址。
     *
     * <p>Validates and normalizes the RBAC3 base URL.</p>
     *
     * @param value 原始基础地址；raw base URL
     * @return 规范化基础地址；normalized base URL
     */
    private static String validBaseUrl(String value) {
        URI uri = URI.create(required(value, "baseUrl"));
        String scheme = uri.getScheme();
        if (!("http".equalsIgnoreCase(scheme)
                || "https".equalsIgnoreCase(scheme))
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null) {
            throw new IllegalArgumentException("invalid RBAC3 base URL");
        }
        String normalized = uri.toString();
        return normalized.endsWith("/")
                ? normalized.substring(0, normalized.length() - 1)
                : normalized;
    }

    /**
     * 校验必填文本。
     *
     * <p>Validates required text.</p>
     *
     * @param value 待校验值；value to validate
     * @param field 字段名；field name
     * @return 已校验值；validated value
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    /**
     * 创建不暴露传输细节的成员关系失败异常。
     *
     * <p>Creates a membership failure without exposing transport details.</p>
     *
     * @return 成员关系异常；membership exception
     */
    private static TenantMembershipException membershipFailure() {
        return new TenantMembershipException(
                "active tenant membership was not resolved"
        );
    }

    /**
     * RBAC3 身份解析请求。
     *
     * <p>RBAC3 identity-resolution request.</p>
     *
     * @param identitySub 用户身份；user identity
     * @param tenantId 租户；tenant
     * @param clientId Client 标识；Client identifier
     */
    private record ResolveRequest(
            String identitySub,
            String tenantId,
            String clientId
    ) {
    }

    /**
     * RBAC3 单成员响应包。
     *
     * <p>RBAC3 single-membership response envelope.</p>
     *
     * @param data 成员响应；membership response
     */
    private record MembershipEnvelope(MembershipResponse data) {
    }

    /**
     * RBAC3 成员列表示响应包。
     *
     * <p>RBAC3 membership-list response envelope.</p>
     *
     * @param data 成员响应列表；membership response list
     */
    private record MembershipListEnvelope(List<MembershipResponse> data) {
    }

    /**
     * RBAC3 最小成员关系响应。
     *
     * <p>RBAC3 minimal membership response.</p>
     *
     * @param identitySub 用户身份；user identity
     * @param tenantId 租户；tenant
     * @param rbac3UserId RBAC3 用户标识；RBAC3 user identifier
     * @param tenantDisplayName 租户展示名称；tenant display name
     * @param status 成员关系状态；membership status
     */
    private record MembershipResponse(
            String identitySub,
            String tenantId,
            String rbac3UserId,
            String tenantDisplayName,
            String status
    ) {
    }
}
