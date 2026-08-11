package top.egon.cola.platform.idp.admin.token.service.impl;

import top.egon.cola.platform.idp.core.oauth.ClientAssertionAuthentication;
import top.egon.cola.platform.idp.core.oauth.OAuthClient;
import top.egon.cola.platform.idp.core.oauth.OAuthException;
import top.egon.cola.platform.idp.core.port.OAuthClientStore;
import top.egon.cola.platform.idp.core.port.ResourceServerStore;
import top.egon.cola.platform.idp.core.port.TokenSigner;
import top.egon.cola.platform.idp.core.resource.ClientCredentialsAccessPolicy;
import top.egon.cola.platform.idp.core.resource.ClientCredentialsAccessPolicy.ServiceResourceAccess;
import top.egon.cola.platform.idp.core.resource.ResourceAuthorizationException;
import top.egon.cola.platform.idp.core.resource.ResourceServer;
import top.egon.cola.platform.idp.core.token.ServiceAccessToken;
import top.egon.cola.platform.idp.core.token.ServiceAccessTokenClaims;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 在 IdP 内部授权并签发 OAuth Client Credentials SERVICE Token。
 *
 * <p>Authorizes and issues OAuth Client Credentials SERVICE tokens entirely inside IdP.</p>
 *
 * <p>服务复用 Specification 风格的 {@link ClientCredentialsAccessPolicy} 校验目标 Resource、
 * 精确租户和 Scope 子集，并从 Source Client 绑定的 Resource 推导来源三元组；全程不查询
 * RBAC3。</p>
 *
 * <p>The service reuses the Specification-style {@link ClientCredentialsAccessPolicy} to validate
 * the target Resource, exact tenant, and scope subset, and derives the source triple from the
 * Resource bound to the Source Client. RBAC3 is never queried.</p>
 */
public final class ClientCredentialsTokenService {

    /** OAuth Client 查询端口；OAuth Client lookup port. */
    private final OAuthClientStore clients;

    /** Resource Server 查询端口；Resource Server lookup port. */
    private final ResourceServerStore resources;

    /** IdP Service Grant 授权策略；IdP Service Grant authorization policy. */
    private final ClientCredentialsAccessPolicy accessPolicy;

    /** Token 签名端口；token signing port. */
    private final TokenSigner signer;

    /** UTC 业务时钟；UTC business clock. */
    private final Clock clock;

    /** JWT ID 生成器；JWT ID supplier. */
    private final Supplier<String> tokenIds;

    /**
     * 创建 Client Credentials Token 服务。
     *
     * <p>Creates the Client Credentials token service.</p>
     *
     * @param clients OAuth Client 查询端口；OAuth Client lookup port
     * @param resources Resource Server 查询端口；Resource Server lookup port
     * @param accessPolicy IdP Service Grant 授权策略；IdP Service Grant authorization policy
     * @param signer Token 签名端口；token signing port
     * @param clock UTC 业务时钟；UTC business clock
     * @param tokenIds JWT ID 生成器；JWT ID supplier
     */
    public ClientCredentialsTokenService(
            OAuthClientStore clients,
            ResourceServerStore resources,
            ClientCredentialsAccessPolicy accessPolicy,
            TokenSigner signer,
            Clock clock,
            Supplier<String> tokenIds
    ) {
        this.clients = Objects.requireNonNull(clients, "clients");
        this.resources = Objects.requireNonNull(resources, "resources");
        this.accessPolicy = Objects.requireNonNull(
                accessPolicy,
                "accessPolicy"
        );
        this.signer = Objects.requireNonNull(signer, "signer");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.tokenIds = Objects.requireNonNull(tokenIds, "tokenIds");
    }

