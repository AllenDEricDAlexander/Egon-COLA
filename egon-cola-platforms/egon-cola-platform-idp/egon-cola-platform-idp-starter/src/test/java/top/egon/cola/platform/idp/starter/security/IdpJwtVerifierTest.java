package top.egon.cola.platform.idp.starter.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.idp.core.oauth.OAuthClient;
import top.egon.cola.platform.idp.core.resource.ResourceServerStatus;
import top.egon.cola.platform.idp.starter.state.IdentityOAuthClientStateReader;
import top.egon.cola.platform.idp.starter.state.IdentityResourceServerState;
import top.egon.cola.platform.idp.starter.state.IdentityResourceServerStateReader;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class IdpJwtVerifierTest {

    private static final Instant NOW = Instant.parse("2026-08-02T08:00:00Z");
    private static final String RESOURCE_ID = "resource-rbac3-prod";
    private static final URI RESOURCE_URI = URI.create(
            "https://api.example/prod/permission/rbac3");
    private static final String PLATFORM_AUDIENCE = "egon-platform";

    @Test
    void verifiesUserClaimsWithoutRedisUserState() {
        var result = verifier(userJwt()).verifyUser("access-token");

        assertThat(result).isInstanceOf(AccessTokenVerification.Valid.class);
        IdentityPrincipal user = ((AccessTokenVerification.Valid<IdentityPrincipal>) result)
                .principal();
        assertThat(user.subject()).isEqualTo("identity-1");
        assertThat(user.tenantId()).isEqualTo("tenant-1");
        assertThat(user.tokenId()).isEqualTo("token-1");
        assertThat(user.audience()).containsExactly(PLATFORM_AUDIENCE);
    }

    @Test
    void rejectsUserTokenThatCarriesSessionOrPermissionState() {
        Jwt session = copy(userJwt(), builder -> builder.claim("sid", "session-1"));
        Jwt permission = copy(userJwt(), builder -> builder
                .claim("roles", List.of("admin")));

        assertInvalid(verifier(session).verifyUser("access-token"),
                "JWT_FORBIDDEN_CLAIM_SID");
        assertInvalid(verifier(permission).verifyUser("access-token"),
                "JWT_FORBIDDEN_CLAIM_ROLES");
    }

    @Test
    void verifiesServiceClaimsAndCurrentConfidentialClientState() {
        var result = verifier(serviceJwt()).verifyService("service-token");

        assertThat(result).isInstanceOf(AccessTokenVerification.Valid.class);
        ServiceIdentityPrincipal service =
                ((AccessTokenVerification.Valid<ServiceIdentityPrincipal>) result).principal();
        assertThat(service.subject()).isEqualTo("rbac3-service");
        assertThat(service.resourceUri()).isEqualTo(RESOURCE_URI);
        assertThat(service.resourceVersion()).isEqualTo(12L);
        assertThat(service.scopes()).containsExactly("service:authorization:snapshot");
    }

    @Test
    void rejectsNonAccessTypeAndWrongUserAudience() {
        Jwt wrongType = copy(userJwt(), builder -> builder
                .headers(headers -> headers.put("typ", "JWT")));
        Jwt otherAudience = copy(userJwt(), builder -> builder
                .audience(List.of("https://api.example/other")));

        assertInvalid(verifier(wrongType).verifyUser("access-token"),
                "JWT_TYPE_INVALID");
        assertInvalid(verifier(otherAudience).verifyUser("access-token"),
                "JWT_AUDIENCE_INVALID");
    }

    @Test
    void serviceVerificationStillFailsClosedOnResourceOrClientState() {
        var disabledResource = new IdentityResourceServerStateReader() {
            @Override
            public Optional<IdentityResourceServerState> read(String id) {
                return Optional.of(resourceState(ResourceServerStatus.DISABLED,
                        RESOURCE_URI, 12L));
            }
        };
        var result = new IdpJwtVerifier(
                token -> serviceJwt(), disabledResource, clientReader(),
                RESOURCE_ID, RESOURCE_URI, PLATFORM_AUDIENCE, clock())
                .verifyService("service-token");
        assertInvalid(result, "RESOURCE_NOT_ACTIVE");
    }

    @Test
    void expiredAccessTokenHasExplicitExpiredOutcome() {
        Jwt expired = copy(userJwt(), builder -> builder
                .issuedAt(NOW.minusSeconds(600))
                .notBefore(NOW.minusSeconds(600))
                .expiresAt(NOW.minusSeconds(1)));
        assertThat(verifier(expired).verifyUser("access-token"))
                .isInstanceOf(AccessTokenVerification.Expired.class);
    }

    private void assertInvalid(AccessTokenVerification<?> result, String reason) {
        assertThat(result).isInstanceOf(AccessTokenVerification.Invalid.class);
        assertThat(((AccessTokenVerification.Invalid<?>) result).reasonCode())
                .isEqualTo(reason);
    }

    private IdpJwtVerifier verifier(Jwt jwt) {
        return new IdpJwtVerifier(token -> jwt, resourceReader(), clientReader(),
                RESOURCE_ID, RESOURCE_URI, PLATFORM_AUDIENCE, clock());
    }

    private Clock clock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    private IdentityResourceServerStateReader resourceReader() {
        return id -> Optional.of(resourceState(ResourceServerStatus.ACTIVE,
                RESOURCE_URI, 12L));
    }

    private IdentityOAuthClientStateReader clientReader() {
        return id -> Optional.of(new IdentityOAuthClientStateReader.IdentityOAuthClientState(
                "rbac3-service", OAuthClient.ClientType.CONFIDENTIAL,
                OAuthClient.Status.ACTIVE, "resource-idp-prod", 3L));
    }

    private IdentityResourceServerState resourceState(
            ResourceServerStatus status, URI resourceUri, long version) {
        return new IdentityResourceServerState(RESOURCE_ID, resourceUri,
                "permission", "rbac3", "prod", status, version);
    }

    private Jwt userJwt() {
        return Jwt.withTokenValue("access-token")
                .header("alg", "RS256")
                .header("kid", "key-1")
                .header("typ", "at+jwt")
                .issuer("https://idp.local")
                .subject("identity-1")
                .audience(List.of(PLATFORM_AUDIENCE))
                .issuedAt(NOW)
                .notBefore(NOW)
                .expiresAt(NOW.plusSeconds(300))
                .claim("principal_type", "USER")
                .claim("tid", "tenant-1")
                .claim("jti", "token-1")
                .claim("acr", "PASSWORD")
                .claim("auth_time", NOW)
                .build();
    }

    private Jwt serviceJwt() {
        return Jwt.withTokenValue("service-token")
                .header("alg", "RS256")
                .header("kid", "key-1")
                .header("typ", "at+jwt")
                .issuer("https://idp.local")
                .subject("rbac3-service")
                .audience(List.of(RESOURCE_URI.toString()))
                .issuedAt(NOW)
                .notBefore(NOW)
                .expiresAt(NOW.plusSeconds(300))
                .claim("principal_type", "SERVICE")
                .claim("client_id", "rbac3-service")
                .claim("tid", "tenant-1")
                .claim("scope", List.of("service:authorization:snapshot"))
                .claim("source_biz", "permission")
                .claim("source_app", "idp")
                .claim("source_env", "prod")
                .claim("credential_id", "service-key-1")
                .claim("resource_version", 12L)
                .claim("jti", "service-token-1")
                .build();
    }

    private Jwt copy(Jwt source, java.util.function.Consumer<Jwt.Builder> customizer) {
        Jwt.Builder builder = Jwt.withTokenValue(source.getTokenValue())
                .headers(headers -> headers.putAll(source.getHeaders()))
                .claims(claims -> claims.putAll(source.getClaims()));
        customizer.accept(builder);
        return builder.build();
    }
}
