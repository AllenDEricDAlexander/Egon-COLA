package top.egon.cola.platform.idp.admin.resource.service.impl;

import top.egon.cola.platform.idp.admin.oauth.service.impl.PrivateKeyJwtAuthenticator;
import top.egon.cola.platform.idp.admin.token.service.impl.Rs256TokenService;
import top.egon.cola.platform.idp.core.oauth.ClientAssertionAuthentication;
import top.egon.cola.platform.idp.core.oauth.OAuthClient;
import top.egon.cola.platform.idp.core.oauth.OAuthException;
import top.egon.cola.platform.idp.core.port.ClientCredentialStore;
import top.egon.cola.platform.idp.core.port.OAuthClientStore;
import top.egon.cola.platform.idp.core.port.ResourceServerStore;
import top.egon.cola.platform.idp.core.resource.AdmissionRequest;
import top.egon.cola.platform.idp.core.resource.AdmissionTicketClaims;
import top.egon.cola.platform.idp.core.resource.ClientJwkCredential;
import top.egon.cola.platform.idp.core.resource.ResourceAuthorizationException;
import top.egon.cola.platform.idp.core.resource.ResourceServer;
import top.egon.cola.platform.idp.core.resource.ResourceServerAdmissionPolicy;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 编排 Resource Server {@code private_key_jwt} 认证、准入策略和独立 Ticket 签发。
 *
 * <p>Orchestrates Resource Server {@code private_key_jwt} authentication, admission policy, and
 * independent Ticket issuance.</p>
 *
 * <p>业务规则集中在 Specification 风格的 {@link ResourceServerAdmissionPolicy}；本服务只负责
 * 按固定顺序连接认证、查询和签名端口，不引入额外认证 Strategy/Factory 层。</p>
 *
 * <p>Business rules remain centralized in the Specification-style
 * {@link ResourceServerAdmissionPolicy}. This service only connects authentication, lookup, and
 * signing ports in a fixed order, without an additional authentication Strategy or Factory
 * layer.</p>
 */
public final class ResourceServerAdmissionServiceImpl {

    /** Admission RPC 专用 Client Assertion 认证器；RPC-specific assertion authenticator. */
    private final PrivateKeyJwtAuthenticator authenticator;

    /** OAuth Client 查询端口；OAuth Client lookup port. */
    private final OAuthClientStore clients;

    /** Client JWK 查询端口；Client JWK lookup port. */
    private final ClientCredentialStore credentials;

    /** Resource Server 查询端口；Resource Server lookup port. */
    private final ResourceServerStore resources;

    /** Resource Server 准入领域策略；Resource Server admission domain policy. */
    private final ResourceServerAdmissionPolicy policy;

    /** 独立 Admission JWT 签名器；independent Admission JWT signer. */
    private final Rs256TokenService signer;

    /** UTC 业务时钟；UTC business clock. */
    private final Clock clock;

    /** Admission JWT ID 提供器；Admission JWT identifier supplier. */
    private final Supplier<String> tokenIds;

    /**
     * 创建 Resource Server 准入签发服务。
     *
     * <p>Creates the Resource Server admission issuance service.</p>
     *
     * @param authenticator Admission RPC 专用认证器；Admission RPC authenticator
     * @param clients OAuth Client 查询端口；OAuth Client lookup port
     * @param credentials Client JWK 查询端口；Client JWK lookup port
     * @param resources Resource Server 查询端口；Resource Server lookup port
     * @param policy 准入领域策略；admission domain policy
     * @param signer RS256 Ticket 签名器；RS256 Ticket signer
     * @param clock UTC 业务时钟；UTC business clock
     * @param tokenIds JWT ID 提供器；JWT identifier supplier
     */
    public ResourceServerAdmissionServiceImpl(
            PrivateKeyJwtAuthenticator authenticator,
            OAuthClientStore clients,
            ClientCredentialStore credentials,
            ResourceServerStore resources,
            ResourceServerAdmissionPolicy policy,
            Rs256TokenService signer,
            Clock clock,
            Supplier<String> tokenIds
    ) {
        this.authenticator = Objects.requireNonNull(
                authenticator,
                "authenticator"
        );
        this.clients = Objects.requireNonNull(clients, "clients");
        this.credentials = Objects.requireNonNull(
                credentials,
                "credentials"
        );
        this.resources = Objects.requireNonNull(resources, "resources");
        this.policy = Objects.requireNonNull(policy, "policy");
        this.signer = Objects.requireNonNull(signer, "signer");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.tokenIds = Objects.requireNonNull(tokenIds, "tokenIds");
    }