    /**
     * 为已认证 Confidential Client 签发单 Resource、单租户 SERVICE Token。
     *
     * <p>Issues a single-Resource, single-tenant SERVICE token for an authenticated Confidential
     * Client.</p>
     *
     * @param authentication 已认证 Client Assertion；authenticated Client Assertion
     * @param resourceUri 目标 Resource URI；target Resource URI
     * @param tenantId 精确租户；exact tenant
     * @param requestedScopes 请求的 Scope；requested scopes
     * @param requestedTtl 请求的短期 Token 有效期；requested short-lived token lifetime
     * @return 不含 Refresh Token 的签发结果；issuance result without a refresh token
     */
    public ServiceAccessToken issue(
            ClientAssertionAuthentication authentication,
            URI resourceUri,
            String tenantId,
            Set<String> requestedScopes,
            Duration requestedTtl
    ) {
        Objects.requireNonNull(authentication, "authentication");
        Instant now = clock.instant();
        if (!authentication.expiresAt().isAfter(now)) {
            throw oauth("invalid_client");
        }
        OAuthClient client = clients.findById(authentication.clientId())
                .orElseThrow(() -> oauth("unauthorized_client"));
        if (client.status() != OAuthClient.Status.ACTIVE
                || client.clientType()
                != OAuthClient.ClientType.CONFIDENTIAL) {
            throw oauth("unauthorized_client");
        }
        ResourceServer target = resources.findByUri(
                        Objects.requireNonNull(resourceUri, "resourceUri")
                )
                .orElseThrow(() -> oauth("invalid_target"));
        ServiceResourceAccess access;
        try {
            access = accessPolicy.authorize(
                    client,
                    target,
                    tenantId,
                    requestedScopes
            );
        } catch (ResourceAuthorizationException exception) {
            if ("IDP_CLIENT_CREDENTIALS_UNAUTHORIZED".equals(
                    exception.code()
            ) || "IDP_CLIENT_DISABLED".equals(exception.code())) {
                throw oauth("unauthorized_client");
            }
            if ("IDP_SERVICE_SCOPE_INVALID".equals(exception.code())) {
                throw oauth("invalid_scope");
            }
            throw oauth("invalid_target");
        } catch (IllegalArgumentException exception) {
            throw oauth("invalid_target");
        }
        Duration ttl = effectiveTtl(requestedTtl, client.accessTokenTtl());
        Instant expiresAt = now.plus(ttl);
        ServiceAccessTokenClaims claims = new ServiceAccessTokenClaims(
                client.clientId(),
                client.clientId(),
                access.targetResourceUri(),
                access.tenantId(),
                access.sourceBizCode(),
                access.sourceAppCode(),
                access.sourceEnvironment(),
                authentication.credentialId(),
                access.targetResourceVersion(),
                access.scopes(),
                required(tokenIds.get(), "tokenId"),
                now,
                now,
                expiresAt
        );
        return new ServiceAccessToken(
                signer.signServiceAccess(claims),
                "Bearer",
                expiresAt,
                access.scopes()
        );
    }

    /**
     * 将配置 TTL 限制在 Client 自身的最大有效期内。
     *
     * <p>Caps the configured lifetime at the Client's own maximum lifetime.</p>
     *
     * @param requested 请求 TTL；requested lifetime
     * @param clientMaximum Client 最大 TTL；Client maximum lifetime
     * @return 有效 TTL；effective lifetime
     */
    private static Duration effectiveTtl(
            Duration requested,
            Duration clientMaximum
    ) {
        Objects.requireNonNull(requested, "requestedTtl");
        Objects.requireNonNull(clientMaximum, "clientMaximum");
        if (requested.isZero() || requested.isNegative()) {
            throw oauth("invalid_request");
        }
        return requested.compareTo(clientMaximum) > 0
                ? clientMaximum
                : requested;
    }

    /**
     * 创建安全 OAuth 协议错误。
     *
     * <p>Creates a safe OAuth protocol error.</p>
     *
     * @param error OAuth 错误码；OAuth error code
     * @return OAuth 异常；OAuth exception
     */
    private static OAuthException oauth(String error) {
        return new OAuthException(error, error);
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
            throw oauth("invalid_request");
        }
        return value;
    }
}
