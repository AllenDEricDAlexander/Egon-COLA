package top.egon.cola.platform.idp.gateway.security;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import top.egon.cola.component.gateway.core.context.GatewayPrincipal;
import top.egon.cola.component.gateway.core.security.AuthenticationDecision;
import top.egon.cola.component.gateway.core.security.AuthenticationFailure;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.component.gateway.core.security.GatewayAuthenticationProvider;
import top.egon.cola.component.gateway.core.security.GatewayCredential;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.idp.contract.IdpPrincipal;
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 在 Gateway 安全链中执行仅身份级的 IdP 认证。
 * JWT 解码和 Redis 状态读取可能阻塞，因此验证工作调度到有界弹性线程池；
 * 任意验证异常都收敛为拒绝结果，不向下游泄露内部失败细节。
 *
 * <p>Performs identity-only IdP authentication in the Gateway security chain. JWT decoding and
 * Redis state access may block, so verification runs on the bounded-elastic scheduler. Any
 * verification exception is converted into a denial without exposing internal failure details
 * downstream.</p>
 */
public final class IdpIdentityAuthenticationProvider
        implements GatewayAuthenticationProvider {

    /**
     * Gateway 策略引用本认证提供者时使用的稳定标识。
     *
     * <p>Stable identifier used by Gateway policy to select this authentication provider.</p>
     */
    public static final String PROVIDER_ID = "idp-jwt";

    /**
     * 访问令牌验证端口。
     *
     * <p>Access-token verification port.</p>
     */
    private final TokenVerifier verifier;

    /**
     * 创建 IdP 身份认证提供者。
     *
     * <p>Creates the IdP identity authentication provider.</p>
     *
     * @param verifier 访问令牌验证端口；access-token verification port
     */
    public IdpIdentityAuthenticationProvider(TokenVerifier verifier) {
        this.verifier = Objects.requireNonNull(verifier, "verifier");
    }

    /**
     * 返回认证提供者稳定标识。
     *
     * <p>Returns the stable authentication-provider identifier.</p>
     *
     * @return {@value #PROVIDER_ID}
     */
    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    /**
     * 返回本提供者支持的凭据类型。
     *
     * <p>Returns the credential types supported by this provider.</p>
     *
     * @return 仅包含 {@code bearer} 的集合；a set containing only {@code bearer}
     */
    @Override
    public Set<String> supportedCredentialTypes() {
        return Set.of("bearer");
    }

    /**
     * 异步验证 Bearer 凭据并生成 Gateway 认证决策。
     *
     * <p>Asynchronously validates a Bearer credential and produces a Gateway authentication
     * decision.</p>
     *
     * @param context 当前 Gateway 认证上下文；current Gateway authentication context
     * @param credential 待验证凭据；credential to verify
     * @return 异步允许或拒绝决策；asynchronous allow-or-deny decision
     */
    @Override
    public Publisher<AuthenticationDecision> authenticate(
            GatewayAuthContext context,
            GatewayCredential credential
    ) {
        if (!"bearer".equalsIgnoreCase(credential.type())) {
            return Mono.just(AuthenticationDecision.deny(
                    "IDP_CREDENTIAL_TYPE_INVALID"));
        }
        return Mono.fromCallable(() -> decision(verifier.verify(
                        context, credential.tokenReference())))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorReturn(AuthenticationDecision.error(
                        "IDP_AUTHENTICATION_FAILED"));
    }

    /**
     * 把统一身份主体转换为已认证的 Gateway 主体。
     * 属性保存令牌、时间和 USER Audience 审计信息；SERVICE 主体额外保存客户端与资源属性。
     *
     * <p>Converts the unified identity principal into an authenticated Gateway principal. Its
     * attributes retain token, timestamp, and the USER audience for protocol/application
     * isolation; SERVICE principals additionally retain client and resource attributes for
     * trusted machine-identity mapping and auditing.</p>
     *
     * @param principal 已验证的统一身份主体；validated unified identity principal
     * @return 允许访问认证链后续阶段的决策；decision allowing the next authentication stage
     */
    private AuthenticationDecision decision(IdpGatewayJwtVerifier.Verification verification) {
        if (verification.failure() == AuthenticationFailure.EXPIRED) {
            return AuthenticationDecision.expired(verification.reason());
        }
        if (verification.failure() != AuthenticationFailure.NONE) {
            return AuthenticationDecision.deny(verification.reason());
        }
        IdpPrincipal principal = verification.principal();
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("idp.token-id", principal.tokenId());
        attributes.put("idp.issued-at", principal.issuedAt().toString());
        attributes.put("idp.expires-at", principal.expiresAt().toString());
        if (principal instanceof IdentityPrincipal user) {
            attributes.put("idp.audience", user.audience().stream()
                    .sorted()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "USER audience is required")));
            attributes.put("idp-auth-acr", user.authenticationContext().acr());
            attributes.put("idp-auth-time", user.authenticationContext().authTime().toString());
        } else if (principal instanceof ServiceIdentityPrincipal service) {
            attributes.put("idp.client-id", service.clientId());
            attributes.put("idp.resource-uri", service.resourceUri().toString());
            attributes.put("idp.resource-version", Long.toString(service.resourceVersion()));
            attributes.put("idp.source-biz", service.sourceBizCode());
            attributes.put("idp.source-app", service.sourceAppCode());
            attributes.put("idp.source-env", service.sourceEnvironment());
            attributes.put("idp.service-scopes", String.join(
                    " ", new java.util.TreeSet<>(service.scopes())));
            attributes.put("idp.credential-id", service.credentialId());
        }
        return AuthenticationDecision.allow(new GatewayPrincipal(
                principal.subject(),
                principal.principalType().name(),
                principal.tenantId(),
                null,
                true,
                attributes));
    }

    /**
     * 隔离 Gateway 认证编排与具体访问令牌验证实现的函数式端口。
     *
     * <p>Functional port separating Gateway authentication orchestration from the concrete
     * access-token verifier.</p>
     */
    @FunctionalInterface
    public interface TokenVerifier {

        /**
         * 验证访问令牌并返回统一身份主体。
         *
         * <p>Validates an access token and returns the unified identity principal.</p>
         *
         * @param context 可信 Gateway 路由上下文；trusted Gateway route context
         * @param token 原始 Bearer 访问令牌；raw Bearer access token
         * @return 已验证的统一身份主体；validated unified identity principal
         */
        IdpGatewayJwtVerifier.Verification verify(
                GatewayAuthContext context,
                String token);
    }
}
