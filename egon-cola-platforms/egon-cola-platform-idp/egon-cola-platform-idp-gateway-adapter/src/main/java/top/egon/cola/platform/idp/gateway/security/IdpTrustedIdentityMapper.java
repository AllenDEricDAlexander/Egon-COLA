package top.egon.cola.platform.idp.gateway.security;

import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.core.context.GatewayPrincipal;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.component.gateway.core.security.GatewayIdentityMapper;
import top.egon.cola.component.gateway.core.security.TrustedIdentity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 把已验证且不可变的身份字段映射为固定的后端可信请求头。
 * 映射前要求主体已经认证且租户存在，并只输出用户主体、租户、会话、客户端和令牌标识；
 * 用户资料与权限不会透传。
 *
 * <p>Maps only verified immutable identity fields into fixed trusted downstream headers. The
 * principal must be authenticated and have a tenant, and only subject, tenant, session, client,
 * and token identifiers are emitted. User profile data and authorities are not propagated.</p>
 */
public final class IdpTrustedIdentityMapper implements GatewayIdentityMapper {

    /**
     * Gateway 策略引用本身份映射器时使用的稳定标识。
     *
     * <p>Stable identifier used by Gateway policy to select this identity mapper.</p>
     */
    public static final String MAPPER_ID = "idp-identity";

    /**
     * 创建固定 IdP 可信身份映射器。
     *
     * <p>Creates the fixed IdP trusted-identity mapper.</p>
     */
    public IdpTrustedIdentityMapper() {
    }

    /**
     * 返回身份映射器稳定标识。
     *
     * <p>Returns the stable identity-mapper identifier.</p>
     *
     * @return {@value #MAPPER_ID}
     */
    @Override
    public String mapperId() {
        return MAPPER_ID;
    }

    /**
     * 返回本映射器支持的 Gateway 协议。
     *
     * <p>Returns the Gateway protocols supported by this mapper.</p>
     *
     * @return HTTP 与 RPC 协议集合；HTTP and RPC protocol set
     */
    @Override
    public Set<GatewayProtocol> supportedProtocols() {
        return Set.of(GatewayProtocol.HTTP, GatewayProtocol.RPC);
    }

    /**
     * 将已认证 Gateway 主体转换为发往 HTTP 后端的可信身份头。
     *
     * <p>Converts an authenticated Gateway principal into trusted identity headers for an HTTP
     * backend.</p>
     *
     * @param context 当前 Gateway 认证上下文；current Gateway authentication context
     * @return 包含固定 IdP 身份头的可信身份；trusted identity containing fixed IdP headers
     * @throws IllegalArgumentException 当主体未认证、租户缺失或必需声明缺失时；when the
     *                                  principal is unauthenticated, the tenant is absent, or a
     *                                  required claim is missing
     */
    @Override
    public TrustedIdentity map(GatewayAuthContext context) {
        GatewayPrincipal principal = context.principal();
        if (!principal.authenticated() || principal.tenantId() == null) {
            throw new IllegalArgumentException(
                    "authenticated IdP principal is required");
        }
        Map<String, String> attributes = principal.attributes();
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Egon-Principal-Type", principal.principalType());
        headers.put("X-Egon-Identity-Sub", principal.principalId());
        headers.put("X-Egon-Tenant-Id", principal.tenantId());
        headers.put("X-Egon-Client-Id", required(
                attributes, "idp.client-id"));
        headers.put("X-Egon-Token-Id", required(
                attributes, "idp.token-id"));
        headers.put("X-Egon-Resource-Uri", required(
                attributes, "idp.resource-uri"));
        if ("USER".equals(principal.principalType())) {
            headers.put("X-Egon-Session-Id", required(
                    attributes, "idp.session-id"));
            headers.put("X-Egon-Token-Version", required(
                    attributes, "idp.token-version"));
        } else if ("SERVICE".equals(principal.principalType())) {
            headers.put("X-Egon-Resource-Version", required(
                    attributes, "idp.resource-version"));
            headers.put("X-Egon-Source-Biz", required(
                    attributes, "idp.source-biz"));
            headers.put("X-Egon-Source-App", required(
                    attributes, "idp.source-app"));
            headers.put("X-Egon-Source-Env", required(
                    attributes, "idp.source-env"));
            headers.put("X-Egon-Service-Scopes", required(
                    attributes, "idp.service-scopes"));
            headers.put("X-Egon-Credential-Id", required(
                    attributes, "idp.credential-id"));
        } else {
            throw new IllegalArgumentException("unsupported IdP principal type");
        }
        Map<String, String> rpcMetadata = new LinkedHashMap<>();
        headers.forEach((name, value) -> rpcMetadata.put(
                "egon-gateway-" + name.substring("X-Egon-".length())
                        .toLowerCase(java.util.Locale.ROOT), value));
        return new TrustedIdentity(headers, rpcMetadata);
    }

    /**
     * 读取映射所必需的已验证属性。
     *
     * <p>Reads a verified attribute required by the mapping.</p>
     *
     * @param values 已验证主体属性；verified principal attributes
     * @param name 属性名称；attribute name
     * @return 非空属性值；non-blank attribute value
     * @throws IllegalArgumentException 当属性缺失或为空时；when the attribute is absent or blank
     */
    private String required(Map<String, String> values, String name) {
        String value = values.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "missing verified claim " + name);
        }
        return value;
    }
}
