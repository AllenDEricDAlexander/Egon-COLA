package top.egon.cola.platform.idp.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.core.context.GatewayPrincipal;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.platform.idp.contract.IdentityUserState;
import top.egon.cola.platform.idp.core.oauth.OAuthClient;
import top.egon.cola.platform.idp.core.resource.ResourceServerStatus;
import top.egon.cola.platform.idp.starter.state.IdentityResourceServerState;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验收 Gateway 可信路由 Resource 与普通后端共享的精确 Token 绑定。
 * Accepts exact token binding shared by trusted Gateway routes and regular backends.
 */
class IdpGatewayResourceBindingTest {

    private static final Instant NOW = Instant.parse("2026-08-10T00:00:00Z");
    private static final URI IDP_URI = URI.create(
            "https://api.egon.internal/prod/permission/idp");
    private static final URI RBAC3_URI = URI.create(
            "https://api.egon.internal/prod/permission/rbac3");

    @Test
    void tokenForAnotherApplicationCannotBeForwardedThroughTheRoute() {
        IdentityResourceServerState idp = new IdentityResourceServerState(
                "permission-idp-prod", IDP_URI, "permission", "idp", "prod",
                ResourceServerStatus.ACTIVE, 7L);
        GatewayResourceServerResolver resolver =
                new GatewayResourceServerResolver(
                        key -> key.startsWith("scope:")
                                ? "permission-idp-prod" : null,
                        id -> Optional.of(idp), "scope:", "uri:");
        IdpGatewayJwtVerifier verifier = new IdpGatewayJwtVerifier(
                token -> userJwt(RBAC3_URI),
                subject -> Optional.of(new IdentityUserState(
                        subject, IdentityUserState.Status.ACTIVE, 3L, NOW)),
                id -> Optional.of(idp),
                client -> Optional.of(
                        new top.egon.cola.platform.idp.starter.state
                                .IdentityOAuthClientStateReader.IdentityOAuthClientState(
                                client, OAuthClient.ClientType.CONFIDENTIAL,
                                OAuthClient.Status.ACTIVE,
                                "permission-idp-prod", 1L)),
                resolver);

        assertThatThrownBy(() -> verifier.verify(context(), "token-for-rbac3"))
                .isInstanceOf(IdpGatewayJwtVerifier.TokenVerificationException.class)
                .hasMessage("IDP_RESOURCE_AUDIENCE_MISMATCH");
    }

    @Test
    void routeTripleCannotResolveASiblingApplicationResource() {
        IdentityResourceServerState rbac3 = new IdentityResourceServerState(
                "permission-rbac3-prod", RBAC3_URI, "permission", "rbac3",
                "prod", ResourceServerStatus.ACTIVE, 4L);
        GatewayResourceServerResolver resolver =
                new GatewayResourceServerResolver(
                        key -> "permission-rbac3-prod",
                        id -> Optional.of(rbac3), "scope:", "uri:");

        assertThatThrownBy(() -> resolver.resolve(context().attributes()))
                .isInstanceOf(GatewayResourceServerResolver
                        .ResourceResolutionException.class)
                .hasMessage("IDP_RESOURCE_ROUTE_MISMATCH");
    }

    private static GatewayAuthContext context() {
        return new GatewayAuthContext(
                AccessZone.PUBLIC, GatewayProtocol.HTTP, "operation-1", "route-1",
                "policy-1", "/api", "GET", Set.of("bearer"),
                GatewayPrincipal.anonymous(), "127.0.0.1", "trace-1", "request-1",
                NOW.plusSeconds(5), "release-1", Map.of(
                        "idp.biz-code", "permission",
                        "idp.app-code", "idp",
                        "idp.env", "prod"));
    }

    private static Jwt userJwt(URI resourceUri) {
        return Jwt.withTokenValue("access-token")
                .header("alg", "RS256")
                .header("kid", "idp-key-1")
                .header("typ", "at+jwt")
                .issuer("https://idp.example")
                .subject("alice")
                .audience(List.of(resourceUri.toString()))
                .issuedAt(NOW)
                .notBefore(NOW)
                .expiresAt(NOW.plusSeconds(300))
                .claim("principal_type", "USER")
                .claim("tid", "tenant-1")
                .claim("sid", "session-1")
                .claim("client_id", "idp-admin-web")
                .claim("jti", "token-1")
                .claim("token_version", 3L)
                .claim("resource_version", 7L)
                .build();
    }
}