    /**
     * 验证端点绑定 Assertion 并签发精确实例 Admission Ticket。
     *
     * <p>Authenticates an endpoint-bound assertion and issues an exact-instance Admission
     * Ticket.</p>
     *
     * @param assertionType RFC 7523 Assertion 类型；RFC 7523 assertion type
     * @param clientId Management Client 标识；Management Client identifier
     * @param assertion 紧凑 Client Assertion；compact Client Assertion
     * @param request 精确 Resource 实例声明；exact Resource instance declaration
     * @return Ticket 密文和本地续签调度时间；Ticket credential and local renewal schedule
     */
    public IssuedAdmissionTicket issue(
            String assertionType,
            String clientId,
            String assertion,
            AdmissionRequest request
    ) {
        Objects.requireNonNull(request, "request");
        ClientAssertionAuthentication authentication;
        try {
            authentication = authenticator.authenticate(
                    assertionType,
                    clientId,
                    assertion
            );
        } catch (OAuthException exception) {
            if ("IDP_CLIENT_ASSERTION_AUDIENCE_INVALID".equals(
                    exception.internalCode())
                    || "IDP_CLIENT_ASSERTION_REPLAYED".equals(
                    exception.internalCode())) {
                throw exception;
            }
            throw new OAuthException(
                    exception.oauthError(),
                    "invalid_client",
                    "IDP_RESOURCE_SERVER_CREDENTIAL_INVALID"
            );
        }
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        ResourceServer resource = resources.findById(
                        request.resourceServerId())
                .orElseThrow(() -> denied(
                        "IDP_RESOURCE_SERVER_NOT_FOUND",
                        "Resource Server was not found"
                ));
        OAuthClient client = clients.findById(authentication.clientId())
                .orElseThrow(() -> denied(
                        "IDP_RESOURCE_SERVER_CLIENT_INVALID",
                        "Management Client is invalid"
                ));
        ClientJwkCredential credential = credentials
                .findByClientIdAndKeyId(
                        authentication.clientId(),
                        authentication.credentialId()
                )
                .orElseThrow(() -> denied(
                        "IDP_RESOURCE_SERVER_CREDENTIAL_INVALID",
                        "Resource Server credential is invalid"
                ));
        ResourceServerAdmissionPolicy.AdmissionAuthorization authorized =
                policy.authorize(
                        resource,
                        client,
                        credential,
                        request,
                        now
                );
        Instant expiresAt = now.plus(resource.admissionTicketTtl());
        AdmissionTicketClaims claims = new AdmissionTicketClaims(
                authorized.resourceServerId(),
                authorized.resourceUri(),
                authorized.resourceVersion(),
                authorized.bizCode(),
                authorized.appCode(),
                authorized.environment(),
                authorized.instanceId(),
                authorized.credentialId(),
                required(tokenIds.get(), "tokenId"),
                now,
                now,
                expiresAt
        );
        return new IssuedAdmissionTicket(
                signer.signAdmission(claims),
                expiresAt
        );
    }

    /**
     * 创建稳定且不泄露内部状态的准入错误。
     *
     * <p>Creates a stable admission error without exposing internal state.</p>
     *
     * @param code 稳定错误码；stable error code
     * @param message 安全错误描述；safe error description
     * @return Resource 授权异常；Resource authorization exception
     */
    private static ResourceAuthorizationException denied(
            String code,
            String message
    ) {
        return new ResourceAuthorizationException(code, message);
    }

    /**
     * 校验内部生成的必填文本。
     *
     * <p>Validates internally generated required text.</p>
     *
     * @param value 待校验值；value to validate
     * @param field 字段名；field name
     * @return 已校验文本；validated text
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalStateException(field + " is invalid");
        }
        return value;
    }

    /**
     * Admission RPC 返回的最小票据结果。
     *
     * <p>Minimal Ticket result returned by the Admission RPC.</p>
     *
     * @param ticket 紧凑 Admission JWT；compact Admission JWT
     * @param expiresAt 票据过期时间；ticket expiration instant
     */
    public record IssuedAdmissionTicket(
            String ticket,
            Instant expiresAt
    ) {

        /**
         * 校验最小签发结果。
         *
         * <p>Validates the minimal issuance result.</p>
         */
        public IssuedAdmissionTicket {
            ticket = required(ticket, "ticket");
            expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        }

        /**
         * 返回不包含 JWT 密文的安全诊断文本。
         *
         * <p>Returns safe diagnostic text excluding the JWT credential.</p>
         *
         * @return 脱敏结果；redacted result
         */
        @Override
        public String toString() {
            return "IssuedAdmissionTicket["
                    + "ticket=<redacted>, expiresAt=" + expiresAt + ']';
        }
    }
}
