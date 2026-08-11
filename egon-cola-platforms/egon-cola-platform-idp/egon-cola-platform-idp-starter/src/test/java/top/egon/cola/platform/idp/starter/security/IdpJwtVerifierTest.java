package top.egon.cola.platform.idp.starter.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.idp.contract.IdentityUserState;
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.idp.core.oauth.OAuthClient;
import top.egon.cola.platform.idp.core.resource.ResourceServerStatus;
import top.egon.cola.platform.idp.starter.state.IdentityOAuthClientStateReader;
import top.egon.cola.platform.idp.starter.state.IdentityResourceServerState;
import top.egon.cola.platform.idp.starter.state.IdentityResourceServerStateReader;
import top.egon.cola.platform.idp.starter.state.IdentityUserStateReader;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdpJwtVerifierTest {

    private static final Instant NOW = Instant.parse("2026-08-02T08:00:00Z");
    private static final String RESOURCE_ID = "resource-rbac3-prod";
    private static final URI RESOURCE_URI = URI.create(
            "https://api.example/prod/permission/rbac3"
    );

    @Test
    void verifiesUserClaimsAndCurrentResourceAndUserState() {
        Object principal = verifier(userJwt(7L, 12L)).verify("access-token");

        assertThat(principal).isInstanceOf(IdentityPrincipal.class);
        IdentityPrincipal user = (IdentityPrincipal) principal;
        assertThat(user.subject()).isEqualTo("identity-1");
        assertThat(user.tenantId()).isEqualTo("tenant-1");
        assertThat(user.sessionId()).isEqualTo("session-1");
        assertThat(user.clientId()).isEqualTo("gateway-admin");
        assertThat(user.audience()).containsExactly(RESOURCE_URI.toString());
    }

    @Test
    void verifiesServiceClaimsAndCurrentConfidentialClientState() {
        Object principal = verifier(serviceJwt()).verify("service-token");

        assertThat(principal).isInstanceOf(ServiceIdentityPrincipal.class);
        ServiceIdentityPrincipal service = (ServiceIdentityPrincipal) principal;
        assertThat(service.subject()).isEqualTo("rbac3-service");
        assertThat(service.resourceUri()).isEqualTo(RESOURCE_URI);
        assertThat(service.resourceVersion()).isEqualTo(12L);
        assertThat(service.scopes()).containsExactly(
                "service:authorization:snapshot"
        );
        assertThat(service.sourceBizCode()).isEqualTo("permission");
        assertThat(service.sourceAppCode()).isEqualTo("idp");
        assertThat(service.sourceEnvironment()).isEqualTo("prod");
        assertThat(service.credentialId()).isEqualTo("service-key-1");
    }

    @Test
    void rejectsNonAccessJwtAndAdmissionTicketTypes() {
        Jwt wrongType = copy(userJwt(7L, 12L), builder -> builder
                .headers(headers -> headers.put("typ", "JWT")));
        Jwt admission = copy(userJwt(7L, 12L), builder -> builder
                .headers(headers -> headers.put("typ", "rs-admission+jwt"))
                .claim("token_use", "resource_server_admission"));

        assertInvalid(wrongType, "JWT_TYPE_INVALID");
        assertInvalid(admission, "JWT_TYPE_INVALID");
    }

    @Test
    void rejectsAudienceThatIsNotExactlyTheConfiguredResourceUri() {
        Jwt other = copy(userJwt(7L, 12L), builder -> builder
                .audience(List.of("https://api.example/other")));
        Jwt multiple = copy(userJwt(7L, 12L), builder -> builder
                .audience(List.of(RESOURCE_URI.toString(),
                        "https://api.example/other")));

        assertInvalid(other, "JWT_AUDIENCE_INVALID");
        assertInvalid(multiple, "JWT_AUDIENCE_INVALID");
    }

    @Test
    void rejectsMissingDisabledMismatchedOrStaleResourceProjection() {
        IdentityResourceServerState disabled = resourceState(
                ResourceServerStatus.DISABLED, RESOURCE_URI, 12L
        );
        IdentityResourceServerState wrongUri = resourceState(
                ResourceServerStatus.ACTIVE,
                URI.create("https://api.example/other"),
                12L
        );
        IdentityResourceServerState newer = resourceState(
                ResourceServerStatus.ACTIVE, RESOURCE_URI, 13L
        );

        assertThatThrownBy(() -> verifier(
                userJwt(7L, 12L), subject -> Optional.of(userState()),
                id -> Optional.empty(), clientReader()
        ).verify("access-token")).hasMessage("RESOURCE_STATE_MISSING");
        assertResourceInvalid(disabled, "RESOURCE_NOT_ACTIVE");
        assertResourceInvalid(wrongUri, "RESOURCE_URI_MISMATCH");
        assertResourceInvalid(newer, "RESOURCE_VERSION_STALE");
    }

    @Test
    void rejectsMalformedOrUnavailableResourceProjectionFailClosed() {
        IdentityResourceServerStateReader unavailable = id -> {
            throw new IllegalStateException("redis unavailable");
        };

        assertThatThrownBy(() -> verifier(
                userJwt(7L, 12L), subject -> Optional.of(userState()),
                unavailable, clientReader()
        ).verify("access-token"))
                .isInstanceOf(IdpJwtVerifier.InvalidTokenException.class)
                .hasMessage("RESOURCE_STATE_UNAVAILABLE");
    }

    @Test
    void rejectsStaleOrDisabledUserState() {
        IdentityUserState disabled = new IdentityUserState(
                "identity-1", IdentityUserState.Status.DISABLED, 7L, NOW
        );
        IdentityUserState stale = new IdentityUserState(
                "identity-1", IdentityUserState.Status.ACTIVE, 8L, NOW
        );

        assertUserInvalid(disabled, "IDENTITY_NOT_ACTIVE");
        assertUserInvalid(stale, "IDENTITY_TOKEN_VERSION_STALE");
    }

    @Test
    void rejectsUnavailableUserStateFailClosed() {
        IdentityUserStateReader unavailable = subject -> {
            throw new IllegalStateException("redis unavailable");
        };

        assertThatThrownBy(() -> verifier(
                userJwt(7L, 12L), unavailable, resourceReader(),
                clientReader()
        ).verify("access-token"))
                .isInstanceOf(IdpJwtVerifier.InvalidTokenException.class)
                .hasMessage("IDENTITY_STATE_UNAVAILABLE");
    }

    @Test
    void rejectsMissingDisabledPublicOrUnavailableServiceClient() {
        var activePublic = clientState(
                OAuthClient.ClientType.PUBLIC, OAuthClient.Status.ACTIVE
        );
        var disabled = clientState(
                OAuthClient.ClientType.CONFIDENTIAL,
                OAuthClient.Status.DISABLED
        );
        IdentityOAuthClientStateReader unavailable = clientId -> {
            throw new IllegalStateException("redis unavailable");
        };

        assertServiceClientInvalid(clientId -> Optional.empty(),
                "OAUTH_CLIENT_STATE_MISSING");
        assertServiceClientInvalid(clientId -> Optional.of(disabled),
                "OAUTH_CLIENT_NOT_ACTIVE");
        assertServiceClientInvalid(clientId -> Optional.of(activePublic),
                "OAUTH_CLIENT_TYPE_INVALID");
        assertServiceClientInvalid(unavailable,
                "OAUTH_CLIENT_STATE_UNAVAILABLE");
    }

    @Test
    void rejectsUnknownPrincipalTypeAndMissingRequiredTimes() {
        Jwt unknown = copy(userJwt(7L, 12L), builder -> builder
                .claim("principal_type", "ROBOT"));
        Jwt missingNotBefore = Jwt.withTokenValue("access-token")
                .headers(headers -> headers.putAll(
                        userJwt(7L, 12L).getHeaders()))
                .claims(claims -> claims.putAll(
                        userJwt(7L, 12L).getClaims()))
                .claim("nbf", null)
                .build();

        assertInvalid(unknown, "JWT_PRINCIPAL_TYPE_INVALID");
        assertInvalid(missingNotBefore, "JWT_CLAIM_INVALID_NBF");
    }

    private void assertResourceInvalid(
            IdentityResourceServerState state,
            String reason
    ) {
        assertThatThrownBy(() -> verifier(
                userJwt(7L, 12L), subject -> Optional.of(userState()),
                id -> Optional.of(state), clientReader()
        ).verify("access-token")).hasMessage(reason);
    }

    private void assertUserInvalid(IdentityUserState state, String reason) {
        assertThatThrownBy(() -> verifier(
                userJwt(7L, 12L), subject -> Optional.of(state),
                resourceReader(), clientReader()
        ).verify("access-token")).hasMessage(reason);
    }

    private void assertServiceClientInvalid(
            IdentityOAuthClientStateReader reader,
            String reason
    ) {
        assertThatThrownBy(() -> verifier(
                serviceJwt(), subject -> Optional.of(userState()),
                resourceReader(), reader
        ).verify("service-token")).hasMessage(reason);
    }

    private void assertInvalid(Jwt jwt, String reason) {
        assertThatThrownBy(() -> verifier(jwt).verify("access-token"))
                .isInstanceOf(IdpJwtVerifier.InvalidTokenException.class)
                .hasMessage(reason);
    }

    private IdpJwtVerifier verifier(Jwt jwt) {
        return verifier(
                jwt,
                subject -> Optional.of(userState()),
                resourceReader(),
                clientReader()
        );
    }

    private IdpJwtVerifier verifier(
            Jwt jwt,
            IdentityUserStateReader users,
            IdentityResourceServerStateReader resources,
            IdentityOAuthClientStateReader clients
    ) {
        JwtDecoder decoder = token -> jwt;
        return new IdpJwtVerifier(
                decoder,
                users,
                resources,
                clients,
                RESOURCE_ID,
                RESOURCE_URI
        );
    }

    private IdentityResourceServerStateReader resourceReader() {
        return id -> Optional.of(resourceState(
                ResourceServerStatus.ACTIVE, RESOURCE_URI, 12L
        ));
    }

    private IdentityOAuthClientStateReader clientReader() {
        return id -> Optional.of(clientState(
                OAuthClient.ClientType.CONFIDENTIAL,
                OAuthClient.Status.ACTIVE
        ));
    }

    private IdentityResourceServerState resourceState(
            ResourceServerStatus status,
            URI resourceUri,
            long version
    ) {
        return new IdentityResourceServerState(
                RESOURCE_ID,
                resourceUri,
                "permission",
                "rbac3",
                "prod",
                status,
                version
        );
    }

    private IdentityOAuthClientStateReader.IdentityOAuthClientState
            clientState(
                    OAuthClient.ClientType type,
                    OAuthClient.Status status) {
        return new IdentityOAuthClientStateReader.IdentityOAuthClientState(
                "rbac3-service",
                type,
                status,
                "resource-idp-prod",
                3L
        );
    }

    private IdentityUserState userState() {
        return new IdentityUserState(
                "identity-1", IdentityUserState.Status.ACTIVE, 7L, NOW
        );
    }

    private Jwt userJwt(long tokenVersion, long resourceVersion) {
        return Jwt.withTokenValue("access-token")
                .header("alg", "RS256")
                .header("kid", "key-1")
                .header("typ", "at+jwt")
                .issuer("https://idp.local")
                .subject("identity-1")
                .audience(List.of(RESOURCE_URI.toString()))
                .issuedAt(NOW)
                .notBefore(NOW)
                .expiresAt(NOW.plusSeconds(300))
                .claim("principal_type", "USER")
                .claim("tid", "tenant-1")
                .claim("sid", "session-1")
                .claim("client_id", "gateway-admin")
                .claim("jti", "token-1")
                .claim("token_version", tokenVersion)
                .claim("resource_version", resourceVersion)
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
                .claim("scope", List.of(
                        "service:authorization:snapshot"))
                .claim("source_biz", "permission")
                .claim("source_app", "idp")
                .claim("source_env", "prod")
                .claim("credential_id", "service-key-1")
                .claim("resource_version", 12L)
                .claim("jti", "service-token-1")
                .build();
    }

    private Jwt copy(Jwt source, java.util.function.Consumer<
            Jwt.Builder> customizer) {
        Jwt.Builder builder = Jwt.withTokenValue(source.getTokenValue())
                .headers(headers -> headers.putAll(source.getHeaders()))
                .claims(claims -> claims.putAll(source.getClaims()));
        customizer.accept(builder);
        return builder.build();
    }
}
